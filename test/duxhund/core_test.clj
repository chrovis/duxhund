(ns duxhund.core-test
  (:require [clojure.test :refer [deftest is]]
            [duxhund.core :as dux]))

(deftest generate-seqs-test
  (is (= [["qname1:chr4:12357/A" 1 "R8" "AAAAATGCATGC" "ABBCCCDDDDEE"]
          ["qname1:chr4:12357/B" 1 "L12" "ATGCCCCC" "EEEFFFFF"]
          ["qname1:chr4:12357/B" 2 nil "GCATGGCCAATTGGCCAATT" "FFFFFEEEEEDDDDCCCBBA"]
          ["qname3:chr10:12350/A" 1 "L5" "TGCATGCATGCCCCC" "CDDDDEEEEEFFFFF"]
          ["qname3:chr10:12350/B" 1 "R15" "AAAAA" "ABBCC"]
          ["qname3:chr10:12350/B" 2 nil "GCATGGCCAATTGGCCAATT" "FFFFFEEEEEDDDDCCCBBA"]
          ["qname3:chr10:12359/A" 1 "L14" "GCCCCC" "EFFFFF"]
          ["qname3:chr10:12359/B" 1 "R6" "AAAAATGCATGCAT" "ABBCCCDDDDEEEE"]
          ["qname3:chr10:12359/B" 2 nil "GCATGGCCAATTGGCCAATT" "FFFFFEEEEEDDDDCCCBBA"]
          ["qname4:chr10:12350/A" 1 "R15" "TTTTT" "CCBBA"]
          ["qname4:chr10:12350/B" 1 "L5" "GGGGGCATGCATGCA" "FFFFFEEEEEDDDDC"]
          ["qname4:chr10:12350/B" 2 nil "AATTGGCCAATTGGCCATGC" "ABBCCCDDDDEEEEEFFFFF"]
          ["qname4:chr14:23469/A" 2 "L13" "GCCATGC" "EEFFFFF"]
          ["qname4:chr14:23469/B" 2 "R7" "AATTGGCCAATTG" "ABBCCCDDDDEEE"]
          ["qname4:chr14:23469/B" 1 nil "GGGGGCATGCATGCATTTTT" "FFFFFEEEEEDDDDCCCBBA"]]
         (->> [["qname1" 99 "chr4" 12345 "12M8S" "AAAAATGCATGCATGCCCCC" "ABBCCCDDDDEEEEEFFFFF"]
               ["qname1" 147 "chr4" 23456 "20M" "AATTGGCCAATTGGCCATGC" "ABBCCCDDDDEEEEEFFFFF"]
               ["qname2" 83 "chr10" 12345 "20M" "AAAAATGCATGCATGCCCCC" "ABBCCCDDDDEEEEEFFFFF"]
               ["qname2" 163 "chr10" 23456 "3S17M" "AATTGGCCAATTGGCCATGC" "ABBCCCDDDDEEEEEFFFFF"]
               ["qname3" 99 "chr10" 12345 "5S9M6S" "AAAAATGCATGCATGCCCCC" "ABBCCCDDDDEEEEEFFFFF"]
               ["qname3" 355 "chr10" 12456 "5H9M6H" "*" "*"]
               ["qname3" 147 "chr10" 10234 "20M" "AATTGGCCAATTGGCCATGC" "ABBCCCDDDDEEEEEFFFFF"]
               ["qname4" 83 "chr10" 12345 "5S15M" "AAAAATGCATGCATGCCCCC" "ABBCCCDDDDEEEEEFFFFF"]
               ["qname4" 339 "chr10" 54321 "5H15M" "*" "*"]
               ["qname4" 163 "chr14" 23456 "13M7S" "AATTGGCCAATTGGCCATGC" "ABBCCCDDDDEEEEEFFFFF"]
               ["qname4" 419 "chr14" 65432 "13M7H" "*" "*"]]
              (map (partial zipmap [:qname :flag :rname :pos :cigar :seq :qual]))
              (dux/generate-seqs 5)
              (map (juxt :qname :r1r2 :cutoff :seq :qual))))))

(def ^:private test-qname->seq
  {["qname1" 1] {:seq "AAAAATGCATGCATGCCCCC" :qual "ABBCCCDDDDEEEEEFFFFF"}
   ["qname1" 2] {:seq "GCATGGCCAATTGGCCAATT" :qual "FFFFFEEEEEDDDDCCCBBA"}
   ["qname3" 1] {:seq "AAAAATGCATGCATGCCCCC" :qual "ABBCCCDDDDEEEEEFFFFF"}
   ["qname3" 2] {:seq "GCATGGCCAATTGGCCAATT" :qual "FFFFFEEEEEDDDDCCCBBA"}
   ["qname4" 1] {:seq "GGGGGCATGCATGCATTTTT" :qual "FFFFFEEEEEDDDDCCCBBA"}
   ["qname4" 2] {:seq "AATTGGCCAATTGGCCATGC" :qual "ABBCCCDDDDEEEEEFFFFF"}})

