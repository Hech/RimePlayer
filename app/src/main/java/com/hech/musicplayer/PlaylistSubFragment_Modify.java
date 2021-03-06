package com.hech.musicplayer;

import android.app.Activity;
import android.app.Fragment;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import static com.hech.musicplayer.R.id.play_pause_toggle;

public class PlaylistSubFragment_Modify extends Fragment {
    private Playlist playlistCopy;
    private Playlist options;
    private Playlist selected;
    private ListView songView;
    private boolean addToPlaylist;
    View view;
    private SeekBar seekBar;
    private Runnable runnable;
    private final Handler handler = new Handler();

    public PlaylistSubFragment_Modify() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_song, container, false);
        //Tell existence of fragment's option list
        setHasOptionsMenu(true);
        // Get the song view
        songView = (ListView) view.findViewById(R.id.song_list);
        //Receive playlist id and title from playlist parent fragment
        Bundle bundle = this.getArguments();
        if (bundle != null) {
            if(bundle.getString("playlist_opt").equals("add")){
                addToPlaylist = true;
            }
            else if(bundle.getString("playlist_opt").equals("delete")) {
                addToPlaylist = false;
            }

            selected = new Playlist(-1, "selections");
            playlistCopy = new Playlist(bundle.getLong("playlist_id"),
                    bundle.getString("playlist_name"));
            fillPlaylist(playlistCopy);
        }
        if(addToPlaylist) { //if adding songs
            options = getMissingSongs(playlistCopy);
        }
        else{ //if removing existing songs
            options = playlistCopy;
        }
        //Show/Hide the Controller View
        String currSong = ((MainActivity) getActivity()).getCurrentSongName();
        if (((MainActivity) getActivity()).getMusicService().playing ||
                ((MainActivity) getActivity()).getMusicService().paused) {
            showController();
            setControllerSong(currSong);
            //If paused, toggle the controller correctly
            if (((MainActivity) getActivity()).getMusicService().paused) {
                ToggleButton toggle = (ToggleButton) view.findViewById(play_pause_toggle);
                toggle.setChecked(true);

            }
            //Track the song's progress
            seekBar = (SeekBar) view.findViewById(R.id.seek_bar);
            trackProgressBar();
        } else {
            seekBar = (SeekBar) view.findViewById(R.id.seek_bar);
            hideController();
        }
        final SongMapper songMap = new SongMapper(view.getContext(), options.getSongList());
        songView.setAdapter(songMap);
        songView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);

        //MultiChoice Listener for song list
        songView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
            @Override
            public void onItemCheckedStateChanged(ActionMode mode, int position,
                                                  long id, boolean checked) {
                //Get total Item Count
                final int checkCount = songView.getCheckedItemCount();
                mode.setTitle(checkCount + " Selected");
                if (checked) {
                    selected.addSong(options.getSong(position));
                }
                else {
                    selected.removeSong(options.getSong(position).getID());
                }
            }

            @Override
            public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                menu.clear();
                mode.getMenuInflater().inflate(R.menu.playlist_modify, menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_playlist_confirm:
                        if(addToPlaylist) {
                            for (int index = 0; index < selected.getSongList().size(); ++index) {
                                addSongtoPlaylist(playlistCopy.getID(), selected.getSong(index).getID());
                            }
                            Toast.makeText(getActivity().getApplicationContext(),
                                    playlistCopy.getTitle() + ": song(s) added",
                                    Toast.LENGTH_SHORT).show();
                        }
                        else {
                            for (int index = 0; index < selected.getSongList().size(); ++index) {
                                removeSongfromPlaylist(playlistCopy.getID(), selected.getSong(index).getID());
                            }
                            Toast.makeText(getActivity().getApplicationContext(),
                                    playlistCopy.getTitle() + ": song(s) removed",
                                    Toast.LENGTH_SHORT).show();
                        }
                        break;
                    default:
                        break;
                }
                //End CAB
                mode.finish();
                //Make fragment back to playlists
                getActivity().getFragmentManager().popBackStackImmediate();
                return false;
            }
            @Override
            public void onDestroyActionMode(ActionMode mode) {}
        });
        //Give Instructions
        try {
            getActivity().getActionBar().setTitle("Hold to Select");
        } catch(NullPointerException e){
            Log.e("Set Title: ",e.toString());
        }
        return view;
    }
    public void addSongtoPlaylist(long pID, long sID){
            ContentResolver resolver = getActivity().getContentResolver();
            String[] cols = new String[] {
                    "count(*)"
            };
            Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", pID);
            Cursor cur = resolver.query(uri, cols, null, null, null);
            cur.moveToFirst();
            final int base = cur.getInt(0);
            cur.close();
            ContentValues values = new ContentValues();
            values.put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, Long.valueOf(base + sID));
            values.put(MediaStore.Audio.Playlists.Members.AUDIO_ID, sID);
            resolver.insert(uri, values);
            Toast.makeText(getActivity().getApplicationContext(), "Song(s) Added",
                    Toast.LENGTH_SHORT).show();
    }
    public void removeSongfromPlaylist(long pID, long sID){
        ContentResolver resolver = getActivity().getContentResolver();
        String[] cols = new String[] {"count(*)"};
        Uri uri = MediaStore.Audio.Playlists.Members.getContentUri("external", pID);
        Cursor cur = resolver.query(uri, cols, null, null, null);
        cur.moveToFirst();
        final int base = cur.getInt(0);
        cur.close();
        ContentValues values = new ContentValues();
        resolver.delete(uri, MediaStore.Audio.Playlists.Members.AUDIO_ID + "=" + sID, null);
    }
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
    public Playlist getMissingSongs(Playlist ignore){
        Playlist ret = new Playlist(-1, "missing");
        //retrieve song info
        ContentResolver musicResolver = getActivity().getContentResolver();
        Uri musicUri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = musicResolver.query(musicUri,
                null,
                MediaStore.Audio.Media.IS_MUSIC+" != 0",
                null,
                null);
        if (musicCursor != null && musicCursor.moveToFirst()) {
            //get columns
            int titleColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.TITLE);
            int idColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media._ID);
            int artistColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.ARTIST);
            int albumColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.ALBUM);
            //add songs to list
            do {
                long thisId = musicCursor.getLong(idColumn);
                String thisTitle = musicCursor.getString(titleColumn);
                String thisArtist = musicCursor.getString(artistColumn);
                String thisAlbum = musicCursor.getString(albumColumn);

                ArrayList<Song> cmp = ignore.getSongList();
                Song s;
                int index = 0;
                for(; index < cmp.size(); ++index) {
                    s = cmp.get(index);
                    if(s.getID() == thisId){
                        break;
                    }
                }
                if(index >= cmp.size()) {
                    ret.addSong(new Song(thisId, thisTitle, thisArtist, thisAlbum));
                }
            }
            while (musicCursor.moveToNext());
        }
        return ret;
    }

    @Override
    public void onDestroy(){ super.onDestroy(); }
    @Override
    public void onPause(){ super.onPause(); }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
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
                    //Update the music service's position
                    ((MainActivity)getActivity())
                            .getMusicService().setPlayerPos(progress*duration/100);
                    handler.postDelayed(this, 1000);
                }
            }
        };
        handler.postDelayed(runnable, 1000);

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
    public void setControllerSong(String songName){
        TextView currentSong = (TextView)view
                .findViewById(R.id.music_current_song);
        currentSong.setText(songName);

    }
}
