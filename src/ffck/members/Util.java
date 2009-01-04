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

/**
 * Utility class for non-business related work. Contains static methods...
 */
public class Util {

    /**
     * Use the static methods instead...
     */
    private Util() {
    }

    /**
     * Prepend the given newValue to the given array.
     * 
     * @param newValue String value to be prepended (at first position) to the
     *            array
     * @param array array to be modified (could be null)
     * @return new instance of an array of Strings, with the newValue at first
     *         position
     */
    public static String[] safePrepend(String newValue, String[] array) {
        if (array == null) {
            return new String[] {
                newValue
            };
        }

        String[] newArray = new String[array.length + 1];
        newArray[0] = newValue;
        for (int i = 0; i < array.length; i++) {
            newArray[i + 1] = array[i];
        }
        return newArray;
    }

}
