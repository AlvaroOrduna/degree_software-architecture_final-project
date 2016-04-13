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

package io.ordunaleon.publicappshub.fragment;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import io.ordunaleon.publicappshub.AddCodeActivity;
import io.ordunaleon.publicappshub.R;
import io.ordunaleon.publicappshub.adapter.CodeListAdapter;
import io.ordunaleon.publicappshub.adapter.ImageListAdapter;
import io.ordunaleon.publicappshub.model.PublicAppsHubContract.AppEntry;
import io.ordunaleon.publicappshub.model.PublicAppsHubContract.CodeEntry;
import io.ordunaleon.publicappshub.model.PublicAppsHubContract.ImageEntry;


public class AppDetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor>, View.OnClickListener {

    public static final String ARGS_URI = "args_uri";
    public static final String ARGS_UPDATE_TITLE = "args_update_title";

    private static final int APP_IMAGES_LOADER = 0;
    private static final int APP_CODES_LOADER = 1;

    private Uri mUri;
    private boolean mUpdateTitle;

    private TextView mNameText;
    private TextView mCategoryText;
    private TextView mDescriptionText;
    private TextView mVisualDescriptionText;
    private TextView mCodeText;
    private TextView mServiceText;

    private RecyclerView mImagesRecyclerView;
    private RecyclerView mCodesRecyclerView;
    private RecyclerView mServicesRecyclerView;

