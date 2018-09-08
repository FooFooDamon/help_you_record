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

import java.util.HashMap;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.SQLException;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.android_assistant.Hint;
import com.android_assistant.Version;

public class CategoryDetailsActivity extends Activity {

    private Menu gMenu = null;

    private DbHelper mDbHelper = null;

    private EditText mEtxName = null;
    private EditText mEtxRemarks = null;

    private DialogInterface.OnClickListener mExitActivity = null;

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details_page);
        getActionBar().setBackgroundDrawable(
            getResources().getDrawable(R.drawable.default_action_bar_style));

        initResources();

        adjustViews();

        Intent intent = getIntent();
        String primaryId = intent.getStringExtra("id");

        setTitle(getString(R.string.category));

        if (Version.SDK <= Version.getDeprecatedVersionUpperBound())
            doExtraJobsForLowerVersions();

        switchEditStatus(false);

        HashMap<String, String> mapDetails = mDbHelper.queryCategoryDetails(primaryId);

        if (null == mapDetails) {
            Hint.alert(this, getString(R.string.db_error),
                getString(R.string.not_found) + ": " + "id = " + primaryId,
                mExitActivity);
        }
        fillDetailContents(mapDetails);
    }

    @Override
    public void onDestroy() {
        if (null != mDbHelper)
            mDbHelper.close();

        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.details_page, menu);
        gMenu = menu;
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (R.id.menu_edit == id) {
            item.setVisible(false);
            gMenu.findItem(R.id.menu_save).setVisible(true);
            gMenu.findItem(R.id.menu_cancel).setVisible(true);
            switchEditStatus(true);
        }
        else if (R.id.menu_save == id) {
            try {
                mDbHelper.updateCategoryDetails(new String[] {
                    mEtxName.getText().toString(),
                    mEtxRemarks.getText().toString(),
                    getIntent().getStringExtra("id")
                });

                Hint.alert(this, R.string.save_successfully, R.string.asking_after_save_operation,
                    mExitActivity, null);
            } catch(SQLException e) {
                Hint.alert(this, getString(R.string.alert_failed_to_update),
                    getString(R.string.alert_checking_input) + e.getMessage());
            }
        }
        else if (R.id.menu_cancel == id) {
            item.setVisible(false);
            gMenu.findItem(R.id.menu_save).setVisible(false);
            gMenu.findItem(R.id.menu_edit).setVisible(true);
            switchEditStatus(false);
        }
        else
            ;

        return super.onOptionsItemSelected(item);
    }

    private void initResources() {
        if (null == mEtxName)
            mEtxName = (EditText) findViewById(R.id.etx_name);

        if (null == mEtxRemarks)
            mEtxRemarks = (EditText) findViewById(R.id.etx_remarks);

        if (null == mDbHelper) {
            mDbHelper = new DbHelper(this);
            mDbHelper.openOrCreate();
        }

        if (null == mExitActivity) {
            mExitActivity = new OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    CategoryDetailsActivity.this.finish();
                }
            };
        }
    }

    private void adjustViews() {
        Spinner spnCategory = (Spinner) findViewById(R.id.spn_category);
        TextView[] textViews = {
            (TextView) findViewById(R.id.txv_category),
            (TextView) findViewById(R.id.txv_brief),
            (TextView) findViewById(R.id.txv_details)
        };
        EditText[] editTexts = {
            (EditText) findViewById(R.id.etx_brief),
            (EditText) findViewById(R.id.etx_details)
        };
        ImageView[] delimeters = {
            (ImageView) findViewById(R.id.imgv_delimeter_name),
            (ImageView) findViewById(R.id.imgv_delimeter_category),
            (ImageView) findViewById(R.id.imgv_delimeter_brief),
            (ImageView) findViewById(R.id.imgv_delimeter_details)
        };

        spnCategory.setVisibility(TextView.GONE);
        for (int i = 0; i < textViews.length; ++i) {
            textViews[i].setVisibility(TextView.GONE);
        }
        for (int i = 0; i < editTexts.length; ++i) {
            editTexts[i].setVisibility(TextView.GONE);
        }
        for (int i = 0; i < delimeters.length; ++i) {
            delimeters[i].setVisibility(TextView.GONE);
        }
    }

    private void doExtraJobsForLowerVersions() {
        TextView[] textViews = {
            (TextView) findViewById(R.id.txv_name),
            (TextView) findViewById(R.id.txv_remarks)
        };

        for (int i = 0; i < textViews.length; ++i) {
            com.android_assistant.TextView.setDefaultTextShadow(textViews[i]);
        }
    }

    private void switchEditStatus(boolean editable) {

        EditText[] editTexts = {
            mEtxName,
            mEtxRemarks
        };

        for (int i = 0; i < editTexts.length; ++i) {
            editTexts[i].setEnabled(editable);
            com.android_assistant.TextView.setDefaultTextShadow(editTexts[i]);
        }
    }

    private void fillDetailContents(HashMap<String, String> mapDetails) {
        if (null == mapDetails)
            return;

        String[] textColumnNames = {
            "name",
            "remarks"
        };

        EditText[] editTexts = {
            mEtxName,
            mEtxRemarks
        };

        for (int i = 0; i < textColumnNames.length; ++i) {
            editTexts[i].setText(mapDetails.get(textColumnNames[i]));
        }
    }
}
