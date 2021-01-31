/*
 * Copyright (C) 2017 The Android Open Source Project
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

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import androidx.leanback.app.VideoSupportFragment;
import androidx.leanback.app.VideoSupportFragmentGlueHost;
import androidx.leanback.widget.*;
import androidx.core.app.ActivityOptionsCompat;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import com.example.android.wouritv.R;
import com.example.android.wouritv.config.Java_AES_Cipher;
import com.example.android.wouritv.data.VideoContract;
import com.example.android.wouritv.model.Movie;
import com.example.android.wouritv.model.Playlist;
import com.example.android.wouritv.model.Video;
import com.example.android.wouritv.model.VideoCursorMapper;
import com.example.android.wouritv.player.VideoPlayerGlue;
import com.example.android.wouritv.presenter.CardPresenter;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.ext.leanback.LeanbackPlayerAdapter;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.crypto.Cipher;

import java.util.ArrayList;
import java.util.List;

//import static com.example.android.wouritv.ui.PlaybackFragment.VideoLoaderCallbacks.RELATED_VIDEOS_LOADER;

/**
 * Plays selected video, loads playlist and related videos, and delegates playback to {@link
 * VideoPlayerGlue}.
 */
public class PlaybackFragment extends VideoSupportFragment {

    private static final int UPDATE_DELAY = 16;

    private VideoPlayerGlue mPlayerGlue;
    private LeanbackPlayerAdapter mPlayerAdapter;
    private SimpleExoPlayer mPlayer;
    private TrackSelector mTrackSelector;
    private PlaylistActionListener mPlaylistActionListener;

    private Movie mVideo;
    private Playlist mPlaylist;
   // private VideoLoaderCallbacks mVideoLoaderCallbacks;
    private CursorObjectAdapter mVideoCursorAdapter;

    private static Cipher ecipher;
    private static byte[] iv = {

            (byte)0xB2, (byte)0x12, (byte)0xD5, (byte)0xB2,(byte)0x44, (byte)0x21, (byte)0xC3, (byte)0xC3

    };

    /* access modifiers changed from: private */
    public static Context sContext;
    private final String KEY = "geekofcode2017";
    String cle2 = "WELCOMEONWOURITV";
    public String datafinale = "";
    byte[] decrpt;
    byte[] incrept;
    String iv2 = "fedcba9876543210";

    private String position;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mVideo = (Movie) getActivity().getIntent().getSerializableExtra(VideoDetailsActivity.VIDEO);
        this.datafinale = getActivity().getIntent().getStringExtra("donnee");
        this.position = getActivity().getIntent().getStringExtra("position");

        String str = "";
        String str2 = "DATA_VIDEO";

        if (this.mVideo.getType().equals("1")) {
            String content = this.mVideo.getContent();
            StringBuilder sb = new StringBuilder();
            sb.append(str);
            sb.append(this.position);

            try {
                JSONArray jSONArray = new JSONArray(content);
                String string = jSONArray.getJSONObject(Integer.parseInt(this.position)).getString("linkfilm");
                String title = this.mVideo.getTitle();
                Log.e("SERIES", string);
                Movie movie = this.mVideo;
                StringBuilder sb2 = new StringBuilder();
                sb2.append(title);
                sb2.append(" - ");
                sb2.append(jSONArray.getJSONObject(Integer.parseInt(this.position)).getString("titre"));
                movie.setTitle(sb2.toString());
                this.mVideo.setVideoUrl(string);
            } catch (JSONException unused) {
            }
        }

        if (!this.mVideo.getVideoUrl().isEmpty()) {
            if (this.mVideo.getVideoprovider().equals("3")) {
                str = this.mVideo.getVideoUrl();
            } else {
                str = Java_AES_Cipher.decrypt(this.cle2, this.mVideo.getVideoUrl());
            }
        }

        String replace = str.replace("https","http");

        Log.e("URL", str);

        Log.e("URL1", replace);

        this.mVideo.setVideoUrl(replace);

        mPlaylist = this.PlayListBuild(datafinale);


