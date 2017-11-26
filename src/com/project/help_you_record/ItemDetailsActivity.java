package com.project.help_you_record;

import java.util.HashMap;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.database.SQLException;
import android.os.Bundle;
import android.text.method.KeyListener;
import android.view.Menu;
import android.view.MenuItem;
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
	
	private KeyListener ORIGINAL_ETX_KEY_LISTENER = null;

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_details_page);
		getActionBar().setBackgroundDrawable(
			getResources().getDrawable(R.drawable.default_action_bar_style));
			
		Intent intent = getIntent();
		String primaryId = intent.getStringExtra("id");
		//String name = intent.getStringExtra("name");
		
		setTitle(getString(R.string.item_details));
		
		if (Version.SDK <= Version.getDeprecatedVersionUpperBound())
			doExtraJobsForLowerVersions();
		
		mEtxName = (EditText) findViewById(R.id.etx_name);
		mEtxBrief = (EditText) findViewById(R.id.etx_brief);
		mEtxDetails = (EditText) findViewById(R.id.etx_details);
		mEtxRemarks = (EditText) findViewById(R.id.etx_remarks);
		
		mSpnCategory = (Spinner) findViewById(R.id.spn_category);
		
		mDbHelper = new DbHelper(this);
		mDbHelper.openOrCreate();
		
		ORIGINAL_ETX_KEY_LISTENER = mEtxName.getKeyListener();
		switchEditStatus(false);
		mDbHelper.fillCategorySpinner(R.id.spn_category);
		
		HashMap<String, String> mapDetails = mDbHelper.queryItemDetails(primaryId);
		
		if (null == mapDetails) {
			Hint.alert(this, getString(R.string.db_error),
				getString(R.string.not_found) + ": " + "id = " + primaryId,
				new OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							ItemDetailsActivity.this.finish();
						}
				});
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
				mDbHelper.updateItemDetails(new String[] {
					mEtxName.getText().toString(),
					String.valueOf(mSpnCategory.getSelectedItemPosition()),
					mEtxBrief.getText().toString(),
					mEtxDetails.getText().toString(),
					mEtxRemarks.getText().toString(),
					getIntent().getStringExtra("id")
				});
				
				Hint.alert(this, R.string.save_successfully, R.string.asking_after_save_operation,
					new OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog, int which) {
								ItemDetailsActivity.this.finish();
							}
					}, null);
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
		
		for (int i = 0; i < editTexts.length; ++i) {
			editTexts[i].setEnabled(editable);
			/*editTexts[i].setEnabled(true);
			editTexts[i].setAutoLinkMask(editable ? 0 : Linkify.ALL);
			editTexts[i].setLinksClickable(!editable);
			editTexts[i].setKeyListener(editable ? ORIGINAL_ETX_KEY_LISTENER : null);*/
			com.android_assistant.TextView.setDefaultTextShadow(editTexts[i]);
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
}
