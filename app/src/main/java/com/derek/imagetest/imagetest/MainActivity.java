package com.derek.imagetest.imagetest;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        final Context self = this;
        final ViewGroup content = (ViewGroup) findViewById(R.id.content);

        // fetch and parse stylesheet
        new StylesheetFetchTask("https://a.thumbs.redditmedia.com/6P4OIeFCpLBkNmc487sF271nPlaZcP-BJo7NXNoXjR0.css"){
            @Override
            protected void onPostExecute(FlairStylesheet flairStylesheet) {
                super.onPostExecute(flairStylesheet);

                // remove pending message
                View pending_view = findViewById(R.id.pending_view);
                content.removeView(pending_view);

                // display list
                View contentView = getLayoutInflater().inflate(R.layout.content_main, null);
                content.addView(contentView);

                List<String> ids = flairStylesheet.getListOfFlairIds();
                ListView list = (ListView) contentView.findViewById(R.id.list);
                list.setAdapter(new FlairListDisplayAdapter(self, ids.toArray(new String[ids.size()]), flairStylesheet));
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

class FlairListDisplayAdapter extends ArrayAdapter<String>{
    private final Context context;
    private final String[] ids;
    private FlairStylesheet flairStylesheet;

    FlairListDisplayAdapter(Context context, String[] ids, FlairStylesheet flairStylesheet){
        super(context, -1, ids);
        this.context = context;
        this.ids = ids;
        this.flairStylesheet = flairStylesheet;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View entry = convertView != null ? convertView : inflater.inflate(R.layout.flair_entry, parent, false);

        String id = ids[position];

        ((TextView) entry.findViewById(R.id.flairText)).setText(id);

        ImageView imageView = (ImageView) entry.findViewById(R.id.flair);

        Picasso.with(context).cancelRequest(imageView);
        flairStylesheet.loadFlairById(id, context).into(imageView);

        imageView.getLayoutParams().width = (int) (flairStylesheet.prevDimension.width * context.getResources().getDisplayMetrics().density);
        imageView.getLayoutParams().height = (int) (flairStylesheet.prevDimension.height * context.getResources().getDisplayMetrics().density);

        return entry;
    }
}