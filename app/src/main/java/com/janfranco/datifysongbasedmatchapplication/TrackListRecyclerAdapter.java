package com.janfranco.datifysongbasedmatchapplication;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Typeface;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class TrackListRecyclerAdapter extends RecyclerView.Adapter<TrackListRecyclerAdapter.TrackHolder> {

    private ArrayList<Song> songs;

    private static Typeface metropolisLight;
    private static Typeface metropolisExtraLightItalic;

    TrackListRecyclerAdapter(Context context, ArrayList<Song> songs) {
        metropolisLight = Typeface.createFromAsset(context.getAssets(), "fonts/Metropolis-Light.otf");
        metropolisExtraLightItalic = Typeface.createFromAsset(context.getAssets(), "fonts/Metropolis-ExtraLightItalic.otf");
        this.songs = songs;
    }

    @NonNull
    @Override
    public TrackHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());
        View view = layoutInflater.inflate(R.layout.tracklist_row, parent, false);
        return new TrackHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull TrackHolder holder, int position) {
        Song song = songs.get(position);

        holder.trackName.setText(song.getTrackName());
        holder.artistName.setText(song.getArtistName());
        holder.date.setText(DateFormat.format(
                Constants.DATE_MESSAGE,
                song.getAddDate() * 1000L
        ).toString());
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    static class TrackHolder extends RecyclerView.ViewHolder {

        TextView trackName, artistName, date;

        TrackHolder(@NonNull View itemView) {
            super(itemView);

            trackName = itemView.findViewById(R.id.trackListTrackName);
            artistName = itemView.findViewById(R.id.trackListArtistName);
            date = itemView.findViewById(R.id.trackListAddDate);

            trackName.setTypeface(metropolisLight);
            artistName.setTypeface(metropolisLight);
            date.setTypeface(metropolisExtraLightItalic);
        }
    }

}
