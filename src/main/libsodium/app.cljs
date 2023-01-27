(ns libsodium.app
  (:require ["react-dom/client" :as rdom]
            [clojure.string :as str]
            [helix.core :refer [$ defnc]]
            [helix.dom :as d]
            [helix.hooks :as hooks]
            [libsodium.components :as components]
            [libsodium.sodium :as sodium]))

;; hooks
(defn encrypt-hook!
  [{:encrypt/keys [nonce key message]} set-state]
  (when-not (str/blank? message)
    (sodium/encrypt! message nonce key
                     #(do (set-state assoc :encrypt/error "")
                          (set-state assoc :encrypt/encrypted %))
                     #(set-state assoc :encrypt/error %))))

(defn decrypt-hook!
  [{:decrypt/keys [nonce key encrypted]} set-state]
  (when-not (str/blank? encrypted)
    (sodium/decrypt! encrypted nonce key
                     #(do (set-state assoc :decrypt/error "")
                          (set-state assoc :decrypt/message %))
                     #(set-state assoc :decrypt/error %))))

;; app
(defnc app []
  (let [[state set-state] (hooks/use-state {:encrypt/nonce ""
                                            :encrypt/key ""
                                            :encrypt/message ""
                                            :encrypt/encrypted ""
                                            :encrypt/error ""
                                            :decrypt/nonce ""
                                            :decrypt/key ""
                                            :decrypt/encrypted ""
                                            :decrypt/message ""
                                            :decrypt/error ""})]

    (hooks/use-effect
     :once
     (sodium/generate-nonce! #(set-state assoc :encrypt/nonce %))
     (sodium/generate-key! #(set-state assoc :encrypt/key %)))

    (hooks/use-effect
     [(:encrypt/message state)]
     (encrypt-hook! state set-state))

    (hooks/use-effect
     [(:decrypt/nonce state) (:decrypt/key state) (:decrypt/encrypted state)]
     (decrypt-hook! state set-state))

    (d/div

     (d/h1 "Libsodium Playground!")

     (d/div
      {:class "grid"}
      (d/article

       (d/header (d/h2 "Encrypt (SecretBox Easy)"))

       ($ components/labeled-input-readonly-copy {:id "encrypt-nonce"
                                                  :label "nonce"
                                                  :value (:encrypt/nonce state)})

       ($ components/labeled-input-readonly-copy {:id "encrypt-key"
                                                  :label "key"
                                                  :value (:encrypt/key state)})

       (d/div
        (d/label {:for "encrypt-message"} "message: ")
        (d/textarea {:value (:encrypt/message state)
                     :id "encrypt-message"
                     :type "button"
                     :placeholder "Anything in your mind..."
                     :disabled (or (str/blank? (:encrypt/nonce state))
                                   (str/blank? (:encrypt/key state)))
                     :on-change #(set-state assoc :encrypt/message (.. % -target -value))}))

       ($ components/show-message
          {:title "Encrypted Message:"
           :message (:encrypt/encrypted state)
           :error (:encrypt/error state)}))

      (d/article

       (d/header (d/h2 "Decrypt"))

       ($ components/labeled-input
          {:id "decrypt-nonce"
           :label "nonce"
           :value (:decrypt/nonce state)
           :on-change #(set-state assoc :decrypt/nonce (.. % -target -value))})

       ($ components/labeled-input
          {:id "decrypt-key"
           :label "key"
           :value (:decrypt/key state)
           :on-change #(set-state assoc :decrypt/key (.. % -target -value))})

       (d/div
        (d/label {:for "decrypt-encrypted"} "encrypted message: ")
        (d/textarea {:value (:decrypt/encrypted state)
                     :id "decrypt-encrypted"
                     :placeholder "MDAwZmxvY2F0aW9..."
                     :on-change #(set-state assoc :decrypt/encrypted (.. % -target -value))}))

       ($ components/show-message
          {:title "Message:"
           :message (:decrypt/message state)
           :error (:decrypt/error state)}))))))

;; start your app with your React renderer
(defn ^:export init []
  (doto (rdom/createRoot (js/document.getElementById "app"))
    (.render ($ app))))
