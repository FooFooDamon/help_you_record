/*
 * Copyright (c) 2017-2018, Wen Xiongchang <udc577 at 126 dot com>
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

import com.android_assistant.Hint;
import com.android_assistant.ResourceExports;
import com.android_assistant.Version;

public class CategoryQueryEntryActivity extends Activity
    implements OnItemClickListener {

    private DbHelper mDbHelper = null;

    private Intent mIntent = null;

    private View.OnClickListener mOnAddButtonClicked = null;
    private DialogInterface.OnClickListener mAddCategoryAction = null;

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_query_page);
        getActionBar().setBackgroundDrawable(
            getResources().getDrawable(R.drawable.default_action_bar_style));
        setTitle(getString(R.string.category));

        initResources();

        adjustViews();

        if (Version.SDK <= Version.getDeprecatedVersionUpperBound())
            doExtraJobsForLowerVersions();

        Button btnAdd = (Button) findViewById(R.id.btn_add);

        btnAdd.setOnClickListener(mOnAddButtonClicked);
    }

    @Override
    protected void onDestroy() {
        if (null != mDbHelper)
            mDbHelper.close();

        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.brief_page, menu);
        for (int i = 0; i < menu.size(); ++i) {
            MenuItem item = menu.getItem(i);

            if (R.id.menu_query == item.getItemId())
                continue;

            item.setVisible(false);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (R.id.menu_query == id)
            queryCategories();
        else if (R.id.menu_export == id)
            ; // TODO: ...
        else
            ; // more things in future ...

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int pos, long id) {
        ListView listView = (ListView) parent;
        ItemBrief item = (ItemBrief) listView.getItemAtPosition(pos);

        mIntent.putExtra("id", item.id);
        mIntent.putExtra("name", item.name);
        startActivity(mIntent);
    }

    private void initResources() {
        if (null == mDbHelper) {
            mDbHelper = new DbHelper(this);
            mDbHelper.openOrCreate();
        }

        if (null == mIntent)
            mIntent = new Intent(this, CategoryDetailsActivity.class);

        if (null == mAddCategoryAction) {
            mAddCategoryAction = new OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String sqlCheckCategory = getString(R.string.sql_query_category_records_by_name);
                    String newCategory = getString(R.string.new_category);
                    String[] sqlArgs = (new String[]{ newCategory });
                    SQLiteDatabase db = mDbHelper.getDatabase();
                    Cursor c = db.rawQuery(sqlCheckCategory, sqlArgs);

                    if (c.moveToNext()) {
                        Hint.alert(CategoryQueryEntryActivity.this, R.string.alert_reusing_category_title,
                            R.string.alert_reusing_category_contents);
                        c.close();
                        return;
                    }
                    c.close();

                    String sqlCategory = getString(R.string.sql_make_categories_data);

                    db.execSQL(sqlCategory, new String[] { newCategory });
                    Hint.alert(CategoryQueryEntryActivity.this, getString(R.string.add_category) + getString(R.string.successful),
                        getString(R.string.hint_after_adding_category));

                    EditText etxName = (EditText) findViewById(R.id.etx_name);

                    etxName.setText(newCategory);
                    queryCategories();
                }
            };
        }

        if (null == mOnAddButtonClicked) {
            mOnAddButtonClicked = new View.OnClickListener() {

                @Override
                public void onClick(View v) {
                    Hint.alert(CategoryQueryEntryActivity.this, R.string.asking_before_adding_category,
                        R.string.confirm_or_cancel_guide, mAddCategoryAction, null);
                }
            };
        }
    }

    private void adjustViews() {
        TextView txvCategory = (TextView) findViewById(R.id.txv_category);
        Spinner spnCategory = (Spinner) findViewById(R.id.spn_category);
        Button btnAdd = (Button)findViewById(R.id.btn_add);
        Button btnRefresh = (Button)findViewById(R.id.btn_refresh);

        txvCategory.setVisibility(TextView.GONE);
        spnCategory.setVisibility(TextView.GONE);
        btnAdd.setText(R.string.add_category);
        btnRefresh.setVisibility(TextView.GONE);
    }

    private void doExtraJobsForLowerVersions() {
        com.android_assistant.TextView.setDefaultTextShadow(
            (TextView) findViewById(R.id.txv_name));
    }

    private void queryCategories() {
        ArrayList<ItemBrief> itemList = new ArrayList<ItemBrief>();
        EditText etxName = (EditText) findViewById(R.id.etx_name);
        String itemName = etxName.getText().toString();
        int sqlResId = ((itemName.length() > 0)
            ? R.string.sql_query_category_records_by_name
            : R.string.sql_query_unlimited_category_records);
        String sql = getString(sqlResId);
        String[] sqlArgs = ((itemName.length() > 0)
            ? (new String[]{ "%" + itemName + "%" })
            : null);
        Cursor c = mDbHelper.getDatabase().rawQuery(sql, sqlArgs);

        while (c.moveToNext()) {
            itemList.add(new ItemBrief(String.valueOf(c.getInt(c.getColumnIndex("category_id"))),
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
