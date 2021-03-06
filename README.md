[![Build Status](https://travis-ci.org/dasdy/mups.svg?branch=master)](https://travis-ci.org/dasdy/mups)

## What does this thing actually do

Here is the problem I believe some of us face regularly: you have large audio library, a with few hundred (thousand?) artists. It becomes really tedious to search whether somebody made a new release - you can't rely on large music news sources since they might ignore some of your favourites. Well, with this thing you can scan your media library, and look up into last.fm to check whether anybody in your library made a new album.

App provides CLI to scan your music library, filter unneeded authors and scan for albums in last.fm that are missing from yours

## Usage:
### Command-line
1. Set your environment variable `LASTFM_API` and `LASTFM_PRIVATEKEY`.
Alternatively, before deploying, put files `api_key` and `shared_secret` into `resources` folder (near `src` and `test`)

2. When run, you can use following options:
  * `-m` or `--music-path` - path to your music library. If omitted, the `-c` option must be provided
  * `-c` or `--cached-path` - path to result of previous launch of a program. If used, music-path option can be omitted.
  * `-o` or `--output` - where to save results of scan. Defaults to `cached-path`. If it is also not given, its `diff.html`
  * `-i` or `--ignore-path` - path to file to ignore certain authors or albums

Example : `lein run --music-path=/home/user/Music`


### Clojure repl
Since it is written in clojure, you get the chance to hack around as you wish. Functions that might interest you are:

#### Scan your music library
```clojure
(build-user-collection mpath cachepath ignored-stuff)
```
only scan your library, without any looking-up from the web. Parameters :
  * `mpath` - path to your music library
  * `cachepath` - path to resulting cache file (if it already exists, result of this scan will be merged into cache file)
  * `ignored-stuff` - clojure representation of content of ignore-file

##### Example:
```clojure
   (build-user-collection "/home/user/Music" "/home/user/Music/lib-cache.json" nil)
```

#### Build difference
```clojure
(build-diff user-collection ignored-stuff lastfmpath)
```
build difference between user-collection and whatever will be looked up in last.fm . Parameters:
   * `user-collection` result of call of `build-user-collection`
   * `ignored-stuff` - clojure representation of content of ignore-file
   * `lastfmpath` - path to result of lookup of your library in last.fm

#### Saving diff

```clojure
(save-diff diff-writer diff outputpath)
```

Persist diff onto storage

   * `diff-writer` - either `:html` (default value) or `:json` - for more hackable output
   * `diff` - diff object, built using `build-diff` function
   * `outputpath` - path to file, where diff should be serialized

#### Filter unlistened authors
```clojure
(author-is-listened [[author-name author-info]])
```
a function used by `build-user-collection` to detect whether you really listen to this author or are your just randomly have this 1 song of his. You might hack around to get different results if my filter doesn't work for you. Similar to previous function, but goes over last.fm results to skip albums you might not need:

```clojure
(remove-singles [collection])
```



### File formats:
1. Collections (cache of music library, cache of last.fm lookup results) - json file of following format:
```JSON
{"author_name": {"album": {"song-count": 10,
                           "album-url": "/album/url",
                           "image-url" : "/album/image/url"
                  }}}
```

Fields `album-url` and `image-url` are only present for last.fm cache file

2. Ignore file - json file to provide facility of ignoring entire bands, all albums with a certain title, or certain albums of some author:
  ```JSON
  { "ignored_authors" : ["author name"],
    "ignored_albums": ["album title"],
    "ignored_author_albums": [{"author" : ["album title"]}]
  }
  ```


3. Diff file - static HTML with artists, grouped by alphabet.
