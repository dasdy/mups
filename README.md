# mp3-update-scanner
Scan your mp3 library and find if bands you listen to released new albums!

[![Build Status](https://travis-ci.org/dasdy/mp3-update-scanner.svg?branch=master)](https://travis-ci.org/dasdy/mp3-update-scanner)


## Usage:
1. Set your environment variable LASTFM_API and LASTFM_PRIVATEKEY. Alternatively, before deploying, put files `api_key` and `shared_secret` into `resources` folder (near `src` and `test`)

2. When run, you can use following options:
  * `-m` or `--music-path` - path to your music library
  * `-c` or `--cached-path` - path to result of previous launch of a program. If used, music-path option can be omitted. 
  * `-o` or `--output` - where to save results of scan. Defaults to `cached-path`. If it is also not given, its `out.json`
  * `-i` or `--ignore-path` - path to file to ignore certain authors or albums

Example : `java -jar mp3-update-scanner --music-path=/home/user/Music --cached-path=prev_results.json`
