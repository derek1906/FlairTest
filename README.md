# FlairTest
Reddit flair test

A proof of concept of retrieving flair images on Android through parsing subreddit stylesheets.

## Screenshot
<img src="https://github.com/derek1906/FlairTest/blob/master/screenshot.png" width="300">
Figure 1: A list of flairs found in the /r/rickandmorty stylesheet. Currently flairs are cached but not the size/offset information.

## Analysis
# Performance
Even when it "parses" the the stylesheet everytime a flair is request, impact on performance was not noticable. (Tested with a relatively large amount of /r/rickandmorty flairs.)

#Reliability
During testing, different subreddits used various techniques in setting the dimensions of the flair image, such as `width`, `min-width`, and `text-indent` etc. for width. Currently it works with all subreddits with flairs, however it might fail to read flair information in hack-ish CSS rules.

## GIF backgrounds
Picasso does not support GIFs, but can be replaced with Glide to add support for GIFs.
