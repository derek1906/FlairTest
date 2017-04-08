package com.derek.imagetest.imagetest;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

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

                List<String> ids = flairStylesheet.getListOfFlairIds();
                Log.d("ImageTest", Arrays.toString(ids.toArray()));

                // display a random flair
                String id = ids.get(new Random().nextInt(ids.size()));

                ((TextView) findViewById(R.id.flairText)).setText(id);

                flairStylesheet.loadFlairById(id, self).into(imageView);
                imageView.getLayoutParams().width = (int) (flairStylesheet.prevDimension.width * self.getResources().getDisplayMetrics().density);
                imageView.getLayoutParams().height = (int) (flairStylesheet.prevDimension.height * self.getResources().getDisplayMetrics().density);
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
            Log.d("ImageTest", "Malformed URL Exception");
            return null;
        } catch (IOException e) {
            Log.d("ImageTest", "IO Exception");
            return null;
        }
    }
}