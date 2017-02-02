package com.lambdasoup.blockvote;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class StatsProvider extends ContentProvider {

	public static final String AUTHORITY = BuildConfig.APPLICATION_ID;

	public static final Uri CONTENT_URI = new Uri.Builder().authority(AUTHORITY).scheme("content").appendEncodedPath(Stats.TABLE_NAME).build();

	private static final int STATS = 1;

	private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

	static {
		uriMatcher.addURI(AUTHORITY, Stats.TABLE_NAME + "/", STATS);
	}

	// state
	private DbHelper dbHelper;

	@Override
	public boolean onCreate() {
		dbHelper = new DbHelper(getContext());
		return true;
	}

	@Nullable
	@Override
	public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		return null;
	}

	@Nullable
	@Override
	public String getType(@NonNull Uri uri) {
		throw new UnsupportedOperationException("getType is not supported");
	}

	private Context context() {
		Context context = getContext();
		if (context == null) {
			throw new RuntimeException("cannot acquire context before #onCreate");
		}
		return context;
	}

	@Nullable
	@Override
	public Uri insert(@NonNull Uri uri, ContentValues values) {
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		upsert(db, Stats.TABLE_NAME, values, Stats.ID);
		db.close();
		context().getContentResolver().notifyChange(uri, null);
		return uri;
	}

	@Override
	public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
		throw new UnsupportedOperationException("delete is not supported");
	}

	// upsert as per variant 'update-first', from http://stackoverflow.com/a/418988/470509
	private void upsert(@NonNull SQLiteDatabase db, @NonNull String table, @NonNull ContentValues values, @NonNull String idCol) {
		try {
			db.beginTransaction();
			String   where = idCol + " = ?";
			String[] args  = {values.getAsString(idCol)};
			int      rows  = db.update(table, values, where, args);
			if (rows == 0) {
				long inserted = db.insert(table, null, values);
				if (inserted == -1) {
					throw new SQLException("failed to insert row into '" + table + "' - see logcat.");
				}
			}
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
		}
	}


	@Override
	public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		throw new UnsupportedOperationException("update is not supported. use <insert> with PUT semantics");
	}

	static class DbHelper extends SQLiteOpenHelper {

		static final int    DATABASE_VERSION = 1;
		static final String DATABASE_NAME    = "stats.db";

// @formatter:off

		private static final String SQL_CREATE_STATS =
				"CREATE TABLE " + Stats.TABLE_NAME + " ("
						+ Stats.ID     + " TEXT PRIMARY KEY NOT NULL,"
						+ Stats.D30    + " REAL NOT NULL,"
						+ Stats.D7     + " REAL NOT NULL,"
						+ Stats.D1     + " REAL NOT NULL"
						+ ");";

		private static final String SQL_DELETE_STATS     = "DROP TABLE IF EXISTS " + Stats.TABLE_NAME;

// @formatter:on

		DbHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		public void onCreate(SQLiteDatabase db) {
			db.execSQL(SQL_CREATE_STATS);
		}

		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			onRecreate(db);
		}

		public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			onRecreate(db);
		}

		private void onRecreate(SQLiteDatabase db) {
			db.execSQL(SQL_DELETE_STATS);
			onCreate(db);
		}
	}
}