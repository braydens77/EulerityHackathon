package com.braydensorenson.eulerityhackathon;

import android.graphics.Bitmap;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.zomato.photofilters.imageprocessors.Filter;

import java.util.List;

public class FiltersListAdapter extends RecyclerView.Adapter<FilterViewHolder> {
    List<Filter> filters;
    List<String> filterNames;
    Bitmap originalImage;
    Bitmap thumbnailImage;
    FiltersFragment filtersFrag;

    public FiltersListAdapter(List<Filter> filters, List<String> filterNames, int imgId, FiltersFragment frag){
        this.filters = filters;
        this.filterNames = filterNames;
        this.filtersFrag = frag;
        this.originalImage = MainActivity.bitmaps.get(imgId);
        int width = originalImage.getWidth();
        int height = originalImage.getHeight();
        double scale = 1.0;
        if(width > 1000 || height > 1000){
            scale =0.25;
        }else if(width > 500 || height > 500){
            scale =0.5;
        }
        width = (int)(width * scale);
        height = (int)(height * scale);
        this.thumbnailImage = Bitmap.createScaledBitmap(originalImage, width, height, false);
    }

    @Override
    public FilterViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.filter_items, parent, false);
        FilterViewHolder holder = new FilterViewHolder(view);
        return holder;
    }

    @Override
    public void onBindViewHolder(final FilterViewHolder holder, int position) {
        String name = filterNames.get(position);
        holder.filterText.setText(name);
        final Filter f = filters.get(position);
        Bitmap preview = f.processFilter(thumbnailImage.copy(Bitmap.Config.ARGB_8888, true));
        holder.filterThumbnail.setImageBitmap(preview);
        holder.filterThumbnail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                filtersFrag.onThumbnailClick(f);
            }
        });
    }

    @Override
    public int getItemCount() {
        return filters.size();
    }

}

class FilterViewHolder extends RecyclerView.ViewHolder {
    public ImageView filterThumbnail;
    public TextView filterText;
    public RelativeLayout filterContainer;

    public FilterViewHolder(View v){
        super(v);
        filterThumbnail = (ImageView) v.findViewById(R.id.filterThumbnail);
        filterText = (TextView) v.findViewById(R.id.filterName);
        filterContainer = (RelativeLayout) v.findViewById(R.id.filterContainer);
    }

}
