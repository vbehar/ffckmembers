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

import static ffck.members.Members.Columns.ADDRESS;
import static ffck.members.Members.Columns.BIRTH_DATE;
import static ffck.members.Members.Columns.CITY;
import static ffck.members.Members.Columns.CODE;
import static ffck.members.Members.Columns.COUNTRY;
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

import android.content.ContentValues;
import android.content.Context;
import android.text.TextUtils;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * A simple CSV Importer, specific to the FFCK Members CSV format. It uses a CSV
 * file as the source, parse it and return its rows (members data) one by one
 * (see the <code>nextMember</code> method). The importer is not responsible for
 * inserting the data in the database.
 */
public class MembersCsvImporter {

    /** The CSV separator */
    private static final String CSV_SEPARATOR = ";";

    /** Static mapping between the CSV header and the database columns */
    private static final Map<String, String> MAPPING = new HashMap<String, String>();

    /** The buffered reader instance used to read data from the CSV file */
    private BufferedReader br;

    /** The CSV header tokens */
    private String[] header;

    /** The android context from which the importer is used */
    private Context context;

    /*
     * Static initialization
     */

    static {
        MAPPING.put("CODE ADHERENT", CODE);
        MAPPING.put("NOM", LAST_NAME);
        MAPPING.put("PRENOM", FIRST_NAME);
        MAPPING.put("NE LE", BIRTH_DATE);
        MAPPING.put("SEXE", GENDER);
        MAPPING.put("ADRESSE", ADDRESS);
        MAPPING.put("CODE POSTAL", POSTAL_CODE);
        MAPPING.put("VILLE", CITY);
        MAPPING.put("PAYS", COUNTRY);
        MAPPING.put("TEL", PHONE_HOME);
        MAPPING.put("AUTRE TEL", PHONE_OTHER);
        MAPPING.put("MOBILE", PHONE_MOBILE);
        MAPPING.put("EMAIL", EMAIL);
        MAPPING.put("AUTRE MOBILE", PHONE_MOBILE_2);
        MAPPING.put("AUTRE EMAIL", EMAIL_2);
        MAPPING.put("DERNIERE LICENCE", LAST_LICENSE);
    }

    /*
     * Constructors
     */

    /**
     * Build a new Importer instance for the CSV file represented by the given
     * path.
     * 
     * @param context android context from which this instance will be used
     *            (usually 'this')
     * @param path of the CSV file to be used as the source for the import
     *            operation
     */
    public MembersCsvImporter(Context context, String path) {
        this.context = context;
        File file = new File(path);
        try {
            FileReader reader = new FileReader(file);
            br = new BufferedReader(reader);
            try {
                header = readNextTokens();
            } catch (IOException e) {
                br.close();
                br = null;
                throw e;
            }
        } catch (IOException e) {
            Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    /*
     * Business methods
     */

    /**
     * Read the next line from the CSV file, parse the data and convert them as
     * ContentValues (that could be inserted into the database).
     * 
     * @return ContentValues instance representing a member, or null is an error
     *         occurs or if we hit the end of the file.
     */
    public ContentValues nextMember() {
        String[] values = null;
        try {
            values = readNextTokens();
        } catch (IOException e) {
            Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
        }
        if (values == null) {
            return null;
        }

        ContentValues member = new ContentValues();
        for (int i = 0; i < values.length; i++) {
            String column = MAPPING.get(header[i]);
            if (column != null) {
                String value = values[i];

                // clean names...
                if (column.equals(FIRST_NAME) || column.equals(LAST_NAME)) {
                    value = capitalize(value);
                }
                // ... and cities & countries
                if (column.equals(CITY) || column.equals(COUNTRY)) {
                    value = capitalize(value);
                }

                // clean phone numbers
                if (column.equals(PHONE_HOME) || column.equals(PHONE_MOBILE)
                        || column.equals(PHONE_MOBILE_2) || column.equals(PHONE_OTHER)) {
                    if (!TextUtils.isEmpty(value)) {
                        value = value.replaceAll("\\.", "");
                        value = value.replaceFirst("0", "+33");
                    }
                }

                // fix gender code
                if (column.equals(GENDER)) {
                    if ("H".equals(value)) { // H = 'Homme'
                        value = "M"; // M = 'Male'
                    }
                }

                // address has the same header twice in the CSV...
                if (column.equals(ADDRESS)) {
                    value = value.toLowerCase(Locale.FRANCE);
                    if (member.containsKey(column)) {
                        if (TextUtils.isEmpty(value)) {
                            value = member.getAsString(column);
                        } else {
                            value = member.getAsString(column) + "\n" + value;
                        }
                    }
                }

                member.put(column, value);
            }
        }

        if (member.size() == 0) {
            return null;
        }
        return member;
    }

    /*
     * Helper methods
     */

    /**
     * Read the next line from the CSV file, and split it in tokens (based on
     * the CSV_SEPARATOR value). If we hit the end of the file, it will close
     * the buffered reader.
     * 
     * @return array of Strings representing a row, of null if EOF
     * @throws IOException if unable to read the next line from the file
     */
    private String[] readNextTokens() throws IOException {
        if (br == null) {
            return null;
        }

        String nextLine = br.readLine();
        if (nextLine == null) {
            br.close();
            br = null;
            return null;
        }

        return TextUtils.split(nextLine, CSV_SEPARATOR);
    }

    /**
     * Capitalize the given input string
     * 
     * @param input string that needs to be capitalized
     * @return capitalized string, won't be null
     */
    private String capitalize(String input) {
        if (TextUtils.isEmpty(input)) {
            return "";
        }

        List<Character> separators = Arrays.asList(new Character[] {
                ' ', '-', '_', ',', '.'
        });
        String lowerCased = input.toLowerCase(Locale.FRANCE);
        StringBuilder output = new StringBuilder();
        boolean needToUpperNext = true;
        for (int i = 0; i < input.length(); i++) {
            if (needToUpperNext) {
                output.append(Character.toUpperCase(lowerCased.charAt(i)));
                needToUpperNext = false;
            } else {
                output.append(lowerCased.charAt(i));
            }
            if (separators.contains(lowerCased.charAt(i))) {
                needToUpperNext = true;
            }
        }
        return output.toString();
    }
}
