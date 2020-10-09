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
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.RequiresApi;
import androidx.leanback.app.BackgroundManager;
import androidx.leanback.app.BrowseSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.CursorObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ImageCardView;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.OnItemViewSelectedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.PresenterSelector;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import androidx.core.app.ActivityOptionsCompat;
import androidx.loader.app.LoaderManager;
import androidx.core.content.ContextCompat;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.android.wouritv.R;
import com.example.android.wouritv.config.HttpQuery;
import com.example.android.wouritv.data.FetchVideoService;
import com.example.android.wouritv.data.VideoContract;
import com.example.android.wouritv.model.Movie;
import com.example.android.wouritv.model.MovieList;
import com.example.android.wouritv.model.Video;
import com.example.android.wouritv.model.VideoCursorMapper;
import com.example.android.wouritv.presenter.CardPresenter;
import com.example.android.wouritv.presenter.CardPresenter1;
import com.example.android.wouritv.presenter.GridItemPresenter;
import com.example.android.wouritv.presenter.IconHeaderItemPresenter;
import com.example.android.wouritv.recommendation.UpdateRecommendationsService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static android.content.Context.MODE_PRIVATE;

/*
 * Main class to show BrowseFragment with header and rows of videos
 */
public class MainFragment extends BrowseSupportFragment
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int BACKGROUND_UPDATE_DELAY = 300;
    private final Handler mHandler = new Handler();
    private ArrayObjectAdapter mCategoryRowAdapter;
    private Drawable mDefaultBackground;
    private DisplayMetrics mMetrics;
    private Runnable mBackgroundTask;
    private Uri mBackgroundURI;
    private BackgroundManager mBackgroundManager;
    private LoaderManager mLoaderManager;
    private static final int CATEGORY_LOADER = 123; // Unique ID for Category Loader.

    SharedPreferences sharedPreferences;
    private static final String PREFS = "PREFS";
    private static final String PREFS_USER = "PREFS_USER";
    private static final String PREFS_NAME = "PREFS_NAME";

    private static final int GRID_ITEM_WIDTH = 285;
    private static final int GRID_ITEM_HEIGHT = 428;
    private static final int NUM_ROWS = 2;
    private static final int NUM_COLS = 15;
    public String datafinale1 = "",datafinale2 = "", datafinale3 = "";

    // Maps a Loader Id to its CursorObjectAdapter.
    private Map<Integer, CursorObjectAdapter> mVideoCursorAdapters;

   /* @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        // Create a list to contain all the CursorObjectAdapters.
        // Each adapter is used to render a specific row of videos in the MainFragment.
        mVideoCursorAdapters = new HashMap<>();

        // Start loading the categories from the database.
        mLoaderManager = LoaderManager.getInstance(this);
        mLoaderManager.initLoader(CATEGORY_LOADER, null, this);
    } */

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        // Final initialization, modifying UI elements.
        super.onActivityCreated(savedInstanceState);

        sharedPreferences = getActivity().getBaseContext().getSharedPreferences(PREFS, MODE_PRIVATE);

        if (sharedPreferences.contains(PREFS_USER) ) {

            String iduser2 = sharedPreferences.getString(PREFS_USER, "");

            if(isConnected()) new GetCode().execute("getcode",iduser2);

        }

        // Prepare the manager that maintains the same background image between activities.
        prepareBackgroundManager();

        setupUIElements();

        loadRows();

        setupEventListeners();
        //prepareEntranceTransition();

        // Map category results from the database to ListRow objects.
        // This Adapter is used to render the MainFragment sidebar labels.
       // mCategoryRowAdapter = new ArrayObjectAdapter(new ListRowPresenter());
       // setAdapter(mCategoryRowAdapter);

      //  updateRecommendations();
    }

    @Override
    public void onDestroy() {
        mHandler.removeCallbacks(mBackgroundTask);
        mBackgroundManager = null;
        super.onDestroy();
    }

    @Override
    public void onStop() {
        mBackgroundManager.release();
        super.onStop();
    }

    private void prepareBackgroundManager()                                                                 {
        mBackgroundManager = BackgroundManager.getInstance(getActivity());
        mBackgroundManager.attach(getActivity().getWindow());
        mDefaultBackground = getResources().getDrawable(R.drawable.default_background, null);
        mBackgroundTask = new UpdateBackgroundTask();
        mMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(mMetrics);
    }

    private void setupUIElements() {
        setBadgeDrawable(
                getActivity().getResources().getDrawable(R.drawable.logo, null));
        setTitle(getString(R.string.browse_title)); // Badge, when set, takes precedent over title
        setHeadersState(HEADERS_ENABLED);
        setHeadersTransitionOnBackEnabled(true);

        // Set fastLane (or headers) background color
        setBrandColor(ContextCompat.getColor(getActivity(), R.color.fastlane_background));

        // Set search icon color.
        setSearchAffordanceColor(ContextCompat.getColor(getActivity(), R.color.search_opaque));

        setHeaderPresenterSelector(new PresenterSelector() {
            @Override
            public Presenter getPresenter(Object o) {
                return new IconHeaderItemPresenter();
            }
        });
    }

    private void setupEventListeners() {
        setOnSearchClickedListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), SearchActivity.class);
                startActivity(intent);
            }
        });

        setOnItemViewClickedListener(new ItemViewClickedListener());
        setOnItemViewSelectedListener(new ItemViewSelectedListener());
    }

    private void updateBackground(String uri) {
        int width = mMetrics.widthPixels;
        int height = mMetrics.heightPixels;

        RequestOptions options = new RequestOptions()
                .centerCrop()
                .error(mDefaultBackground);

        Glide.with(getActivity())
                .asBitmap()
                .load(uri)
                .apply(options)
                .into(new SimpleTarget<Bitmap>(width, height) {
                    @Override
                    public void onResourceReady(
                            Bitmap resource,
                            Transition<? super Bitmap> transition) {
                        mBackgroundManager.setBitmap(resource);
                    }
                });
    }

    private void startBackgroundTimer() {
        mHandler.removeCallbacks(mBackgroundTask);
        mHandler.postDelayed(mBackgroundTask, BACKGROUND_UPDATE_DELAY);
    }

    private void updateRecommendations() {
        Intent recommendationIntent = new Intent(getActivity(), UpdateRecommendationsService.class);
        getActivity().startService(recommendationIntent);
    }

    public boolean isConnected(){
        ConnectivityManager connMgr = (ConnectivityManager) getActivity().getSystemService(Activity.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected())
            return true;
        else
            return false;
    }

    private void loadRows() {

        //  List<Movie> list = MovieList.setupMovies();

        Toast.makeText(getActivity(),"Chargement des donées...",Toast.LENGTH_LONG).show();

        mCategoryRowAdapter = new ArrayObjectAdapter(new ListRowPresenter());

        CardPresenter1 cardPresenter = new CardPresenter1();

        int i;

        for (i = 0; i < NUM_ROWS; i++) {

            List<Movie> list = new ArrayList<Movie>();

            if (i != 0) {
                Collections.shuffle(list);
            }


            ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(cardPresenter);

            String data = ""; String cat = ""; String type = "";

            if(isConnected()) try {

                if(i == 0)  {
                    data = new AllFilm().execute("allfilm","1").get(); cat = "1"; type = "0"; datafinale1 = data;
                }

                else if(i == 1) {
                    data = new AllSerie().execute("allserie").get(); cat = "1"; type = "1"; datafinale2 = data;
                }

                /*else if(i == 2) {
                   data = new AllSerie().execute("capsule").get(); cat = "2"; type="1"; datafinale3 = data;
               }*/

                if(data != null){

                    Log.e("DATA",data);

                    JSONObject jsonObject = new JSONObject(data);

                    if (jsonObject.getString("error").equals("false")) {

                        JSONArray liste = jsonObject.getJSONArray("message");

                        for (int a = 0; a < liste.length(); a++) {


                            JSONObject current = (JSONObject) liste.get(a);

                            String studio = "";

                            if(current.getInt("premium") == 1) {

                                studio = "Premium"; String lienfilm = "";

                                if (type.equals("0")) {

                                    lienfilm = current.getString("linkfilm").replace("\\", "");

                                }

                                String cardImageUrl = "https://www.wouri.tv/images/" + current.getString("couverture");

                                String bgImageUrl = "https://www.wouri.tv/images/" + current.getString("couverture2");

                                Movie movie = new Movie();
                                movie.setId(current.getInt("id"));
                                Movie.incCount();
                                movie.setTitle(current.getString("titre"));
                                movie.setDescription(current.getString("description"));
                                movie.setStudio(studio);
                                movie.setCategory(cat);
                                movie.setType(type);
                                movie.setCardImageUrl(cardImageUrl);
                                movie.setBackgroundImageUrl(bgImageUrl);
                                movie.setContent(current.getString("content"));
                                movie.setVideoprovider(current.getString("providercontent"));
                                movie.setBaprovider(current.getString("providerba"));
                                if (type.equals("0"))
                                {
                                    movie.setVideoUrl(lienfilm);
                                }
                                else movie.setVideoUrl("");
                                movie.setBaUrl("");

                                list.add(movie);

                            }

                        }

                    }

                }

            } catch (InterruptedException e) {

                e.printStackTrace();
            } catch (ExecutionException e) {

                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }


            for (int j = 0; j < list.size(); j++) {

                listRowAdapter.add(list.get(j));
            }

            HeaderItem header = new HeaderItem(i, MovieList.MOVIE_CATEGORY[i]);

            mCategoryRowAdapter.add(new ListRow(header, listRowAdapter));

        }

        HeaderItem gridHeader = new HeaderItem(i, "PARAMETRES");

        GridItemPresenter1 mGridPresenter = new GridItemPresenter1();

        ArrayObjectAdapter gridRowAdapter = new ArrayObjectAdapter(mGridPresenter);
        gridRowAdapter.add(getResources().getString(R.string.refresh));
        gridRowAdapter.add(getResources().getString(R.string.account));

        mCategoryRowAdapter.add(new ListRow(gridHeader, gridRowAdapter));

        setAdapter(mCategoryRowAdapter);

    }

    private class GridItemPresenter1 extends Presenter {
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent) {
            TextView view = new TextView(parent.getContext());
            view.setLayoutParams(new ViewGroup.LayoutParams(GRID_ITEM_WIDTH, GRID_ITEM_HEIGHT));
            view.setFocusable(true);
            view.setFocusableInTouchMode(true);
            view.setBackgroundColor(getResources().getColor(R.color.default_background));
            view.setTextColor(Color.WHITE);
            view.setTextSize(14);
            view.setGravity(Gravity.CENTER);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder viewHolder, Object item) {
            ((TextView) viewHolder.view).setText((String) item);
        }

        @Override
        public void onUnbindViewHolder(ViewHolder viewHolder) {
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {

        if (id == CATEGORY_LOADER) {

            return new CursorLoader(
                    getContext(),
                    VideoContract.VideoEntry.CONTENT_URI, // Table to query
                    new String[]{"DISTINCT " + VideoContract.VideoEntry.COLUMN_CATEGORY},
                    // Only categories
                    null, // No selection clause
                    null, // No selection arguments
                    null  // Default sort order
            );

        } else {

            // Assume it is for a video.
            String category = args.getString(VideoContract.VideoEntry.COLUMN_CATEGORY);

            // This just creates a CursorLoader that gets all videos.
            return new CursorLoader(
                    getContext(),
                    VideoContract.VideoEntry.CONTENT_URI, // Table to query
                    null, // Projection to return - null means return all fields
                    VideoContract.VideoEntry.COLUMN_CATEGORY + " = ?", // Selection clause
                    new String[]{category},  // Select based on the category id.
                    null // Default sort order
            );
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        if (data != null && data.moveToFirst()) {

            final int loaderId = loader.getId();

            if (loaderId == CATEGORY_LOADER) {

                // Every time we have to re-get the category loader, we must re-create the sidebar.
                mCategoryRowAdapter.clear();

                // Iterate through each category entry and add it to the ArrayAdapter.
                while (!data.isAfterLast()) {

                    int categoryIndex =
                            data.getColumnIndex(VideoContract.VideoEntry.COLUMN_CATEGORY);
                    String category = data.getString(categoryIndex);

                    // Create header for this category.
                    HeaderItem header = new HeaderItem(category);

                    int videoLoaderId = category.hashCode(); // Create unique int from category.
                    CursorObjectAdapter existingAdapter = mVideoCursorAdapters.get(videoLoaderId);
                    if (existingAdapter == null) {

                        // Map video results from the database to Video objects.
                        CursorObjectAdapter videoCursorAdapter =
                                new CursorObjectAdapter(new CardPresenter());
                        videoCursorAdapter.setMapper(new VideoCursorMapper());
                        mVideoCursorAdapters.put(videoLoaderId, videoCursorAdapter);

                        ListRow row = new ListRow(header, videoCursorAdapter);
                        mCategoryRowAdapter.add(row);

                        // Start loading the videos from the database for a particular category.
                        Bundle args = new Bundle();
                        args.putString(VideoContract.VideoEntry.COLUMN_CATEGORY, category);
                        mLoaderManager.initLoader(videoLoaderId, args, this);
                    } else {
                        ListRow row = new ListRow(header, existingAdapter);
                        mCategoryRowAdapter.add(row);
                    }

                    data.moveToNext();
                }

                // Create a row for this special case with more samples.
                HeaderItem gridHeader = new HeaderItem(getString(R.string.setting));
                GridItemPresenter gridPresenter = new GridItemPresenter(this);
                ArrayObjectAdapter gridRowAdapter = new ArrayObjectAdapter(gridPresenter);
                gridRowAdapter.add(getString(R.string.refresh));
                gridRowAdapter.add(getString(R.string.account));
                ListRow row = new ListRow(gridHeader, gridRowAdapter);
                mCategoryRowAdapter.add(row);

                startEntranceTransition(); // TODO: Move startEntranceTransition to after all
                // cursors have loaded.
            }

            else {

                // The CursorAdapter contains a Cursor pointing to all videos.
                mVideoCursorAdapters.get(loaderId).changeCursor(data);
            }

        }

        else {
            // Start an Intent to fetch the videos.
            Intent serviceIntent = new Intent(getActivity(), FetchVideoService.class);
            getActivity().startService(serviceIntent);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        int loaderId = loader.getId();
        if (loaderId != CATEGORY_LOADER) {
            mVideoCursorAdapters.get(loaderId).changeCursor(null);
        } else {
            mCategoryRowAdapter.clear();
        }
    }

    private class UpdateBackgroundTask implements Runnable {

        @Override
        public void run() {
            if (mBackgroundURI != null) {
                updateBackground(mBackgroundURI.toString());
            }
        }
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (item instanceof Movie) {

                Movie movie = (Movie) item;

                String datafinale = "";

                if(movie.getCategory().equals("1")) {

                    if(movie.getType().equals("0")) datafinale = datafinale1;

                    else datafinale = datafinale2;
                }

                else if(movie.getCategory().equals("2")) datafinale = datafinale3;

                // mise a jour du contenu du film ou de la serie

                try {

                    String data = "";

                    if(movie.getType().equals("0")) {

                        Toast.makeText(getActivity(),"Chargement du film...",Toast.LENGTH_LONG).show();

                        data = new Film().execute("film",movie.getId()+"").get();

                    }

                    else {

                        Toast.makeText(getActivity(),"Chargement de la série...",Toast.LENGTH_LONG).show();

                        data = new Serie().execute("serie",movie.getId()+"").get();

                    }

                    if (data != null) {

                        JSONObject jsonObject = new JSONObject(data);

                        if (jsonObject.getString("error").equals("false")) {

                            JSONArray liste = jsonObject.getJSONArray("message");

                            JSONObject current = (JSONObject) liste.get(0);

                            String lienfilm = "";

                            if (movie.getType().equals("0")) {

                                lienfilm = current.getString("linkfilm").replace("\\", "");

                                movie.setVideoUrl(lienfilm); movie.setContent("");

                                Log.e("film",lienfilm);
                            }

                            else {

                                lienfilm = current.getString("content");

                                movie.setContent(lienfilm);
                            }

                        }

                    }

                }
                catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                Intent intent = new Intent(getActivity(), VideoDetailsActivity.class);

                intent.putExtra(VideoDetailsActivity.VIDEO, movie);

                intent.putExtra(VideoDetailsActivity.DATA,datafinale);

                Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                        getActivity(),
                        ((ImageCardView) itemViewHolder.view).getMainImageView(),
                        VideoDetailsActivity.SHARED_ELEMENT_NAME).toBundle();

                getActivity().startActivity(intent,bundle);


            }

            else if (item instanceof String) {

                if (((String) item).contains(getString(R.string.refresh))) {

                    Intent intent = new Intent(getActivity(), MainActivity.class);
                    startActivity(intent);

                } else if (((String) item).contains(getString(R.string.account))) {

                    Intent intent = new Intent(getActivity(), SettingActivity1.class);
                    Bundle bundle =
                            ActivityOptionsCompat.makeSceneTransitionAnimation(getActivity())
                                    .toBundle();
                    startActivity(intent, bundle);

                } else {
                    Toast.makeText(getActivity(), ((String) item), Toast.LENGTH_SHORT)
                            .show();
                }
            }
        }
    }

    private final class ItemViewSelectedListener implements OnItemViewSelectedListener {
        @Override
        public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                RowPresenter.ViewHolder rowViewHolder, Row row) {
            if (item instanceof Movie) {
                mBackgroundURI = Uri.parse(((Movie) item).getBackgroundImageUrl());
                startBackgroundTimer();
            }

        }
    }


    private class AllFilm extends AsyncTask<String, Void, String> {
        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        protected String doInBackground(String... args) {

            HttpQuery httpQuery = new HttpQuery();

            try {
                return httpQuery.allfilm(args[0],args[1]);
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

    private class AllSerie extends AsyncTask<String, Void, String> {
        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        protected String doInBackground(String... args) {

            HttpQuery httpQuery = new HttpQuery();

            try {
                return httpQuery.allserie(args[0]);
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

    private class Film extends AsyncTask<String, Void, String> {
        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        protected String doInBackground(String... args) {

            HttpQuery httpQuery = new HttpQuery();

            try {
                return httpQuery.film(args[0],args[1]);
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

    private class Serie extends AsyncTask<String, Void, String> {
        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        protected String doInBackground(String... args) {

            HttpQuery httpQuery = new HttpQuery();

            try {
                return httpQuery.serie(args[0],args[1]);
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
}
