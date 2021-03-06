package com.hech.musicplayer;

import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.concurrent.TimeUnit;

import static com.hech.musicplayer.R.id.action_settings;
import static com.hech.musicplayer.R.id.play_pause_toggle;


public class PlaylistSubFragment_Members extends Fragment {
    Playlist playlist;
    ListView songView;
    //private MusicService musicService;
    //private Intent playIntent;
    //private boolean musicBound = false;
    private boolean TitleAscending = false;
    private boolean ArtistAscending = false;
    private boolean isRecentlyPlayed = false;
    private View view;
    private final Handler handler = new Handler();
    private SeekBar seekBar;
    private Runnable runnable;


    public PlaylistSubFragment_Members() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view =  inflater.inflate(R.layout.fragment_song, container, false);
        //Tell existence of fragment's option list
        setHasOptionsMenu(true);

        //Show/Hide the Controller View
        String currSong = ((MainActivity)getActivity()).getCurrentSongName();
        if(((MainActivity)getActivity()).getMusicService().playing ||
                ((MainActivity)getActivity()).getMusicService().paused) {
            showController();
            setControllerSong(currSong);
            //If paused, toggle the controller correctly
            if(((MainActivity)getActivity()).getMusicService().paused){
                ToggleButton toggle = (ToggleButton)view.findViewById(play_pause_toggle);
                toggle.setChecked(true);
            }
            seekBar = (SeekBar)view.findViewById(R.id.seek_bar);
            trackProgressBar();
        }
        else{
            seekBar = (SeekBar)view.findViewById(R.id.seek_bar);
            hideController();
        }
        // Get the song view
        songView = (ListView)view.findViewById(R.id.song_list);
       //Receive playlist id and title from playlist parent fragment
        Bundle bundle = this.getArguments();
        if(bundle != null){
            playlist = new Playlist(bundle.getLong("playlist_id"),
                                    bundle.getString("playlist_name"));
            if(playlist.getTitle().equals("Recently Added")){
                getRecentlyAdded();
            }
            else if(playlist.getTitle().equals("Recently Played")){
                getRecentlyPlayed();
                isRecentlyPlayed = true;
            }
            else if(playlist.getTitle().equals("Recently Downloaded")){
                getRecentlyDownloaded();
            }
            else{
                fillPlaylist(playlist);
            }
            //Set Title
            try {
                getActivity().getActionBar().setTitle(playlist.getTitle());
            } catch(NullPointerException e){
                Log.e("Set Title: ",e.toString());
            }
        }
        //Set Music Service Now Playing List
        //((MainActivity)getActivity()).getMusicService().setNowPlaying(playlist.getSongList());
        SongMapper songMap = new SongMapper(view.getContext(), playlist.getSongList());
        songView.setAdapter(songMap);
        //Click Listener for song list
        songView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView parent, final View v,
                                    int position, long id) {
                Song s = new Song(playlist.getSongList().get(position).getID(),
                        playlist.getSongList().get(position).getTitle(),
                        playlist.getSongList().get(position).getArtist(),
                        playlist.getSongList().get(position).getAlbum());

                    ((MainActivity) getActivity()).setNewSongsAvail(false);
                    //Update controller's song name
                    setControllerSong(s.getTitle());
                    //Update current song in MainActivity
                    ((MainActivity) getActivity()).setCurrentSongName(s.getTitle());
                    //Update playlist's list
                    ((MainActivity) getActivity()).getMusicService()
                            .setNowPlaying(playlist.getSongList());
                    //Play chosen song from list
                    songPicked(v);
                    //Update the recently played playlist
                    ((MainActivity) getActivity()).setRecentlyPlayed(s);
                    if(isRecentlyPlayed) {
                        getRecentlyPlayed();
                        //((MainActivity) getActivity()).getMusicService()
                        //        .setNowPlaying(playlist.getSongList());
                        SongMapper songMap = new SongMapper
                                (view.getContext(), playlist.getSongList());
                        songView.setAdapter(songMap);
                    }
                    //Force pause option in controller
                    ToggleButton toggle = (ToggleButton) view
                            .findViewById(R.id.play_pause_toggle);
                    toggle.setChecked(false);
                seekBar = (SeekBar)view.findViewById(R.id.seek_bar);
                trackProgressBar();
            }
        });
        //Click Listener for Play/Pause
        view.findViewById(R.id.play_pause_toggle).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(((MainActivity)getActivity()).getMusicService().playing){
                    ((MainActivity)getActivity()).getMusicService().pausePlay();
                }
                else{
                    ((MainActivity)getActivity()).getMusicService().resumePlay();
                    trackProgressBar();
                }
            }
        });
        //Listener for MediaPlayer Song Complete
        ((MainActivity)getActivity()).getMusicService()
                .getPlayer().setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                Log.d("MediaPlayerListener", "Song Complete");
                //If It isn't null
                if(getActivity() == null){
                    Log.d("FragmentListener", "Preventing Crash");
                    return;
                }
                //If there isn't more to play
                if(!((MainActivity)getActivity()).getMusicService().getContinuousPlayMode()) {
                    ((MainActivity) getActivity()).getMusicService()
                            .stoppedState();
                    //Force play option in controller
                    ToggleButton toggle = (ToggleButton) view
                            .findViewById(R.id.play_pause_toggle);
                    toggle.setChecked(true);
                }
            }
        });
        //Listen for when the seekbar is touched
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            //If the seekbar was touched by the user
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(getActivity() != null  && fromUser){
                    int duration = ((MainActivity) getActivity())
                            .getMusicService().getPlayer().getDuration();
                    Log.d("SeekBar Heading To ", String.valueOf(progress*duration/100));
                    //Manually seek to position
                    ((MainActivity)getActivity())
                            .getMusicService().getPlayer().seekTo(progress*duration/100);
                    //Update the music service's position
                    ((MainActivity)getActivity())
                            .getMusicService().setPlayerPos(progress*duration/100);
                }
            }
        });

        return view;
    }
    //Fill the Recently Added Playlist
    public void getRecentlyAdded(){
        ContentResolver recentResolver = getActivity().getContentResolver();
        Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor recentCursor = recentResolver.query(musicUri,
                null,
                MediaStore.Audio.Media.IS_MUSIC+" != 0",
                null,
                //Sort in descending order for newly added first
                MediaStore.Audio.Media.DATE_ADDED + " DESC");
        if(recentCursor != null && recentCursor.moveToFirst()){
            //get columns
            int titleColumn = recentCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.TITLE);
            int idColumn = recentCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media._ID);
            int artistColumn = recentCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.ARTIST);
            int albumColumn = recentCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.ALBUM);
            int num = 0;
            do {
                long thisId = recentCursor.getLong(idColumn);
                String thisTitle = recentCursor.getString(titleColumn);
                String thisArtist = recentCursor.getString(artistColumn);
                String thisAlbum = recentCursor.getString(albumColumn);
                playlist.addSong(new Song(thisId, thisTitle, thisArtist, thisAlbum));
                ++num;
            }while (recentCursor.moveToNext() && num < 10);
        }
    }
    //Fill the Recently Played Playlist
    public void getRecentlyPlayed(){
        playlist = ((MainActivity)getActivity()).getRecentlyPlayed();
    }
    //Fill the Recently Downloaded Playlist
    public void getRecentlyDownloaded(){
        playlist = ((MainActivity)getActivity()).getRecentlyDownloaded();
    }
    // Create the connection to the music service
   // private ServiceConnection musicConnection = new ServiceConnection() {

        //Initialize the music service once a connection is established
   //     public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
   //         MusicService.MusicBinder binder = (MusicService.MusicBinder) iBinder;
   //         musicService = binder.getService();
   //         musicService.setSongsList(playlist.getSongList());
   //         musicService.setCurrUser(((MainActivity) getActivity()).getUserLoggedin());
   //         musicBound = true;
   //     }
   //     public void onServiceDisconnected(ComponentName componentName) {
   //         musicBound = false;
   //     }
   // };
    //Fill playlist object with its songs
    public void fillPlaylist(Playlist pList){
        String [] projection = {
                MediaStore.Audio.Playlists.Members.ARTIST,
                MediaStore.Audio.Playlists.Members.TITLE,
                MediaStore.Audio.Playlists.Members.AUDIO_ID,
                MediaStore.Audio.Playlists.Members.ALBUM
        };
        Cursor playlistCursor = getActivity().getContentResolver().query(
                MediaStore.Audio.Playlists.Members.getContentUri("external",
                pList.getID()),
                projection,
                MediaStore.Audio.Media.IS_MUSIC+" != 0",
                null,
                null);
        if(playlistCursor != null && playlistCursor.moveToFirst()){
            //get columns
            int idColumn = playlistCursor.getColumnIndex
                    (MediaStore.Audio.Playlists.Members.AUDIO_ID);
            int titleColumn = playlistCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.TITLE);
            int artistColumn = playlistCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.ARTIST);
            int albumColumn = playlistCursor.getColumnIndex
                    (MediaStore.Audio.Media.ALBUM);
            do{
                long thisId = playlistCursor.getLong(idColumn);
                String thisTitle = playlistCursor.getString(titleColumn);
                String thisArtist = playlistCursor.getString(artistColumn);
                String thisAlbum = playlistCursor.getString(albumColumn);
                pList.addSong(new Song(thisId, thisTitle, thisArtist, thisAlbum));
            }while(playlistCursor.moveToNext());
        }
    }
    //If the user selects a song from the list, play it
    public void songPicked(View view){
     //   musicService.setSong(Integer.parseInt(view.getTag().toString()));
     //   musicService.playSong();
        //Set the position in NowPlaying to as the same as the seleted song
        ((MainActivity)getActivity()).getMusicService()
                .setSong(Integer.parseInt(view.getTag().toString()));
        //Play song in the NowPlaying list at given position
        ((MainActivity)getActivity()).getMusicService().playSong();
        //Active Song
        Log.d("MPlayer Current Song: ", ((MainActivity)getActivity()).getMusicService().songTitle);
        //Display music controller
        showController();
    }
    // Connects MainActivity to the music service on startup, also starts the music service
    public void onStart(){
        super.onStart();
    //    if(playIntent == null){
    //        playIntent = new Intent(getActivity(), MusicService.class);
    //        getActivity().bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
    //        getActivity().startService(playIntent);
    //    }
    }
    @Override
    public void onPause(){ super.onPause(); }
    @Override
    public void onDestroy(){
    //    getActivity().stopService(playIntent);
    //    getActivity().unbindService(musicConnection);
    //    musicService = null;
        super.onDestroy();
    }
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.song, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == action_settings) {
            return true;
        }
        if (id == R.id.action_continuousPlay)
        {
            ((MainActivity)getActivity()).getMusicService().setContinuousPlayMode(true);
            ((MainActivity)getActivity()).getMusicService().playSong();
            Log.d("SongFragment", "MusicPlayCalled");
        }
        if(id == R.id.action_stopPlay)
        {
            ((MainActivity)getActivity()).getMusicService().setContinuousPlayMode(false);
            Log.d("SongFragment", "MusicStopCalled");
            ((MainActivity)getActivity()).getMusicService().stopPlay();
        }
        if(id == R.id.action_sort_title)
        {
            if(TitleAscending)
            {
                TitleAscending = false;
                ArtistAscending = false;
                playlist.pList = ((MainActivity)getActivity()).getMusicService()
                        .sortSongsByAttribute(playlist.getSongList(), 0, false);
            }
            else
            {
                TitleAscending = true;
                ArtistAscending = false;
                playlist.pList = ((MainActivity)getActivity()).getMusicService()
                        .sortSongsByAttribute(playlist.getSongList(), 0, true);
            }
            SongMapper songMap = new SongMapper(songView.getContext(), playlist.getSongList());
            ((MainActivity)getActivity()).getMusicService()
                    .setNowPlaying(playlist.getSongList());
            songView.setAdapter(songMap);
        }
        if(id == R.id.action_sort_artist)
        {
            if(ArtistAscending)
            {
                TitleAscending = false;
                ArtistAscending = false;
                playlist.pList = ((MainActivity)getActivity()).getMusicService()
                        .sortSongsByAttribute(playlist.getSongList(), 1, false);
            }
            else
            {
                TitleAscending = false;
                ArtistAscending = true;
                playlist.pList = ((MainActivity)getActivity()).getMusicService()
                        .sortSongsByAttribute(playlist.getSongList(), 1, true);
            }
            SongMapper songMap = new SongMapper(songView.getContext(), playlist.getSongList());
            ((MainActivity)getActivity()).getMusicService()
                    .setNowPlaying(playlist.getSongList());
            songView.setAdapter(songMap);
        }
        if(id == R.id.action_shuffle)
        {
            playlist.pList = ((MainActivity)getActivity()).getMusicService().shuffle();
            SongMapper songMap = new SongMapper(songView.getContext(), playlist.getSongList());
            ((MainActivity)getActivity()).getMusicService()
                    .setNowPlaying(playlist.getSongList());
            songView.setAdapter(songMap);
        }
        if(id == R.id.action_end)
        {
            ((MainActivity)getActivity()).getMusicService()
                    .stopService(((MainActivity) getActivity()).getPlayIntent());
            //getActivity().stopService(playIntent);
            ((MainActivity)getActivity()).setMusicServiceNull();
            Log.d("SongFragment", "AppCloseCalled");
            System.exit(0);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onStop() {

        handler.removeCallbacks(runnable);
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onInflate(Activity activity, AttributeSet attrs, Bundle savedInstanceState) {
        super.onInflate(activity, attrs, savedInstanceState);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
    public void setControllerSong(String songName){
        TextView currentSong = (TextView)view
                .findViewById(R.id.music_current_song);
        currentSong.setText(songName);

    }
    public void showController(){
        LinearLayout current = (LinearLayout)view
                .findViewById(R.id.music_current);
        current.setVisibility(View.VISIBLE);
        RelativeLayout controller = (RelativeLayout)view.findViewById(R.id.music_controller);
        controller.setVisibility(View.VISIBLE);
    }
    public void hideController(){
        LinearLayout current = (LinearLayout) view
                .findViewById(R.id.music_current);
        current.setVisibility(View.GONE);
        RelativeLayout controller = (RelativeLayout) view
                .findViewById(R.id.music_controller);
        controller.setVisibility(View.GONE);
    }
    public void trackProgressBar(){
        runnable = new Runnable() {
            @Override
            public void run() {
                String currentTime;
                String endTime;
                if (getActivity() != null && ((MainActivity)getActivity())
                        .getMusicService().getPlayer().isPlaying()) {

                    int currentPosition = ((MainActivity) getActivity())
                            .getMusicService().getPlayer().getCurrentPosition();
                    int duration = ((MainActivity) getActivity())
                            .getMusicService().getPlayer().getDuration();
                    int progress = (currentPosition * 100) / duration;

                    currentTime = String.format("%01d:%02d",
                            TimeUnit.MILLISECONDS.toMinutes(currentPosition),
                            TimeUnit.MILLISECONDS.toSeconds(currentPosition) -
                                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS
                                            .toMinutes(currentPosition))
                    );
                    endTime = String.format("%01d:%02d",
                            TimeUnit.MILLISECONDS.toMinutes(duration),
                            TimeUnit.MILLISECONDS.toSeconds(duration) -
                                    TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS
                                            .toMinutes(duration))
                    );

                    TextView currentSong = (TextView) view
                            .findViewById(R.id.seek_bar_curr);
                    currentSong.setText(currentTime);

                    TextView currentEnd = (TextView) view
                            .findViewById(R.id.seek_bar_max);
                    currentEnd.setText(endTime);

                    seekBar.setProgress(progress);
                    handler.postDelayed(this, 1000);
                }
            }
        };
        handler.postDelayed(runnable, 1000);

    }

}