    private ImageListAdapter mImageListAdapter;
    private CodeListAdapter mCodeListAdapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_app_detail, container, false);

        Bundle args = getArguments();
        if (args != null && args.containsKey(ARGS_URI)) {
            mUri = args.getParcelable(ARGS_URI);
            mUpdateTitle = args.getBoolean(ARGS_UPDATE_TITLE, false);
        } else {
            return rootView;
        }

        // Get basic info views
        mNameText = (TextView) rootView.findViewById(R.id.app_detail_name);
        mCategoryText = (TextView) rootView.findViewById(R.id.app_detail_category);
        mDescriptionText = (TextView) rootView.findViewById(R.id.app_detail_description);

        // Get visual description views
        mVisualDescriptionText = (TextView) rootView.findViewById(R.id.app_detail_visual_description);
        mImagesRecyclerView = (RecyclerView) rootView.findViewById(R.id.app_detail_images_recyclerview);
        configureVisualDescription();

        // Get code implementations views
        Button codeAddButton = (Button) rootView.findViewById(R.id.app_detail_code_add_button);
        codeAddButton.setOnClickListener(this);
        mCodeText = (TextView) rootView.findViewById(R.id.app_detail_code_text);
        mCodesRecyclerView = (RecyclerView) rootView.findViewById(R.id.app_detail_code_recyclerview);
        configureCodeImplementations();

        // Get service deployment views
        Button serviceAddButton = (Button) rootView.findViewById(R.id.app_detail_service_add_button);
        serviceAddButton.setOnClickListener(this);
        mServiceText = (TextView) rootView.findViewById(R.id.app_detail_service_text);
        mServicesRecyclerView = (RecyclerView) rootView.findViewById(R.id.app_detail_service_recyclerview);
        configureServiceDeployment();

        // Fill views with app data
        Cursor cursor = getActivity().getContentResolver().query(mUri, null, null, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                // Get data form cursor
                String name = cursor.getString(cursor.getColumnIndex(AppEntry.COLUMN_APP_NAME));
                String category = cursor.getString(cursor.getColumnIndex(AppEntry.COLUMN_APP_CATEGORY));
                String description = cursor.getString(cursor.getColumnIndex(AppEntry.COLUMN_APP_DESCRIPTION));

                // Populate view with data obtained from cursor
                mNameText.setText(name);
                mCategoryText.setText(category);
                mDescriptionText.setText(description);

                // Update Activity title if required
                if (mUpdateTitle && name != null) {
                    ActionBar actionBar = ((AppCompatActivity) getActivity()).getSupportActionBar();
                    if (actionBar != null) {
                        actionBar.setTitle(name);
                    }
                }
            }

            cursor.close();
        }

        return rootView;
    }

    private void configureVisualDescription() {
        mImageListAdapter = new ImageListAdapter(getActivity(), null, new ImageListAdapter.OnClickHandler() {
            @Override
            public void onClick(Uri imageUri) {
                ((Callback) getActivity()).onImageSelected(imageUri);
            }
        });

        LinearLayoutManager linearLayoutManager =
                new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false);

        mImagesRecyclerView.setLayoutManager(linearLayoutManager);
        mImagesRecyclerView.setAdapter(mImageListAdapter);

        // Init app's images loader
        getLoaderManager().initLoader(APP_IMAGES_LOADER, null, this);
    }

    private void configureCodeImplementations() {
        mCodeListAdapter = new CodeListAdapter(getActivity(), null, new CodeListAdapter.OnClickHandler() {
            @Override
            public void onClick(Uri codeUri) {
                ((Callback) getActivity()).onCodeSelected(codeUri);
            }
        });

        LinearLayoutManager linearLayoutManager =
                new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false);

        mCodesRecyclerView.setLayoutManager(linearLayoutManager);
        mCodesRecyclerView.setAdapter(mCodeListAdapter);

        // Init app's codes loader
        getLoaderManager().initLoader(APP_CODES_LOADER, null, this);
    }

    private void configureServiceDeployment() {
        // TODO: set RecyclerView layout manager, set RecyclerView adapter and init loader
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case APP_IMAGES_LOADER:
                if (mUri != null) {
                    return new CursorLoader(
                            getActivity(),
                            ImageEntry.CONTENT_URI,
                            null,
                            ImageEntry.COLUMN_IMAGE_APP_KEY + " = ?",
                            new String[]{AppEntry.getAppIdFromUri(mUri)},
                            null);
                }
                break;
            case APP_CODES_LOADER:
                if (mUri != null) {
                    return new CursorLoader(
                            getActivity(),
                            CodeEntry.CONTENT_URI,
                            null,
                            CodeEntry.COLUMN_CODE_APP_KEY + " = ?",
                            new String[]{AppEntry.getAppIdFromUri(mUri)},
                            null);
                }
                break;
        }

        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        switch (loader.getId()) {
            case APP_IMAGES_LOADER:
                if (data != null && data.getCount() > 0) {
                    mVisualDescriptionText.setVisibility(View.GONE);
                    mImageListAdapter.swapCursor(data);
                } else {
                    mVisualDescriptionText.setVisibility(View.VISIBLE);
                }
                break;
            case APP_CODES_LOADER:
                if (data != null && data.getCount() > 0) {
                    mCodeText.setVisibility(View.GONE);
                    mCodeListAdapter.swapCursor(data);
                } else {
                    mCodeText.setVisibility(View.VISIBLE);
                }
                break;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
            case APP_IMAGES_LOADER:
                mImageListAdapter.swapCursor(null);
                break;
            case APP_CODES_LOADER:
                mCodeListAdapter.swapCursor(null);
                break;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.app_detail_code_add_button:
                Intent intent = new Intent(getActivity(), AddCodeActivity.class);
                intent.putExtra(ARGS_URI, mUri);
                startActivity(intent);
                break;
            case R.id.app_detail_service_add_button:
                Toast.makeText(getActivity(),
                        R.string.app_detail_service_add_button,
                        Toast.LENGTH_LONG)
                        .show();
                break;
        }
    }

    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of image
     * selections.
     */
    public interface Callback {
        /**
         * AppDetailFragmentCallback for when an image has been selected.
         */
        void onImageSelected(Uri imageUri);

        /**
         * AppDetailFragmentCallback for when a code has been selected.
         */
        void onCodeSelected(Uri codeUri);
    }
}
