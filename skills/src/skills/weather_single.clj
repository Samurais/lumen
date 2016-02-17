(ns skills.weather-single
  (:gen-class)
  (:use skills.adapt))

;; Port of Adapt's weather single intent:
;; https://github.com/MycroftAI/adapt/blob/master/examples/single_intent_parser.py

(entity :weatherKeyword ["weather"])

(entity :weatherType ["snow" "rain" "wind" "sleet" "sun"])

(entity :location ["Seattle" "San Francisco" "Tokyo"])

(intent :weatherIntent
        (require-entity :weatherKeyword)
        (optionally-entity :weatherType)
        (require-entity :location)
)