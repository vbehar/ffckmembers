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

package ffck.members.provider;

import ffck.members.Member;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;

/**
 * FFCK Members content provider.
 */
public class MembersProvider extends ContentProvider {

    private static final UriMatcher URI_MATCHER;

    private static final int MATCH_MEMBERS = 1;

    private static final int MATCH_MEMBER = 2;

    private static final String MEMBERS_TABLE = "members";

    private DatabaseHelper dbHelper;

    /*
     * static initialization
     */

    static {
        URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
        URI_MATCHER.addURI("ffck.members", "members", MATCH_MEMBERS);
        URI_MATCHER.addURI("ffck.members", "members/*", MATCH_MEMBER);
    }

    /*
     * Business methods
     */

    @Override
    public boolean onCreate() {
        dbHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String orderBy) {
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        queryBuilder.setTables(MEMBERS_TABLE);

        switch (URI_MATCHER.match(uri)) {
            case MATCH_MEMBERS:
                break;
            case MATCH_MEMBER:
                queryBuilder.appendWhere(Member.CODE + "=?");
                selectionArgs = safePrepend(uri.getLastPathSegment(), selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        // if no sort order is specified use the default
        if (TextUtils.isEmpty(orderBy)) {
            orderBy = Member.DEFAULT_ORDER_BY;
        }

        // run the query and return the results as a Cursor
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = queryBuilder.query(db, projection, selection, selectionArgs, null, null,
                orderBy);
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Override
    public String getType(Uri uri) {
        switch (URI_MATCHER.match(uri)) {
            case MATCH_MEMBERS:
                return "vnd.android.cursor.dir/vnd.ffck.member";
            case MATCH_MEMBER:
                return "vnd.android.cursor.item/vnd.ffck.member";
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        // Validate the requested URI
        if (URI_MATCHER.match(uri) != MATCH_MEMBER) {
            throw new IllegalArgumentException("Unknown URI " + uri);
        }

        // Insert into database
        dbHelper.getWritableDatabase().insertOrThrow(MEMBERS_TABLE, null, values);

        // Notify any watchers of the change
        Uri newUri = Uri.withAppendedPath(Member.CONTENT_URI, uri.getLastPathSegment());
        getContext().getContentResolver().notifyChange(newUri, null);
        return newUri;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int count = 0;

        switch (URI_MATCHER.match(uri)) {
            case MATCH_MEMBERS:
                count = db.delete(MEMBERS_TABLE, selection, selectionArgs);
                break;
            case MATCH_MEMBER:
                count = db.delete(MEMBERS_TABLE, Member.CODE + "=?", new String[] {
                    uri.getLastPathSegment()
                });
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        // Notify any watchers of the change
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int count = 0;

        switch (URI_MATCHER.match(uri)) {
            case MATCH_MEMBERS:
                count = db.update(MEMBERS_TABLE, values, selection, selectionArgs);
                break;
            case MATCH_MEMBER:
                count = db.update(MEMBERS_TABLE, values, Member.CODE + "=?", new String[] {
                    uri.getLastPathSegment()
                });
                break;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        // Notify any watchers of the change
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    /*
     * Helper methods
     */

    /**
     * Prepend the given newValue to the given array.
     * 
     * @param newValue String value to be prepended (at first position) to the
     *            array
     * @param array array to be modified (could be null)
     * @return new instance of an array of Strings, with the newValue at first
     *         position
     */
    private String[] safePrepend(String newValue, String[] array) {
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

    /*
     * Inner classes
     */

    /**
     * Helper class for working with the FFCK Members database (creating,
     * upgrading and opening it).
     */
    private class DatabaseHelper extends SQLiteOpenHelper {

        private static final String DATABASE_NAME = "members.db";

        private static final int DATABASE_VERSION = 1;

        /**
         * Build a new DatabaseHelper instance for the given context
         * 
         * @param context to use to open or create the database
         */
        public DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            StringBuilder sql = new StringBuilder();
            sql.append("CREATE TABLE IF NOT EXISTS ").append(MEMBERS_TABLE).append(" (");
            sql.append(Member.ID).append(" INTEGER PRIMARY KEY AUTOINCREMENT, ");
            sql.append(Member.CODE).append(" TEXT UNIQUE NOT NULL, ");
            sql.append(Member.FIRST_NAME).append(" TEXT NOT NULL, ");
            sql.append(Member.LAST_NAME).append(" TEXT NOT NULL, ");
            sql.append(Member.BIRTH_DATE).append(" TEXT, ");
            sql.append(Member.GENDER).append(" TEXT, ");
            sql.append(Member.ADDRESS).append(" TEXT, ");
            sql.append(Member.POSTAL_CODE).append(" TEXT, ");
            sql.append(Member.CITY).append(" TEXT, ");
            sql.append(Member.COUNTRY).append(" TEXT, ");
            sql.append(Member.PHONE_HOME).append(" TEXT, ");
            sql.append(Member.PHONE_OTHER).append(" TEXT, ");
            sql.append(Member.PHONE_MOBILE).append(" TEXT, ");
            sql.append(Member.PHONE_MOBILE_2).append(" TEXT, ");
            sql.append(Member.EMAIL).append(" TEXT, ");
            sql.append(Member.EMAIL_2).append(" TEXT, ");
            sql.append(Member.LAST_LICENSE).append(" TEXT");
            sql.append(");");
            db.execSQL(sql.toString());
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // This is the first version, we have nothing to do here...
        }

    }

}
