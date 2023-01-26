(ns libsodium.app
  (:require ["libsodium-wrappers" :as libsodium]
            ["react-dom/client" :as rdom]
            [clojure.string :as str]
            [helix.core :refer [$ defnc]]
            [helix.dom :as d]
            [helix.hooks :as hooks]))

;; libsodium fns
(defn generate-nonce!
  [callback]
  (-> (.-ready libsodium)
      (.then #(callback (->> (.randombytes_buf libsodium libsodium/crypto_secretbox_NONCEBYTES)
                             (.to_base64 libsodium))))
      (.catch #(js/console.error %))))

(defn generate-key!
  [callback]
  (-> (.-ready libsodium)
      (.then #(callback (->> (.randombytes_buf libsodium libsodium/crypto_secretbox_KEYBYTES)
                             (.to_base64 libsodium))))
      (.catch #(js/console.error %))))

(defn encrypt!
  [message nonce key callback callback-error]
  (js/console.log "encrypt!" message nonce key)
  (-> (.-ready libsodium)
      (.then #(let [key-bytes (.from_base64 libsodium key)
                    nonce-bytes (.from_base64 libsodium nonce)]
                (callback (->> (.crypto_secretbox_easy libsodium
                                                       (str message)
                                                       nonce-bytes
                                                       key-bytes)
                               (.to_base64 libsodium)))))
      (.catch #(callback-error (str %)))))

(defn decrypt!
  [message nonce key callback callback-error]
  (js/console.log "decrypt!" message nonce key)
  (-> (.-ready libsodium)
      (.then #(let [key-bytes (.from_base64 libsodium key)
                    nonce-bytes (.from_base64 libsodium nonce)
                    message-bytes (.from_base64 libsodium message)]
                (callback (->> (.crypto_secretbox_open_easy libsodium
                                                            message-bytes
                                                            nonce-bytes
                                                            key-bytes)
                               (.to_string libsodium)))))
      (.catch #(callback-error (str %)))))

;; clipboard function
(defn copy-to-clip [value]
  (-> (.writeText js/navigator.clipboard value)
      (.then #(js/console.log "result:" %))
      (.catch #(js/console.log "error:" %))
      (.finally #(js/console.log "cleanup"))))

;; components
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

;; hooks
(defn encrypt-hook!
  [{:encrypt/keys [nonce key message]} set-state]
  (when-not (str/blank? message)
    (encrypt! message nonce key
              #(do (set-state assoc :encrypt/error "")
                   (set-state assoc :encrypt/encrypted %))
              #(set-state assoc :encrypt/error %))))

(defn decrypt-hook!
  [{:decrypt/keys [nonce key encrypted]} set-state]
  (when-not (str/blank? encrypted)
    (decrypt! encrypted nonce key
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
     (generate-nonce! #(set-state assoc :encrypt/nonce %))
     (generate-key! #(set-state assoc :encrypt/key %)))

    (hooks/use-effect
     [(:encrypt/message state)]
     (encrypt-hook! state set-state))

    (hooks/use-effect
     [(:decrypt/nonce state)
      (:decrypt/key state)
      (:decrypt/encrypted state)]
     (decrypt-hook! state set-state))

    (d/div
     (d/h1 "Libsodium Playground!")

     (d/div
      {:class "grid"}
      (d/article

       (d/header (d/h2 "Encrypt (SecretBox Easy)"))

       (d/div
        (d/label {:for "encrypt-nonce"} "nonce: ")
        (d/input {:value (:encrypt/nonce state)
                  :id "encrypt-nonce"
                  :placeholder "Generating..."
                  :disabled true}))

       (d/div
        (d/label {:for "encrypt-key"} "key: ")
        (d/input {:value (:encrypt/key state)
                  :id "encrypt-key"
                  :placeholder "Generating..."
                  :disabled true}))

       (d/div
        (d/label {:for "encrypt-message"} "message: ")
        (d/textarea {:value (:encrypt/message state)
                     :id "encrypt-message"
                     :placeholder "Anything in your mind..."
                     :disabled (or (str/blank? (:encrypt/nonce state))
                                   (str/blank? (:encrypt/key state)))
                     :on-change #(set-state assoc :encrypt/message (.. % -target -value))}))

       ($ show-message {:title "Encrypted Message:"
                        :message (:encrypt/encrypted state)
                        :error (:encrypt/error state)}))

      (d/article

       (d/header (d/h2 "Decrypt"))

       (d/div
        (d/label {:for "decrypt-nonce"} "nonce: ")
        (d/input {:value (:decrypt/nonce state)
                  :id "decrypt-nonce"
                  :placeholder "MDAwZmxvY2F0aW9..."
                  :on-change #(set-state assoc :decrypt/nonce (.. % -target -value))}))

       (d/div
        (d/label {:for "decrypt-key"} "key: ")
        (d/input {:value (:decrypt/key state)
                  :id "decrypt-key"
                  :placeholder "MDAwZmxvY2F0aW9..."
                  :on-change #(set-state assoc :decrypt/key (.. % -target -value))}))

       (d/div
        (d/label {:for "decrypt-encrypted"} "encrypted message: ")
        (d/input {:value (:decrypt/encrypted state)
                  :id "decrypt-encrypted"
                  :placeholder "MDAwZmxvY2F0aW9..."
                  :on-change #(set-state assoc :decrypt/encrypted (.. % -target -value))}))

       ($ show-message {:title "Message:"
                        :message (:decrypt/message state)
                        :error (:decrypt/error state)}))))))

;; start your app with your React renderer
(defn ^:export init []
  (doto (rdom/createRoot (js/document.getElementById "app"))
    (.render ($ app))))
