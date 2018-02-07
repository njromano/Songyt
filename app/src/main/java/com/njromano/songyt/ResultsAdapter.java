package com.njromano.songyt;

import android.content.Intent;
import android.net.Uri;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import static android.support.v4.content.ContextCompat.startActivity;

/**
 * Created by Nick on 1/31/18.
 */

public class ResultsAdapter extends RecyclerView.Adapter<ResultsAdapter.ViewHolder> {
    private ArrayList<YouTubeResult> mDataset;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public static class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public TextView mTitle, mChannel;
        public ImageView mImage;
        public ViewHolder(View v) {
            super(v);
            mTitle = v.findViewById(R.id.result_title);
            mChannel = v.findViewById(R.id.result_channel);
            mImage = v.findViewById(R.id.result_image);
        }
    }

    // Provide a suitable constructor (depends on the kind of dataset)
    public ResultsAdapter(ArrayList<YouTubeResult> myDataset) {
        mDataset = myDataset;
    }

    // Create new views (invoked by the layout manager)
    @Override
    public ResultsAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                   int viewType) {
        // create a new view
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.results_card, parent, false);
        // set the view's size, margins, paddings and layout parameters

        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        // - get element from your dataset at this position
        // - replace the contents of the view with that element
        final YouTubeResult result = mDataset.get(position);
        holder.mTitle.setText(result.getTitle());
        holder.mChannel.setText(result.getChannel());
        new YouTubeResult.DownloadImageTask(holder.mImage).execute(result.getImageURL());

        final String resultID = result.getVideoId();
        holder.mImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String result = "https://www.youtube.com/watch?v=" + resultID;
                view.getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(result)));
            }
        });
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return mDataset.size();
    }

    public void setItems(ArrayList<YouTubeResult> in)
    {
        mDataset = in;
    }
}
