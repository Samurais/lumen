(ns skills.runner (:gen-class))

;; Standalone Clojure program

(defn -main
  "Greets user"
  [& args]
  (println "Assalaamu'alaikum!")
  args)

(def userName "Hendy Irawan")

(-main)
