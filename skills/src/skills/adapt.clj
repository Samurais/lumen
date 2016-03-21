(ns skills.adapt)

(defn entity [id values]
  (println "Entity: " id)
  (println "Values: " values))

(defn intent [id & dependencies]
  (println "Intent: " id)
  (println "Dependencies: " (vec dependencies)))

(defn require-entity [dependencyId]
  (str dependencyId "@req"))

(defn optionally-entity [dependencyId]
  (str dependencyId "@opt"))