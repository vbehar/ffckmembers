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

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Interface defining constants for working with the FFCK Members content
 * provider.
 */
public interface Members {

    /** Members table name in the database */
    String TABLE_NAME = "members";

    /** Authority used by the content provider */
    String AUTHORITY = "ffck.members";

    /** Content URI used by the content provider */
    Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + TABLE_NAME);

    /** The MIME type for a directory of members */
    String CONTENT_TYPE_DIR = "vnd.android.cursor.dir/vnd.ffck.member";

    /** The MIME type for a single member */
    String CONTENT_TYPE_ITEM = "vnd.android.cursor.item/vnd.ffck.member";

    /** The default 'order by' for sorting lists */
    String DEFAULT_ORDER_BY = Columns.LAST_NAME + " ASC";

    /**
     * Interface defining all columns for the "Members" table.
     */
    public interface Columns extends BaseColumns {
        /**
         * FFCK license code. 6 digits, but stored as a String because it may
         * starts with some '0'.
         */
        String CODE = "code";

        /** first name of the member (String) */
        String FIRST_NAME = "first_name";

        /** last name of the member (String) */
        String LAST_NAME = "last_name";

        /** birth date, as a String, format dd/MM/yyyy */
        String BIRTH_DATE = "birth_date";

        /** gender, as a String, either H (male) or F (female) */
        String GENDER = "gender";

        /** address (String) */
        String ADDRESS = "address";

        /** postal code, as a String (5 chars) */
        String POSTAL_CODE = "postal_code";

        /** city (String) */
        String CITY = "city";

        /** country (String) */
        String COUNTRY = "country";

        /** home phone number, as a String */
        String PHONE_HOME = "phone_home";

        /** other phone number, as a String */
        String PHONE_OTHER = "phone_other";

        /** mobile phone number, as a String */
        String PHONE_MOBILE = "phone_mobile";

        /** second mobile phone number, as a String */
        String PHONE_MOBILE_2 = "phone_mobile_2";

        /** email (String) */
        String EMAIL = "email";

        /** second email (String) */
        String EMAIL_2 = "email_2";

        /** year of last FFCK license, as a String, format yyyy */
        String LAST_LICENSE = "last_license";
    }

}
