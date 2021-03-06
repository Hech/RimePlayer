package com.hech.musicplayer;

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

//Adding new review files

/**
 * Created by Susie on 11/29/14.
 */
public class Review_ViewPager extends Fragment {
    ViewPager viewPager = null;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        View view = inflater.inflate(R.layout.viewpager_store,
                container, false);
        viewPager = (ViewPager) view.findViewById(R.id.pager_store);
        FragmentManager fragmentManager = getFragmentManager();
        if(viewPager == null){
            Toast.makeText(getActivity().getApplication(), "Null Pager", Toast.LENGTH_LONG).show();
        }
        viewPager.setAdapter(new pagerAdapte(fragmentManager));
        return view;
    }
}

class pagerAdapte extends FragmentStatePagerAdapter {
    public pagerAdapte(FragmentManager fragmentManager){
        super(fragmentManager);
    }
    @Override
    public Fragment getItem(int i) {
        android.app.Fragment choice = null;
        switch (i) {
            case 0:
                choice = new SongReviews();
                break;
            case 1:
                choice = new Store_RecommendFragment();
                break;
        }
        return choice;
    }
    @Override
    /*The total number of tabs*/
    //TODO make 3 when hot list fragment is made
    public int getCount(){
        return 2;
    }
    @Override
    public CharSequence getPageTitle(int position){
        if(position == 0){
            return "Reviews";
        }
        if(position == 1){
            return "Song";
        }
        if(position == 2){
            return "Priscilla Part";
        }
        return "";
    }
}