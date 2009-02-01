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
import ffck.members.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * FFCK Member details activity. Display the details of a member, and allow to
 * delete it. When the member is deleted, this activity will be finished. Also
 * allow to add the member to your contacts.
 */
public class MemberDetailsActivity extends Activity {

    /*
     * Dialogs identifiers
     */

    /** Identifier for the 'Add to contacts' dialog */
    private static final int DIALOG_ADD_TO_CONTACTS = 1;

    /** Identifier for the 'Delete' dialog */
    private static final int DIALOG_DELETE = 2;

    /*
     * Instance-specific variables
     */

    /** The member for which we display the details */
    private Member member;

    /*
     * Activity lifecycle
     */

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.member_details);

        // retrieve the cursor for the member represented by the URI
        Uri uri = getIntent().getData();
        Cursor cursor = managedQuery(uri, null, null, null, null);

        // nothing found, let's just going back...
        if (cursor == null || !cursor.moveToFirst()) {
            setResult(Activity.RESULT_CANCELED);
            finish();
            return;
        }

        // bind the member's data to the view
        member = new Member(cursor);
        bindMember();
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
                showDialog(DIALOG_ADD_TO_CONTACTS);
                return true;
            case R.id.member_details_menu_delete:
                showDialog(DIALOG_DELETE);
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
            case DIALOG_ADD_TO_CONTACTS:
                AlertDialog.Builder addToContacts = new AlertDialog.Builder(this);
                addToContacts.setIcon(android.R.drawable.ic_dialog_alert);
                addToContacts.setTitle(R.string.dialog_add_to_contacts_title);
                addToContacts.setMessage(getString(R.string.dialog_add_to_contacts_text, member
                        .getFirstName(), member.getLastName()));
                addToContacts.setPositiveButton(R.string.dialog_add_to_contacts_button_ok,
                        new OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                addToContacts();
                            }
                        });
                addToContacts.setNegativeButton(R.string.dialog_add_to_contacts_button_nok, null);
                return addToContacts.create();
            case DIALOG_DELETE:
                AlertDialog.Builder delete = new AlertDialog.Builder(this);
                delete.setIcon(android.R.drawable.ic_dialog_alert);
                delete.setTitle(R.string.dialog_delete_member_title);
                delete.setMessage(getString(R.string.dialog_delete_member_text, member
                        .getFirstName(), member.getLastName()));
                delete.setPositiveButton(R.string.dialog_delete_member_button_ok,
                        new OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                deleteMember();
                            }
                        });
                delete.setNegativeButton(R.string.dialog_delete_member_button_nok, null);
                return delete.create();
            default:
                return null;
        }
    }

    /*
     * Menu actions
     */

    /**
     * Add the member (represented by the cursor of this activity) to the
     * contacts. Will display a success message after adding.
     */
    private void addToContacts() {
        member.addToContacts(this);

        // display success message (toaster)
        Toast.makeText(
                this,
                getString(R.string.toast_add_member_to_contacts, member.getFirstName(), member
                        .getLastName()), Toast.LENGTH_LONG).show();
    }

    /**
     * Delete the member represented by the cursor of this activity, and finish
     * the activity. Will display a success message after deletion.
     */
    private void deleteMember() {
        // delete the member
        getContentResolver().delete(member.getUri(), null, null);

        // display success message (toaster)
        Toast
                .makeText(
                        this,
                        getString(R.string.toast_delete_member, member.getFirstName(), member
                                .getLastName()), Toast.LENGTH_SHORT).show();

        // finish the activity
        setResult(Activity.RESULT_OK);
        finish();
    }

    /*
     * Helper methods
     */

    /**
     * Bind the member's data to the views
     */
    private void bindMember() {
        // gender icon
        if (member.isMale()) {
            ((ImageView)findViewById(R.id.member_details_gender_icon))
                    .setImageResource(R.drawable.paddler_male);
        } else {
            ((ImageView)findViewById(R.id.member_details_gender_icon))
                    .setImageResource(R.drawable.paddler_female);
        }

        // name and license
        ((TextView)findViewById(R.id.member_details_fullname)).setText(member.getFullName());
        ((TextView)findViewById(R.id.member_details_last_license)).setText(member.getLastLicense());
        ((TextView)findViewById(R.id.member_details_code)).setText(member.getCode());

        // birth date, age and category
        ((TextView)findViewById(R.id.member_details_birth_date)).setText(member
                .getBirthDateAsString());
        ((TextView)findViewById(R.id.member_details_age)).setText(String.valueOf(member
                .calculateAge()));
        ((TextView)findViewById(R.id.member_details_category)).setText(getString(member
                .calculateCategory().getValueResId()));

        // phone numbers
        if (TextUtils.isEmpty(member.getPhoneMobile())) {
            findViewById(R.id.member_details_section_phone_mobile).setVisibility(View.GONE);
        } else {
            ((TextView)findViewById(R.id.member_details_phone_mobile)).setText(member
                    .getPhoneMobile());
        }
        if (TextUtils.isEmpty(member.getPhoneMobile2())) {
            findViewById(R.id.member_details_section_phone_mobile_2).setVisibility(View.GONE);
        } else {
            ((TextView)findViewById(R.id.member_details_phone_mobile_2)).setText(member
                    .getPhoneMobile2());
        }
        if (TextUtils.isEmpty(member.getPhoneHome())) {
            findViewById(R.id.member_details_section_phone_home).setVisibility(View.GONE);
        } else {
            ((TextView)findViewById(R.id.member_details_phone_home)).setText(member.getPhoneHome());
        }
        if (TextUtils.isEmpty(member.getPhoneOther())) {
            findViewById(R.id.member_details_section_phone_other).setVisibility(View.GONE);
        } else {
            ((TextView)findViewById(R.id.member_details_phone_other)).setText(member
                    .getPhoneOther());
        }

        // e-mails
        if (TextUtils.isEmpty(member.getEmail())) {
            findViewById(R.id.member_details_section_email).setVisibility(View.GONE);
        } else {
            ((TextView)findViewById(R.id.member_details_email)).setText(member.getEmail());
        }
        if (TextUtils.isEmpty(member.getEmail2())) {
            findViewById(R.id.member_details_section_email_2).setVisibility(View.GONE);
        } else {
            ((TextView)findViewById(R.id.member_details_email_2)).setText(member.getEmail2());
        }

        // address
        ((TextView)findViewById(R.id.member_details_address)).setText(member.getFullAddress());
    }

}
