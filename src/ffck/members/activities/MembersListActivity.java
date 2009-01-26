/* 
 * Copyright (C) 2009 Vincent Behar
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ffck.members.activities;

import ffck.members.Member;
import ffck.members.MembersCsvImporter;
import ffck.members.R;

import org.openintents.intents.FileManagerIntents;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.text.util.Linkify;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.FilterQueryProvider;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

/**
 * FFCK Members list activity. Display a list of all members, and supports
 * filtering. Also provides a menu.
 */
public class MembersListActivity extends ListActivity implements OnSharedPreferenceChangeListener {

    /** The requestCode for the PICK_FILE intent activity result */
    private static final int REQUEST_CODE_PICK_FILE = 1;

    /** Default path for selecting the CSV file to import (the SD card) */
    private static final File IMPORT_CSV_DEFAULT_PATH = new File("/sdcard");

    /*
     * Dialogs identifiers
     */

    /** Identifier for the 'About' dialog */
    private static final int DIALOG_ABOUT = 1;

    /** Identifier for the 'Statistics' dialog */
    private static final int DIALOG_STATS = 2;

    /** Identifier for the 'PICK_FILE activity not found' dialog */
    private static final int DIALOG_PICK_FILE_ACTIVITY_NOT_FOUND = 3;

    /** Identifier for the 'Delete all members' dialog */
    private static final int DIALOG_DELETE_ALL_MEMBERS = 4;

    /** Identifier for the 'Progress bar while importing' dialog */
    private static final int DIALOG_PROGRESS_IMPORT = 5;

    /*
     * DB->View mapping
     */

    /** Projection used to specify the columns to retrieve from the database */
    private static final String[] PROJECTION = {
            Member.ID, Member.CODE, Member.LAST_NAME, Member.FIRST_NAME, Member.GENDER,
            Member.LAST_LICENSE
    };

    /** Source for the DB->View mapping : Columns names */
    private static final String[] FROM = {
            Member.GENDER, Member.LAST_NAME, Member.LAST_LICENSE
    };

    /** Destination for the DB->View mapping : View IDs */
    private static final int[] TO = {
            R.id.members_list_item_gender, R.id.members_list_item_names,
            R.id.members_list_item_last_license
    };

    /*
     * Instance-specific variables
     */

    /** The cursor listAdapter used for managing the DB->View binding */
    private SimpleCursorAdapter cursorAdapter;

    /** The index of the 'code' column (retrieved only once) */
    private int codeColumnIndex = -1;

    /** The handler used to inform the UI thread about background jobs status */
    private Handler handler = new Handler();

