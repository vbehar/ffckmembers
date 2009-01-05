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

import static android.provider.BaseColumns._ID;
import static ffck.members.Members.CONTENT_URI;
import static ffck.members.Members.DEFAULT_ORDER_BY;
import static ffck.members.Members.Columns.CODE;
import static ffck.members.Members.Columns.FIRST_NAME;
import static ffck.members.Members.Columns.LAST_NAME;
import static org.openintents.intents.FileManagerIntents.ACTION_PICK_FILE;
import static org.openintents.intents.FileManagerIntents.EXTRA_BUTTON_TEXT;
import static org.openintents.intents.FileManagerIntents.EXTRA_TITLE;
import ffck.members.MembersCsvImporter;
import ffck.members.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.FilterQueryProvider;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import java.io.File;

/**
 * FFCK Members list activity. Display a list of all members, and supports
 * filtering. Also provides a menu.
 */
public class MembersListActivity extends ListActivity {

    /** The requestCode for the PICK_FILE intent activity result */
    private static final int REQUEST_CODE_PICK_FILE = 1;

    /** Default path for selecting the CSV file to import (the SD card) */
    private static final File IMPORT_CSV_DEFAULT_PATH = new File("/sdcard");

    /*
     * Dialogs identifiers
     */

    /** Identifier for the 'About' dialog */
    private static final int DIALOG_ABOUT = 1;

    /** Identifier for the 'Help' dialog */
    private static final int DIALOG_HELP = 2;

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
            _ID, CODE, LAST_NAME, FIRST_NAME
    };

    /** Source for the DB->View mapping : Columns names */
    private static final String[] FROM = {
            LAST_NAME, FIRST_NAME
    };

    /** Destination for the DB->View mapping : View IDs */
    private static final int[] TO = {
            R.id.members_list_item_last_name, R.id.members_list_item_first_name
    };

    /*
     * Instance-specific variables
     */

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
        Cursor cursor = managedQuery(CONTENT_URI, PROJECTION, null, null, DEFAULT_ORDER_BY);
        codeColumnIndex = cursor.getColumnIndexOrThrow(CODE);
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.members_list_item,
                cursor, FROM, TO);
        setListAdapter(adapter);

        // enable filtering (see MembersFilterQueryProvider javadoc)
        getListView().setTextFilterEnabled(true);
        adapter.setFilterQueryProvider(new MembersFilterQueryProvider());
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
        Uri uri = Uri.withAppendedPath(CONTENT_URI, code);
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
            case R.id.members_list_menu_about:
                showDialog(DIALOG_ABOUT);
                return true;
            case R.id.members_list_menu_help:
                showDialog(DIALOG_HELP);
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
                about.setMessage(R.string.dialog_about_text);
                about.setPositiveButton(R.string.dialog_about_button, null);
                return about.create();
            case DIALOG_HELP:
                AlertDialog.Builder help = new AlertDialog.Builder(this);
                help.setTitle(R.string.dialog_help_title);
                help.setMessage(R.string.dialog_help_text);
                help.setPositiveButton(R.string.dialog_help_button, null);
                return help.create();
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

    /*
     * CSV Import
     */

    /**
     * Starts a new activity for selecting the file to import. We use the
     * OpenIntents PICK_FILE intent to find a matching activity.
     */
    private void selectFileToImport() {
        Intent intent = new Intent(ACTION_PICK_FILE);
        intent.setData(Uri.fromFile(IMPORT_CSV_DEFAULT_PATH));
        intent.putExtra(EXTRA_TITLE, getString(R.string.import_csv_pick_file_title));
        intent.putExtra(EXTRA_BUTTON_TEXT, getString(R.string.import_csv_pick_file_button));
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
                ContentValues member = null;
                while ((member = importer.nextMember()) != null) {
                    String code = member.getAsString(CODE);
                    Uri uri = Uri.withAppendedPath(CONTENT_URI, code);
                    getContentResolver().insert(uri, member);
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
     * Delete all members. Will display a success message after deletion.
     */
    private void deleteAllMembers() {
        getContentResolver().delete(CONTENT_URI, null, null);
        Toast.makeText(this, R.string.toast_delete_all_members, Toast.LENGTH_SHORT).show();
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
                selectionBuilder.append("UPPER(").append(LAST_NAME).append(") GLOB ?");
                selectionBuilder.append(" OR ");
                selectionBuilder.append("UPPER(").append(FIRST_NAME).append(") GLOB ?");
                selection = selectionBuilder.toString();

                // and the associated search term
                String searchTerm = constraint.toString().toUpperCase() + "*";
                selectionArgs = new String[] {
                        searchTerm, searchTerm
                };
            }

            // execute the query and return the new cursor
            return getContentResolver().query(CONTENT_URI, PROJECTION, selection, selectionArgs,
                    DEFAULT_ORDER_BY);
        }

    }

}
