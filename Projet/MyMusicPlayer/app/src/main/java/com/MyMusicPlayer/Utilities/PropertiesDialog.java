package com.MyMusicPlayer.Utilities;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.MyMusicPlayer.Activity.MainActivity;
import com.MyMusicPlayer.R;
import com.MyMusicPlayer.Song.Song;


public class PropertiesDialog extends DialogFragment
{
    ////////////////
    // Attributes //
    ////////////////

    Song targetSong;
    MainActivity activity;

    //////////////////
    // Constructors //
    //////////////////


    public PropertiesDialog newInstance(Song p_targetSong, MainActivity p_activity) {
        PropertiesDialog frag = new PropertiesDialog();
        Bundle args = new Bundle();

        activity = p_activity;
        targetSong = p_targetSong;

        args.putParcelable("Target Song", targetSong);
        frag.setArguments(args);
        return frag;
    }

    ////////////////////////
    // Overridden Methods //
    ////////////////////////

    // DialogFragment methods //

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        // Use the Builder class for convenient dialog construction
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = getActivity().getLayoutInflater();
        @SuppressLint("InflateParams") final View view = inflater.inflate(R.layout.properties, null);

        if (savedInstanceState != null)
        {
            targetSong = savedInstanceState.getParcelable("Target Song");
            activity = targetSong.getActivity();
        }
        else
        {
            ((EditText) view.findViewById(R.id.song_title_prop)).setText(targetSong.getTitle());
            ((EditText) view.findViewById(R.id.album_title_prop)).setText(targetSong.getAlbum());
            ((EditText) view.findViewById(R.id.year_prop)).setText("" + targetSong.getYear());
        }

        // Save button action
        Button saveButton = (Button) view.findViewById(R.id.dialog_ok);
        saveButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                // Get the info of all fields
                String title = ((EditText) view.findViewById(R.id.song_title_prop)).getText().toString();
                String album = ((EditText) view.findViewById(R.id.album_title_prop)).getText().toString();
                String sYear = ((EditText) view.findViewById(R.id.year_prop)).getText().toString();
                int year;

                if (!sYear.equals("")) year = Integer.parseInt(sYear);
                else year = 0;

                // Update target song data
                updateSongData(title, album, year);

                dismiss();
            }
        });

        // Cancel button action
        Button cancelButton = (Button) view.findViewById(R.id.dialog_cancel);
        cancelButton.setOnClickListener(new View.OnClickListener()
        {
            public void onClick(View v)
            {
                dismiss();
            }
        });

        builder.setView(view);
        builder.setMessage(R.string.properties);

        // Create the AlertDialog object and return it
        return builder.create();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable("Target Song", targetSong);
    }


    /////////////
    // Methods //
    /////////////

    public void updateSongData (String title, String album, int year)
    {
        targetSong.setTitle (title);
        targetSong.setAlbum (album);
        targetSong.setYear (year);
        activity.viewPager.setAdapter(activity.adapter); // Refresh the list
    }
}
