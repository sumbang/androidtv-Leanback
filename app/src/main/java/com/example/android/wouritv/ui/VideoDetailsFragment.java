/*
 * Copyright (c) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.wouritv.ui;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.RequiresApi;
import androidx.leanback.app.BackgroundManager;
import androidx.leanback.app.DetailsSupportFragment;
import androidx.leanback.widget.*;
import androidx.core.app.ActivityOptionsCompat;
import androidx.loader.app.LoaderManager;
import androidx.core.content.ContextCompat;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.android.wouritv.R;
import com.example.android.wouritv.Utils;
import com.example.android.wouritv.config.HttpQuery;
import com.example.android.wouritv.data.VideoContract;
import com.example.android.wouritv.model.Contenu;
import com.example.android.wouritv.model.Movie;
import com.example.android.wouritv.model.Video;
import com.example.android.wouritv.model.VideoCursorMapper;
import com.example.android.wouritv.presenter.CardPresenter;
import com.example.android.wouritv.presenter.DetailsDescriptionPresenter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import static android.content.Context.MODE_PRIVATE;

/*
 * VideoDetailsFragment extends DetailsFragment, a Wrapper fragment for leanback details screens.
 * It shows a detailed view of video and its metadata plus related videos.
 */
public class VideoDetailsFragment extends DetailsSupportFragment
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int NO_NOTIFICATION = -1;
    private static final int ACTION_WATCH_TRAILER = 1;
    private static final int ACTION_RENT = 2;
    private static final int ACTION_BUY = 3;

    private static final String TAG = "VideoDetailsFragment";

    // ID for loader that loads related videos.
    private static final int RELATED_VIDEO_LOADER = 1;

    // ID for loader that loads the video from global search.
    private int mGlobalSearchVideoId = 2;

    private Movie mSelectedVideo;
    private ArrayObjectAdapter mAdapter;
    private ClassPresenterSelector mPresenterSelector;
    private BackgroundManager mBackgroundManager;
    private Drawable mDefaultBackground;
    private DisplayMetrics mMetrics;
    private CursorObjectAdapter mVideoCursorAdapter;
    private FullWidthDetailsOverviewSharedElementHelper mHelper;
    private final VideoCursorMapper mVideoCursorMapper = new VideoCursorMapper();

    SharedPreferences sharedPreferences;
    private static final String PREFS = "PREFS";
    private static final String PREFS_USER = "PREFS_USER";
    private static final String PREFS_NAME = "PREFS_NAME";
    private static final String PREFS_PINCODE = "PIN_CODE";
    public String datafinale = "";

    private static final int ACTION_BANDE_ANNONCE = 1;
    private static final int ACTION_ABONNER = 2;
    private static final int ACTION_PAYER = 3;
    private static final int ACTION_CODE = 4;
    private static final int ACTION_VOIR = 5;
    private static final int ACTION_EPISODE = 6;

    private static final int DETAIL_THUMB_WIDTH = 285;
    private static final int DETAIL_THUMB_HEIGHT = 428;

    private static final int NUM_COLS = 10;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPreferences = getActivity().getBaseContext().getSharedPreferences(PREFS, MODE_PRIVATE);

        if (sharedPreferences.contains(PREFS_USER) ) {

            String iduser2 = sharedPreferences.getString(PREFS_USER, "");

            if(isConnected()) new GetCode().execute("getcode",iduser2);

        }

        prepareBackgroundManager();
        mVideoCursorAdapter = new CursorObjectAdapter(new CardPresenter());
        mVideoCursorAdapter.setMapper(mVideoCursorMapper);

        mSelectedVideo = (Movie) getActivity().getIntent()
                .getSerializableExtra(VideoDetailsActivity.VIDEO);

        datafinale = getActivity().getIntent().getStringExtra(VideoDetailsActivity.DATA);

        if (mSelectedVideo != null || !hasGlobalSearchIntent()) {

            removeNotification(getActivity().getIntent()
                    .getIntExtra(VideoDetailsActivity.NOTIFICATION_ID, NO_NOTIFICATION));

            setupAdapter();

            setupDetailsOverviewRow();

            setupMovieListRow();

            updateBackground(mSelectedVideo.getBackgroundImageUrl());

            // When a Related Video item is clicked.
            setOnItemViewClickedListener(new ItemViewClickedListener());
        }

        else {
            Intent intent = new Intent(getActivity(), MainActivity.class);
            startActivity(intent);
        }
    }

    public boolean isConnected(){
        ConnectivityManager connMgr = (ConnectivityManager) getActivity().getSystemService(Activity.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected())
            return true;
        else
            return false;
    }

    private void removeNotification(int notificationId) {
        if (notificationId != NO_NOTIFICATION) {
            NotificationManager notificationManager = (NotificationManager) getActivity()
                    .getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.cancel(notificationId);
        }
    }

    @Override
    public void onStop() {
        mBackgroundManager.release();
        super.onStop();
    }

    /**
     * Check if there is a global search intent. If there is, load that video.
     */
    private boolean hasGlobalSearchIntent() {
        Intent intent = getActivity().getIntent();
        String intentAction = intent.getAction();
        String globalSearch = getString(R.string.global_search);

        if (globalSearch.equalsIgnoreCase(intentAction)) {
            Uri intentData = intent.getData();
            String videoId = intentData.getLastPathSegment();

            Bundle args = new Bundle();
            args.putString(VideoContract.VideoEntry._ID, videoId);
            getLoaderManager().initLoader(mGlobalSearchVideoId++, args, this);
            return true;
        }
        return false;
    }

    private void prepareBackgroundManager() {
        mBackgroundManager = BackgroundManager.getInstance(getActivity());
        mBackgroundManager.attach(getActivity().getWindow());
        mDefaultBackground = getResources().getDrawable(R.drawable.default_background, null);
        mMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(mMetrics);
    }

    private void updateBackground(String uri) {
        RequestOptions options = new RequestOptions()
                .centerCrop()
                .error(mDefaultBackground);

        Glide.with(getActivity())
                .asBitmap()
                .load(uri)
                .apply(options)
                .into(new SimpleTarget<Bitmap>(mMetrics.widthPixels, mMetrics.heightPixels) {
                    @Override
                    public void onResourceReady(
                            Bitmap resource,
                            Transition<? super Bitmap> transition) {
                        mBackgroundManager.setBitmap(resource);
                    }
                });
    }

    private void setupAdapter() {
        // Set detail background and style.

        FullWidthDetailsOverviewRowPresenter detailsPresenter =
                new FullWidthDetailsOverviewRowPresenter(new DetailsDescriptionPresenter(),
                        new MovieDetailsOverviewLogoPresenter());

        detailsPresenter.setBackgroundColor(
                ContextCompat.getColor(getActivity(), R.color.selected_background));

        detailsPresenter.setInitialState(FullWidthDetailsOverviewRowPresenter.STATE_HALF);

        // Hook up transition element.
        mHelper = new FullWidthDetailsOverviewSharedElementHelper();
        mHelper.setSharedElementEnterTransition(getActivity(),
                VideoDetailsActivity.SHARED_ELEMENT_NAME);
        detailsPresenter.setListener(mHelper);
        detailsPresenter.setParticipatingEntranceTransition(false);
        prepareEntranceTransition();

        detailsPresenter.setOnActionClickedListener(new OnActionClickedListener() {
            @Override
            public void onActionClicked(Action action) {

                sharedPreferences = getActivity().getBaseContext().getSharedPreferences(PREFS, getActivity().MODE_PRIVATE);

                if (action.getId() == ACTION_VOIR) {

                    if (sharedPreferences.contains(PREFS_USER) ) {

                        // check de la validite du pin code en cours d'utilisation

                        String pincode = sharedPreferences.getString(PREFS_PINCODE, "");

                        String iduser = sharedPreferences.getString(PREFS_USER, ""); String data = "";

                        try {

                            String data2 = new ValidPin().execute("getcode2",pincode).get();

                            if(data2.equals("1")) {

                                data = new HaveAccess().execute("haveaccess", iduser, "" + mSelectedVideo.getId()).get();

                                if (data != null) {

                                    JSONObject jsonObject = new JSONObject(data);

                                    if (jsonObject.getString("retour").equals("ok")) {

                                        String content = mSelectedVideo.getTitle() + " - " + mSelectedVideo.getVideoUrl() + " - " + mSelectedVideo.getVideoprovider();

                                        Log.e("DATA", content);

                                     /*   Intent intent = new Intent(getActivity(), PlaybackOverlayActivity.class);

                                        intent.putExtra(DetailsActivity.MOVIE, mSelectedMovie);

                                        intent.putExtra("position", "0");

                                        intent.putExtra("donnee", datafinale);

                                        getActivity().startActivity(intent); */

                                        Intent intent = new Intent(getActivity(), PlaybackActivity.class);
                                        intent.putExtra(VideoDetailsActivity.VIDEO, mSelectedVideo);
                                        intent.putExtra("position", "0");
                                        intent.putExtra("donnee", datafinale);
                                        startActivity(intent);

                                    }

                                    else if (jsonObject.getString("retour").equals("nok")) {

                                        Toast.makeText(getActivity(), "Veuillez vous abonner pour voir ce film", Toast.LENGTH_SHORT).show();
                                    }

                                    else if (jsonObject.getString("retour").equals("nok2")) {

                                        Toast.makeText(getActivity(), "Veuillez payer ce film pour le voir.", Toast.LENGTH_SHORT).show();
                                    }

                                }

                            }

                            else {

                                Toast.makeText(getActivity(), "Votre PIN de connexion n'est plus valide, votre session sera fermée", Toast.LENGTH_SHORT).show();

                                if (sharedPreferences.contains(PREFS_USER) ) {

                                    sharedPreferences.edit().clear().commit();
                                }
                            }

                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }


                    }

                    else {

                        Toast.makeText(getActivity(), "Veuillez vous identifier pour voir ce film", Toast.LENGTH_SHORT).show();

                    }


                }

                else  if (action.getId() == ACTION_EPISODE) {

                    String contenu = mSelectedVideo.getContent(); String select = action.getLabel1().toString();

                    Log.e("SERIES","2 -"+contenu);

                    int currentp = 0;

                    if(!contenu.isEmpty()) {

                        try {

                            JSONArray jsonArray = new JSONArray(contenu);

                            for (int i = 0; i < jsonArray.length(); i++){

                                JSONObject current = jsonArray.getJSONObject(i);

                                if(select.equals(current.getString("titre"))) currentp = i;

                            }

                        } catch (JSONException e) {

                        }

                    }

                    Log.e("SERIES","3 - "+select+" - "+currentp+" - "+mSelectedVideo.getId());

                    // si connecte

                    if (sharedPreferences.contains(PREFS_USER) ) {

                        // check de la validite du pin code en cours d'utilisation

                        String pincode = sharedPreferences.getString(PREFS_PINCODE, "");

                        try {

                            String data2 = new ValidPin().execute("getcode2",pincode).get();

                            if(data2.equals("1")) {

                                String iduser = sharedPreferences.getString(PREFS_USER, "");
                                String data = "";

                                data = new HaveAccess1().execute("haveaccess1", iduser,"" + mSelectedVideo.getId()).get();
                                if (data != null) {

                                    Log.e("DATA",data);

                                    JSONObject jsonObject = new JSONObject(data);

                                    if (jsonObject.getString("retour").equals("ok")) {

                                        //String content = mSelectedMovie.getTitle() + " - " + mSelectedMovie.getContent();

                                        //  Log.e("SERIES", content);

                                     /*   Intent intent = new Intent(getActivity(), PlaybackOverlayActivity.class);

                                        intent.putExtra(DetailsActivity.MOVIE, mSelectedMovie);

                                        intent.putExtra("position", "" + currentp);

                                        intent.putExtra("donnee", datafinale);

                                        getActivity().startActivity(intent); */

                                        Intent intent = new Intent(getActivity(), PlaybackActivity.class);
                                        intent.putExtra(VideoDetailsActivity.VIDEO, mSelectedVideo);
                                        intent.putExtra("position", ""+currentp);
                                        intent.putExtra("donnee", datafinale);
                                        startActivity(intent);

                                    }

                                    else if (jsonObject.getString("retour").equals("nok")) {

                                        Toast.makeText(getActivity(), "Veuillez vous abonner pour voir ce feuilleton", Toast.LENGTH_SHORT).show();
                                    }

                                    else if (jsonObject.getString("retour").equals("nok2")) {

                                        Toast.makeText(getActivity(), "Veuillez payer ce feuilleton pour le voir", Toast.LENGTH_SHORT).show();
                                    }


                                }

                            }

                            else {

                                Toast.makeText(getActivity(), "Votre PIN de connexion n'est plus valide, votre session sera fermée", Toast.LENGTH_SHORT).show();

                                if (sharedPreferences.contains(PREFS_USER) ) {

                                    sharedPreferences.edit().clear().commit();
                                }
                            }

                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    else {

                        Toast.makeText(getActivity(), "Veuillez vous identifier pour voir ce film", Toast.LENGTH_SHORT).show();
                    }

                }

                else {
                    Toast.makeText(getActivity(), action.toString(), Toast.LENGTH_SHORT).show();
                }
            }
        });

        mPresenterSelector = new ClassPresenterSelector();
        mPresenterSelector.addClassPresenter(DetailsOverviewRow.class, detailsPresenter);
        mPresenterSelector.addClassPresenter(ListRow.class, new ListRowPresenter());
        mAdapter = new ArrayObjectAdapter(mPresenterSelector);
        setAdapter(mAdapter);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case RELATED_VIDEO_LOADER: {
                String category = args.getString(VideoContract.VideoEntry.COLUMN_CATEGORY);
                return new CursorLoader(
                        getActivity(),
                        VideoContract.VideoEntry.CONTENT_URI,
                        null,
                        VideoContract.VideoEntry.COLUMN_CATEGORY + " = ?",
                        new String[]{category},
                        null
                );
            }
            default: {
                // Loading video from global search.
                String videoId = args.getString(VideoContract.VideoEntry._ID);
                return new CursorLoader(
                        getActivity(),
                        VideoContract.VideoEntry.CONTENT_URI,
                        null,
                        VideoContract.VideoEntry._ID + " = ?",
                        new String[]{videoId},
                        null
                );
            }
        }

    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor != null && cursor.moveToNext()) {
            switch (loader.getId()) {
                case RELATED_VIDEO_LOADER: {
                    mVideoCursorAdapter.changeCursor(cursor);
                    break;
                }
                default: {
                    // Loading video from global search.
                    mSelectedVideo = (Movie) mVideoCursorMapper.convert(cursor);

                    setupAdapter();
                    setupDetailsOverviewRow();
                    setupMovieListRow();
                    updateBackground(mSelectedVideo.getBackgroundImageUrl());

                    // When a Related Video item is clicked.
                    setOnItemViewClickedListener(new ItemViewClickedListener());
                }
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mVideoCursorAdapter.changeCursor(null);
    }

    static class MovieDetailsOverviewLogoPresenter extends DetailsOverviewLogoPresenter {

        static class ViewHolder extends DetailsOverviewLogoPresenter.ViewHolder {
            public ViewHolder(View view) {
                super(view);
            }

            public FullWidthDetailsOverviewRowPresenter getParentPresenter() {
                return mParentPresenter;
            }

            public FullWidthDetailsOverviewRowPresenter.ViewHolder getParentViewHolder() {
                return mParentViewHolder;
            }
        }

        @Override
        public Presenter.ViewHolder onCreateViewHolder(ViewGroup parent) {

            ImageView imageView = (ImageView) LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.lb_fullwidth_details_overview_logo, parent, false);

            Resources res = parent.getResources();
            int width = DETAIL_THUMB_WIDTH;
            int height = DETAIL_THUMB_HEIGHT;
            imageView.setLayoutParams(new ViewGroup.MarginLayoutParams(width, height));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            return new ViewHolder(imageView);
        }

        @Override
        public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
            DetailsOverviewRow row = (DetailsOverviewRow) item;
            ImageView imageView = ((ImageView) viewHolder.view);

            imageView.setImageDrawable(row.getImageDrawable());


            if (isBoundToImage((ViewHolder) viewHolder, row)) {
                MovieDetailsOverviewLogoPresenter.ViewHolder vh =
                        (MovieDetailsOverviewLogoPresenter.ViewHolder) viewHolder;
                vh.getParentPresenter().notifyOnBindLogo(vh.getParentViewHolder());
            }
        }
    }

    private void setupDetailsOverviewRow() {

     /*   final DetailsOverviewRow row = new DetailsOverviewRow(mSelectedVideo);

        RequestOptions options = new RequestOptions()
                .error(R.drawable.default_background)
                .dontAnimate();

        Glide.with(getActivity())
                .asBitmap()
                .load(mSelectedVideo.getCardImageUrl())
                .apply(options)
                .into(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(
                            Bitmap resource,
                            Transition<? super Bitmap> transition) {
                        row.setImageBitmap(getActivity(), resource);
                        startEntranceTransition();
                    }
                });

        SparseArrayObjectAdapter adapter = new SparseArrayObjectAdapter();

        adapter.set(ACTION_WATCH_TRAILER, new Action(ACTION_WATCH_TRAILER, getResources()
                .getString(R.string.watch_trailer_1),
                getResources().getString(R.string.watch_trailer_2)));
        adapter.set(ACTION_RENT, new Action(ACTION_RENT, getResources().getString(R.string.rent_1),
                getResources().getString(R.string.rent_2)));
        adapter.set(ACTION_BUY, new Action(ACTION_BUY, getResources().getString(R.string.buy_1),
                getResources().getString(R.string.buy_2)));
        row.setActionsAdapter(adapter);

        mAdapter.add(row); */

        Log.e(TAG, "doInBackground: " + mSelectedVideo.toString());

        final DetailsOverviewRow row = new DetailsOverviewRow(mSelectedVideo);

        row.setImageDrawable(getResources().getDrawable(R.drawable.default_background));

        int width = Utils.convertDpToPixel(getActivity().getApplicationContext(), DETAIL_THUMB_WIDTH);

        int height = Utils.convertDpToPixel(getActivity().getApplicationContext(), DETAIL_THUMB_HEIGHT);

        RequestOptions options = new RequestOptions()
                .error(R.drawable.default_background)
                .dontAnimate();

        Glide.with(getActivity())
                .asBitmap()
                .load(mSelectedVideo.getCardImageUrl())
                .apply(options)
                .into(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(
                            Bitmap resource,
                            Transition<? super Bitmap> transition) {
                        row.setImageBitmap(getActivity(), resource);
                        startEntranceTransition();
                    }
                });

        // creation des options de menu pour la video

        // si c'est un film
        if(mSelectedVideo.getType().equals("0")){

            row.addAction(new Action(ACTION_VOIR, "VOIR LE FILM", ""));

        }

        // si c'est une serie

        else {

            //row.addAction(new Action(ACTION_BANDE_ANNONCE, "BANDE ANNONCE", ""));

            String contenu = mSelectedVideo.getContent();

            if(!contenu.isEmpty()) {

                try {

                    JSONArray jsonArray = new JSONArray(contenu);

                    for (int i = 0; i < jsonArray.length(); i++){

                        JSONObject current = jsonArray.getJSONObject(i);

                        Contenu contenu1 = new Contenu();

                        contenu1.setId(current.getInt("id")); contenu1.setTitre(current.getString("titre"));

                        contenu1.setProvider(current.getString("provider")); contenu1.setVideo(current.getString("content"));

                        row.addAction(new Action(ACTION_EPISODE, current.getString("titre"), ""));

                    }

                } catch (JSONException e) {

                }

            }

        }

        mAdapter.add(row);
    }

    private void setupMovieListRow() {

        /*String subcategories[] = {getString(R.string.related_movies)};

        // Generating related video list.
        String category = mSelectedVideo.getCategory();

        Bundle args = new Bundle();
        args.putString(VideoContract.VideoEntry.COLUMN_CATEGORY, category);
        getLoaderManager().initLoader(RELATED_VIDEO_LOADER, args, this);

        HeaderItem header = new HeaderItem(0, subcategories[0]);
        mAdapter.add(new ListRow(header, mVideoCursorAdapter));

        */

        Log.e(TAG, "doInBackground: " + mSelectedVideo.toString());

        final DetailsOverviewRow row = new DetailsOverviewRow(mSelectedVideo);

        row.setImageDrawable(getResources().getDrawable(R.drawable.default_background));

        int width = Utils.convertDpToPixel(getActivity().getApplicationContext(), DETAIL_THUMB_WIDTH);

        int height = Utils.convertDpToPixel(getActivity().getApplicationContext(), DETAIL_THUMB_HEIGHT);

        Drawable mDefaultCardImage = getResources().getDrawable(R.drawable.movie, null);

        Glide.with(getActivity())
                .asBitmap()
                .load(mSelectedVideo.getCardImageUrl())
                .apply(RequestOptions.errorOf(mDefaultCardImage))
                .into(new SimpleTarget<Bitmap>(width, height) {
                    @Override
                    public void onResourceReady(
                            Bitmap resource,
                            Transition<? super Bitmap> transition) {
                        mBackgroundManager.setBitmap(resource);
                    }
                });


        // creation des options de menu pour la video

        // si c'est un film
        if(mSelectedVideo.getType().equals("0")){

            row.addAction(new Action(ACTION_VOIR, "VOIR LE FILM", ""));

        }

        // si c'est une serie

        else {

            //row.addAction(new Action(ACTION_BANDE_ANNONCE, "BANDE ANNONCE", ""));

            String contenu = mSelectedVideo.getContent();

            if(!contenu.isEmpty()) {

                try {

                    JSONArray jsonArray = new JSONArray(contenu);

                    for (int i = 0; i < jsonArray.length(); i++){

                        JSONObject current = jsonArray.getJSONObject(i);

                        Contenu contenu1 = new Contenu();

                        contenu1.setId(current.getInt("id")); contenu1.setTitre(current.getString("titre"));

                        contenu1.setProvider(current.getString("provider")); contenu1.setVideo(current.getString("content"));

                        row.addAction(new Action(ACTION_EPISODE, current.getString("titre"), ""));

                    }

                } catch (JSONException e) {

                }

            }

        }

        mAdapter.add(row);
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (item instanceof Video) {
                Video video = (Video) item;
                Intent intent = new Intent(getActivity(), VideoDetailsActivity.class);
                intent.putExtra(VideoDetailsActivity.VIDEO, video);

                Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                        getActivity(),
                        ((ImageCardView) itemViewHolder.view).getMainImageView(),
                        VideoDetailsActivity.SHARED_ELEMENT_NAME).toBundle();
                getActivity().startActivity(intent, bundle);
            }
        }
    }

    private class GetCode extends AsyncTask<String, Void, String> {
        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        protected String doInBackground(String... args) {

            HttpQuery httpQuery = new HttpQuery();

            try {
                return httpQuery.getcode2(args[0],args[1]);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }

        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {

            if(result != null) {

                Log.e("DATA Serie","Le resultat de la recherche est : "+result);

                try {

                    JSONObject jsonObject = new JSONObject(result);

                    if(jsonObject.has("data")) {

                    }

                    else {

                        if(jsonObject.getString("status").equals("0")) {

                        }

                        else if(jsonObject.getString("status").equals("2")) {

                            sharedPreferences.edit().clear().commit();
                        }

                        else {

                        }


                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        protected void onPreExecute() {

            // Things to be done before execution of long running operation. For
            // example showing ProgessDialog

            Log.e("DATA","Connexion au serveur ...");

        }
    }

    private class ValidPin extends AsyncTask<String, Void, String> {
        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        protected String doInBackground(String... args) {

            HttpQuery httpQuery = new HttpQuery();

            try {
                return httpQuery.getcode3(args[0],args[1]);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }

        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {


        }

        @Override
        protected void onPreExecute() {

            // Things to be done before execution of long running operation. For
            // example showing ProgessDialog

            Log.e("DATA","Connexion au serveur ...");

        }
    }

    private class HaveAccess extends AsyncTask<String, Void, String> {
        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        protected String doInBackground(String... args) {

            HttpQuery httpQuery = new HttpQuery();

            try {
                return httpQuery.haveaccess(args[0],args[1],args[2]);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }

        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {


        }

        @Override
        protected void onPreExecute() {

            // Things to be done before execution of long running operation. For
            // example showing ProgessDialog

            Log.e("DATA","Connexion au serveur ...");

        }
    }

    private class HaveAccess1 extends AsyncTask<String, Void, String> {
        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        protected String doInBackground(String... args) {

            HttpQuery httpQuery = new HttpQuery();

            try {
                return httpQuery.haveaccess2(args[0],args[1],args[2]);
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }

        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {


        }

        @Override
        protected void onPreExecute() {

            // Things to be done before execution of long running operation. For
            // example showing ProgessDialog

            Log.e("DATA","Connexion au serveur ...");

        }
    }
}
