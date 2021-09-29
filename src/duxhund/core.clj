(ns duxhund.core
  (:require [cljam.io.bed :as bed]
            [cljam.io.sam :as sam]
            [cljam.io.sam.util.cigar :as cigar]
            [cljam.io.sam.util.flag :as flag]
            [cljam.io.sam.util.option :as option]
            [cljam.util.intervals :as intervals]
            [cljam.util.sequence :as seq]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(defn- primary-mapped? [flag]
  (zero? (bit-and flag (flag/encoded #{:secondary :supplementary :unmapped}))))

(defn- collect-primary-alignments [chunk]
  (->> chunk
       (filter (comp primary-mapped? :flag))
       (reduce (fn [m aln]
                 (cond-> m
                   (flag/r1? (:flag aln)) (assoc :r1 aln)
                   (flag/r2? (:flag aln)) (assoc :r2 aln)))
               {})))

(defn- clipped-len [thres [cigar-len cigar-op]]
  ;; we can assume here that the first/last cigar ops are either `S` or `M`
  ;; and so safely ignore the cases where those ops are `H`, because bwa mem
  ;; only emits hardclips for primary alignments
  (if (and (= cigar-op \S) (>= cigar-len thres))
    cigar-len
    0))

(defn- revcomp-if-necessary [x flag]
  (-> x
      (update :seq #(cond-> % (flag/reversed? flag) seq/revcomp))
      (update :qual #(cond-> % (flag/reversed? flag) str/reverse))))

(defn- sort-alignments [aln' [a1 a2]]
  (if (= (:chr a1) (:chr a2))
    (if (>= (Math/abs (- (long (:pos a1)) (long (:pos aln'))))
            (Math/abs (- (long (:pos a2)) (long (:pos aln')))))
      [a1 a2]
      [a2 a1])
    (if (>= (count (:seq a1)) (count (:seq a2)))
      [a1 a2]
      [a2 a1])))

(defn- generate-triplet [left? clipped-len {:keys [flag chr pos seq qual] :as aln} aln']
  (let [len (count seq)
        offset (if left? clipped-len (- len clipped-len))
        qname (str (:qname aln) \: (:rname aln) \: (+ pos offset))
        r1r2 (flag/r1r2 flag)
        [a1 a2] (->> [{:qname qname :r1r2 r1r2 :chr chr :pos pos
                       :cutoff (str \R (- len offset))
                       :seq (subs seq 0 offset)
                       :qual (subs qual 0 offset)}
                      {:qname qname :r1r2 r1r2 :chr chr :pos (+ pos offset)
                       :cutoff (str \L offset)
                       :seq (subs seq offset)
                       :qual (subs qual offset)}]
                     (map #(revcomp-if-necessary % flag))
                     (sort-alignments aln'))]
    [(update a1 :qname str "/A")
     (update a2 :qname str "/B")
     (-> {:qname (str qname "/B")
          :r1r2 (flag/r1r2 (:flag aln'))
          :seq (:seq aln')
          :qual (:qual aln')}
         (revcomp-if-necessary (:flag aln')))]))

(defn- generate-soft-clipped-seqs [thres alns]
  (for [chunk (partition-by :qname alns)
        :let [{:keys [r1 r2]} (collect-primary-alignments chunk)]
        aln chunk
        :let [cigar (cigar/parse (:cigar aln))
              left-clip (clipped-len thres (first cigar))
              right-clip (clipped-len thres (last cigar))
              aln' (if (flag/r1? (:flag aln)) r2 r1)]
        :when (and aln' (or (>= left-clip thres) (>= right-clip thres)))
        a (cond-> []
            (>= left-clip thres)
            (conj (generate-triplet true left-clip aln aln'))

            (>= right-clip thres)
            (conj (generate-triplet false right-clip aln aln')))]
    a))

(defn generate-seqs [min-softclip-len alns]
  (->> alns
       (generate-soft-clipped-seqs min-softclip-len)
       (sequence cat)))

(defn generate-cache [alns]
  (->> alns
       (partition-by :qname)
       (reduce
        (fn [m chunk]
          (let [{:keys [r1 r2]} (collect-primary-alignments chunk)]
            (reduce
             (fn [m {:keys [flag] :as r}]
               (cond-> m
                 r (assoc [(:qname r) (flag/r1r2 flag)]
                          (-> r
                              (select-keys [:seq :qual])
                              (revcomp-if-necessary flag)))))
             m
             [r1 r2])))
        {})))

(defn- write-as-fastq [alns writer]
  (binding [*out* writer]
    (doseq [{:keys [qname r1r2 cutoff seq qual]} alns]
      ;; embed info about flag and cutoff as qname comment
      ;; generated fastq should be aligned with `bwa mem -C` to output the comment
      (printf "@%s CO:Z:%d:%s\n" qname r1r2 (or cutoff ""))
      (println seq)
      (println "+")
      (println qual))))

(defn generate-fastq
  ([bam target-bed output-dir]
   (generate-fastq bam target-bed output-dir {}))
  ([bam target-bed output-dir
    {:keys [min-softclip-len] :or {min-softclip-len 20}}]
   (with-open [bed-reader (bed/reader target-bed)
               bam-reader (sam/reader bam)
               fastq-writer (io/writer (io/file output-dir "out.fastq"))
               cache-writer (io/writer (io/file output-dir "cache.edn"))]
     (let [target (intervals/index-intervals (bed/read-fields bed-reader))
           alns (for [chunk (->> (sam/read-alignments bam-reader)
                                 (partition-by :qname))
                      :when (some #(seq (intervals/find-overlap-intervals
                                         target (:rname %) (:pos %) (:end %)))
                                  chunk)
                      aln chunk]
                  aln)]
       (write-as-fastq (generate-seqs min-softclip-len alns) fastq-writer)
       (binding [*out* cache-writer]
         (prn (generate-cache alns)))))))

(defn- fixup-cigar [cigar cutoff]
  (let [[_ dir n] (re-matches #"([LR])(\d+)" cutoff)
        n (Long/parseLong n)
        parsed (cigar/parse cigar)]
    (case (first dir)
      \L (let [[cigar-len cigar-op] (first parsed)]
           (->> (if (= cigar-op \S)
                  (cons [(+ cigar-len n) \S] (rest parsed))
                  (cons [n \S] parsed))
                (apply concat)
                str/join))
      \R (let [[cigar-len cigar-op] (last parsed)]
           (->> (if (= cigar-op \S)
                  (concat (butlast parsed) [[(+ cigar-len n) \S]])
                  (concat parsed [[n \S]]))
                (apply concat)
                str/join)))))

(defn- fixup-flag [[a1 a2 a3 :as alns]]
  (letfn [(matches-of [a]
            (->> (cigar/parse (:cigar a))
                 (keep (fn [[n op]] (when (= op \M) n)))
                 (apply +)))]
    (if (and (nil? a3)
             (or (nil? a2)
                 (not= (flag/r1r2 (:flag a1))
                       (flag/r1r2 (:flag a2)))))
      alns
      (let [flag-inherited (flag/encoded #{:multiple :properly-aligned
                                           :next-unmapped :next-reversed})
            a1' (assoc a1 :flag
                       (bit-or (:flag a1)
                               (bit-and (:flag a2) flag-inherited)))]
        (if (>= (matches-of a1') (matches-of a2))
          [a1' (update a2 :flag bit-or (flag/encoded #{:secondary})) a3]
          [(update a1' :flag bit-or (flag/encoded #{:secondary})) a2 a3])))))

(defn fixup-alignments [qname->seq alns]
  (->> (for [aln alns
             :when (primary-mapped? (:flag aln))
             :let [[_ qname qname'] (re-matches #"((.*?):[^:]+:\d+)/[AB]" (:qname aln))
                   [_ r1r2 cutoff] (re-matches #"(\d):([LR]\d+)?"
                                               (option/value-for-tag :CO aln))
                   r1r2 (Long/parseLong r1r2)
                   {:keys [seq qual]} (qname->seq [qname' r1r2])]]
         (-> aln
             (assoc :qname qname :seq seq :qual qual)
             (revcomp-if-necessary (:flag aln))
             (update :flag #(bit-or (bit-and % (bit-not (flag/encoded #{:first :last})))
                                    (bit-shift-left r1r2 6)))
             (cond-> cutoff (update :cigar fixup-cigar cutoff))))
       (partition-by :qname)
       (mapcat fixup-flag)))

(defn fixup-sam [in-sam cache-edn out-sam]
  (let [qname->seq (edn/read-string (slurp cache-edn))]
    (with-open [r (sam/reader in-sam)
                w (sam/writer out-sam)]
      (let [header (sam/read-header r)]
        (sam/write-header w header)
        (sam/write-refs w header)
        (->> (sam/read-alignments r)
             (fixup-alignments qname->seq)
             (#(sam/write-alignments w % header)))))))
