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
import static ffck.members.Members.Columns.LAST_LICENSE;
import ffck.members.R;

import android.database.Cursor;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Basic preference activity. Allows to choose the names format to display, and
 * filter on the last license year.
 */
public class MembersPreferenceActivity extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        // retrieve license years from DB
        String[] licenseYears = retrieveAllLicenseYears();
        ListPreference lastLicensePref = (ListPreference)findPreference(getString(R.string.preferences_last_license_key));
        lastLicensePref.setEntries(licenseYears);
        lastLicensePref.setEntryValues(licenseYears);
    }

    /*
     * Helper methods
     */

    /**
     * @return array of string containing all licenses year (from the DB)
     */
    private String[] retrieveAllLicenseYears() {
        Cursor cursor = getContentResolver().query(CONTENT_URI, new String[] {
                _ID, LAST_LICENSE
        }, null, null, LAST_LICENSE + " DESC");
        int columnIndex = cursor.getColumnIndexOrThrow(LAST_LICENSE);
        List<String> licenseYears = new ArrayList<String>();
        licenseYears.add(getString(R.string.all));
        while (cursor.moveToNext()) {
            String licenseYear = cursor.getString(columnIndex);
            if (!licenseYears.contains(licenseYear)) {
                licenseYears.add(licenseYear);
            }
        }
        cursor.close();
        return licenseYears.toArray(new String[licenseYears.size()]);
    }

}