     /*   mVideoLoaderCallbacks = new VideoLoaderCallbacks(mPlaylist);

        // Loads the playlist.
        Bundle args = new Bundle();
        args.putString(VideoContract.VideoEntry.COLUMN_CATEGORY, mVideo.getCategory());

        getLoaderManager()
                .initLoader(VideoLoaderCallbacks.QUEUE_VIDEOS_LOADER, args, mVideoLoaderCallbacks);

        mVideoCursorAdapter = setupRelatedVideosCursor();*/
    }

    @Override
    public void onStart() {
        super.onStart();
        if (Util.SDK_INT > 23) {
            initializePlayer();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if ((Util.SDK_INT <= 23 || mPlayer == null)) {
            initializePlayer();
        }
    }

    /** Pauses the player. */
    @TargetApi(Build.VERSION_CODES.N)
    @Override
    public void onPause() {
        super.onPause();

        if (mPlayerGlue != null && mPlayerGlue.isPlaying()) {
            mPlayerGlue.pause();
        }
        if (Util.SDK_INT <= 23) {
            releasePlayer();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (Util.SDK_INT > 23) {
            releasePlayer();
        }
    }

    private void initializePlayer() {
        BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        TrackSelection.Factory videoTrackSelectionFactory =
                new AdaptiveTrackSelection.Factory(bandwidthMeter);

        mTrackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);

        mPlayer = ExoPlayerFactory.newSimpleInstance(getActivity(), mTrackSelector);

        mPlayerAdapter = new LeanbackPlayerAdapter(getActivity(), mPlayer, UPDATE_DELAY);

        mPlaylistActionListener = new PlaylistActionListener(mPlaylist);

        mPlayerGlue = new VideoPlayerGlue(getActivity(), mPlayerAdapter, mPlaylistActionListener);

        mPlayerGlue.setHost(new VideoSupportFragmentGlueHost(this));

        mPlayerGlue.playWhenPrepared();

        play(mVideo);

        ArrayObjectAdapter mRowsAdapter = initializeRelatedVideosRow();

        setAdapter(mRowsAdapter);
    }

    private void releasePlayer() {
        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
            mTrackSelector = null;
            mPlayerGlue = null;
            mPlayerAdapter = null;
            mPlaylistActionListener = null;
        }
    }

    private void play(Movie video) {
        mPlayerGlue.setTitle(video.getTitle());
        mPlayerGlue.setSubtitle(video.getDescription());
        prepareMediaForPlaying(Uri.parse(video.getVideoUrl()));
        mPlayerGlue.play();
    }

    private void prepareMediaForPlaying(Uri mediaSourceUri) {
        String userAgent = Util.getUserAgent(getActivity(), "VideoPlayerGlue");
        MediaSource mediaSource =
                new ExtractorMediaSource(
                        mediaSourceUri,
                        new DefaultDataSourceFactory(getActivity(), userAgent),
                        new DefaultExtractorsFactory(),
                        null,
                        null);

        mPlayer.prepare(mediaSource);
    }

    /*
     * To add a new row to the mPlayerAdapter and not lose the controls row that is provided by the
     * glue, we need to compose a new row with the controls row and our related videos row.
     *
     * We start by creating a new {@link ClassPresenterSelector}. Then add the controls row from
     * the media player glue, then add the related videos row.
     */

    private ArrayObjectAdapter initializeRelatedVideosRow() {

        ClassPresenterSelector presenterSelector = new ClassPresenterSelector();

        presenterSelector.addClassPresenter(
                mPlayerGlue.getControlsRow().getClass(), mPlayerGlue.getPlaybackRowPresenter());

        presenterSelector.addClassPresenter(ListRow.class, new ListRowPresenter());

        ArrayObjectAdapter rowsAdapter = new ArrayObjectAdapter(presenterSelector);

        rowsAdapter.add(mPlayerGlue.getControlsRow());

      /*  HeaderItem header = new HeaderItem(getString(R.string.related_movies));
        ListRow row = new ListRow(header, mVideoCursorAdapter);
        rowsAdapter.add(row); */

        setOnItemViewClickedListener(new ItemViewClickedListener());

        return rowsAdapter;
    }

  /*  private CursorObjectAdapter () {
        CursorObjectAdapter videoCursorAdapter = new CursorObjectAdapter(new CardPresenter());
        videoCursorAdapter.setMapper(new VideoCursorMapper());

        Bundle args = new Bundle();
        args.putString(VideoContract.VideoEntry.COLUMN_CATEGORY, mVideo.getCategory());
        getLoaderManager().initLoader(RELATED_VIDEOS_LOADER, args, mVideoLoaderCallbacks);

        return videoCursorAdapter;
    } */

    public void skipToNext() {
        //mPlayerGlue.next();
        Log.e("TEST","mext");
    }

    public void skipToPrevious() {
       // mPlayerGlue.previous();
    }

    public void rewind() {
        mPlayerGlue.rewind();
    }

    public void fastForward() {
        mPlayerGlue.fastForward();
    }

    /** Opens the video details page when a related video has been clicked. */
    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(
                Presenter.ViewHolder itemViewHolder,
                Object item,
                RowPresenter.ViewHolder rowViewHolder,
                Row row) {

            if (item instanceof Video) {
                Video video = (Video) item;

                Intent intent = new Intent(getActivity(), VideoDetailsActivity.class);
                intent.putExtra(VideoDetailsActivity.VIDEO, video);

                Bundle bundle =
                        ActivityOptionsCompat.makeSceneTransitionAnimation(
                                        getActivity(),
                                        ((ImageCardView) itemViewHolder.view).getMainImageView(),
                                        VideoDetailsActivity.SHARED_ELEMENT_NAME)
                                .toBundle();
                getActivity().startActivity(intent, bundle);
            }
        }
    }

    /** Loads a playlist from json data */
    protected Playlist PlayListBuild (String data) {

        Playlist playlist = new Playlist(); int position = 0;

        List<Movie> mymovies = new ArrayList<>();

        try {

            JSONObject jsonObject = new JSONObject(data);

            if (jsonObject.getString("error").equals("false")) {

                JSONArray liste = jsonObject.getJSONArray("message");

                for (int a = 0; a < liste.length(); a++) {

                    JSONObject current = (JSONObject) liste.get(a);

                    String studio = ""; String cat = ""; String type = "";

                    if(current.getInt("premium") == 1) {

                        studio = "Premium"; String lienfilm = "";

                        if (!current.getString("linkfilm").isEmpty()) {

                            lienfilm = current.getString("linkfilm").replace("\\", "");

                            cat = "1"; type = "0";
                        }

                        else {

                            cat = "1"; type = "1";
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

                        mymovies.add(movie);

                    }

                }

            }

            playlist.setCurrentPosition(0);
            playlist.setPlaylist(mymovies);

            return playlist;

        } catch (JSONException e) {
            e.printStackTrace();
        }


        return null;
    }

    /** Loads a playlist with videos from a cursor and also updates the related videos cursor. */
  /*  protected class VideoLoaderCallbacks implements LoaderManager.LoaderCallbacks<Cursor> {

        static final int RELATED_VIDEOS_LOADER = 1;
        static final int QUEUE_VIDEOS_LOADER = 2;

        private final VideoCursorMapper mVideoCursorMapper = new VideoCursorMapper();

        private final Playlist playlist;

        private VideoLoaderCallbacks(Playlist playlist) {
            this.playlist = playlist;
        }

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {

            // When loading related videos or videos for the playlist, query by category.

            String category = args.getString(VideoContract.VideoEntry.COLUMN_CATEGORY);

            return new CursorLoader(
                    getActivity(),
                    VideoContract.VideoEntry.CONTENT_URI,
                    null,
                    VideoContract.VideoEntry.COLUMN_CATEGORY + " = ?",
                    new String[] {category},
                    null);
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {

            if (cursor == null || !cursor.moveToFirst()) {
                return;
            }

            int id = loader.getId();

            if (id == QUEUE_VIDEOS_LOADER) {

                playlist.clear();

                do {
                    Movie video = (Movie) mVideoCursorMapper.convert(cursor);

                    // Set the current position to the selected video.
                    if (video.getId() == mVideo.getId()) {
                        playlist.setCurrentPosition(playlist.size());
                    }

                    playlist.add(video);

                } while (cursor.moveToNext());

            } else if (id == RELATED_VIDEOS_LOADER) {
                mVideoCursorAdapter.changeCursor(cursor);
            }
        }

        @Override
        public void onLoaderReset(Loader<Cursor> loader) {
            mVideoCursorAdapter.changeCursor(null);
        }
    }

    */

    class PlaylistActionListener implements VideoPlayerGlue.OnActionClickedListener {

        private Playlist mPlaylist;

        PlaylistActionListener(Playlist playlist) {
            this.mPlaylist = playlist;
        }

        @Override
        public void onPrevious() {
            //play(mPlaylist.previous());
            Log.e("TEST","mext2");
            Toast.makeText(getContext(),"Pas de playlist disponible pour le moment",Toast.LENGTH_LONG).show();
        }

        @Override
        public void onNext() {
          //  play(mPlaylist.next());
            Log.e("TEST","mext");
            Toast.makeText(getContext(),"Pas de playlist disponible pour le moment",Toast.LENGTH_LONG).show();
        }
    }
}
