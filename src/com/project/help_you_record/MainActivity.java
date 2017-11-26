package com.project.help_you_record;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import com.android_assistant.App;
import com.android_assistant.Hint;
import com.android_assistant.ResourceExports;
import com.android_assistant.Version;

public class MainActivity extends Activity
	implements OnItemClickListener {
	
	private DbHelper mDbHelper = null;
	
	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_query_page);
		getActionBar().setBackgroundDrawable(
			getResources().getDrawable(R.drawable.default_action_bar_style));
		
		mDbHelper = new DbHelper(this);
		
		try {
			// WARNING: DO NOT use it here, otherwise init() will make a wrong decision
			//     about initializing database.
			//mDbHelper.openOrCreate();
			
			mDbHelper.init();
		} catch (Exception e) {
			Hint.alert(this, getString(R.string.data_init_error), e.getMessage(), new OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					MainActivity.this.finish();
				}
			});
		}
		
		mDbHelper.fillCategorySpinner(R.id.spn_category);
		
		if (Version.SDK <= Version.getDeprecatedVersionUpperBound())
			doExtraJobsForLowerVersions();
		
		Button btnAdd = (Button) findViewById(R.id.btn_add);
		
		btnAdd.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				String sqlCheckItem = getString(R.string.sql_query_items_by_name);
				String newItem = getString(R.string.new_item);
				String[] sqlArgs = (new String[]{ newItem });
				SQLiteDatabase db = mDbHelper.getDatabase();
				Cursor c = db.rawQuery(sqlCheckItem, sqlArgs);
				
				if (c.moveToNext()) {
					Hint.alert(MainActivity.this, R.string.alert_reusing_item_title, R.string.alert_reusing_item_contents);
					c.close();
					return;
				}
				c.close();
				
				String sqlAddItem = getString(R.string.sql_make_items_data);
				
				db.execSQL(sqlAddItem, new String[] { newItem, String.valueOf(0), "" });
				Hint.alert(MainActivity.this, getString(R.string.add_item) + getString(R.string.successful),
					getString(R.string.hint_after_adding_item));
			}
		});
		
		Button btnRefresh = (Button) findViewById(R.id.btn_refresh);
		
		btnRefresh.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mDbHelper.fillCategorySpinner(R.id.spn_category);
				Hint.shortToast(MainActivity.this, getString(R.string.refresh_category)
					+ getString(R.string.successful));
			}
		});
	}
	
	@Override
	protected void onDestroy() {
		if (null != mDbHelper)
			mDbHelper.close();
		
		App.cancelNotification(this);
		super.onDestroy();
	}
	
	@Override
	protected void onPause() {
		if (App.isInBackground(this)) {
			String appName = App.getAppName(this);

			App.displayNotification(this, appName, getString(R.string.click_to_restore),
				appName + getString(R.string.running_in_background), R.drawable.ic_launcher);
		}
		super.onPause();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		App.cancelNotification(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.brief_page, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		
		if (R.id.menu_query == id)
			queryItems();
		else if (R.id.menu_export == id)
			; // TODO: ...
		else if (R.id.menu_edit_category == id)
			startActivity(new Intent(this, CategoryQueryEntryActivity.class));
		else if (R.id.menu_settings == id){
			Hint.alert(this, ResourceExports.getString(this, R.array.function_not_implemented),
				getString(R.string.settings_not_implemented));
			// startActivity(new Intent(this, SettingsActivity.class));
		}
		else if (R.id.menu_about == id)
			startActivity(new Intent(this, AboutActivity.class));
		else if (R.id.menu_help == id)
			App.showHelpText(this, getString(R.string.help_info_for_main_page));
		else if (R.id.menu_terms_of_note == id)
			Hint.alert(this, R.string.terms_of_note, R.string.terms_of_note_contents);
		else if (R.id.menu_copyright == id)
			Hint.alert(this, getString(R.string.copyright),
				getString(R.string.copyright_info) + "\n\n"
				+ getString(R.string.cht_copyright_info) + "\n\n"
				+ getString(R.string.en_copyright_info));
		else if (R.id.menu_version_log == id)
			Hint.alert(this, R.string.version_log, R.string.version_log_contents);
		else if (R.id.menu_exit == id)
			App.exit(this);
		else
			; // more things in future ...
		
		return super.onOptionsItemSelected(item);
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

		if (KeyEvent.KEYCODE_BACK != keyCode)
			return super.onKeyDown(keyCode, event);

		App.moveTaskToBack(this, App.getAppName(this), true, R.drawable.ic_launcher);

		return true;
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
		ListView listView = (ListView) parent;
		ItemBrief item = (ItemBrief) listView.getItemAtPosition(pos);
		final Intent intent = new Intent(this, ItemDetailsActivity.class);
		
		intent.putExtra("id", item.id);
		intent.putExtra("name", item.name);
		startActivity(intent);
	}

	private void doExtraJobsForLowerVersions() {
		com.android_assistant.TextView.setDefaultTextShadow(
			(TextView) findViewById(R.id.txv_name));
		
		com.android_assistant.TextView.setDefaultTextShadow(
			(TextView) findViewById(R.id.txv_category));
	}
	
	private void queryItems() {
		ArrayList<ItemBrief> itemList = new ArrayList<ItemBrief>();
		EditText etxName = (EditText) findViewById(R.id.etx_name);
		String itemName = etxName.getText().toString();
		Spinner spnCategory = (Spinner) findViewById(R.id.spn_category);
		int selectedCategoryPos = spnCategory.getSelectedItemPosition();
		int sqlResId = (selectedCategoryPos > 0)
			? ((itemName.length() > 0)
				? R.string.sql_query_items_by_name_and_category
				: R.string.sql_query_items_by_category)
			: ((itemName.length() > 0)
				? R.string.sql_query_items_by_name
				: R.string.sql_query_unlimited_items);
		String sql = getString(sqlResId);
		String[] sqlArgs = (selectedCategoryPos > 0)
			? ((itemName.length() > 0)
				? (new String[]{ "%" + itemName + "%", String.valueOf(selectedCategoryPos)})
				: (new String[]{ String.valueOf(selectedCategoryPos) }) )
			: ((itemName.length() > 0)
				? (new String[]{ "%" + itemName + "%" })
				: null);
		Cursor c = mDbHelper.getDatabase().rawQuery(sql, sqlArgs);
		
		while (c.moveToNext()) {
			itemList.add(new ItemBrief(String.valueOf(c.getInt(c.getColumnIndex("item_id"))),
				c.getString(c.getColumnIndex("name"))));
		}
		c.close();
		
		int resultCount = itemList.size();
		
		if (resultCount <= 0)
			Hint.alert(this, getString(R.string.target_item) + " " + getString(R.string.not_found),
				getString(R.string.hint_add_when_not_found));
		
		Hint.shortToast(this, ResourceExports.getString(this, R.array.query_result)
			+ ResourceExports.getString(this, R.array.quantity)
			+ ": " + resultCount);
		
		ListView lsvQueryResult = (ListView) findViewById(R.id.lsv_query_items);
		ItemBriefAdapter adapter = new ItemBriefAdapter(this, itemList);

		lsvQueryResult.setAdapter(adapter);
		lsvQueryResult.setOnItemClickListener(this);
	}
	
	private class ItemBrief {
		public String id;
		public String name;

		public ItemBrief() {}

		public ItemBrief(String id, String name) {
			this.id = id;
			this.name = name;
		}
	}
	
	private class ItemBriefAdapter extends BaseAdapter {

		private final Context mContext;
		private final List<ItemBrief> mItemList;
		private final LayoutInflater mInflater;

		public ItemBriefAdapter(Context context, List<ItemBrief> itemList) {
			super();
			this.mItemList = itemList;
			this.mContext = context;
			mInflater = LayoutInflater.from(context);
		}

		@Override
		public int getCount() {
			return mItemList.size();
		}

		@Override
		public Object getItem(int position) {
			return mItemList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(final int position, View convertView,
				ViewGroup parent) {

			ViewHolder holder = null;

			if (convertView == null) {
				holder = new ViewHolder();
				convertView = mInflater.inflate(R.layout.brief, null);
				holder.checkBox = (CheckBox) convertView.findViewById(R.id.chkbox_brief);
				holder.id = (TextView) convertView.findViewById(R.id.txv_brief_id);
				holder.name = (TextView) convertView.findViewById(R.id.txv_brief_name);
				convertView.setTag(holder);
			}
			else {
				holder = (ViewHolder) convertView.getTag();
			}

			ItemBrief item = mItemList.get(position);
			
			holder.checkBox.setVisibility(TextView.GONE);

			holder.id.setText(item.id);
			com.android_assistant.TextView.setDefaultTextShadow(holder.id);
			holder.id.setLineSpacing(0, 1.5F);
			holder.id.setVisibility(TextView.GONE);
			
			holder.name.setText(item.name);
			com.android_assistant.TextView.setDefaultTextShadow(holder.name);
			holder.name.setLineSpacing(0, 1.5F);

			return convertView;
		}

		private class ViewHolder {
			CheckBox checkBox;
			TextView id;
			TextView name;
		}
	}
}
