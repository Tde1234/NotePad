package com.example.android.notepad;

import com.example.android.notepad.NotePad;

import android.content.ClipDescription;
import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.content.ContentProvider.PipeDataWriter;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.LiveFolders;
import android.text.TextUtils;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;

public class NotePadProvider extends ContentProvider implements PipeDataWriter<Cursor> {
    private static final String TAG = "NotePadProvider";

    private static final String DATABASE_NAME = "note_pad.db";

    private static final int DATABASE_VERSION = 4;

    private static HashMap<String, String> sNotesProjectionMap;

    private static HashMap<String, String> sLiveFolderProjectionMap;

    private static HashMap<String, String> sCategoriesProjectionMap;

    private static final String[] READ_NOTE_PROJECTION = new String[] {
            NotePad.Notes._ID,
            NotePad.Notes.COLUMN_NAME_NOTE,
            NotePad.Notes.COLUMN_NAME_TITLE,
    };
    private static final int READ_NOTE_NOTE_INDEX = 1;
    private static final int READ_NOTE_TITLE_INDEX = 2;

    private static final int NOTES = 1;

    private static final int NOTE_ID = 2;

    private static final int LIVE_FOLDER_NOTES = 3;

    private static final int CATEGORIES = 4;

    private static final int CATEGORY_ID = 5;

    private static final UriMatcher sUriMatcher;

    private DatabaseHelper mOpenHelper;

    static {
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        sUriMatcher.addURI(NotePad.AUTHORITY, "notes", NOTES);

        sUriMatcher.addURI(NotePad.AUTHORITY, "notes/#", NOTE_ID);

        sUriMatcher.addURI(NotePad.AUTHORITY, "live_folders/notes", LIVE_FOLDER_NOTES);

        sUriMatcher.addURI(NotePad.AUTHORITY, "categories", CATEGORIES);

        sUriMatcher.addURI(NotePad.AUTHORITY, "categories/#", CATEGORY_ID);

        sNotesProjectionMap = new HashMap<String, String>();

        sNotesProjectionMap.put(NotePad.Notes._ID, NotePad.Notes._ID);

        sNotesProjectionMap.put(NotePad.Notes.COLUMN_NAME_TITLE, NotePad.Notes.COLUMN_NAME_TITLE);

        sNotesProjectionMap.put(NotePad.Notes.COLUMN_NAME_NOTE, NotePad.Notes.COLUMN_NAME_NOTE);

        sNotesProjectionMap.put(NotePad.Notes.COLUMN_NAME_CREATE_DATE,
                NotePad.Notes.COLUMN_NAME_CREATE_DATE);

        sNotesProjectionMap.put(
                NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE,
                NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE);

        sNotesProjectionMap.put(
                NotePad.Notes.COLUMN_NAME_CATEGORY_ID,
                NotePad.Notes.COLUMN_NAME_CATEGORY_ID);

        sNotesProjectionMap.put(
                NotePad.Notes.COLUMN_NAME_COMPLETED,
                NotePad.Notes.COLUMN_NAME_COMPLETED);

        sLiveFolderProjectionMap = new HashMap<String, String>();

        sLiveFolderProjectionMap.put(LiveFolders._ID, NotePad.Notes._ID + " AS " + LiveFolders._ID);

        sLiveFolderProjectionMap.put(LiveFolders.NAME, NotePad.Notes.COLUMN_NAME_TITLE + " AS " +
                LiveFolders.NAME);

        sCategoriesProjectionMap = new HashMap<String, String>();

        sCategoriesProjectionMap.put(NotePad.Categories._ID, NotePad.Categories._ID);

        sCategoriesProjectionMap.put(NotePad.Categories.COLUMN_NAME_NAME, NotePad.Categories.COLUMN_NAME_NAME);

        sCategoriesProjectionMap.put(NotePad.Categories.COLUMN_NAME_COLOR, NotePad.Categories.COLUMN_NAME_COLOR);

        sCategoriesProjectionMap.put(NotePad.Categories.COLUMN_NAME_CREATE_DATE, NotePad.Categories.COLUMN_NAME_CREATE_DATE);
    }

    static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE " + NotePad.Notes.TABLE_NAME + " ("
                    + NotePad.Notes._ID + " INTEGER PRIMARY KEY,"
                    + NotePad.Notes.COLUMN_NAME_TITLE + " TEXT,"
                    + NotePad.Notes.COLUMN_NAME_NOTE + " TEXT,"
                    + NotePad.Notes.COLUMN_NAME_CREATE_DATE + " INTEGER,"
                    + NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE + " INTEGER,"
                    + NotePad.Notes.COLUMN_NAME_CATEGORY_ID + " INTEGER DEFAULT NULL,"
                    + NotePad.Notes.COLUMN_NAME_COMPLETED + " INTEGER DEFAULT 0"
                    + ");");

