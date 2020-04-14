(ns statisk.core
  (:gen-class)
  (:require [selmer.parser :as s])
  (:require [clojure.java.io :as io])
  (:require [clojure.string :as cljstr]))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))

(defn read-data [data-file]
  (if (.exists data-file)
    (read-string (slurp data-file))
    {}))

(defn read-directory-data [directory]
  (let [data-file (io/file directory "data.edn")]
    (read-data data-file)))

(defn read-data-for-template-file [template-file]
  (let [dir (.getParentFile template-file)
        data-file (io/file dir (str (.getName template-file) ".edn"))]
    (read-data data-file)))

(defn strip-template-suffix [filename]
  (cljstr/reverse (cljstr/replace-first (cljstr/reverse filename) (cljstr/reverse ".statisk") "")))

(defn render-file [file resource-path data]
  (s/set-resource-path! resource-path)
  (s/render-file (.getName file) data))

(defn write-target-file [target-dir filename content]
  (let [target-file (io/file target-dir (strip-template-suffix filename))]
    (spit target-file content)))

(defn process-file [file resource-path directory-data target-dir]
  (let [file-data (read-data-for-template-file file)
        data (merge directory-data file-data)
        content (render-file file resource-path data)
        filename (.getName file)]
    (write-target-file target-dir filename content)))

(defn copy-file [file target-dir]
  (io/copy file (io/file target-dir (.getName file))))

(defn template-file? [file]
  (.endsWith (.getName file) ".statisk"))

(defn data-file? [file]
  (.endsWith (.getName file) ".edn"))

(defn plain-file? [file]
  (not (or (template-file? file) (data-file? file))))

(defn is-file-ignored? [file ignore-patterns]
  (let [filename (.getName file)]
    (println "ignore patterns" ignore-patterns)
    (some #(re-matches (re-pattern %) filename) ignore-patterns)))

(defn process-dir [directory target-dir parent-data]
  (let [resource-path directory
        _ (println "in directory: " directory)
        _ (println "getting data")
        data (merge parent-data (read-directory-data directory))
        _ (println "getting files")
        files (.listFiles (io/file directory))
        _ (println "about to filter ignored files")
        not-ignored-files (filter #(not (is-file-ignored? % (:ignored-files data))) files)
        real-files (filter #(.isFile %) not-ignored-files)
        template-files (filter template-file? real-files)
        files-to-copy (filter plain-file? real-files)
        subdirs (filter #(.isDirectory %) not-ignored-files)
        ]
    (println "doing stuff")
    (.mkdirs (io/file target-dir))
      
    (doseq [template-file template-files]
      (println "processing template file" (.getName template-file))
      (process-file template-file resource-path data target-dir))
    (doseq [file-to-copy files-to-copy]
      (copy-file file-to-copy target-dir))
    (apply println (map #(.getAbsolutePath %) subdirs))
    (doseq [subdir subdirs]
      (let [subdir-name (.getName subdir)
            target-subdir (io/file target-dir subdir-name)]
        (println "processing subdir: " (.getAbsolutePath subdir))
        (if (not= (.getAbsolutePath subdir) (.getAbsolutePath (io/file directory)))
          (process-dir (.getAbsolutePath subdir) (.getAbsolutePath target-subdir) data))))))
