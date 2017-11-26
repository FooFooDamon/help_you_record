/*
 * Copyright (c) 2017, Wen Xiongchang <udc577 at 126 dot com>
 * All rights reserved.
 *
 * This software is provided 'as-is', without any express or implied
 * warranty. In no event will the authors be held liable for any
 * damages arising from the use of this software.
 *
 * Permission is granted to anyone to use this software for any
 * purpose, including commercial applications, and to alter it and
 * redistribute it freely, subject to the following restrictions:
 *
 * 1. The origin of this software must not be misrepresented; you must
 * not claim that you wrote the original software. If you use this
 * software in a product, an acknowledgment in the product documentation
 * would be appreciated but is not required.
 *
 * 2. Altered source versions must be plainly marked as such, and
 * must not be misrepresented as being the original software.
 *
 * 3. This notice may not be removed or altered from any source
 * distribution.
 */

// NOTE: The original author also uses (short/code) names listed below,
//       for convenience or for a certain purpose, at different places:
//       wenxiongchang, wxc, Damon Wen, udc577

package com.project.help_you_record;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.android_assistant.App;
import com.android_assistant.Hint;

public class DbHelper {
	private Context mContext = null;
	private String mDbDir = android.os.Environment.getExternalStorageDirectory().getAbsolutePath();
	private SQLiteDatabase mDbInstance = null;
	
	public DbHelper(Context context) {
		if (null == context)
			throw new NullPointerException();
		
		mContext = context;
	}
	
	public DbHelper(Context context, String dbDirectory) {
		if (null == context)
			throw new NullPointerException();
		
		mContext = context;
		
		File dir = new File(dbDirectory);
		
		if (!dir.exists())
			dir.mkdirs();
		
		mDbDir = dbDirectory;
	}
	
	public void openOrCreate() {
		if (null == mDbInstance) {
			mDbInstance = com.android_assistant.DbHelper.openOrCreate(getDatabaseDirectory(),
					getDatabaseName());
		}
	}
	
	// TODO: finalize() { close(); }
	
	public void close() {
		com.android_assistant.DbHelper.close(mDbInstance);
	}
	
	public SQLiteDatabase getDatabase() {		
		if (null == mDbInstance)
			openOrCreate();
		
		return mDbInstance;
	}
	
	public String getDatabaseName() {
		return App.getAppName(mContext) + ".db";
	}
	
	public String getDatabaseDirectory() {
		// TODO: Get it with SharedPreferences!
		return mDbDir;
	}
	
	public void setDatabaseDirectory(String dir) {
		if (null == dir)
			return;
		
		// TODO: Set it with SharedPreferences!
		mDbDir = dir;
	}
	
	public void init() throws Exception {
		String dbPath = getDatabaseDirectory() + "/" + getDatabaseName();
		File dbFile = new File(dbPath);
		boolean dbExits = dbFile.exists();
		
		if (dbExits) {
			//Hint.shortToast(mContext, dbPath);
			return;
		}
		
		Hint.shortToast(mContext, R.string.hint_db_creating);

		prepareDefaultData();
		
		Hint.longToast(mContext, mContext.getResources().getString(R.string.hint_db_created)
			+ "\n" + dbPath);
	}
	
	public void prepareDefaultData() throws Exception {
		createTable(R.string.sql_create_categories_table);
		makePresetData(R.string.sql_make_categories_data,
			R.array.default_categories, 1, true);
		
		createTable(R.string.sql_create_items_table);
		makePresetData(R.string.sql_make_items_data,
			R.array.default_items, 3, true);
	}
	
	public String[] queryCategoryNames(String firstItem) {
		/*String sql = mContext.getString(R.string.sql_query_category_names);
		Cursor c = getDatabase().rawQuery(sql, null);
		ArrayList<String> results = new ArrayList<String>();
		
		if (null != firstItem)
			results.add(firstItem);
		while (c.moveToNext()) {
			results.add(c.getString(c.getColumnIndex("name")));
		}
		c.close();
		
		if (0 == results.size())
			return null;
		
		return results.toArray(new String[results.size()]);*/
		return queryAllNames("categories", "category_id", firstItem);
	}
	
	public HashMap<String, String> queryCategoryDetails(String category_id) {
		Cursor c = getDatabase().rawQuery(mContext.getString(R.string.sql_query_category_details_by_id),
			new String[] { category_id });
		HashMap<String, String> results = new HashMap<String, String>();
		
		if (!c.moveToNext()) {
			c.close();
			
			return null;
		}
		
		final String[] columnNames = {
			"name", "remarks"
		};
		
		for (int i = 0; i < columnNames.length; ++i) {
			String key = columnNames[i];
			int columnIndex = c.getColumnIndex(key);
			String value = c.getString(columnIndex);
			
			results.put(key, value);
		}
		
		c.close();
		
		return results;
	}
	
