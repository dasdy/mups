(ns mp3-update-scanner.core-test
  (:require [clojure.test :refer :all]
            [mp3-update-scanner.core :refer :all]
            [mp3-update-scanner.libscan :refer :all]
            [mp3-update-scanner.lastfm :refer :all]))

(deftest add-author-info-tests
  (testing "add-author to existing authors"
    (is (= (add-author-info {:artist "artist1" :album "album"}
                            {"artist1" {"album" 1}})
           {"artist1" {"album" 2}}))
    (is (= (add-author-info {:artist "artist1" :album "album"}
                            {"artist1" {"album" 1}
                             "artist2" {"album3" 6}
                             "artist3" {"album4" 8}})
           {"artist1" {"album" 2}
            "artist2" {"album3" 6}
            "artist3" {"album4" 8}}))
    (is (= (add-author-info {:artist "artist1" :album "album2"}
                            {"artist1" {"album" 1
                                        "album2" 3}
                             "artist2" {"album3" 6}
                             "artist3" {"album4" 8}})
           {"artist1" {"album" 1
                       "album2" 4}
            "artist2" {"album3" 6}
            "artist3" {"album4" 8}})))
  (testing "add-author to non-existing authors"
    (is (= (add-author-info {:artist "artist1" :album "album"}
                            {})
           {"artist1" {"album" 1}}))
    (is (= (add-author-info {:artist "artist2" :album "album"}
                            {"artist1" {"album" 1}})
           {"artist1" {"album" 1}
            "artist2" {"album" 1}})))
  (testing "add-author to authors, without albums"
    (is (= (add-author-info {:artist "artist1" :album "album2"}
                            {"artist1" {"album" 9}})
           {"artist1" {"album" 9
                       "album2" 1}}))))

(deftest author-song-count-tests
  (testing "on empty collection"
    (is (= 0 (author-song-count {} "author")))
    (is (= 0 (author-song-count {"author2" {"album2" 5}} "author"))))
  (testing "count tests"
    (is (= 5 (author-song-count {"author" {"album1" 5}} "author")))
    (is (= 5 (author-song-count {"author" {"album1" 2 "album2" 3}} "author")))))

(deftest build-collection-tests
  (testing "building collection of mp3 info data"
    (is (= (build-collection '({:artist "author2" :album "album"}
                               {:artist "author" :album "album"}
                               {:artist "author" :album "album2"})
                             {})
         {"author" {"album" 1 "album2" 1}
          "author2" {"album" 1}}))
    (is (= (build-collection '({:artist "author2" :album "album"}
                               {:artist "author" :album "album"}
                               {:artist "author" :album "album2"})
                             {"author" {"album" 10}})
         {"author" {"album" 11 "album2" 1}
          "author2" {"album" 1}}))))

(defn file-mock
  "object with getName and getPath properties, same as path"
  [path filename]
  (proxy [java.io.File] [path]
    (getName [] filename)
    (getPath [] (str path "/" filename))))

