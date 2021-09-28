(ns duxhund.cli-test
  (:require [clojure.test :refer [deftest is testing]]
            [duxhund.cli :as cli]))

(deftest parse-opts-test
  (testing "generate-fastq"
    (let [{:keys [options arguments errors]}
          (cli/parse-opts ["--bam" "aligned.bam"
                           "--sorted-bam" "aligned.sorted.bam"
                           "--target" "target.bed"
                           "--output" "out-dir"]
                          cli/generate-fastq-opts)]
      (is (= {:bam "aligned.bam"
              :sorted-bam "aligned.sorted.bam"
              :target "target.bed"
              :output "out-dir"
              :min-softclip-len 20}
             options))
      (is (empty? arguments))
      (is (empty? errors)))
    (let [{:keys [options arguments errors]}
          (cli/parse-opts ["--bam" "aligned.bam"
                           "--sorted-bam" "aligned.sorted.bam"
                           "-t" "target.bed"
                           "-o" "out-dir"
                           "--min-softclip-len" 15]
                          cli/generate-fastq-opts)]
      (is (= {:bam "aligned.bam"
              :sorted-bam "aligned.sorted.bam"
              :target "target.bed"
              :output "out-dir"
              :min-softclip-len 15}
             options))
      (is (empty? arguments))
      (is (empty? errors)))
    (let [{:keys [_options arguments errors]}
          (cli/parse-opts ["--bam" "aligned.bam"
                           "-t" "target.bed"
                           "-o" "out-dir"]
                          cli/generate-fastq-opts)]
      (is (empty? arguments))
      (is (seq errors))))
  (testing "fixup-sam"
    (let [{:keys [options arguments errors]}
          (cli/parse-opts ["--input" "input.bam"
                           "--cache" "cache.edn"
                           "--output" "output.bam"]
                          cli/fixup-sam-opts)]
      (is (= {:input "input.bam"
              :cache "cache.edn"
              :output "output.bam"}
             options))
      (is (empty? arguments))
      (is (empty? errors)))
    (let [{:keys [options arguments errors]}
          (cli/parse-opts ["-i" "input.bam"
                           "-c" "cache.edb"
                           "-o" "output.bam"]
                          cli/fixup-sam-opts)]
      (is (= {:input "input.bam"
              :cache "cache.edb"
              :output "output.bam"}
             options))
      (is (empty? arguments))
      (is (empty? errors)))
    (let [{:keys [_options arguments errors]}
          (cli/parse-opts ["-i" "input.bam"
                           "-o" "output.bam"]
                          cli/fixup-sam-opts)]
      (is (empty? arguments))
      (is (seq errors)))))
