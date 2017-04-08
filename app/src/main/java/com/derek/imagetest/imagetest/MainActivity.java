package com.derek.imagetest.imagetest;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        new StylesheetFetchTask("https://a.thumbs.redditmedia.com/6P4OIeFCpLBkNmc487sF271nPlaZcP-BJo7NXNoXjR0.css"){
            @Override
            protected void onPostExecute(FlairStylesheet flairStylesheet) {
                super.onPostExecute(flairStylesheet);

                flairStylesheet.loadFlairById("morty1", self).into(imageView);
            }
        }.execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}

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

class FlairStylesheet {
    String stylesheetString;
    Dimensions defaultDimension = new Dimensions();
    Location defaultLocation = new Location();

    class Dimensions{
        int width, height;
        Boolean missing = true;
        Dimensions(int width, int height){
            this.width = width;
            this.height = height;
            missing = false;
        }
        Dimensions(){}
    }
    class Location{
        int x, y;
        Boolean missing = true;
        Location(int x, int y){
            this.x = x;
            this.y = y;
            missing = false;
        }
        Location(){}
    }

    FlairStylesheet(String stylesheetString){
        this.stylesheetString = stylesheetString;

        String baseFlairDef = getClass(stylesheetString, "flair");
        if(baseFlairDef == null)    return;

        defaultDimension = getBackgroundSize(baseFlairDef);
        defaultLocation = getBackgroundPosition(baseFlairDef);
    }

    String getClass(String cssDefinitionString, String className){
        Pattern propertyDefinition = Pattern.compile("\\." + className + "\\s*\\{(.+?)\\}");
        Matcher matches = propertyDefinition.matcher(cssDefinitionString);

        if(matches.find()){
            return matches.group(1);
        }else {
            return null;
        }
    }

    String getProperty(String classDefinitionsString, String property){
        Pattern propertyDefinition = Pattern.compile(property + "\\s*:\\s*(.+?)(;|$)");
        Matcher matches = propertyDefinition.matcher(classDefinitionsString);

        if(matches.find()){
            return matches.group(1);
        }else {
            return null;
        }
    }

    String getBackgroundURL(String classDefinitionString){
        Pattern urlDefinition = Pattern.compile("url\\([\"\'](.+?)[\"\']\\)");
        String backgroundProperty = getProperty(classDefinitionString, "background");
        if(backgroundProperty != null){
            // check "background"
            Matcher matches = urlDefinition.matcher(backgroundProperty);
            if(matches.find()){
                String url = matches.group(1);
                if(url.startsWith("//"))    url = "https:" + url;
                return url;
            }
        }
        // either backgroundProperty is null or url cannot be found
        String backgroundImageProperty = getProperty(classDefinitionString, "background-image");
        if(backgroundImageProperty != null){
            // check "background-image"
            Matcher matches = urlDefinition.matcher(backgroundImageProperty);
            if(matches.find()){
                String url = matches.group(1);
                if(url.startsWith("//"))    url = "https:" + url;
                return url;
            }
        }
        // could not find any background url
        return null;
    }

    Dimensions getBackgroundSize(String classDefinitionString){
        Pattern numberDefinition = Pattern.compile("(\\d+)\\s*px");

        String widthProperty = getProperty(classDefinitionString, "width");
        if(widthProperty == null)   widthProperty = getProperty(classDefinitionString, "min-width");
        if(widthProperty == null)   return new Dimensions();

        String heightProperty = getProperty(classDefinitionString, "height");
        if(heightProperty == null)   heightProperty = getProperty(classDefinitionString, "min-height");
        if(heightProperty == null)   return new Dimensions();

        int width, height;
        Matcher matches;

        matches = numberDefinition.matcher(widthProperty);
        if(matches.find())  width = Integer.parseInt(matches.group(1));
        else    return new Dimensions();

        matches = numberDefinition.matcher(heightProperty);
        if(matches.find())  height = Integer.parseInt(matches.group(1));
        else    return new Dimensions();

        return new Dimensions(width, height);
    }

    Location getBackgroundPosition(String classDefinitionString){
        Pattern positionDefinition = Pattern.compile("([+-]?\\d+|0)\\s+([+-]?\\d+|0)\\s*px");

        String backgroundPositionProperty = getProperty(classDefinitionString, "background-position");
        if(backgroundPositionProperty == null)  return new Location();

        Matcher matches  = positionDefinition.matcher((backgroundPositionProperty));
        if(matches.find()){
            return new Location(
                    -Integer.parseInt(matches.group(1)),
                    -Integer.parseInt(matches.group(2))
            );
        }else{
           return new Location();
        }
    }

    RequestCreator loadFlairById(String id, Context context){
        String classDef = getClass(stylesheetString, "flair-" + id);
        if(classDef == null)    return null;
        String backgroundURL = getBackgroundURL(classDef);

        if(backgroundURL != null)
            Log.d("ImageTest", backgroundURL);

        Dimensions flairDimensions = getBackgroundSize(classDef);
        if(flairDimensions.missing) flairDimensions = defaultDimension;
        Log.d("ImageTest", "Dimensions: " + flairDimensions.width + " " + flairDimensions.height);

        Location flairLocation = getBackgroundPosition(classDef);
        if(flairLocation.missing)   flairLocation = defaultLocation;
        Log.d("ImageTest", "Location: " + flairLocation.x + " " + flairLocation.y);

        return Picasso
                .with(context)
                .load(backgroundURL)
                .transform(new CropTransformation(
                        context,
                        flairDimensions.width,
                        flairDimensions.height,
                        flairLocation.x,
                        flairLocation.y
                ));
    }
}