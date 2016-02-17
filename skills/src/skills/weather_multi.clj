(skill weather-multi
       (ns skills.weather-multi
         (:gen-class)
         (:use skills.adapt))

       ;; Port of Adapt's weather multi intent:
       ;; https://github.com/MycroftAI/adapt/blob/master/examples/multi_intent_parser.py

       (entity :weatherKeyword ["weather"])

       (entity :weatherType ["snow" "rain" "wind" "sleet" "sun"])

       (entity :location ["Seattle" "San Francisco" "Tokyo"])

       (intent :weatherIntent
               (require-entity :weatherKeyword)
               (optionally-entity :weatherType)
               (require-entity :location)
               )

       (process weatherIntent
                (println "Keyword: " weatherKeyword)
                (println "Type: " weatherType)
                (println "Location: " location)
                )

       )