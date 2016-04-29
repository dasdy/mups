(ns mp3-update-scanner.core-test
  (:require [clojure.test :refer :all]
            [mp3-update-scanner.core :refer :all]))

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
                               {:artist "author" :album "album2"}))
         {"author" {"album" 1 "album2" 1}
            "author2" {"album" 1}}))))