	public void fillCategorySpinner(int spinnerResId) {
		Spinner spnCategory = (Spinner) ((Activity)mContext).findViewById(spinnerResId);
		ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(mContext,
			R.drawable.default_spinner_text, queryCategoryNames(mContext.getString(R.string.not_limited)));
		
		spnCategory.setAdapter(spinnerAdapter);
	}
	
	public void updateCategoryDetails(String[] bindArgs) {
		SQLiteDatabase db = getDatabase();
		String sql = mContext.getString(R.string.sql_update_category_details_by_id);
		
		db.execSQL(sql, bindArgs);
	}
	
	public HashMap<String, String> queryItemDetails(String item_id) {
		Cursor c = getDatabase().rawQuery(mContext.getString(R.string.sql_query_item_details_by_id),
			new String[] { item_id });
		HashMap<String, String> results = new HashMap<String, String>();
		
		if (!c.moveToNext()) {
			c.close();
			
			return null;
		}
		
		final String[] columnNames = {
			"name", "category",
			"brief", "details",
			"remarks"
		};
		
		for (int i = 0; i < columnNames.length; ++i) {
			String key = columnNames[i];
			int columnIndex = c.getColumnIndex(key);
			String value = "category".equals(key)
				? String.valueOf(c.getInt(columnIndex))
				: c.getString(columnIndex);
			
			results.put(key, value);
		}
		
		c.close();
		
		return results;
	}
	
	public void updateItemDetails(String[] bindArgs) {
		SQLiteDatabase db = getDatabase();
		String sql = mContext.getString(R.string.sql_update_item_details_by_id);
		
		db.execSQL(sql, bindArgs);
	}
	
	// NOTE: This method should be used to tables with a small quantity of data!
	private String[] queryAllNames(String table, String primaryId, String firstItem) {
		String sql = "select name from `"
			+ table + "`"
			+ " order by " + primaryId + " asc";
		Cursor c = getDatabase().rawQuery(sql, null);
		ArrayList<String> results = new ArrayList<String>();
		
		if (null != firstItem)
			results.add(firstItem);
		while (c.moveToNext()) {
			results.add(c.getString(c.getColumnIndex("name")));
		}
		c.close();
		
		if (0 == results.size())
			return null;
		
		return results.toArray(new String[results.size()]);
	}
	
	private void createTable(String createSql) {
		getDatabase().execSQL(createSql);
	}
	
	private void createTable(int createSqlResId) {
		createTable(mContext.getResources().getString(createSqlResId));
	}
	
	private void makePresetData(String sqlString, int valuesArrayResId,
		int bindArgsCount, boolean enablesTransaction) throws Exception {
		String dbError = mContext.getResources().getString(R.string.db_error);
		
		if (valuesArrayResId > 0) {
			if (bindArgsCount <= 0) {
				Hint.alert(mContext, dbError,
					sqlString + "\n\nbindArgsCount = " + String.valueOf(bindArgsCount));
				return;
			}
			
			String[] bindArgValues = mContext.getResources().getStringArray(valuesArrayResId);
			int actualArgCount = bindArgValues.length;
			
			if (0 != actualArgCount % bindArgsCount) {
				Hint.alert(mContext, dbError,
					sqlString + "\n\nactualArgCount(" + String.valueOf(actualArgCount) + ")"
					+ " % bindArgsCount(" + String.valueOf(bindArgsCount) + ") != 0");
				return;
			}
			
			SQLiteDatabase db = getDatabase();
			String args[] = new String[bindArgsCount];
			
			if (!enablesTransaction) {
				for (int i = 0; i < actualArgCount; i += bindArgsCount) {
					for (int j = 0; j < bindArgsCount; ++j) {
						args[j] = bindArgValues[i + j];
					}
					
					db.execSQL(sqlString, args);
				}
				
				return;
			}
			
			db.beginTransaction();
			try {
				for (int i = 0; i < actualArgCount; i += bindArgsCount) {
					for (int j = 0; j < bindArgsCount; ++j) {
						args[j] = bindArgValues[i + j];
					}
					
					db.execSQL(sqlString, args);
				}
				
				db.setTransactionSuccessful();
			} catch (Exception e) {
				throw e;
			} finally {
				db.endTransaction();
			}
			
			return;
		}
		
		getDatabase().execSQL(sqlString);
	}
	
	private void makePresetData(int sqlStringResId, int valuesArrayResId,
		int bindArgsCount, boolean enablesTransaction) throws Exception {
		String sql = mContext.getResources().getString(sqlStringResId);
		
		makePresetData(sql, valuesArrayResId, bindArgsCount, enablesTransaction);
	}
}
