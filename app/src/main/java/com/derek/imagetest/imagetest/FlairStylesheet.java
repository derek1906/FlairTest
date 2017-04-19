package com.derek.imagetest.imagetest;

import android.content.Context;
import android.util.Log;

import com.squareup.picasso.MemoryPolicy;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;
import com.squareup.picasso.Transformation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
    String defaultURL = "";

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
        Boolean isPercentage = false;
        Boolean missing = true;
        Location(int x, int y){
            this.x = x;
            this.y = y;
            missing = false;
        }
        Location(int x, int y, boolean isPercentage) {
            this.x = x;
            this.y = y;
            this.isPercentage = isPercentage;
            missing = false;
        }
        Location(){}
    }

    FlairStylesheet(String stylesheetString){
        this.stylesheetString = stylesheetString;

        String baseFlairDef = getClass(stylesheetString, "flair");
        if(baseFlairDef == null)    return;

        // Attempts to find default dimension, offset and image URL
        defaultDimension = getBackgroundSize(baseFlairDef);
        defaultLocation = getBackgroundPosition(baseFlairDef);
        defaultURL = getBackgroundURL(baseFlairDef);
    }

    /**
     * Get class definition string by class name.
     * @param cssDefinitionString
     * @param className
     * @return
     */
    String getClass(String cssDefinitionString, String className){
        Pattern propertyDefinition = Pattern.compile("\\." + className + "(,[^\\{]*)*\\{(.+?)\\}");
        Matcher matches = propertyDefinition.matcher(cssDefinitionString);

        String properties = null;

        while(matches.find()){
            if(properties == null)  properties = "";
            properties = matches.group(2) + ";" + properties;   // append properties to simulate property overriding
        }

        return properties;
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

        // check common properties used to define width
        String widthProperty = getProperty(classDefinitionString, "width");
        if(widthProperty == null)   widthProperty = getProperty(classDefinitionString, "min-width");
        if(widthProperty == null)   widthProperty = getProperty(classDefinitionString, "text-indent");
        if(widthProperty == null)   return new Dimensions();

        // check common properties used to define height
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
        Pattern positionDefinitionPx = Pattern.compile("([+-]?\\d+|0)px\\s+([+-]?\\d+|0)px"),
                positionDefinitionPercentage = Pattern.compile("([+-]?\\d+|0)%\\s+([+-]?\\d+|0)%");

        String backgroundPositionProperty = getProperty(classDefinitionString, "background-position");
        if(backgroundPositionProperty == null)  return new Location();

        Matcher matches = positionDefinitionPx.matcher(backgroundPositionProperty);
        if(matches.find()){
            return new Location(
                    -Integer.parseInt(matches.group(1)),
                    -Integer.parseInt(matches.group(2))
            );
        }else{
            matches = positionDefinitionPercentage.matcher(backgroundPositionProperty);
            if(matches.find()){
                return new Location(
                        Integer.parseInt(matches.group(1)),
                        Integer.parseInt(matches.group(2)),
                        true
                );
            }
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
        if(backgroundURL == null)   backgroundURL = defaultURL;

        Dimensions flairDimensions = getBackgroundSize(classDef);
        if(flairDimensions.missing) flairDimensions = defaultDimension;

        prevDimension = flairDimensions;

        Location flairLocation = getBackgroundPosition(classDef);
        if(flairLocation.missing)   flairLocation = defaultLocation;

        Transformation transformation;
        if(flairLocation.isPercentage){
            transformation = new CropTransformation(
                    context,
                    id,
                    flairDimensions.width,
                    flairDimensions.height,
                    flairLocation.x,
                    flairLocation.y,
                    true
            );
        }else{
            transformation = new CropTransformation(
                    context,
                    id,
                    flairDimensions.width,
                    flairDimensions.height,
                    flairLocation.x,
                    flairLocation.y
            );
        }


        return Picasso
                .with(context)
                .load(backgroundURL)
                .transform(transformation);
    }

    /**
     * Util function
     * @return
     */
    List<String> getListOfFlairIds(){
        Pattern flairId = Pattern.compile("\\.flair-(\\w+)[^\\{]*\\{");
        Matcher matches  = flairId.matcher(stylesheetString);

        List<String> flairIds = new ArrayList<>();
        while(matches.find()){
            flairIds.add(matches.group(1));
        }

        Collections.sort(flairIds);
        return flairIds;
    }
}