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
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.SQLException;
import android.os.Bundle;
import android.text.method.KeyListener;
import android.text.util.Linkify;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.android_assistant.Hint;
import com.android_assistant.Version;

public class ItemDetailsActivity extends Activity {

    private Menu gMenu = null;

    private DbHelper mDbHelper = null;

    private EditText mEtxName = null;
    private EditText mEtxBrief = null;
    private EditText mEtxDetails = null;
    private EditText mEtxRemarks = null;

    private Spinner mSpnCategory = null;

    private String[] mPageItemValues = {
        null, // name
        null, // category (spinner)
        null, // brief
        null, // details
        null, // remarks
        null // id
    };

    private static final int PAGE_ITEM_INDEX_NAME = 0;
    private static final int PAGE_ITEM_INDEX_CATEGORY = 1;
    private static final int PAGE_ITEM_INDEX_BRIEF = 2;
    private static final int PAGE_ITEM_INDEX_DETAILS = 3;
    private static final int PAGE_ITEM_INDEX_REMARKS = 4;

    // TODO: Should I use one or multiple listener(s)??
    //private KeyListener ORIGINAL_KEY_LISTENER = null;
    private KeyListener[] ORIGINAL_KEY_LISTENERS = {
        null, // name
        null, // category (spinner)
        null, // brief
        null, // details
        null // remarks
    };

    private DialogInterface.OnClickListener mExitActivity = null;

    private View.OnLongClickListener mCrashHandlerForLinkify = new OnLongClickListener() {

        @Override
        public boolean onLongClick(View v) {
            EditText[] candidateEeditTexts = {
                mEtxName,
                null,
                mEtxBrief,
                mEtxDetails,
                mEtxRemarks
            };
            final EditText targetEditText = (EditText)v;
            int index = (int)targetEditText.getTag();
            final boolean FIXED_RETURN_VALUE = true; // DO NOT return false, otherwise it may crash.

            //Hint.alert(ItemDetailsActivity.this, "有特殊格式的内容", currentEditText.getText().toString());

            if (null == candidateEeditTexts[index])
                return FIXED_RETURN_VALUE;

            View dialogView = getLayoutInflater().inflate(R.layout.text_edit_dialog, null);
            final EditText etxDialogContents = (EditText) dialogView.findViewById(R.id.etx_dialog_contents);
            final String initialText = new String(targetEditText.getText().toString());

            etxDialogContents.setText(targetEditText.getText().toString());

            new AlertDialog.Builder(ItemDetailsActivity.this)
                .setTitle(R.string.tmp_dialog_title)
                .setView(dialogView)
                .setPositiveButton(R.string.confirm,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String currentText = etxDialogContents.getText().toString();
                            if (!currentText.equals(initialText))
                                targetEditText.setText(currentText);
                        }
                    })
                .setNegativeButton(R.string.cancal, null)
                .show();

