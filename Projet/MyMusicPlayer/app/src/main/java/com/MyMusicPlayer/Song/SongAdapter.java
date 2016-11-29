package com.MyMusicPlayer.Song;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.MyMusicPlayer.R;
import com.MyMusicPlayer.RecyclerViewFastScroll.RecyclerViewClickListener;
import com.MyMusicPlayer.RecyclerViewFastScroll.FastScrollRecyclerView;
import com.MyMusicPlayer.Utilities.MusicUtils;

import java.util.ArrayList;


public class SongAdapter extends RecyclerView.Adapter<SongAdapter.ViewHolder> implements FastScrollRecyclerView.SectionedAdapter
{

    ////////////////
    // Attributes //
    ////////////////

    private ArrayList<Song> songs;                          // The song_tab list
    private int itemLayout;                                 // The resource id of item Layout
    private static RecyclerViewClickListener itemListener;  // The click listener of the recycle view


    //////////////////
    // Constructors //
    //////////////////

    public SongAdapter(ArrayList<Song> theSongs, int p_itemLayout, RecyclerViewClickListener p_itemListener)
    {
        songs = theSongs;
        itemLayout = p_itemLayout;
        itemListener = p_itemListener;
    }


    ////////////////////////
    // Overridden Methods //
    ////////////////////////

    // View Holder methods //

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType)
    {

        // Get inflater and get view by resource id itemLayout
        View v = LayoutInflater.from(parent.getContext()).inflate(itemLayout, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position)
    {

        // Find song_tab by position
        Song song = songs.get(position);

        // Save information in holder
        Log.d("SongAdapter", song.getTitle());
        if (holder != null)
        {
            holder.albumArt.setImageBitmap(song.getBitmap());
            holder.songName.setText(song.getTitle());
            holder.album.setText(song.getAlbum());
            holder.artist.setText(song.getArtist());
            holder.duration.setText(MusicUtils.getDurationToString(song.getDuration()));
            holder.itemView.setTag(song);

            // Color the background, even = classic, odd = classic dark
            if ((position % 2) == 0)
            {
                holder.itemView.setBackgroundResource(R.color.colorPrimary);
            }
            else
            {
                holder.itemView.setBackgroundResource(R.color.colorPrimaryDark);
            }
        }
    }

    @Override
    public long getItemId(int arg0)
    {
        return songs.get(arg0).getSongId();
    }

    @Override
    public int getItemCount()
    {
        return songs.size();
    }

    // FastScrollRecyclerView.SectionedAdapter methods //

    @NonNull
    @Override
    public String getSectionName(int position)
    {
        return "" + songs.get(position).getTitle().charAt(0);
    }


    /////////////
    // Methods //
    /////////////


    /////////////////
    // Other class //
    /////////////////

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener
    {

        ImageView albumArt;
        TextView songName;
        TextView album;
        TextView artist;
        TextView duration;

        ViewHolder(View itemView)
        {
            super(itemView);

            albumArt = (ImageView) itemView.findViewById(R.id.album_art);
            songName = (TextView) itemView.findViewById(R.id.song_title);
            album = (TextView) itemView.findViewById(R.id.song_album);
            artist = (TextView) itemView.findViewById(R.id.song_artist);
            duration = (TextView) itemView.findViewById(R.id.song_duration);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v)
        {
            itemListener.recyclerViewListClickedSong(v, songs.get(this.getLayoutPosition()), this.getLayoutPosition());
        }
    }
}
