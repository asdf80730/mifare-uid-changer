package li.power.app.mifareuidchanger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.Button;
import android.widget.EditText;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowDialog;
import org.robolectric.Shadows;

import java.lang.reflect.Field;

    import li.power.app.mifareuidchanger.model.UidItem;
import java.lang.reflect.Method;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class MainActivitySmokeTest {

    private Object getField(Object target, String name) throws Exception {
        Field f = MainActivity.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(target);
    }

    /** Truth source: the RecyclerView's own adapter, not the internal list field.
     *  A list that exists but is not bound to the view displays nothing. */
    private int displayedCount(MainActivity act) throws Exception {
        androidx.recyclerview.widget.RecyclerView rv =
                act.findViewById(R.id.rv_uid_list);
        androidx.recyclerview.widget.RecyclerView.Adapter<?> a = rv.getAdapter();
        if (a == null) {
            throw new AssertionError("RecyclerView must have the adapter bound in onCreate");
        }
        return a.getItemCount();
    }

    private void save(MainActivity act, String uid, String name) throws Exception {
        Method m = MainActivity.class.getDeclaredMethod("saveTagUid", String.class, String.class);
        m.setAccessible(true);
        m.invoke(act, uid, name);
    }

    private void setQuery(MainActivity act, String q) throws Exception {
        Field f = MainActivity.class.getDeclaredField("searchQuery");
        f.setAccessible(true);
        f.set(act, q);
    }

    /** Opens the prefilled dialog the way a user does: long-press -> EDIT button. */
    private AlertDialog openEditViaLongPress(MainActivity act, UidItem item) throws Exception {
        Method del = MainActivity.class.getDeclaredMethod("showDeleteTagUidDialog",
                UidItem.class, int.class);
        del.setAccessible(true);
        del.invoke(act, item, 0);
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
        ((AlertDialog) ShadowDialog.getLatestDialog())
                .getButton(DialogInterface.BUTTON_NEUTRAL).performClick();
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
        return (AlertDialog) ShadowDialog.getLatestDialog();
    }

    private MainActivity newActivity() {
        return Robolectric.buildActivity(MainActivity.class).create().start().get();
    }

    @Test
    public void addFirstCard_showsInList() throws Exception {
        MainActivity act = newActivity();
        assertEquals(0, displayedCount(act));
        save(act, "11223344", "A+1");
        assertEquals("added A+1 is displayed", 1, displayedCount(act));
    }

    @Test
    public void addPlainName_showsInList() throws Exception {
        MainActivity act = newActivity();
        save(act, "DEADBEEF", "plainname");
        assertEquals(1, displayedCount(act));
    }

    @Test
    public void addWhileFilterActive_autoClearsFilter() throws Exception {
        MainActivity act = newActivity();
        save(act, "11111111", "Home+Desk");
        setQuery(act, "Home+");
        Method rd = MainActivity.class.getDeclaredMethod("refreshDisplay");
        rd.setAccessible(true);
        rd.invoke(act);
        assertEquals(1, displayedCount(act));
        save(act, "22222222", "A+1");
        assertEquals("hidden new card must force filter clear", 2, displayedCount(act));
    }

    @Test
    public void duplicateUid_renamesExisting() throws Exception {
        MainActivity act = newActivity();
        save(act, "11223344", "first");
        save(act, "11223344", "second");
        assertEquals(1, displayedCount(act));
    }

    /**
     * Drives the REAL save path: showAddTagDialog -> fill UID + name EditTexts -> click SAVE button.
     * This is the path the user actually uses in the app.
     */
    @Test
    public void saveViaAddDialogClick_showsInList() throws Exception {
        MainActivity act = newActivity();

        Method showAdd = MainActivity.class.getDeclaredMethod("showAddTagDialog");
        showAdd.setAccessible(true);
        showAdd.invoke(act);
        // dialog window attaches asynchronously; let the main looper dispatch onShow 
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();

        AlertDialog dialog = (AlertDialog) ShadowDialog.getLatestDialog();
        assertTrue("add dialog must be showing", dialog != null && dialog.isShowing());

        EditText uidField = dialog.findViewById(R.id.edit_text_default_uid);
        EditText nameField = dialog.findViewById(R.id.edit_text_default_uid_name);
        uidField.setText("1122AABB");
        nameField.setText("A+1");
        Button save = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        System.out.println("save button null? " + (save == null) + " dialog==addTag? ");
        // is the dialog we grabbed actually the add dialog? check via ShadowDialog identity
        Object latest = ShadowDialog.getLatestDialog();
        System.out.println("latest dialog same as clicked? " + (latest == dialog));
        try {
            save.performClick();
        } catch (Throwable t) {
            System.out.println("PERFORMCLICK THREW: " + t);
            t.printStackTrace(System.out);
            throw t;
        }
        java.util.List<?> sl = (java.util.List<?>) getField(act, "displayList");
        Object st = getField(act, "settings");
        Object inner = st.getClass().getMethod("getList").invoke(st);
        System.out.println("display=" + sl.size() + " settings-list=" + ((java.util.List<?>) inner).size());
        for (org.robolectric.shadows.ShadowLog.LogItem item : org.robolectric.shadows.ShadowLog.getLogsForTag("SAVETEST")) {
            System.out.println("LOG: " + item.msg);
        }
        assertEquals("SAVE click must add via real click listener", 1, displayedCount(act));
    }

    /** Real click path with no name typed -> auto-generated Tag name, still added. */
    @Test
    public void saveViaAddDialogEmptyName_showsInList() throws Exception {
        MainActivity act = newActivity();

        Method showAdd = MainActivity.class.getDeclaredMethod("showAddTagDialog");
        showAdd.setAccessible(true);
        showAdd.invoke(act);
        // dialog window attaches asynchronously; let the main looper dispatch onShow
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();

        AlertDialog dialog = (AlertDialog) ShadowDialog.getLatestDialog();
        ((EditText) dialog.findViewById(R.id.edit_text_default_uid)).setText("CAFEBABE");
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();

        assertEquals("empty name still adds (auto Tag name)", 1, displayedCount(act));
    }

    /** Real click path with INVALID uid -> must NOT add, error shown, dialog stays. */
    @Test
    public void saveViaAddDialogInvalidUid_doesNotAdd() throws Exception {
        MainActivity act = newActivity();

        Method showAdd = MainActivity.class.getDeclaredMethod("showAddTagDialog");
        showAdd.setAccessible(true);
        showAdd.invoke(act);
        // dialog window attaches asynchronously; let the main looper dispatch onShow
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();

        AlertDialog dialog = (AlertDialog) ShadowDialog.getLatestDialog();
        ((EditText) dialog.findViewById(R.id.edit_text_default_uid)).setText("not-hex");
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();

        assertEquals("invalid uid must not add", 0, displayedCount(act));
        assertTrue("dialog must stay open on invalid uid", dialog.isShowing());
    }

    /** Invariant: no NFC hardware means the app must exit (setupNfc finish()).
     *  Also the tripwire for smoke-APK patches leaking into master. */
    @Test
    public void withoutNfc_activityMustFinish() {
        MainActivity act = Robolectric.buildActivity(MainActivity.class).create().start().resume().get();
        assertTrue("no NFC must terminate the app", act.isFinishing());
    }

    /** Fresh-install journey: welcome dialog -> OK -> FAB -> fill -> SAVE. Exactly the user's path. */
    @Test
    public void freshInstall_fabToSave_addsFirstCard() throws Exception {
        MainActivity act = newActivity(); // settings==null -> onCreate shows welcome dialog
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
        AlertDialog welcome = (AlertDialog) ShadowDialog.getLatestDialog();
        welcome.getButton(DialogInterface.BUTTON_POSITIVE).performClick();

        act.findViewById(R.id.fab).performClick();
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
        AlertDialog add = (AlertDialog) ShadowDialog.getLatestDialog();
        ((EditText) add.findViewById(R.id.edit_text_default_uid)).setText("1A2B3C4D");
        ((android.widget.AutoCompleteTextView) add.findViewById(R.id.edit_text_default_uid_name)).setText("Home+Door");
        add.getButton(DialogInterface.BUTTON_POSITIVE).performClick();

        assertEquals("fresh install must accept first card", 1, displayedCount(act));
    }



    /** Long-press dialog must offer EDIT (neutral slot) alongside YES/NO delete. */
    @Test
    public void longPress_offersEditOption() throws Exception {
        MainActivity act = newActivity();
        save(act, "11223344", "Keep");
        Object st = getField(act, "settings");
        java.util.List<?> items = (java.util.List<?>) st.getClass().getMethod("getList").invoke(st);
        Method del = MainActivity.class.getDeclaredMethod("showDeleteTagUidDialog",
                Class.forName("li.power.app.mifareuidchanger.model.UidItem"), int.class);
        del.setAccessible(true);
        del.invoke(act, items.get(0), 0);
        Shadows.shadowOf(android.os.Looper.getMainLooper()).idle();
        AlertDialog dialog = (AlertDialog) ShadowDialog.getLatestDialog();
        assertEquals("long-press must offer EDIT", "EDIT",
                dialog.getButton(DialogInterface.BUTTON_NEUTRAL).getText().toString());
    }

    /** Edit with SAME uid: upsert renames in place, no duplicate card. */
    @Test
    public void editSameUid_renamesInPlace() throws Exception {
        MainActivity act = newActivity();
        save(act, "11223344", "Old");
        Object st = getField(act, "settings");
        java.util.List<?> items = (java.util.List<?>) st.getClass().getMethod("getList").invoke(st);
        AlertDialog dialog = openEditViaLongPress(act, (UidItem) items.get(0));
        EditText uidField = dialog.findViewById(R.id.edit_text_default_uid);
        EditText nameField = dialog.findViewById(R.id.edit_text_default_uid_name);
        assertEquals("edit dialog prefills uid", "11223344", uidField.getText().toString());
        assertEquals("edit dialog prefills name", "Old", nameField.getText().toString());
        nameField.setText("Renamed");
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();

        assertEquals("same-uid edit must not duplicate", 1, displayedCount(act));
        assertEquals("renamed in place", "Renamed",
                ((UidItem) items.get(0)).getName());
    }

    /** Edit with CHANGED uid: acts as copy — new card appears, original untouched. */
    @Test
    public void editNewUid_duplicatesKeepingOriginal() throws Exception {
        MainActivity act = newActivity();
        save(act, "11223344", "Origin");
        Object st = getField(act, "settings");
        java.util.List<?> items = (java.util.List<?>) st.getClass().getMethod("getList").invoke(st);
        AlertDialog dialog = openEditViaLongPress(act, (UidItem) items.get(0));
        ((EditText) dialog.findViewById(R.id.edit_text_default_uid)).setText("DEADBEEF");
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick();

        assertEquals("uid-changed edit must create a second card", 2, displayedCount(act));
        assertEquals("original card untouched", "11223344",
                ((UidItem) items.get(0)).getId());
    }
}