(deftest generate-saved-seqs-test
  (is (= test-qname->seq
         (->> [["qname1" 99 "chr4" 12345 "12M8S" "AAAAATGCATGCATGCCCCC" "ABBCCCDDDDEEEEEFFFFF"]
               ["qname1" 147 "chr4" 23456 "20M" "AATTGGCCAATTGGCCATGC" "ABBCCCDDDDEEEEEFFFFF"]
               ["qname2" 4 "*" 0 "*" "AAAAATGCATGCATGCCCCC" "ABBCCCDDDDEEEEEFFFFF"]
               ["qname2" 4 "*" 0 "*" "AATTGGCCAATTGGCCATGC" "ABBCCCDDDDEEEEEFFFFF"]
               ["qname3" 99 "chr10" 12345 "5S9M6S" "AAAAATGCATGCATGCCCCC" "ABBCCCDDDDEEEEEFFFFF"]
               ["qname3" 355 "chr10" 12456 "5H9M6H" "*" "*"]
               ["qname3" 147 "chr10" 10234 "20M" "AATTGGCCAATTGGCCATGC" "ABBCCCDDDDEEEEEFFFFF"]
               ["qname4" 83 "chr10" 12345 "5S15M" "AAAAATGCATGCATGCCCCC" "ABBCCCDDDDEEEEEFFFFF"]
               ["qname4" 339 "chr10" 54321 "5H15M" "*" "*"]
               ["qname4" 163 "chr14" 23456 "13M7S" "AATTGGCCAATTGGCCATGC" "ABBCCCDDDDEEEEEFFFFF"]
               ["qname4" 419 "chr14" 65432 "13M7H" "*" "*"]]
              (map (partial zipmap [:qname :flag :rname :pos :cigar :seq :qual]))
              dux/generate-saved-seqs))))

(deftest fixup-alignments-test
  (is (= [["qname1:chr4:12357" 99 "12M8S" "AAAAATGCATGCATGCCCCC" "ABBCCCDDDDEEEEEFFFFF"]
          ["qname1:chr4:12357" 355 "12S8M" "AAAAATGCATGCATGCCCCC" "ABBCCCDDDDEEEEEFFFFF"]
          ["qname1:chr4:12357" 147 "20M" "AATTGGCCAATTGGCCATGC" "ABBCCCDDDDEEEEEFFFFF"]
          ;; These alignments will be filtered out because they are incomplete,
          ;; which means one or more alignments in the same triplet was not
          ;; mapped as a primary alignment
          ;; ["qname3:chr10:12350" 64 "5S9M6S" "AAAAATGCATGCATGCCCCC" "ABBCCCDDDDEEEEEFFFFF"]
          ;; ["qname3:chr10:12350" 144 "20M" "AATTGGCCAATTGGCCATGC" "ABBCCCDDDDEEEEEFFFFF"]
          ["qname3:chr10:12359" 419 "14S6M" "GCATGGCCAATTGGCCAATT" "FFFFFEEEEEDDDDCCCBBA"]
          ["qname3:chr10:12359" 163 "11M9S" "GCATGGCCAATTGGCCAATT" "FFFFFEEEEEDDDDCCCBBA"]
          ["qname3:chr10:12359" 83 "20M" "GGGGGCATGCATGCATTTTT" "FFFFFEEEEEDDDDCCCBBA"]
          ["qname4:chr10:12350" 371 "5M15S" "AAAAATGCATGCATGCCCCC" "ABBCCCDDDDEEEEEFFFFF"]
          ["qname4:chr10:12350" 99 "8S12M" "GGGGGCATGCATGCATTTTT" "FFFFFEEEEEDDDDCCCBBA"]
          ["qname4:chr10:12350" 147 "13M7S" "GCATGGCCAATTGGCCAATT" "FFFFFEEEEEDDDDCCCBBA"]]
         (->> [["qname1:chr4:12357/A" 0 "12M" "1:R8"]
               ["qname1:chr4:12357/B" 99 "8M" "1:L12"]
               ["qname1:chr4:12357/B" 147 "20M" "2:"]
               ["qname3:chr10:12350/A" 0 "9M6S" "1:L5"]
               ["qname3:chr10:12350/B" 36 "*" "1:R15"]
               ["qname3:chr10:12350/B" 144 "20M" "2:"]
               ["qname3:chr10:12359/A" 0 "6M" "2:L14"]
               ["qname3:chr10:12359/B" 99 "11M3S" "2:R6"]
               ["qname3:chr10:12359/B" 147 "20M" "1:"]
               ["qname4:chr10:12350/A" 16 "5M" "1:R15"]
               ["qname4:chr10:12350/B" 99 "3S12M" "1:L5"]
               ["qname4:chr10:12350/B" 147 "13M7S" "2:"]]
              (map (fn [[qname flag cigar comment]]
                     {:qname qname :flag flag :cigar cigar :options [{:CO {:value comment}}]}))
              (dux/fixup-alignments test-qname->seq)
              (map (juxt :qname :flag :cigar :seq :qual))))))
