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
        MAPPING.put("CODE ADHERENT", Member.CODE);
        MAPPING.put("NOM", Member.LAST_NAME);
        MAPPING.put("PRENOM", Member.FIRST_NAME);
        MAPPING.put("NE LE", Member.BIRTH_DATE);
        MAPPING.put("SEXE", Member.GENDER);
        MAPPING.put("ADRESSE", Member.ADDRESS);
        MAPPING.put("CODE POSTAL", Member.POSTAL_CODE);
        MAPPING.put("VILLE", Member.CITY);
        MAPPING.put("PAYS", Member.COUNTRY);
        MAPPING.put("TEL", Member.PHONE_HOME);
        MAPPING.put("AUTRE TEL", Member.PHONE_OTHER);
        MAPPING.put("MOBILE", Member.PHONE_MOBILE);
        MAPPING.put("EMAIL", Member.EMAIL);
        MAPPING.put("AUTRE MOBILE", Member.PHONE_MOBILE_2);
        MAPPING.put("AUTRE EMAIL", Member.EMAIL_2);
        MAPPING.put("DERNIERE LICENCE", Member.LAST_LICENSE);
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
     * a Member instance.
     * 
     * @return Member instance, or null is an error occurs or if we hit the end
     *         of the file.
     */
    public Member nextMember() {
        String[] original = null;
        try {
            original = readNextTokens();
        } catch (IOException e) {
            Toast.makeText(context, e.getMessage(), Toast.LENGTH_LONG).show();
        }
        if (original == null) {
            return null;
        }

        ContentValues values = new ContentValues();
        for (int i = 0; i < original.length; i++) {
            String column = MAPPING.get(header[i]);
            if (column != null) {
                String value = original[i];

                // clean names...
                if (column.equals(Member.FIRST_NAME) || column.equals(Member.LAST_NAME)) {
                    value = capitalize(value);
                }
                // ... and cities & countries
                if (column.equals(Member.CITY) || column.equals(Member.COUNTRY)) {
                    value = capitalize(value);
                }

                // clean phone numbers
                if (column.equals(Member.PHONE_HOME) || column.equals(Member.PHONE_MOBILE)
                        || column.equals(Member.PHONE_MOBILE_2)
                        || column.equals(Member.PHONE_OTHER)) {
                    if (!TextUtils.isEmpty(value)) {
                        value = value.replaceAll("\\.", "");
                        value = value.replaceFirst("0", "+33");
                    }
                }

                // fix gender code
                if (column.equals(Member.GENDER)) {
                    if ("H".equals(value)) { // H = 'Homme'
                        value = Member.GENDER_MALE;
                    }
                }

                // address has the same header twice in the CSV...
                if (column.equals(Member.ADDRESS)) {
                    value = value.toLowerCase(Locale.FRANCE);
                    if (values.containsKey(column)) {
                        if (TextUtils.isEmpty(value)) {
                            value = values.getAsString(column);
                        } else {
                            value = values.getAsString(column) + "\n" + value;
                        }
                    }
                }

                values.put(column, value);
            }
        }

        if (values.size() == 0) {
            return null;
        }
        return new Member(values);
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