            return FIXED_RETURN_VALUE;
        }
    };

    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details_page);
        getActionBar().setBackgroundDrawable(
            getResources().getDrawable(R.drawable.default_action_bar_style));

        initResources();

        Intent intent = getIntent();
        String primaryId = intent.getStringExtra("id");
        //String name = intent.getStringExtra("name");

        setTitle(getString(R.string.item_details));

        if (Version.SDK <= Version.getDeprecatedVersionUpperBound())
            doExtraJobsForLowerVersions();

        //ORIGINAL_KEY_LISTENER = mEtxName.getKeyListener();
        switchEditStatus(false);
        mDbHelper.fillCategorySpinner(R.id.spn_category);

        HashMap<String, String> mapDetails = mDbHelper.queryItemDetails(primaryId);

        if (null == mapDetails) {
            Hint.alert(this, getString(R.string.db_error),
                getString(R.string.not_found) + ": " + "id = " + primaryId,
                mExitActivity);
        }
        fillDetailContents(mapDetails);

        ImageView[] delimeters = {
            (ImageView) findViewById(R.id.imgv_delimeter_name),
            (ImageView) findViewById(R.id.imgv_delimeter_category),
            (ImageView) findViewById(R.id.imgv_delimeter_brief),
            (ImageView) findViewById(R.id.imgv_delimeter_details)
        };

        for (int i = 0; i < delimeters.length; ++i) {
            delimeters[i].setBackgroundColor(android.graphics.Color.TRANSPARENT);
        }
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
                mPageItemValues[0] = mEtxName.getText().toString();
                mPageItemValues[1] = String.valueOf(mSpnCategory.getSelectedItemPosition());
                mPageItemValues[2] = mEtxBrief.getText().toString();
                mPageItemValues[3] = mEtxDetails.getText().toString();
                mPageItemValues[4] = mEtxRemarks.getText().toString();
                mPageItemValues[5] = getIntent().getStringExtra("id");
                mDbHelper.updateItemDetails(mPageItemValues);

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
        if (null == mEtxName) {
            mEtxName = (EditText) findViewById(R.id.etx_name);
            ORIGINAL_KEY_LISTENERS[PAGE_ITEM_INDEX_NAME] = mEtxName.getKeyListener();
        }

        if (null == mEtxBrief) {
            mEtxBrief = (EditText) findViewById(R.id.etx_brief);
            ORIGINAL_KEY_LISTENERS[PAGE_ITEM_INDEX_BRIEF] = mEtxBrief.getKeyListener();
        }

        if (null == mEtxDetails) {
            mEtxDetails = (EditText) findViewById(R.id.etx_details);
            ORIGINAL_KEY_LISTENERS[PAGE_ITEM_INDEX_DETAILS] = mEtxDetails.getKeyListener();
        }

        if (null == mEtxRemarks) {
            mEtxRemarks = (EditText) findViewById(R.id.etx_remarks);
            ORIGINAL_KEY_LISTENERS[PAGE_ITEM_INDEX_REMARKS] = mEtxRemarks.getKeyListener();
        }

        if (null == mSpnCategory) {
            mSpnCategory = (Spinner) findViewById(R.id.spn_category);
        }

        if (null == mDbHelper) {
            mDbHelper = new DbHelper(this);
            mDbHelper.openOrCreate();
        }

        if (null == mExitActivity) {
            mExitActivity = new OnClickListener() {

                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ItemDetailsActivity.this.finish();
                }
            };
        }
    }

    private void doExtraJobsForLowerVersions() {
        TextView[] textViews = {
            (TextView) findViewById(R.id.txv_name),
            (TextView) findViewById(R.id.txv_category),
            (TextView) findViewById(R.id.txv_brief),
            (TextView) findViewById(R.id.txv_details),
            (TextView) findViewById(R.id.txv_remarks)
        };

        for (int i = 0; i < textViews.length; ++i) {
            com.android_assistant.TextView.setDefaultTextShadow(textViews[i]);
        }
    }

    private void switchEditStatus(boolean editable) {
        mSpnCategory.setEnabled(editable);

        EditText[] editTexts = {
            mEtxName,
            mEtxBrief,
            mEtxDetails,
            mEtxRemarks
        };
        int[] listenerIndexes = {
            0,
            2,
            3,
            4
        };

        for (int i = 0; i < editTexts.length; ++i) {
            String texts = editTexts[i].getText().toString();
            //boolean hasSpecialContents = hasAutoLinkContents(texts, true);

            //editTexts[i].setEnabled(editable); // wrong
            editTexts[i].setEnabled(true);
            editTexts[i].setAutoLinkMask(editable ? 0 : Linkify.ALL);
            //editTexts[i].setLinksClickable(!editable); // seems no use
            editTexts[i].setKeyListener(editable ? ORIGINAL_KEY_LISTENERS[listenerIndexes[i]] : null);
            editTexts[i].setTag(listenerIndexes[i]);
            if (editable)
                editTexts[i].setOnLongClickListener(/*hasSpecialContents ? */mCrashHandlerForLinkify/* : null*/);
            else
                editTexts[i].setOnLongClickListener(null);
            com.android_assistant.TextView.setDefaultTextShadow(editTexts[i]);
            editTexts[i].setText(texts); // sets text again to erase the auto-link style when in edit mode.
        }
    }

    private void fillDetailContents(HashMap<String, String> mapDetails) {
        if (null == mapDetails)
            return;

        mSpnCategory.setSelection(com.android_assistant.Integer.parseInt(
            mapDetails.get("category"), 10, 0));

        String[] textColumnNames = {
            "name",
            "brief",
            "details",
            "remarks"
        };

        EditText[] editTexts = {
            mEtxName,
            mEtxBrief,
            mEtxDetails,
            mEtxRemarks
        };

        for (int i = 0; i < textColumnNames.length; ++i) {
            editTexts[i].setText(mapDetails.get(textColumnNames[i]));
        }
    }

    // TODO: unfinished ...
    private boolean hasAutoLinkContents(String str, boolean returnValueIfFailed) {

        final String[] patterns = {
            "([a-z0-9A-Z]+[-|_|\\.]?)+[a-z0-9A-Z]@([a-z0-9A-Z]+(-[a-z0-9A-Z]+)?\\.)+[a-zA-Z]{2,}", // E-Mail
            "(((13[0-9])|(15([0-3]|[5-9]))|(18[0,5-9]))\\d{8})|(0\\d{2}-\\d{8})|(0\\d{3}-\\d{7})", // Phone number
            "(([A-Za-z0-9-~]+).)+([A-Za-z0-9-~\\/])+(\\?{0,1}(([A-Za-z0-9-~]+\\={0,1})([A-Za-z0-9-~]*)\\&{0,1})*)" // URL
        };

        try {
            for (int i = 0; i < patterns.length; ++i) {
                if (java.util.regex.Pattern.compile(patterns[i]).matcher(str).matches())
                    return true;
            }

            return false;
        }
        catch (Exception e) {
            return returnValueIfFailed;
        }
    }
}
