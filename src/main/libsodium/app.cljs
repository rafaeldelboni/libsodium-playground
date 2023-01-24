(ns libsodium.app
  (:require ["react-dom/client" :as rdom]
            ["libsodium-wrappers" :as libsodium]
            [helix.core :refer [$ defnc]]
            [helix.dom :as d]))

(defn init-sodium []
  (-> (.-ready libsodium)
      (.then #(let [key (.crypto_secretstream_xchacha20poly1305_keygen libsodium)
                    res (.crypto_secretstream_xchacha20poly1305_init_push libsodium key)
                    state-out (.-state res)
                    header (.-header res)
                    c1 (.crypto_secretstream_xchacha20poly1305_push libsodium
                                                                    state-out
                                                                    (.from_string libsodium "message 1")
                                                                    nil
                                                                    libsodium/crypto_secretstream_xchacha20poly1305_TAG_MESSAGE)
                    c2 (.crypto_secretstream_xchacha20poly1305_push libsodium
                                                                    state-out
                                                                    (.from_string libsodium "message 2")
                                                                    nil
                                                                    libsodium/crypto_secretstream_xchacha20poly1305_TAG_FINAL)

                    state-in (.crypto_secretstream_xchacha20poly1305_init_pull libsodium header key)
                    r1 (.crypto_secretstream_xchacha20poly1305_pull libsodium state-in c1)
                    m1 (.to_string libsodium (.-message r1))
                    r2 (.crypto_secretstream_xchacha20poly1305_pull libsodium state-in c2)
                    m2 (.to_string libsodium (.-message r2))]
                (js/console.log "messages:")
                (js/console.log m1)
                (js/console.log m2)))
      (.catch #(js/console.log %))
      (.finally #(js/console.log "cleanup"))))

;; app
(defnc app []
  (let [_init-sodium (init-sodium)]
    (d/div
     (d/h1 "Libsodium! Playground!")
     (d/div "Open your console and search for 'messages:'"))))

;; start your app with your React renderer
(defn ^:export init []
  (doto (rdom/createRoot (js/document.getElementById "app"))
    (.render ($ app))))
