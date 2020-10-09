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

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.RequiresApi;
import androidx.leanback.app.SearchSupportFragment;
import androidx.leanback.widget.ArrayObjectAdapter;
import androidx.leanback.widget.CursorObjectAdapter;
import androidx.leanback.widget.HeaderItem;
import androidx.leanback.widget.ImageCardView;
import androidx.leanback.widget.ListRow;
import androidx.leanback.widget.ListRowPresenter;
import androidx.leanback.widget.ObjectAdapter;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import androidx.leanback.widget.SpeechRecognitionCallback;
import androidx.core.app.ActivityOptionsCompat;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.example.android.wouritv.BuildConfig;
import com.example.android.wouritv.R;
import com.example.android.wouritv.config.HttpQuery;
import com.example.android.wouritv.data.VideoContract;
import com.example.android.wouritv.model.Movie;
import com.example.android.wouritv.model.Video;
import com.example.android.wouritv.model.VideoCursorMapper;
import com.example.android.wouritv.presenter.CardPresenter;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.crypto.Cipher;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

/*
 * This class demonstrates how to do in-app search
 */
public class SearchFragment extends SearchSupportFragment
        implements SearchSupportFragment.SearchResultProvider {

    private static final String TAG = "SearchFragment";
    private static final boolean DEBUG = BuildConfig.DEBUG;
    private static final boolean FINISH_ON_RECOGNIZER_CANCELED = true;
    private static final int REQUEST_SPEECH = 0x00000010;

    private final Handler mHandler = new Handler();
    private ArrayObjectAdapter mRowsAdapter;
    private String mQuery;
    private final CursorObjectAdapter mVideoCursorAdapter =
            new CursorObjectAdapter(new CardPresenter());

    private int mSearchLoaderId = 1;
    private boolean mResultsFound = false;


    public String datafinale1 = "",datafinale2 = "", datafinale3 = "";

    private static final String TAG1 = SearchFragment.class.getSimpleName();

     public String datafinale = "";

    String iv2 = "fedcba9876543210";
    String cle2 = "WELCOMEONWOURITV";

    byte [] incrept;
    byte [] decrpt;

    private static Cipher ecipher;

    private static byte[] iv = {

            (byte)0xB2, (byte)0x12, (byte)0xD5, (byte)0xB2,(byte)0x44, (byte)0x21, (byte)0xC3, (byte)0xC3

    };

    private final String KEY = "geekofcode2017";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        mVideoCursorAdapter.setMapper(new VideoCursorMapper());

        setSearchResultProvider(this);
        setOnItemViewClickedListener(new ItemViewClickedListener());

        if (DEBUG) {
            Log.d(TAG, "User is initiating a search. Do we have RECORD_AUDIO permission? " +
                hasPermission(Manifest.permission.RECORD_AUDIO));
        }

        if (!hasPermission(Manifest.permission.RECORD_AUDIO)) {
            if (DEBUG) {
                Log.d(TAG, "Does not have RECORD_AUDIO, using SpeechRecognitionCallback");
            }
            // SpeechRecognitionCallback is not required and if not provided recognition will be
            // handled using internal speech recognizer, in which case you must have RECORD_AUDIO
            // permission
            setSpeechRecognitionCallback(new SpeechRecognitionCallback() {
                @Override
                public void recognizeSpeech() {
                    try {
                        startActivityForResult(getRecognizerIntent(), REQUEST_SPEECH);
                    } catch (ActivityNotFoundException e) {
                        Log.e(TAG, "Cannot find activity for speech recognizer", e);
                    }
                }
            });
        } else if (DEBUG) {
            Log.d(TAG, "We DO have RECORD_AUDIO");
        }


    }

    @Override
    public void onPause() {
        mHandler.removeCallbacksAndMessages(null);
        super.onPause();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_SPEECH:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        setSearchQuery(data, true);
                        break;
                    default:
                        // If recognizer is canceled or failed, keep focus on the search orb
                        if (FINISH_ON_RECOGNIZER_CANCELED) {
                            if (!hasResults()) {
                                if (DEBUG) Log.v(TAG, "Voice search canceled");
                                getView().findViewById(R.id.lb_search_bar_speech_orb).requestFocus();
                            }
                        }
                        break;
                }
                break;
        }
    }

    @Override
    public ObjectAdapter getResultsAdapter() {
        return mRowsAdapter;
    }

    @Override
    public boolean onQueryTextChange(String newQuery) {
        if (DEBUG) Log.i(TAG, String.format("Search text changed: %s", newQuery));
        Log.e("RECHERCE","saisie");
        loadRows(newQuery);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        if (DEBUG) Log.i(TAG, String.format("Search text submitted: %s", query));
        Log.e("RECHERCE","debut");
        //loadQuery(query);
        loadRows(query);
        return true;
    }

    public boolean hasResults() {
        return mRowsAdapter.size() > 0 && mResultsFound;
    }

    private boolean hasPermission(final String permission) {
        final Context context = getActivity();
        return PackageManager.PERMISSION_GRANTED == context.getPackageManager().checkPermission(
                permission, context.getPackageName());
    }


    public void focusOnSearch() {
        getView().findViewById(R.id.lb_search_bar).requestFocus();
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (item instanceof Movie) {

                Movie movie = (Movie) item;

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

            else {
                Toast.makeText(getActivity(), ((String) item), Toast.LENGTH_SHORT).show();
            }
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

    private class AllContent extends AsyncTask<String, Void, String> {
        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        protected String doInBackground(String... args) {

            HttpQuery httpQuery = new HttpQuery();

            try {
                return httpQuery.recherche(args[0]);
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

    private void loadRows(final String mQuery) {

        String data = ""; ArrayList<Movie> list = new ArrayList<Movie>();

        if(isConnected()) try {

            data = new AllContent().execute("recherche").get();

            datafinale = data;

            if(data != null){

                Log.e("DATA2",data); datafinale = data;

                JSONArray liste = new JSONArray(data);

                for (int a = 0; a < liste.length(); a++) {

                    JSONObject current = (JSONObject) liste.get(a);

                    String studio = "";

                    if(current.getInt("premium") == 1) {

                        String lienfilm = "";

                        if (current.getString("type").equals("0")) {

                            lienfilm = current.getString("linkfilm").replace("\\", "");

                        }

                        studio = "Premium";

                        String cardImageUrl = "http://www.wouri.tv/images/" + current.getString("couverture");

                        String bgImageUrl = "https://www.wouri.tv/images/" + current.getString("couverture2");

                        Movie movie = new Movie();
                        movie.setId(current.getInt("id"));
                        Movie.incCount();
                        movie.setTitle(current.getString("titre"));
                        movie.setDescription(current.getString("description"));
                        movie.setStudio(studio);
                        movie.setCategory(current.getString("cat"));
                        movie.setType(current.getString("type"));
                        movie.setCardImageUrl(cardImageUrl);
                        movie.setBackgroundImageUrl(bgImageUrl);
                        movie.setContent(current.getString("content"));
                        movie.setVideoprovider(current.getString("providercontent"));
                        movie.setBaprovider(current.getString("providerba"));
                        if (current.getString("type").equals("0"))
                        {
                            movie.setVideoUrl(lienfilm);
                        }
                        else movie.setVideoUrl("");

                        movie.setBaUrl(current.getString("linkba"));

                        list.add(movie);

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

        ArrayList<Movie> mItems = list;

        final List<Movie> result = new ArrayList<>();

        for (Movie movie : mItems) {
            // Main logic of search is here.
            // Just check that "query" is contained in Title or Description or not.
            if (movie.getTitle().toLowerCase(Locale.ENGLISH)
                    .contains(mQuery.toLowerCase(Locale.ENGLISH))
                    || movie.getDescription().toLowerCase(Locale.ENGLISH)
                    .contains(mQuery.toLowerCase(Locale.ENGLISH))) {
                result.add(movie);
            }
        }

        // mRowsAdapter.clear();

        if(mRowsAdapter.size() != 0 ) mRowsAdapter.clear();

        ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(new CardPresenter());
        listRowAdapter.addAll(0, result);
        HeaderItem header = new HeaderItem("Resultats de la recherche");
        mRowsAdapter.add(new ListRow(header, listRowAdapter));

        setOnItemViewClickedListener(new ItemViewClickedListener());
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

}
