(ns libsodium.sodium
  (:require ["libsodium-wrappers" :as libsodium]))

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
