/*
 * Copyright 2017 mh@lambdasoup.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lambdasoup.blockvote;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.support.annotation.NonNull;

public class ConfigProvider extends ContentProvider {

	private static final String AUTHORITY = "com.lambdasoup.blockvote.config";

	public static final Uri CONTENT_URI = new Uri.Builder().authority(AUTHORITY).scheme("content").appendEncodedPath(Config.TABLE_NAME).build();

	private DbHelper dbHelper;

	@Override
	public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
		throw new UnsupportedOperationException("Not supported");
	}

	@Override
	public String getType(@NonNull Uri uri) {
		throw new UnsupportedOperationException("Not supported");
	}

	@Override
	public Uri insert(@NonNull Uri uri, ContentValues values) {
		SQLiteDatabase db = dbHelper.getWritableDatabase();
		db.insertWithOnConflict(Config.TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
		return ContentUris.withAppendedId(uri, values.getAsInteger(Config.COMPLICATION_ID));
	}

	@Override
	public boolean onCreate() {
		dbHelper = new DbHelper(getContext());
		return true;
	}

	@Override
	public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		SQLiteDatabase     db           = dbHelper.getReadableDatabase();
		SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
		queryBuilder.setTables(Config.TABLE_NAME);
		return queryBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder);
	}

	@Override
	public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		throw new UnsupportedOperationException("Not supported - use insert");
	}

	enum Period {
		D1, D7, D30
	}

	static class Config {
		static final String TABLE_NAME = "config";

		static final String COMPLICATION_ID = "complication_id";
		static final String CANDIDATE       = "candidate";
		static final String PERIOD          = "period";
	}

	static class DbHelper extends SQLiteOpenHelper {

		static final int    DATABASE_VERSION = 2;
		static final String DATABASE_NAME    = "config.db";

// @formatter:off

		private static final String SQL_CREATE_CONFIG =
				"CREATE TABLE " + Config.TABLE_NAME + " ("
						+ Config.COMPLICATION_ID + " INTEGER PRIMARY KEY NOT NULL,"
						+ Config.CANDIDATE       + " TEXT NOT NULL,"
						+ Config.PERIOD          + " TEXT NOT NULL"
						+ ");";

		private static final String SQL_DELETE_CONFIG     = "DROP TABLE IF EXISTS " + Config.TABLE_NAME;

// @formatter:on

		DbHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		public void onCreate(SQLiteDatabase db) {
			db.execSQL(SQL_CREATE_CONFIG);
		}

		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			onRecreate(db);
		}

		public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			onRecreate(db);
		}

		private void onRecreate(SQLiteDatabase db) {
			db.execSQL(SQL_DELETE_CONFIG);
			onCreate(db);
		}
	}
}
