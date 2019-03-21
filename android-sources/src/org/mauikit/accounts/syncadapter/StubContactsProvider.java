package org.mauikit.accounts.syncadapter;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

public class StubContactsProvider extends ContentProvider {
    private final static String TAG = "ContactsProvider";

    @Override
    public boolean onCreate() {
      return true;
    }

    @Override
    public String getType(Uri uri) {
      Log.d(TAG, "getType: " + uri);
      return null;
    }

    @Override
    public Cursor query(
            Uri uri,
            String[] projection,
            String selection,
            String[] selectionArgs,
            String sortOrder) {
      Log.d(TAG, "getType: " + uri + ", " + projection + ", " + selection + ", " + selectionArgs + ", " + sortOrder);
      return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
      Log.d(TAG, "insert: " + uri + ", " + values);
      return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
      Log.d(TAG, "delete: " + uri + ", " + selection + selectionArgs);
      return 0;
    }

    @Override
    public int update(
            Uri uri,
            ContentValues values,
            String selection,
            String[] selectionArgs) {
      Log.d(TAG, "update: " + uri + ", " + values + selection + selectionArgs);
      return 0;
    }
}
