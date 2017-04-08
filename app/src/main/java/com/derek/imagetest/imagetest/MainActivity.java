package com.derek.imagetest.imagetest;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.ImageView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final ImageView imageView = (ImageView) findViewById(R.id.flair);
        imageView.getLayoutParams().width = 200;
        imageView.getLayoutParams().height = 200;

        final Context self = this;
        // fetch and parse stylesheet
        new StylesheetFetchTask("https://a.thumbs.redditmedia.com/6P4OIeFCpLBkNmc487sF271nPlaZcP-BJo7NXNoXjR0.css"){
            @Override
            protected void onPostExecute(FlairStylesheet flairStylesheet) {
                super.onPostExecute(flairStylesheet);

                // search for flair with id "summer"
                flairStylesheet.loadFlairById("summer", self).into(imageView);
            }
        }.execute();
    }

}

// fetch stylesheet from the web
class StylesheetFetchTask extends AsyncTask<Void, Void, FlairStylesheet>{
    String url;

    StylesheetFetchTask(String url){
        super();
        this.url = url;
    }

    @Override
    protected FlairStylesheet doInBackground(Void... params) {
        try{
            URL stylesheetURL = new URL(url);
            BufferedReader reader = new BufferedReader(new InputStreamReader(stylesheetURL.openStream()));
            String stylesheet = "";
            String line;
            while((line = reader.readLine()) != null){
                stylesheet += line;
            }

            return new FlairStylesheet(stylesheet);
        } catch (MalformedURLException e) {
            return null;
        } catch (IOException e) {
            return null;
        }
    }
}