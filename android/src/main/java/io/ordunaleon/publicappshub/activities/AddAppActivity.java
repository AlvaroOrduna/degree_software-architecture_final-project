/*
 * Copyright (C) 2016 Álvaro Orduna León
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.ordunaleon.publicappshub.activities;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.parse.ParseException;

import java.util.ArrayList;

import io.ordunaleon.publicappshub.R;
import io.ordunaleon.publicappshub.adapter.AddAppScreenshotListAdapter;
import io.ordunaleon.publicappshub.model.App;

public class AddAppActivity extends AppCompatActivity implements App.StoreCallback {

    private static final String LOG_TAG = "AddAppActivity";

    public static final int REQUEST_READ_EXTERNAL_STORAGE = 1;

    private static final int IMAGE_PICKER_REQUEST = 0;
    private static final String MIME_TYPE_IMAGE = "image/*";

    private CoordinatorLayout mLayout;
    private EditText mName;
    private EditText mDescription;
    private RadioGroup mCategoryRadioGroup;
    private Button mAddScreenshotButton;
    private TextView mScreenshotCount;
    private RecyclerView mScreenshotList;
    private FloatingActionButton mDoneButton;

    private ProgressDialog mProgressDialog;

    private AddAppScreenshotListAdapter mScreenshotListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_app);

        // Lookup all the views
        mLayout = (CoordinatorLayout) findViewById(R.id.add_coordinatorlayout);
        mName = (EditText) mLayout.findViewById(R.id.add_app_name);
        mDescription = (EditText) mLayout.findViewById(R.id.add_app_description);
        mCategoryRadioGroup = (RadioGroup) mLayout.findViewById(R.id.add_app_category_group);
        mScreenshotCount = (TextView) mLayout.findViewById(R.id.add_app_screenshot_count);
        mAddScreenshotButton = (Button) mLayout.findViewById(R.id.add_app_screenshot_add);
        mScreenshotList = (RecyclerView) mLayout.findViewById(R.id.add_app_screenshot_recyclerview);
        mDoneButton = (FloatingActionButton) mLayout.findViewById(R.id.add_done_fab);

        // Get screenshot list adapter if exists. If not, instantiate a new one.
        mScreenshotListAdapter = (AddAppScreenshotListAdapter) getLastCustomNonConfigurationInstance();
        if (mScreenshotListAdapter == null) {
            mScreenshotListAdapter = new AddAppScreenshotListAdapter(new ArrayList<Uri>(),
                    new AddAppScreenshotListAdapter.OnLongClickHandler() {
                        @Override
                        public boolean onLongClick(Uri uri) {
                            removeScreenshot(uri);
                            return true;
                        }
                    });
        }

        // Set name and description listeners
        mName.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) isNameValid(false);
            }
        });
        mDescription.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) isDescriptionValid(false);
            }
        });

        // Set add screenshot button listener
        mAddScreenshotButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int readPermission = ActivityCompat.checkSelfPermission(AddAppActivity.this,
                        Manifest.permission.READ_EXTERNAL_STORAGE);

                if (readPermission == PackageManager.PERMISSION_GRANTED) {
                    showImagePicker();
                } else {
                    ActivityCompat.requestPermissions(AddAppActivity.this,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            REQUEST_READ_EXTERNAL_STORAGE);
                }
            }
        });

        // Set screenshot recycler view adapter and layout manager
        mScreenshotList.setAdapter(mScreenshotListAdapter);
        mScreenshotList.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        // Set done button listener
        mDoneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isFormValid()) {
                    mDoneButton.setClickable(false);
                    storeNewData();
                }
            }
        });

        // Create ProgressDialog to show new app store progress
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setTitle(getString(R.string.add_app_progress_dialog));
        mProgressDialog.setMax(100);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setCancelable(false);
        mProgressDialog.setIndeterminate(false);

        if (savedInstanceState == null) {
            updateScreenshotCount();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        // On configuration change, retain screenshot list adapter
        return mScreenshotListAdapter;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_READ_EXTERNAL_STORAGE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission has been granted. Show activity to pick an image.
                showImagePicker();
            } else {
                // Permission has been denied. Inform the user with a SnackBar.
                Snackbar.make(mLayout, R.string.add_app_permission_denied, Snackbar.LENGTH_LONG)
                        .show();
            }
        }

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void showImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType(MIME_TYPE_IMAGE);
        startActivityForResult(intent, IMAGE_PICKER_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == IMAGE_PICKER_REQUEST && resultCode == RESULT_OK) {
            addScreenshot(data.getData());
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    /**
     * Add screenshot's Uri to the array
     *
     * @param uri Uri to be added to the array
     */
    private void addScreenshot(final Uri uri) {
        mScreenshotListAdapter.add(uri);
        updateScreenshotCount();
    }

    /**
     * Removes screenshot's Uri from the array
     *
     * @param uri Uri to be removed.
     */
    private void removeScreenshot(final Uri uri) {
        mScreenshotListAdapter.remove(uri);
        updateScreenshotCount();
    }

    /**
     * Updates screenshot count text
     */
    private void updateScreenshotCount() {
        int c = mScreenshotListAdapter.getItemCount();
        String text = getResources().getQuantityString(R.plurals.add_app_screenshot_count, c, c);
        mScreenshotCount.setText(text);
    }

    /**
     * Check all necessary fields when submitting new app data
     *
     * @return Boolean indicating whether the data is valid
     */
    private boolean isFormValid() {
        return isNameValid(true) && isDescriptionValid(true);
    }

    private boolean isNameValid(boolean showSnackBar) {
        String name = mName.getText().toString();
        if (!name.isEmpty()) {
            return true;
        }

        if (showSnackBar) {
            Snackbar.make(mLayout, R.string.add_app_name_error_empty, Snackbar.LENGTH_LONG).show();
        }

        mName.setError(getString(R.string.add_app_name_error_empty));
        return false;
    }

    private boolean isDescriptionValid(boolean showSnackBar) {
        String description = mDescription.getText().toString();
        if (!description.isEmpty()) {
            return true;
        }

        if (showSnackBar) {
            Snackbar.make(mLayout, R.string.add_app_name_error_empty, Snackbar.LENGTH_LONG).show();
        }

        mDescription.setError(getString(R.string.add_app_description_error_empty));
        return false;
    }

    /**
     * Stores new data in the database
     */
    private void storeNewData() {
        // Get app name
        String name = mName.getText().toString();

        // Get app category
        int buttonId = mCategoryRadioGroup.getCheckedRadioButtonId();
        String category = null;
        switch (buttonId) {
            case R.id.add_app_category_education:
                category = getString(R.string.add_app_category_education);
                break;
            case R.id.add_app_category_health:
                category = getString(R.string.add_app_category_health);
                break;
            case R.id.add_app_category_transportation:
                category = getString(R.string.add_app_category_transportation);
                break;
        }

        // Get app description
        String descriptionText = mDescription.getText().toString();

        // Instantiate new app
        App app = new App(name, category, descriptionText, null);

        // Add all screenshots to app
        for (Uri screenshotUri : mScreenshotListAdapter.getItems()) {
            if (!app.addScreenshotFromUri(this, screenshotUri)) {
                Snackbar.make(mLayout, R.string.add_app_read_screenshot_error, Snackbar.LENGTH_LONG)
                        .show();
            }
        }

        // Store app data
        mProgressDialog.show();
        app.store(this);
    }

    @Override
    public void onStoreFinish() {
        mProgressDialog.dismiss();
        onBackPressed();
    }

    @Override
    public void onStoreProgress(Integer donePercentage) {
        mProgressDialog.setProgress(donePercentage);
    }

    @Override
    public void onStoreError(ParseException e) {
        Log.e(LOG_TAG, e.getMessage(), e);

        Snackbar.make(mLayout, getString(R.string.add_app_upload_error, e.getMessage()), Snackbar.LENGTH_INDEFINITE)
                .setAction(android.R.string.ok, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                    }
                }).show();

        mProgressDialog.dismiss();
        mDoneButton.setClickable(true);
    }
}
