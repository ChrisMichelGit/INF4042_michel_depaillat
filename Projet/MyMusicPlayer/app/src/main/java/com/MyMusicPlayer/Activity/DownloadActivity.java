package com.MyMusicPlayer.Activity;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.MyMusicPlayer.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutionException;

public class DownloadActivity extends AppCompatActivity
{
    private static final String DEBUG_TAG = "DownloadActivity";
    String request = "https://api.spotify.com/v1/search?q="; //"https://api.spotify.com/v1/albums/3nFkdlSjzX9mRTtwJOzDYB";
    TextView text, downloadState;
    ImageView image;
    String title, album, artist, imageUrlFinal;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download);

        title = getIntent().getStringExtra("Song title");
        album = getIntent().getStringExtra("Song album");
        artist = getIntent().getStringExtra("Song artist");


        title = title.replaceAll(" ", "+");
        album = album.replaceAll(" ", "+");
        artist = artist.replaceAll(" ", "+");

        constructRequestFromSong();
        Log.d(DEBUG_TAG, "Download Activity created !");

        text = (TextView) findViewById(R.id.answer);
        downloadState = (TextView) findViewById(R.id.download_state);
        image = (ImageView) findViewById(R.id.downloaded_image);
        myClickHandler(null);
    }

    public void constructRequestFromSong ()
    {
        String type = "&type=";
        if (!album.equals("Music"))
        {
            request += "album:" + album;
            type += "album";

            if (!artist.equals("<unknow>"))
            {
                request += "+artist:" + artist;
            }
        }
        else if (!artist.equals("<unknow>"))
        {
            type += "track";
            request += "artist:" + artist;
        }
        else
        {
            type += "track";
            request += title;
        }
        request += type;
    }

    // When user clicks button, calls AsyncTask.
    // Before attempting to fetch the URL, makes sure that there is a network connection.
    public void myClickHandler(View view)
    {
        // Gets the URL from the UI's text field.
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

        if (networkInfo != null && networkInfo.isConnected())
        {
            DownloadWebpageTask downloadThread = new DownloadWebpageTask();
            try
            {
                downloadThread.execute("0", request).get();
            }
            catch (InterruptedException | ExecutionException e)
            {
                e.printStackTrace();
            }

            new DownloadWebpageTask().execute("1", imageUrlFinal);
        }
        else
        {
            downloadState.setText(R.string.result);
            text.setText(R.string.no_connection);
        }
    }

    // Given a URL, establishes an HttpUrlConnection and retrieves
// the web page content as a InputStream, which it returns as
// a string.


    private class DownloadWebpageTask extends AsyncTask<String, Void, String>
    {
        int options = 0;
        String usedUrl;
        InputStream is;
        Bitmap bmp;

        @Override
        protected String doInBackground(String... urls) {

            // params comes from the execute() call: params[0] is the url.
            try {
                options = Integer.parseInt(urls[0]);
                usedUrl = urls [1];
                return downloadUrl(usedUrl);
            } catch (IOException e) {
                return "Unable to retrieve web page. URL may be invalid.";
                //return null;
            }
        }
        // onPostExecute displays the results of the AsyncTask.
        @Override
        protected void onPostExecute(String result) {
            text.setText(result);

            if (options != 0)
            { // Prints teh retrieve bitmap
                if (bmp != null)
                {
                    image.setImageBitmap(bmp);
                }
                else text.setText(getResources().getString(R.string.bad_result));
                downloadState.setText(getResources().getString(R.string.result));
            }

            try
            {
                if (is != null) is.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        private String downloadUrl(String requestUrl) throws IOException
        {
            try
            {
                URL url = new URL(requestUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(10000 /* milliseconds */);
                conn.setConnectTimeout(15000 /* milliseconds */);
                conn.setRequestMethod("GET");
                conn.setDoInput(true);

                // Starts the query
                conn.connect();
                int response = conn.getResponseCode();

                // Log.d(DEBUG_TAG, "The response is: " + conn.getHeaderFields());
                Log.d(DEBUG_TAG, "The response is: " + response);
                is = conn.getInputStream();

                if (options == 0)
                { // Parse the first answer (JSON)
                    BufferedReader streamReader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                    StringBuilder responseStrBuilder = new StringBuilder();

                    // Convert the response to string
                    String inputStr;
                    while ((inputStr = streamReader.readLine()) != null)
                        responseStrBuilder.append(inputStr);

                    parseJSON(responseStrBuilder.toString());
                    return responseStrBuilder.toString();
                }

                else
                { // Convert the response to bitmap
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    int bufferSize = 1024;
                    byte[] buffer = new byte[bufferSize];
                    int len;
                    try
                    {
                        while ((len = is.read(buffer)) != -1)
                        {
                            baos.write(buffer, 0, len);
                        }
                        baos.close();
                        byte[] b = baos.toByteArray();
                        bmp = BitmapFactory.decodeByteArray(b, 0, b.length);
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }
                // Makes sure that the InputStream is closed after the app is
                // finished using it.
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            return null;
        }

        // Parse the response string as JSON file
        void parseJSON (String result)
        {
            try
            {
                Log.d (DEBUG_TAG, result);

                // Looking for the images JSONArray
                JSONObject parser = new JSONObject(result);
                JSONObject jobj = parser.getJSONObject("albums");
                JSONArray imageArray = jobj.getJSONArray("items");
                jobj = imageArray.getJSONObject(0);
                imageArray = jobj.getJSONArray("images");

                Log.d(DEBUG_TAG, imageArray.toString());

                // Looking for the image url
                String imageUrl = imageArray.getString(0);
                int startIndex = imageUrl.indexOf("\"url\":\"") + 7;
                int endIndex = imageUrl.indexOf("\",", startIndex);

                char[] buffer = new char[endIndex-startIndex];

                imageUrl.getChars(startIndex, endIndex, buffer, 0); // Get the Url
                imageUrl = new String(buffer);
                imageUrl = imageUrl.replaceAll("\\\\", ""); // Deletes the \

                text.setText(imageUrl);
                imageUrlFinal = imageUrl;
            }
            catch (JSONException e)
            {
                e.printStackTrace();
            }
        }
    }
}
