package com.MyMusicPlayer.Album;


import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.MyMusicPlayer.R;
import com.MyMusicPlayer.RecyclerViewFastScroll.FastScrollRecyclerView;
import com.MyMusicPlayer.RecyclerViewFastScroll.RecyclerViewClickListener;
import com.MyMusicPlayer.Utilities.MusicUtils;

import java.util.ArrayList;

class AlbumAdapter extends RecyclerView.Adapter<AlbumAdapter.ViewHolder> implements FastScrollRecyclerView.SectionedAdapter
{
    ////////////////
    // Attributes //
    ////////////////

    private ArrayList<Album> albums;                        // The album list
    private int itemLayout;                                 // The resource id of item Layout
    private static RecyclerViewClickListener itemListener;  // The click listener of the recycle view


    //////////////////
    // Constructors //
    //////////////////

    AlbumAdapter(ArrayList<Album> theAlbums, int p_itemLayout, RecyclerViewClickListener p_itemListener)
    {
        albums = theAlbums;
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

        // Find song by position
        Album album = albums.get(position);

        // Save information in holder
        if (holder != null && album != null)
        {
            Log.d("AlbumAdapter", album.getTitle());
            holder.albumArt.setImageBitmap(album.getBitmap());
            holder.artist.setText(album.getArtist());
            holder.albumName.setText(album.getTitle());
            holder.duration.setText(MusicUtils.getDurationToString(album.getDuration()));
            String numOfSongs = album.getSongList().size() + " " + album.getActivity().getString(R.string.tracks);
            if (album.getSongList().size() == 1) numOfSongs = numOfSongs.replace("s", "");
            holder.numberOfSongs.setText(numOfSongs);
            holder.itemView.setTag(album);

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
        return albums.get(arg0).getAlbumID();
    }

    @Override
    public int getItemCount()
    {
        return albums.size();
    }

    // FastScrollRecyclerView.SectionedAdapter methods //

    @NonNull
    @Override
    public String getSectionName(int position)
    {
        return "" + albums.get(position).getTitle().charAt(0);
    }


    /////////////
    // Methods //
    /////////////


    /////////////////
    // Other class //
    /////////////////

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        ImageView albumArt;
        TextView albumName;
        TextView artist;
        TextView duration;
        TextView numberOfSongs;

        ViewHolder(View itemView) {
            super(itemView);

            albumArt = (ImageView) itemView.findViewById(R.id.album_art);
            albumName = (TextView) itemView.findViewById(R.id.album_title);
            artist = (TextView) itemView.findViewById(R.id.album_artist);
            duration = (TextView) itemView.findViewById(R.id.album_duration);
            numberOfSongs = (TextView) itemView.findViewById(R.id.album_songs_number);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            itemListener.recyclerViewListClickedAlbum(v, albums.get(this.getLayoutPosition()));
        }
    }
}
