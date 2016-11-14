package com.example.jul.mymusicplayer;

import android.annotation.SuppressLint;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;


class SongAdapter extends RecyclerView.Adapter<SongAdapter.ViewHolder> {

    ////////////////
    // Attributes //
    ////////////////

    private ArrayList<Song> songs;  // The song list
    private int itemLayout;         // The resource id of item Layout
    private static RecyclerViewClickListener itemListener;


    //////////////////
    // Constructors //
    //////////////////

    SongAdapter(ArrayList<Song> theSongs, int p_itemLayout, RecyclerViewClickListener p_itemListener){
        songs = theSongs;
        itemLayout = p_itemLayout;
        itemListener = p_itemListener;
    }


    ////////////////////////
    // Overridden Methods //
    ////////////////////////

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        // Get inflater and get view by resource id itemLayout
        View v = LayoutInflater.from(parent.getContext()).inflate(itemLayout, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        // Find song by position
        Song song = songs.get(position);

        // Save information in holder
        Log.d ("SongAdapter", song.getTitle());
        holder.albumArt.setImageBitmap(song.getBitmap());
        holder.songName.setText(song.getTitle());
        holder.album.setText(song.getAlbum());
        holder.artist.setText(song.getArtist());
        holder.duration.setText(song.getDurationToString());
        holder.itemView.setTag(song);

        // Color the background, even = classic, odd = classic dark
        if ((position % 2) == 0) {
            holder.itemView.setBackgroundResource(R.color.colorPrimary);
        } else {
            holder.itemView.setBackgroundResource(R.color.colorPrimaryDark);
        }
    }

    @Override
    public long getItemId(int arg0) {
        return songs.get(arg0).getSongId();
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }


    /////////////
    // Methods //
    /////////////



    /////////////////
    // Other class //
    /////////////////

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        ImageView albumArt;
        TextView songName;
        TextView album;
        TextView artist;
        TextView duration;

        ViewHolder(View itemView) {
            super(itemView);

            albumArt = (ImageView)itemView.findViewById(R.id.album_art);
            songName = (TextView)itemView.findViewById(R.id.song_title);
            album = (TextView)itemView.findViewById(R.id.song_album);
            artist = (TextView)itemView.findViewById(R.id.song_artist);
            duration = (TextView)itemView.findViewById(R.id.song_duration);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v)
        {
            Log.d ("SongAdapter", "MABBBB");
            itemListener.recyclerViewListClicked(v, this.getLayoutPosition());
        }
    }
}
