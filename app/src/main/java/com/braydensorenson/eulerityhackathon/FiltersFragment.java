package com.braydensorenson.eulerityhackathon;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.zomato.photofilters.SampleFilters;
import com.zomato.photofilters.imageprocessors.Filter;

import java.util.ArrayList;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link FiltersFragment.ThumbnailInteractionListener} interface
 * to handle interaction events.
 * Use the {@link FiltersFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FiltersFragment extends Fragment {
    private ThumbnailInteractionListener mListener;

    public FiltersFragment() {
        // Required empty public constructor
    }

    public static FiltersFragment newInstance(int imgId) {
        FiltersFragment fragment = new FiltersFragment();
        Bundle args = new Bundle();
        args.putInt("imgId", imgId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_filters, container, false);
        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.recyclerViewFilters);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        recyclerView.setLayoutManager(layoutManager);
        List<Filter> filters = getSampleFilters();
        List<String> names = getSampleFilterNames();
        int imgId = getArguments().getInt("imgId");
        recyclerView.setAdapter(new FiltersListAdapter(filters, names, imgId, this));
        return view;
    }

    public List<Filter> getSampleFilters(){
        List<Filter> filters = new ArrayList<>();
        filters.add(SampleFilters.getAweStruckVibeFilter());
        filters.add(SampleFilters.getBlueMessFilter());
        filters.add(SampleFilters.getLimeStutterFilter());
        filters.add(SampleFilters.getNightWhisperFilter());
        filters.add(SampleFilters.getStarLitFilter());
        return filters;
    }

    public List<String> getSampleFilterNames(){
        List<String> names = new ArrayList<>();
        names.add("Struck Vibe");
        names.add("Blue Mess");
        names.add("Lime Stutter");
        names.add("Night Whisper");
        names.add("Starlit");
        return names;
    }

    public void onThumbnailClick(Filter f) {
        if (mListener != null) {
            mListener.onThumbnailClick(f);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof ThumbnailInteractionListener) {
            mListener = (ThumbnailInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement ThumbnailInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface ThumbnailInteractionListener {
        void onThumbnailClick(Filter f);
    }
}