            db.execSQL("CREATE TABLE " + NotePad.Categories.TABLE_NAME + " ("
                    + NotePad.Categories._ID + " INTEGER PRIMARY KEY,"
                    + NotePad.Categories.COLUMN_NAME_NAME + " TEXT NOT NULL,"
                    + NotePad.Categories.COLUMN_NAME_COLOR + " TEXT DEFAULT '#000000',"
                    + NotePad.Categories.COLUMN_NAME_CREATE_DATE + " INTEGER"
                    + ");");

            ContentValues values = new ContentValues();
            values.put(NotePad.Categories.COLUMN_NAME_NAME, "未分类");
            values.put(NotePad.Categories.COLUMN_NAME_COLOR, "#9E9E9E");
            values.put(NotePad.Categories.COLUMN_NAME_CREATE_DATE, System.currentTimeMillis());
            db.insert(NotePad.Categories.TABLE_NAME, null, values);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion);

            if (oldVersion < 2) {
                db.execSQL("DROP TABLE IF EXISTS notes");
                onCreate(db);
            }

            if (oldVersion < 3) {
                db.execSQL("ALTER TABLE " + NotePad.Notes.TABLE_NAME + " ADD COLUMN "
                        + NotePad.Notes.COLUMN_NAME_CATEGORY_ID + " INTEGER DEFAULT NULL");

                db.execSQL("CREATE TABLE " + NotePad.Categories.TABLE_NAME + " ("
                        + NotePad.Categories._ID + " INTEGER PRIMARY KEY,"
                        + NotePad.Categories.COLUMN_NAME_NAME + " TEXT NOT NULL,"
                        + NotePad.Categories.COLUMN_NAME_COLOR + " TEXT DEFAULT '#000000',"
                        + NotePad.Categories.COLUMN_NAME_CREATE_DATE + " INTEGER"
                        + ");");

                ContentValues values = new ContentValues();
                values.put(NotePad.Categories.COLUMN_NAME_NAME, "未分类");
                values.put(NotePad.Categories.COLUMN_NAME_COLOR, "#9E9E9E");
                values.put(NotePad.Categories.COLUMN_NAME_CREATE_DATE, System.currentTimeMillis());
                db.insert(NotePad.Categories.TABLE_NAME, null, values);
            }

            if (oldVersion < 4) {
                db.execSQL("ALTER TABLE " + NotePad.Notes.TABLE_NAME + " ADD COLUMN "
                        + NotePad.Notes.COLUMN_NAME_COMPLETED + " INTEGER DEFAULT 0");
            }
        }

        @Override
        public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Downgrading database from version " + oldVersion + " to "
                    + newVersion);

            if (oldVersion == 4 && newVersion == 3) {
            }
        }
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {

        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        switch (sUriMatcher.match(uri)) {
            case NOTES:
                qb.setTables(NotePad.Notes.TABLE_NAME);
                qb.setProjectionMap(sNotesProjectionMap);
                break;

            case NOTE_ID:
                qb.setTables(NotePad.Notes.TABLE_NAME);
                qb.setProjectionMap(sNotesProjectionMap);
                qb.appendWhere(
                        NotePad.Notes._ID +
                                "=" +
                                uri.getPathSegments().get(NotePad.Notes.NOTE_ID_PATH_POSITION));
                break;

            case LIVE_FOLDER_NOTES:
                qb.setTables(NotePad.Notes.TABLE_NAME);
                qb.setProjectionMap(sLiveFolderProjectionMap);
                break;

            case CATEGORIES:
                qb.setTables(NotePad.Categories.TABLE_NAME);
                qb.setProjectionMap(sCategoriesProjectionMap);
                break;

            case CATEGORY_ID:
                qb.setTables(NotePad.Categories.TABLE_NAME);
                qb.setProjectionMap(sCategoriesProjectionMap);
                qb.appendWhere(
                        NotePad.Categories._ID +
                                "=" +
                                uri.getPathSegments().get(NotePad.Categories.CATEGORY_ID_PATH_POSITION));
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        String orderBy;
        if (TextUtils.isEmpty(sortOrder)) {
            switch (sUriMatcher.match(uri)) {
                case CATEGORIES:
                case CATEGORY_ID:
                    orderBy = NotePad.Categories.DEFAULT_SORT_ORDER;
                    break;
                default:
                    orderBy = NotePad.Notes.DEFAULT_SORT_ORDER;
                    break;
            }
        } else {
            orderBy = sortOrder;
        }

        SQLiteDatabase db = mOpenHelper.getReadableDatabase();

        Cursor c = qb.query(
                db,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                orderBy
        );

        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Override
    public String getType(Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case NOTES:
            case LIVE_FOLDER_NOTES:
                return NotePad.Notes.CONTENT_TYPE;

            case NOTE_ID:
                return NotePad.Notes.CONTENT_ITEM_TYPE;

            case CATEGORIES:
                return NotePad.Categories.CONTENT_TYPE;

            case CATEGORY_ID:
                return NotePad.Categories.CONTENT_ITEM_TYPE;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    static ClipDescription NOTE_STREAM_TYPES = new ClipDescription(null,
            new String[] { ClipDescription.MIMETYPE_TEXT_PLAIN });

    @Override
    public String[] getStreamTypes(Uri uri, String mimeTypeFilter) {
        switch (sUriMatcher.match(uri)) {
            case NOTES:
            case LIVE_FOLDER_NOTES:
            case CATEGORIES:
            case CATEGORY_ID:
                return null;

            case NOTE_ID:
                return NOTE_STREAM_TYPES.filterMimeTypes(mimeTypeFilter);

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Override
    public AssetFileDescriptor openTypedAssetFile(Uri uri, String mimeTypeFilter, Bundle opts)
            throws FileNotFoundException {

        String[] mimeTypes = getStreamTypes(uri, mimeTypeFilter);

        if (mimeTypes != null) {
            Cursor c = query(
                    uri,
                    READ_NOTE_PROJECTION,
                    null,
                    null,
                    null
            );

            if (c == null || !c.moveToFirst()) {
                if (c != null) {
                    c.close();
                }
                throw new FileNotFoundException("Unable to query " + uri);
            }

            return new AssetFileDescriptor(
                    openPipeHelper(uri, mimeTypes[0], opts, c, this), 0,
                    AssetFileDescriptor.UNKNOWN_LENGTH);
        }

        return super.openTypedAssetFile(uri, mimeTypeFilter, opts);
    }

    @Override
    public void writeDataToPipe(ParcelFileDescriptor output, Uri uri, String mimeType,
                                Bundle opts, Cursor c) {
        FileOutputStream fout = new FileOutputStream(output.getFileDescriptor());
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(new OutputStreamWriter(fout, "UTF-8"));
            pw.println(c.getString(READ_NOTE_TITLE_INDEX));
            pw.println("");
            pw.println(c.getString(READ_NOTE_NOTE_INDEX));
        } catch (UnsupportedEncodingException e) {
            Log.w(TAG, "Ooops", e);
        } finally {
            c.close();
            if (pw != null) {
                pw.flush();
            }
            try {
                fout.close();
            } catch (IOException e) {
            }
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues initialValues) {
        ContentValues values;

        if (initialValues != null) {
            values = new ContentValues(initialValues);
        } else {
            values = new ContentValues();
        }

        Long now = Long.valueOf(System.currentTimeMillis());

        String tableName;
        Uri contentUri;
        String nullColumnHack;

        switch (sUriMatcher.match(uri)) {
            case NOTES:
                tableName = NotePad.Notes.TABLE_NAME;
                contentUri = NotePad.Notes.CONTENT_ID_URI_BASE;
                nullColumnHack = NotePad.Notes.COLUMN_NAME_NOTE;

                if (values.containsKey(NotePad.Notes.COLUMN_NAME_CREATE_DATE) == false) {
                    values.put(NotePad.Notes.COLUMN_NAME_CREATE_DATE, now);
                }

                if (values.containsKey(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE) == false) {
                    values.put(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, now);
                }

                if (values.containsKey(NotePad.Notes.COLUMN_NAME_TITLE) == false) {
                    Resources r = Resources.getSystem();
                    values.put(NotePad.Notes.COLUMN_NAME_TITLE, r.getString(android.R.string.untitled));
                }

                if (values.containsKey(NotePad.Notes.COLUMN_NAME_NOTE) == false) {
                    values.put(NotePad.Notes.COLUMN_NAME_NOTE, "");
                }

                if (!values.containsKey(NotePad.Notes.COLUMN_NAME_CATEGORY_ID)) {
                    values.putNull(NotePad.Notes.COLUMN_NAME_CATEGORY_ID);
                }
                break;

            case CATEGORIES:
                tableName = NotePad.Categories.TABLE_NAME;
                contentUri = NotePad.Categories.CONTENT_ID_URI_BASE;
                nullColumnHack = null;

                if (values.containsKey(NotePad.Categories.COLUMN_NAME_CREATE_DATE) == false) {
                    values.put(NotePad.Categories.COLUMN_NAME_CREATE_DATE, now);
                }

                if (values.containsKey(NotePad.Categories.COLUMN_NAME_COLOR) == false) {
                    values.put(NotePad.Categories.COLUMN_NAME_COLOR, "#000000");
                }

                if (!values.containsKey(NotePad.Categories.COLUMN_NAME_NAME)) {
                    throw new SQLException("Category name is required");
                }
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        long rowId = db.insert(
                tableName,
                nullColumnHack,
                values
        );

        if (rowId > 0) {
            Uri newUri = ContentUris.withAppendedId(contentUri, rowId);
            getContext().getContentResolver().notifyChange(newUri, null);
            return newUri;
        }

        throw new SQLException("Failed to insert row into " + uri);
    }

    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        String finalWhere;

        int count;

        switch (sUriMatcher.match(uri)) {
            case NOTES:
                count = db.delete(
                        NotePad.Notes.TABLE_NAME,
                        where,
                        whereArgs
                );
                break;

            case NOTE_ID:
                finalWhere =
                        NotePad.Notes._ID +
                                " = " +
                                uri.getPathSegments().
                                        get(NotePad.Notes.NOTE_ID_PATH_POSITION)
                ;

                if (where != null) {
                    finalWhere = finalWhere + " AND " + where;
                }

                count = db.delete(
                        NotePad.Notes.TABLE_NAME,
                        finalWhere,
                        whereArgs
                );
                break;

            case CATEGORIES:
                count = db.delete(
                        NotePad.Categories.TABLE_NAME,
                        where,
                        whereArgs
                );
                break;

            case CATEGORY_ID:
                long categoryId = ContentUris.parseId(uri);
                if (categoryId == 1) {
                    throw new IllegalArgumentException("Cannot delete default category");
                }

                ContentValues values = new ContentValues();
                values.putNull(NotePad.Notes.COLUMN_NAME_CATEGORY_ID);
                db.update(
                        NotePad.Notes.TABLE_NAME,
                        values,
                        NotePad.Notes.COLUMN_NAME_CATEGORY_ID + " = ?",
                        new String[]{String.valueOf(categoryId)}
                );

                finalWhere =
                        NotePad.Categories._ID +
                                " = " +
                                uri.getPathSegments().
                                        get(NotePad.Categories.CATEGORY_ID_PATH_POSITION)
                ;

                if (where != null) {
                    finalWhere = finalWhere + " AND " + where;
                }

                count = db.delete(
                        NotePad.Categories.TABLE_NAME,
                        finalWhere,
                        whereArgs
                );
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);

        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count;
        String finalWhere;

        if (values != null && (sUriMatcher.match(uri) == NOTES || sUriMatcher.match(uri) == NOTE_ID)) {
            values.put(NotePad.Notes.COLUMN_NAME_MODIFICATION_DATE, System.currentTimeMillis());
        }

        switch (sUriMatcher.match(uri)) {
            case NOTES:
                count = db.update(
                        NotePad.Notes.TABLE_NAME,
                        values,
                        where,
                        whereArgs
                );
                break;

            case NOTE_ID:
                String noteId = uri.getPathSegments().get(NotePad.Notes.NOTE_ID_PATH_POSITION);

                finalWhere =
                        NotePad.Notes._ID +
                                " = " +
                                uri.getPathSegments().
                                        get(NotePad.Notes.NOTE_ID_PATH_POSITION)
                ;

                if (where !=null) {
                    finalWhere = finalWhere + " AND " + where;
                }

                count = db.update(
                        NotePad.Notes.TABLE_NAME,
                        values,
                        finalWhere,
                        whereArgs
                );
                break;

            case CATEGORIES:
                count = db.update(
                        NotePad.Categories.TABLE_NAME,
                        values,
                        where,
                        whereArgs
                );
                break;

            case CATEGORY_ID:
                long categoryId = ContentUris.parseId(uri);
                if (categoryId == 1 && values.containsKey(NotePad.Categories.COLUMN_NAME_NAME)) {
                    throw new IllegalArgumentException("Cannot modify default category name");
                }

                finalWhere =
                        NotePad.Categories._ID +
                                " = " +
                                uri.getPathSegments().
                                        get(NotePad.Categories.CATEGORY_ID_PATH_POSITION)
                ;

                if (where !=null) {
                    finalWhere = finalWhere + " AND " + where;
                }

                count = db.update(
                        NotePad.Categories.TABLE_NAME,
                        values,
                        finalWhere,
                        whereArgs
                );
                break;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }

        getContext().getContentResolver().notifyChange(uri, null);

        return count;
    }

    DatabaseHelper getOpenHelperForTest() {
        return mOpenHelper;
    }
}