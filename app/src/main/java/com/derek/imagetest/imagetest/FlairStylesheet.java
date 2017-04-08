package com.derek.imagetest.imagetest;

import android.content.Context;
import android.util.Log;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class for holding stylesheet information.
 * Parses stylesheet with regex.
 *
 * Proof of concept only.
 *
 * It first attempts to find general properties for all flairs,
 * when a routine requests to load a flair into an ImageView, it
 * will then:
 *
 * 1. Finds the class definition for "flair-REQUESTED_ID".
 * 2. Finds match for "background: url('...')" or "background-image: url('...')".
 * 3. Finds offset and dimensions.
 *
 * Image is loaded via Picasso.
 */
class FlairStylesheet {
    String stylesheetString;
    Dimensions defaultDimension = new Dimensions();
    Location defaultLocation = new Location();

    Dimensions prevDimension = null;

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

        // Attempts to find default dimension and offset
        defaultDimension = getBackgroundSize(baseFlairDef);
        defaultLocation = getBackgroundPosition(baseFlairDef);
    }

    /**
     * Get class definition string by class name.
     * @param cssDefinitionString
     * @param className
     * @return
     */
    String getClass(String cssDefinitionString, String className){
        Pattern propertyDefinition = Pattern.compile("\\." + className + "\\s*\\{(.+?)\\}");
        Matcher matches = propertyDefinition.matcher(cssDefinitionString);

        if(matches.find()){
            return matches.group(1);
        }else {
            return null;
        }
    }

    /**
     * Get property value inside a class definition by property name.
     * @param classDefinitionsString
     * @param property
     * @return
     */
    String getProperty(String classDefinitionsString, String property){
        Pattern propertyDefinition = Pattern.compile(property + "\\s*:\\s*(.+?)(;|$)");
        Matcher matches = propertyDefinition.matcher(classDefinitionsString);

        if(matches.find()){
            return matches.group(1);
        }else {
            return null;
        }
    }

    /**
     * Get flair background url in class definition.
     * @param classDefinitionString
     * @return
     */
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

    /**
     * Get background dimension in class definition.
     * @param classDefinitionString
     * @return
     */
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

    /**
     * Get background offset in class definition.
     * @param classDefinitionString
     * @return
     */
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

    /**
     * Request a flair by flair id. `.into` can be chained onto this method call.
     * @param id
     * @param context
     * @return
     */
    RequestCreator loadFlairById(String id, Context context){
        String classDef = getClass(stylesheetString, "flair-" + id);
        if(classDef == null)    return null;
        String backgroundURL = getBackgroundURL(classDef);

        if(backgroundURL != null)
            Log.d("ImageTest", backgroundURL);

        Dimensions flairDimensions = getBackgroundSize(classDef);
        if(flairDimensions.missing) flairDimensions = defaultDimension;
        Log.d("ImageTest", "Dimensions: " + flairDimensions.width + " " + flairDimensions.height);

        prevDimension = flairDimensions;

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

    /**
     * Util function
     * @return
     */
    List<String> getListOfFlairIds(){
        Pattern flairId = Pattern.compile("\\.flair-(\\w+)\\s*\\{");
        Matcher matches  = flairId.matcher(stylesheetString);

        List<String> flairIds = new ArrayList<>();
        while(matches.find()){
            flairIds.add(matches.group(1));
        }
        return flairIds;
    }
}