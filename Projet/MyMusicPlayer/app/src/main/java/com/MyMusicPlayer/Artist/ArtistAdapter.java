package com.MyMusicPlayer.Artist;


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

import java.util.ArrayList;

class ArtistAdapter extends RecyclerView.Adapter<ArtistAdapter.ViewHolder> implements FastScrollRecyclerView.SectionedAdapter
{
    ////////////////
    // Attributes //
    ////////////////

    private ArrayList<Artist> artists;                      // The artist list
    private int itemLayout;                                 // The resource id of item Layout
    private static RecyclerViewClickListener itemListener;  // The click listener of the recycle view


    //////////////////
    // Constructors //
    //////////////////

    ArtistAdapter(ArrayList<Artist> theArtists, int p_itemLayout, RecyclerViewClickListener p_itemListener)
    {
        artists = theArtists;
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
        Artist artist = artists.get(position);

        // Save information in holder
        if (holder != null && artist != null)
        {
            Log.d("AlbumAdapter", artist.getArtist());
            holder.albumArt.setImageBitmap(artist.getBitmap());
            holder.artist.setText(artist.getArtist());
            String numOfAlbums = artist.getAlbumList().size() + " " + artist.getActivity().getString(R.string.albums);
            if (artist.getAlbumList().size() == 1) numOfAlbums = numOfAlbums.replace("s", "");
            holder.numberOfAlbums.setText(numOfAlbums);
            holder.itemView.setTag(artist);

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
        return artists.get(arg0).getAlbumID();
    }

    @Override
    public int getItemCount()
    {
        return artists.size();
    }

    // FastScrollRecyclerView.SectionedAdapter methods //

    @NonNull
    @Override
    public String getSectionName(int position)
    {
        return "" + artists.get(position).getArtist().charAt(0);
    }


    /////////////
    // Methods //
    /////////////


    /////////////////
    // Other class //
    /////////////////

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        ImageView albumArt;
        TextView artist;
        TextView numberOfAlbums;

        ViewHolder(View itemView) {
            super(itemView);

            albumArt = (ImageView) itemView.findViewById(R.id.album_art);
            artist = (TextView) itemView.findViewById(R.id.album_artist);
            numberOfAlbums = (TextView) itemView.findViewById(R.id.album_songs_number);

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            itemListener.recyclerViewListClickedArtist(v, artists.get(this.getLayoutPosition()));
        }
    }
}
