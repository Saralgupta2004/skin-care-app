package com.example.myapplication;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.HashMap;
import java.util.Map;

public class ImageDatabaseHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_IMAGES = "images";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_IMAGE = "image_data";
    private static final String CREATE_TABLE = "CREATE TABLE " + TABLE_IMAGES + " (" +
            COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_IMAGE + " BLOB)";

    private static final Map<String, ImageDatabaseHelper> instances = new HashMap<>();

    // Factory method to get a per-user instance
    public static synchronized ImageDatabaseHelper getInstance(Context context, String username) {
        if (!instances.containsKey(username)) {
            String dbName = "ImageDatabase_" + username + ".db";
            instances.put(username, new ImageDatabaseHelper(context.getApplicationContext(), dbName));
        }
        return instances.get(username);
    }

    private ImageDatabaseHelper(Context context, String dbName) {
        super(context, dbName, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // For now, simply drop and recreate
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_IMAGES);
        onCreate(db);
    }

    public void insertImage(byte[] imageData) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_IMAGE, imageData);
        db.insert(TABLE_IMAGES, null, values);
        db.close();
    }

    public byte[] getLastImage() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_IMAGES, new String[]{COLUMN_IMAGE}, null, null, null, null, COLUMN_ID + " DESC", "1");
        byte[] image = null;
        if (cursor.moveToFirst()) {
            image = cursor.getBlob(0);
        }
        cursor.close();
        db.close();
        return image;
    }

    public byte[] getSecondLastImage() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_IMAGES, new String[]{COLUMN_IMAGE}, null, null, null, null, COLUMN_ID + " DESC", "2");
        byte[] image = null;
        if (cursor.moveToLast()) {
            image = cursor.getBlob(0);
        }
        cursor.close();
        db.close();
        return image;
    }
}
