(ns libsodium.components
  (:require [clojure.string :as str]
            [helix.core :refer [$ defnc]]
            [helix.dom :as d]
            [helix.hooks :as hooks]))

(defn ^:private copy-to-clip
  [value]
  (-> (.writeText js/navigator.clipboard value)
      (.then #(js/console.log "result:" %))
      (.catch #(js/console.log "error:" %))
      (.finally #(js/console.log "cleanup"))))

(defnc copy-icon-svg []
  (d/svg
   {:style {:cursor "pointer"}
    :fill "#000000"
    :height "18px"
    :width "18px"
    :version "1.1"
    :xmlns "http://www.w3.org/2000/svg"
    :viewBox "0 0 512 512"
    :xmlnsXlink "http://www.w3.org/1999/xlink"
    :enable-background "new 0 0 512 512"}
   (d/g
    {:id "SVGRepo_bgCarrier" :stroke-width "0"})
   (d/g
    {:id "SVGRepo_tracerCarrier" :stroke-linecap "round" :stroke-linejoin "round"})
   (d/g
    {:id "SVGRepo_iconCarrier"}
    (d/g
     (d/path
      {:className "st0"
       :d "M480.6,109.1h-87.5V31.4c0-11.3-9.1-20.4-20.4-20.4H31.4C20.1,11,11,20.1,11,31.4v351c0,11.3,9.1,20.4,20.4,20.4h87.5 v77.7c0,11.3,9.1,20.4,20.4,20.4h341.3c11.3,0,20.4-9.1,20.4-20.4v-351C501,118.3,491.9,109.1,480.6,109.1z M51.8,362V51.8h300.4 v57.3H139.3c-11.3,0-20.4,9.1-20.4,20.4V362H51.8z M460.2,460.2H159.7V150h300.4V460.2z"})))))

(defnc show-message
  "A component which shows a encrypted message"
  [{:keys [message error title]}]
  (let [[state set-state] (hooks/use-state {:tooltip-display nil})]
    (d/div
     (when-not (str/blank? error)
       (d/div
        (d/div
         (d/h3 "Error:")
         (d/mark (str error)))))

     (when-not (str/blank? message)
       (d/div
        (d/div
         (d/h3 title)
         (d/cite (str message)))
        (d/button {:data-tooltip (:tooltip-display state)
                   :style {:margin-top "20px"}
                   :on-mouse-out #(set-state assoc :tooltip-display nil)
                   :on-click #(do (copy-to-clip message)
                                  (set-state assoc :tooltip-display "Copied."))} "Copy"))))))

(defnc labeled-input-readonly-copy
  "An input with label read-only with copy to clipboard on click"
  [{:keys [id label value]}]
  (let [[state set-state] (hooks/use-state {:tooltip-display nil})]
    (d/div
     (d/label
      {:for id}
      (str label ": ")
      (d/a
       {:data-tooltip (:tooltip-display state)
        :on-mouse-out #(set-state assoc :tooltip-display nil)
        :on-click #(do (copy-to-clip value)
                       (set-state assoc :tooltip-display "Copied."))}
       ($ copy-icon-svg)))
     (d/input {:value value
               :id id
               :placeholder "Generating..."
               :disabled true}))))

(defnc labeled-input
  "An input with label"
  [{:keys [id label value on-change]}]
  (d/div
   (d/label {:for id} (str label ": "))
   (d/input {:value value
             :id id
             :placeholder "MDAwZmxvY2F0aW9..."
             :on-change on-change})))
