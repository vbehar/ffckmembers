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

package ffck.members;

import static ffck.members.Members.CONTENT_URI;
import static ffck.members.Members.Columns.ADDRESS;
import static ffck.members.Members.Columns.BIRTH_DATE;
import static ffck.members.Members.Columns.CITY;
import static ffck.members.Members.Columns.CODE;
import static ffck.members.Members.Columns.EMAIL;
import static ffck.members.Members.Columns.EMAIL_2;
import static ffck.members.Members.Columns.FIRST_NAME;
import static ffck.members.Members.Columns.GENDER;
import static ffck.members.Members.Columns.LAST_LICENSE;
import static ffck.members.Members.Columns.LAST_NAME;
import static ffck.members.Members.Columns.PHONE_HOME;
import static ffck.members.Members.Columns.PHONE_MOBILE;
import static ffck.members.Members.Columns.PHONE_MOBILE_2;
import static ffck.members.Members.Columns.PHONE_OTHER;
import static ffck.members.Members.Columns.POSTAL_CODE;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.util.Linkify;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.List;

/**
 * FFCK Member details activity. Display the details of a member, and allow to
 * delete it. When the member is deleted, this activity will be finished.
 */
public class MemberDetailsActivity extends Activity {

    /** Fields that will be 'linkified' */
    private static final List<String> LINKIFIED_FIELDS = Arrays.asList(new String[] {
            PHONE_HOME, PHONE_MOBILE, PHONE_MOBILE_2, PHONE_OTHER, EMAIL, EMAIL_2
    });

    /** Source for the DB->View mapping : Columns names */
    private static final String[] FROM = {
            LAST_NAME, FIRST_NAME, GENDER, BIRTH_DATE, PHONE_MOBILE, PHONE_MOBILE_2, PHONE_HOME,
            PHONE_OTHER, EMAIL, EMAIL_2, ADDRESS, POSTAL_CODE, CITY, CODE, LAST_LICENSE
    };

    /** Destination for the DB->View mapping : View IDs */
    private static final int[] TO = {
            R.id.member_details_last_name, R.id.member_details_first_name,
            R.id.member_details_gender, R.id.member_details_birth_date,
            R.id.member_details_phone_mobile, R.id.member_details_phone_mobile_2,
            R.id.member_details_phone_home, R.id.member_details_phone_other,
            R.id.member_details_email, R.id.member_details_email_2, R.id.member_details_address,
            R.id.member_details_postal_code, R.id.member_details_city, R.id.member_details_code,
            R.id.member_details_last_license
    };

    /** The cursor holding the member's data */
    private Cursor cursor;

    /*
     * Activity lifecycle
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.member_details);

        // retrieve the cursor for the member represented by the URI
        Uri uri = getIntent().getData();
        cursor = managedQuery(uri, null, null, null, null);

        // nothing found, let's just going back...
        if (cursor == null || !cursor.moveToFirst()) {
            setResult(Activity.RESULT_CANCELED);
            finish();
            return;
        }

        // map the cursor's data to the view
        for (int i = 0; i < TO.length; i++) {
            TextView view = (TextView)findViewById(TO[i]);
            String text = cursor.getString(cursor.getColumnIndexOrThrow(FROM[i]));
            view.setText(text);
            if (LINKIFIED_FIELDS.contains(FROM[i])) {
                Linkify.addLinks(view, Linkify.ALL);
            }
        }
    }

    /*
     * Menu management
     */

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!super.onCreateOptionsMenu(menu)) {
            return false;
        }

        // create the menu from XML
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.member_details_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (super.onOptionsItemSelected(item)) {
            return true;
        }

        // when a menu item is selected :
        switch (item.getItemId()) {
            case R.id.member_details_menu_add_contact:
                addToContacts();
                return true;
            case R.id.member_details_menu_delete:
                deleteMember();
                return true;
            default:
                return false;
        }
    }

    /*
     * Menu actions
     */

    /**
     * Add the member (represented by the cursor of this activity) to the
     * contacts. Will display a dialog for confirmation of the action, and a
     * success message after adding.
     */
    private void addToContacts() {
        // TODO
    }

    /**
     * Delete the member represented by the cursor of this activity, and finish
     * the activity. Will display a dialog for confirmation of the action, and a
     * success message after deletion.
     */
    private void deleteMember() {
        final String firstName = cursor.getString(cursor.getColumnIndexOrThrow(FIRST_NAME));
        final String lastName = cursor.getString(cursor.getColumnIndexOrThrow(LAST_NAME));

        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setIcon(android.R.drawable.ic_dialog_alert);
        dialog.setTitle(R.string.dialog_delete_member_title);
        dialog.setMessage(getString(R.string.dialog_delete_member_text, firstName, lastName));
        dialog.setPositiveButton(R.string.dialog_delete_member_button_ok, new OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // delete the member
                String code = cursor.getString(cursor.getColumnIndexOrThrow(CODE));
                Uri uri = Uri.withAppendedPath(CONTENT_URI, code);
                getContentResolver().delete(uri, null, null);

                // display success message (toaster)
                Toast.makeText(MemberDetailsActivity.this,
                        getString(R.string.toaster_delete_member, firstName, lastName),
                        Toast.LENGTH_SHORT).show();

                // finish the activity
                setResult(Activity.RESULT_OK);
                finish();
            }
        });
        dialog.setNegativeButton(R.string.dialog_delete_member_button_nok, null);
        dialog.create().show();
    }

}
