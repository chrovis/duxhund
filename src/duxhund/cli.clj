(ns duxhund.cli
  (:require [clj-sub-command.core :as sub]
            [clojure.set :as set]
            [clojure.string :as str]
            [clojure.tools.cli :as cli]
            [duxhund.core :as dux])
  (:gen-class))

(defn- as-int! [s]
  (cond-> s
    (not (number? s)) Integer/parseInt))

(defn parse-opts
  "Wrapper function for cli/parse-opts. Checks if required options exist."
  [args option-specs]
  (let [s (#'cli/compile-option-specs option-specs) ;; TODO: private fn
        _ (when (some (comp #{"-h"} :short-opt) s)
            (throw (ex-info "\"-h\" will be overwritten"
                            {:args args, :option-specs option-specs})))
        with-help (conj option-specs ["-h" "--help" "Print help"])
        required-ids (into #{} (comp (filter :required) (map :id)) s)
        {:keys [options errors]
         :as parsed} (cli/parse-opts args with-help :strict true)
        missing (set/difference required-ids (set (keys options)))]
    (cond-> parsed
      (and (not errors) (seq missing))
      (assoc :errors (->> s
                          (filter (comp missing :id))
                          (map (juxt :short-opt :long-opt :required))
                          (str/join \space)
                          (str "Missing required arguments for "))))))

(defn get-opts!
  "Returns :options in `parsed-opts` iff no errors occurred and help is not
  requested. Otherwise prints some messages and returns `nil`"
  [parsed-opts]
  (let [{:keys [summary errors options]
         {:keys [help]} :options} parsed-opts]
    (if (or help errors)
      (do (when (not help)
            (print "error: ")
            (println errors))
          (println summary))
      options)))

(def generate-fastq-opts
  [["-i" "--input BAM" "Unsorted BAM path"]
   ["-t" "--target TARGET" "Target BED path"]
   ["-o" "--output DIR" "Output dir path"]
   [nil "--min-softclip-len LEN" "Minimum length of softclip to extract"
    :default 20 :parse-fn as-int!]])

(defn generate-fastq [argv]
  (when-let [{:keys [input target output] :as opts}
             (get-opts! (parse-opts argv generate-fastq-opts))]
    (dux/generate-fastq input target output opts)
    0))

(def fixup-sam-opts
  [["-i" "--input BAM" "Input BAM path"]
   ["-s" "--saved-seqs EDN" "Path to saved-seqs.edn (generated by `generate` subcommand)"]
   ["-o" "--output BAM" "Output BAM path"]])

(defn fixup-sam [argv]
  (when-let [{:keys [input cache output]}
             (get-opts! (parse-opts argv fixup-sam-opts))]
    (dux/fixup-sam input cache output)
    0))

(def sub-commands
  [["generate-fastq" "Generate FASTQ for realignment from BWA alignment result."]
   ["fixup-sam" "Fixup given SAM to make it suitable for fusionfusion call."]])

(defn -main [& argv]
  (let [[_ cmd sub-args help]
        (sub/sub-command
         argv
         "Usage: java -jar duxhund.jar {command} ..."
         :options []
         :commands sub-commands)
        ret (case cmd
              :generate-fastq (generate-fastq sub-args)
              :fixup-sam (fixup-sam sub-args)
              (println help))]
    (shutdown-agents)
    (System/exit (or ret 1))))
