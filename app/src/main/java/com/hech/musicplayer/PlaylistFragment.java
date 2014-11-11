package com.hech.musicplayer;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.PopupMenu;

import java.util.ArrayList;
import static com.hech.musicplayer.R.id.action_newplaylist;

public class PlaylistFragment extends Fragment{
    private ArrayList<Playlist> playlists;
    private ListView playlistView;
    private PlaylistMapper playlistMap;
    private long playlistTransactID;
    private String playlistTranscactStr;

    private LayoutInflater infl;
    private ViewGroup con;

    public PlaylistFragment(){}
    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.fragment_playlist,
                        container, false);
        setHasOptionsMenu(true);
        infl = inflater;
        con = container;
        // Get the playlist view
        playlistView = (ListView)view.findViewById(R.id.play_list);
        // Create empty playlist library
        playlists = new ArrayList<Playlist>();
        //Add a faux recently playlists
        playlists.add(getRecentlyAdded());
        playlists.add(getRecentlyPlayed());
        // Scan device and populate playlist library
        getplaylistList();
        //Map the song list to the song viewer
        playlistMap = new PlaylistMapper(view.getContext(), playlists);
        playlistView.setAdapter(playlistMap);
        //Fragments need Click Listeners
        playlistView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView parent, final View view,
                                    int position, long id) {
                //Bundle up id and title for sub view
                Bundle bundle = new Bundle();
                bundle.putLong("playlist_id", playlists.get(position).getID());
                bundle.putString("playlist_name", playlists.get(position).getTitle());
                playlistTransactID = playlists.get(position).getID();
                playlistTranscactStr = playlists.get(position).getTitle();
                //Switch to subplaylist song view
                Fragment subFragment = new PlaylistSubFragment_Members();
                subFragment.setArguments(bundle);
                FragmentManager fragmentManager = getFragmentManager();
                if(subFragment != null) {
                    Log.d("Playlist", "Switch: Playlist Member View");
                    fragmentManager.beginTransaction().replace(R.id.frame_container,
                            subFragment).addToBackStack(null).commit();
                }
               }

        });
        playlistView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView parent, final View view,
                                           int position, long id) {
                playlistTransactID = playlists.get(position).getID();
                playlistTranscactStr = playlists.get(position).getTitle();
                if (playlistTransactID >= 0) {
                    final PopupMenu popupMenu = new PopupMenu(getActivity(), view);
                    popupMenu.inflate(R.menu.playlist_popup_menu);
                    popupMenu.show();
                    popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem item) {
                            int id = item.getItemId();
                            if (id == R.id.playlist_add) {
                                //Bundle up id and title for sub view
                                Bundle bundle = new Bundle();
                                bundle.putLong("playlist_id", playlistTransactID);
                                bundle.putString("playlist_name", playlistTranscactStr);
                                //Switch to subplaylist song view
                                Fragment subFragment = new PlaylistSubFragment_Modify();
                                subFragment.setArguments(bundle);
                                FragmentManager fragmentManager = getFragmentManager();
                                if (subFragment != null) {
                                    Log.d("Playlist", "Switch: Playlist Member View");
                                    fragmentManager.beginTransaction().replace(R.id.frame_container,
                                            subFragment).addToBackStack(null).commit();
                                }
                            } else if (id == R.id.playlist_delete) {

                            }
                            return true;
                        }
                    });
                }
                return true;
            }
        });
        return view;
    }
    public Playlist getRecentlyAdded(){
        return new Playlist(-1, "Recently Added");
    }
    public Playlist getRecentlyPlayed(){
        return new Playlist(-1, "Recently Played");
    }
    public void getplaylistList() {
        Cursor playlistCursor = getActivity().getContentResolver().query(
                MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
                null,
                null,
                null,
                null);
        if (playlistCursor != null && playlistCursor.moveToFirst()) {
            //get columns
            int idColumn = playlistCursor.getColumnIndex
                    (MediaStore.Audio.Playlists._ID);
            int titleColumn = playlistCursor.getColumnIndex
                    (MediaStore.Audio.Playlists.NAME);
            //fill playlist List
            do {
                long thisId = playlistCursor.getLong(idColumn);
                String thisTitle = playlistCursor.getString(titleColumn);
                playlists.add(new Playlist(thisId, thisTitle));
            } while (playlistCursor.moveToNext());
        }
    }
    public void updatePlaylists(){
        //clear list
        playlists.clear();
        playlistMap.notifyDataSetChanged();
        //Add a faux recently playlists
        playlists.add(getRecentlyAdded());
        playlists.add(getRecentlyPlayed());
        //update list
        getplaylistList();
        playlistMap.notifyDataSetChanged();
    }
    /*Prompt User for name of new playlist*/
    public void namePrompt(){
        AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
        alert.setTitle("Create a new Playlist");
        // Set an EditText view to get user input
        final EditText input = new EditText(getActivity());
        alert.setView(input);
        alert.setPositiveButton("Create", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String playlistName = input.getText().toString();
                createPlaylist(playlistName);
            }
        });
        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {}
        });
        alert.show();
    }
    public void createPlaylist(String playlistName){
        String[] projection = new String[] {
                MediaStore.Audio.Playlists._ID,
                MediaStore.Audio.Playlists.NAME,
                MediaStore.Audio.Playlists.DATA
        };
        ContentValues values = new ContentValues();
        values.put(MediaStore.Audio.Playlists.NAME, playlistName);
        values.put(MediaStore.Audio.Playlists.DATE_ADDED, System.currentTimeMillis());
        values.put(MediaStore.Audio.Playlists.DATE_MODIFIED, System.currentTimeMillis());
        Uri uri = getActivity().getApplicationContext().getContentResolver().insert
                (MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, values);
        if(uri != null){
            getActivity().getApplicationContext().getContentResolver().query
                    (uri, projection, null, null, null);
        }
        updatePlaylists();
    }
    public void onDestroy()
    {
        super.onDestroy();
    }
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.playlist, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == action_newplaylist) {
            namePrompt();
        }
        return super.onOptionsItemSelected(item);
    }
}