(deftest scan-mp3-in-folder-tests
  (let [folder "folder"
        file-list (fn [& args] (map #(file-mock folder %) args))]
   (testing "searching in folder without files"
     (with-redefs [file-seq (constantly (file-list "file1" "file2"))]
       (is (= (get-all-mp3-in-dir ".")
              '())))
     (with-redefs [file-seq (constantly (file-list))]
       (is (= (get-all-mp3-in-dir ".")
              '()))))
   (testing "searching for files"
     (with-redefs [file-seq (constantly (file-list "file1.mp3" "file2.mp3" "file3.mp3" "file4"))]
       (is (= (set (map #(.getName %) (get-all-mp3-in-dir ".")))
              #{"file1.mp3" "file2.mp3" "file3.mp3"}))))))

(deftest remove-trailing-0-tests
  (testing "should remove 0 at end"
    (is (= "mystring"
           (remove-trailing-0 (list \m \y \s \t \r \i \n \g 0))))))

(deftest author-is-listened-tests
  (testing "is-listened?"
    (is (author-is-listened ["author" {"a" 5 "b" 9}]))
    (is (not (author-is-listened ["author" {"a" 5}])))
    (is (author-is-listened ["author" {"a" 1 "b" 1 "c" 1 "d" 1 "e" 1 "f" 2}]))
    (is (not (author-is-listened ["author" {"a" 1 "b" 2 "c" 1}]))))
  (testing "same thing on a list"
    (is (= {"author1" {"a" 5 "b" 9}
            "author3" {"a" 1 "b" 1 "c" 1 "d" 1 "e" 1 "f" 2}}
           (only-listened-authors {"author1" {"a" 5 "b" 9}
                                   "author2" {"a" 5}
                                   "author3" {"a" 1 "b" 1 "c" 1 "d" 1 "e" 1 "f" 2}
                                   "author4" {"a" 1 "b" 2 "c" 1}})))))

(deftest cli-args-tests
  (testing "cli-args-values"
    (is (= ["music" "cache" "out" "ignore" "lastfm"]
           (parse-prog-options ["--ignore-path=ignore" "--output=out"
                                "--music-path=music" "--cached-path=cache"
                                "--lastfm=lastfm"])))
    (is (= ["music" "cache" "diff.json" "ignore" "lastfm.json"]
           (parse-prog-options ["--ignore-path=ignore"
                                "--music-path=music"
                                "--cached-path=cache"])))
    (is (= ["music" nil "diff.json" nil "lastfm.json"]
           (parse-prog-options ["--music-path=music"])))
    (is (validate-args ["music" nil "diff.json" nil nil]))
    (is (validate-args [nil "cache" nil nil nil]))
    (is (not (validate-args [nil nil "diff.json" "ignore.json" nil])))))

(deftest ignore-tests
  (testing "ignore-test"
    (is (= (remove-ignored {"author" {"album1" 1 "album2" 2 "album3" 3 "album4" 4}
                            "author2" {"album1" 2 "album4" 5}}
                           {"authors" ["author2"]
                            "albums" ["album1" "album3"]
                            "author_albums" {"author" ["album2"]}})
           {"author" {"album4" 4}}))
    (is (= (remove-singles {"author" {"s" 1 "s3" 2}
                            "author2" {"x" 2 "k" 1}})
           {"author" {"s3" 2}
            "author2" {"x" 2}}))))

(deftest diff-tests
  (testing "find missing albums in one author"
    (is (= (find-author-missing-albums {"a" 15 "b" 15} {"a" 1 "b" 1 "c" 1})
           {"you have" {}, "you miss" #{"c"}, "both have" #{"a" "b"}}))))

(deftest serialization-tests
  (testing "save-collection"
    (with-local-vars [ file-buf nil]
      (with-redefs [spit (fn [path str] (var-set file-buf str))]
        (do (save-collection {"a" {"b" 1}} "some-path")
            (is (= @file-buf "{\n  \"a\" : [\n    \"b\"\n  ]\n}"))))))
  (testing "read-collection"
    (with-redefs [slurp (fn [_] "{\"a\":[\"b\"]}")]
      (is (= (read-collection "a.json")
             {"a" ["b"]})))))

(deftest get-author-from-lastfm-tests
  (testing "request-test")
  (testing "url test"
    (is (re-seq
         #"http://[a-zA-Z.0-9/]+\?method=artist\.gettopalbums&artist=ArtistName&api_key=[a-zA-Z0-9]+&format=json"
         (lastfm-getalbums-url "ArtistName"))))
  (testing "get-authors-from-collection"
    (with-redefs [concur-get
                  (fn [urls]
                    (repeat (count urls)
                            {:body "dummy_response_body"}))
                  author-response->author-info
                  (fn [body] {"album" 1 "album2" 1 "album3" 1})]
     (is (= {"author1" {"album" 1 "album2" 1 "album3" 1}
             "author2" {"album" 1 "album2" 1 "album3" 1}
             "author3" {"album" 1 "album2" 1 "album3" 1}}
            (get-authors-from-lastfm {"author1" {"album" 12}
                                      "author2" {"album2" 3}
                                      "author3" {"album3" 5}})))))
  (testing "response -> album-info"
    (is (= (albums-from-lastfm {"topalbums" {"album" [{"name" "album1"
                                                       "artist" {"name" "artist1"}}
                                                      {"name" "album2"
                                                       "artist" {"name" "artist1"}}]}})
           {"album1" 1 "album2" 1})))
  (testing "is-error-response"
    (is (is-error-response {"error" 15 "message" "some error message"}))
    (is (not (is-error-response {"topalbums" {"album" [{"name" "album1"}
                                                       {"name" "album2"}]}})))))