    /*
     * Activity lifecycle
     */

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.members_list);

        // initialize the cursor (that contains the data from DB)
        Cursor cursor = buildCursor(null, null);
        startManagingCursor(cursor);
        codeColumnIndex = cursor.getColumnIndexOrThrow(Member.CODE);
        cursorAdapter = new SimpleCursorAdapter(this, R.layout.members_list_item, cursor, FROM, TO);
        setListAdapter(cursorAdapter);

        // we need custom DB->View binding (see MembersViewBinder javadoc)
        cursorAdapter.setViewBinder(new MembersViewBinder());

        // enable filtering (see MembersFilterQueryProvider javadoc)
        getListView().setTextFilterEnabled(true);
        cursorAdapter.setFilterQueryProvider(new MembersFilterQueryProvider());

        // listen to preferences changes
        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // when a 'child' activity finished :
        switch (requestCode) {
            case REQUEST_CODE_PICK_FILE:
                if (resultCode == RESULT_OK && data != null) {
                    String selectedPath = data.getData().getPath();
                    importFile(selectedPath);
                }
                break;
            default:
                break;
        }
    }

    @Override
    protected void onListItemClick(ListView listView, View view, int position, long id) {
        Cursor cursor = (Cursor)getListView().getItemAtPosition(position);
        String code = cursor.getString(codeColumnIndex);

        // Display the selected member (using a specific content provider URI)
        Uri uri = Uri.withAppendedPath(Member.CONTENT_URI, code);
        startActivity(new Intent(Intent.ACTION_VIEW, uri));
    }

    /*
     * Menu Management
     */

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!super.onCreateOptionsMenu(menu)) {
            return false;
        }

        // create menu from XML
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.members_list_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (super.onOptionsItemSelected(item)) {
            return true;
        }

        // when a menu item is selected :
        switch (item.getItemId()) {
            case R.id.members_list_menu_preferences:
                startActivity(new Intent(this, MembersPreferenceActivity.class));
                return true;
            case R.id.members_list_menu_about:
                showDialog(DIALOG_ABOUT);
                return true;
            case R.id.members_list_menu_stats:
                showDialog(DIALOG_STATS);
                return true;
            case R.id.members_list_menu_delete_all:
                showDialog(DIALOG_DELETE_ALL_MEMBERS);
                return true;
            case R.id.members_list_menu_import:
                selectFileToImport();
                return true;
            default:
                return false;
        }
    }

    /*
     * Dialogs management
     */

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case DIALOG_ABOUT:
                AlertDialog.Builder about = new AlertDialog.Builder(this);
                about.setTitle(R.string.dialog_about_title);
                TextView aboutTextView = new TextView(this);
                aboutTextView.setText(R.string.dialog_about_text);
                aboutTextView.setAutoLinkMask(Linkify.WEB_URLS);
                aboutTextView.setLinksClickable(true);
                aboutTextView.setPadding(10, 10, 10, 10);
                Linkify.addLinks(aboutTextView, Linkify.WEB_URLS);
                about.setView(aboutTextView);
                about.setPositiveButton(R.string.dialog_about_button, null);
                return about.create();
            case DIALOG_STATS:
                AlertDialog.Builder stats = new AlertDialog.Builder(this);
                stats.setTitle(R.string.dialog_stats_title);
                stats.setMessage(getString(R.string.dialog_stats_text));
                stats.setPositiveButton(R.string.dialog_stats_button, null);
                return stats.create();
            case DIALOG_PICK_FILE_ACTIVITY_NOT_FOUND:
                AlertDialog.Builder pickFile = new AlertDialog.Builder(this);
                pickFile.setIcon(android.R.drawable.ic_dialog_alert);
                pickFile.setTitle(R.string.dialog_pick_file_not_found_title);
                pickFile.setMessage(R.string.dialog_pick_file_not_found_text);
                pickFile.setPositiveButton(R.string.dialog_pick_file_not_found_button, null);
                return pickFile.create();
            case DIALOG_DELETE_ALL_MEMBERS:
                AlertDialog.Builder dialog = new AlertDialog.Builder(this);
                dialog.setIcon(android.R.drawable.ic_dialog_alert);
                dialog.setTitle(R.string.dialog_delete_all_members_title);
                dialog.setMessage(R.string.dialog_delete_all_members_text);
                dialog.setPositiveButton(R.string.dialog_delete_all_members_button_ok,
                        new OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                deleteAllMembers();
                            }
                        });
                dialog.setNegativeButton(R.string.dialog_delete_all_members_button_nok, null);
                return dialog.create();
            case DIALOG_PROGRESS_IMPORT:
                ProgressDialog progressImport = new ProgressDialog(this);
                progressImport.setTitle(R.string.import_csv_progress_bar_title);
                progressImport.setMessage(getString(R.string.import_csv_progress_bar_text));
                progressImport.setIndeterminate(true);
                progressImport.setCancelable(false);
                return progressImport;
            default:
                return null;
        }
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        super.onPrepareDialog(id, dialog);

        switch (id) {
            case DIALOG_STATS:
                ((AlertDialog)dialog).setMessage(getString(R.string.dialog_stats_text,
                        cursorAdapter.getCount()));
                break;
            default:
                break;
        }
    }

    /*
     * Preferences management
     */

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        // names format or year filtering has changed -> we need a new cursor
        if (key.equals(getString(R.string.preferences_names_format_key))
                || key.equals(getString(R.string.preferences_last_license_key))) {
            stopManagingCursor(cursorAdapter.getCursor());
            Cursor newCursor = buildCursor(null, null);
            startManagingCursor(newCursor);
            cursorAdapter.changeCursor(newCursor);
        }
    }

    /*
     * CSV Import
     */

    /**
     * Starts a new activity for selecting the file to import. We use the
     * OpenIntents PICK_FILE intent to find a matching activity.
     */
    private void selectFileToImport() {
        Intent intent = new Intent(FileManagerIntents.ACTION_PICK_FILE);
        intent.setData(Uri.fromFile(IMPORT_CSV_DEFAULT_PATH));
        intent.putExtra(FileManagerIntents.EXTRA_TITLE,
                getString(R.string.import_csv_pick_file_title));
        intent.putExtra(FileManagerIntents.EXTRA_BUTTON_TEXT,
                getString(R.string.import_csv_pick_file_button));
        try {
            startActivityForResult(intent, REQUEST_CODE_PICK_FILE);
        } catch (ActivityNotFoundException e) {
            showDialog(DIALOG_PICK_FILE_ACTIVITY_NOT_FOUND);
        }
    }

    /**
     * Import members from a CSV file. Called after the PICK_FILE activity has
     * finished, and executed in a background thread.
     * 
     * @param path of the CSV file to be imported
     */
    private void importFile(final String path) {
        (new Thread("CsvImporter") {
            @Override
            public void run() {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        showDialog(DIALOG_PROGRESS_IMPORT);
                    }
                });
                MembersCsvImporter importer = new MembersCsvImporter(MembersListActivity.this, path);
                Member member = null;
                while ((member = importer.nextMember()) != null) {
                    // only import if new or newer than the existing entry
                    // (based on lastLicense)
                    Cursor cursor = getContentResolver().query(member.getUri(), new String[] {
                            Member.ID, Member.LAST_LICENSE
                    }, null, null, null);
                    if (cursor != null && cursor.moveToFirst()) {
                        int lastLicenseDB = cursor.getInt(cursor
                                .getColumnIndex(Member.LAST_LICENSE));
                        cursor.close();
                        if (Integer.parseInt(member.getLastLicense()) >= lastLicenseDB) {
                            getContentResolver().update(member.getUri(), member.getValues(), null,
                                    null);
                        }
                    } else {
                        getContentResolver().insert(member.getUri(), member.getValues());
                    }
                }
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        dismissDialog(DIALOG_PROGRESS_IMPORT);
                    }
                });
            }
        }).start();
    }

    /*
     * Helper methods
     */

    /**
     * Build the cursor used by this list activity.<br />
     * The cursor will have the proper filtering and ordering configured based
     * on the preferences (plus your own if you provide a
     * selection/selectionArgs WHERE clause).
     * 
     * @param selection parameters (WHERE clause)
     * @param selectionArgs arguments for the WHERE clause, as an array of
     *            Strings
     * @return Cursor
     */
    private Cursor buildCursor(String selection, String[] selectionArgs) {
        String lastLicenseSelection = calculateLastLicenseSelection();
        if (!lastLicenseSelection.equals(getString(R.string.all))) {
            if (selection == null) {
                selection = Member.LAST_LICENSE + "=?";
            } else {
                selection = selection + " AND " + Member.LAST_LICENSE + "=?";
            }
            if (selectionArgs == null) {
                selectionArgs = new String[] {
                    lastLicenseSelection
                };
            } else {
                String[] newArgs = new String[selectionArgs.length + 1];
                for (int i = 0; i < selectionArgs.length; i++) {
                    newArgs[i] = selectionArgs[i];
                }
                newArgs[selectionArgs.length] = lastLicenseSelection;
                selectionArgs = newArgs;
            }
        }
        return getContentResolver().query(Member.CONTENT_URI, PROJECTION, selection, selectionArgs,
                calculateOrderBy());
    }

    /**
     * Delete all members. Will display a success message after deletion.
     */
    private void deleteAllMembers() {
        getContentResolver().delete(Member.CONTENT_URI, null, null);
        Toast.makeText(this, R.string.toast_delete_all_members, Toast.LENGTH_SHORT).show();
    }

    /**
     * @return the names format from the preference.
     */
    private String getNamesFormatPreference() {
        return PreferenceManager.getDefaultSharedPreferences(this).getString(
                getString(R.string.preferences_names_format_key),
                getString(R.string.names_format_last_first));
    }

    /**
     * @return the orderBy clause based on the names format preference
     */
    private String calculateOrderBy() {
        String namesFormat = getNamesFormatPreference();
        if (getString(R.string.names_format_first_last).equals(namesFormat)) {
            return Member.FIRST_NAME + " ASC";
        } else if (getString(R.string.names_format_last_first).equals(namesFormat)) {
            return Member.LAST_NAME + " ASC";
        }
        return Member.DEFAULT_ORDER_BY;
    }

    /**
     * @return the last license selection from the preference
     */
    private String calculateLastLicenseSelection() {
        String lastLicenseSelection = PreferenceManager.getDefaultSharedPreferences(this)
                .getString(getString(R.string.preferences_last_license_key),
                        getString(R.string.all));
        return lastLicenseSelection;
    }

    /*
     * Inner classes
     */

    /**
     * FilterQueryProvider implementation for the FFCK Members. Allows to filter
     * the members based on a constraint. The constraint is applied to the first
     * name or last name (of the members).
     */
    private class MembersFilterQueryProvider implements FilterQueryProvider {

        @Override
        public Cursor runQuery(CharSequence constraint) {
            String selection = null;
            String[] selectionArgs = null;

            if (!TextUtils.isEmpty(constraint)) {
                // build the WHERE cause
                StringBuilder selectionBuilder = new StringBuilder();
                selectionBuilder.append(" ( ");
                selectionBuilder.append("( UPPER(").append(Member.LAST_NAME).append(") GLOB ? )");
                selectionBuilder.append(" OR ");
                selectionBuilder.append("( UPPER(").append(Member.FIRST_NAME).append(") GLOB ? )");
                selectionBuilder.append(" ) ");
                selection = selectionBuilder.toString();

                // and the associated search term
                String searchTerm = constraint.toString().toUpperCase() + "*";
                selectionArgs = new String[] {
                        searchTerm, searchTerm
                };
            }

            // build and return the new cursor
            return buildCursor(selection, selectionArgs);
        }
    }

    /**
     * ViewBinder implementation for the FFCK Members. Allows to do custom
     * binding for the gender icon (male or female) and the names (format
     * accordingly to preferences).
     */
    private class MembersViewBinder implements SimpleCursorAdapter.ViewBinder {

        @Override
        public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
            // correctly map the gender icon
            if (view.getId() == R.id.members_list_item_gender) {
                String gender = cursor.getString(columnIndex);
                if (Member.GENDER_MALE.equals(gender)) {
                    ((ImageView)view).setImageResource(R.drawable.male);
                } else if (Member.GENDER_FEMALE.equals(gender)) {
                    ((ImageView)view).setImageResource(R.drawable.female);
                }
                return true;
            }

            // format names accordingly to preferences
            if (view.getId() == R.id.members_list_item_names) {
                String firstName = cursor.getString(cursor.getColumnIndex(Member.FIRST_NAME));
                String lastName = cursor.getString(cursor.getColumnIndex(Member.LAST_NAME));
                String namesFormat = getNamesFormatPreference();
                if (getString(R.string.names_format_first_last).equals(namesFormat)) {
                    ((TextView)view).setText(firstName + " " + lastName);
                } else if (getString(R.string.names_format_last_first).equals(namesFormat)) {
                    ((TextView)view).setText(lastName + " " + firstName);
                }
                return true;
            }

            return false;
        }
    }

}
