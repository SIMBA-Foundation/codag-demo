(ns codag-demo.main
  (:require [compojure.core :refer :all]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [ring.adapter.jetty :as adpater]
            [ring.util.response :refer [response]]
            [ring.middleware.json :as middleware]
            [codag-demo.core :as core]))

(defroutes app
  (POST "/transactions/new" {{:strs [from to amount]} :body}
        (do
          (core/add-transaction! from to amount)
          (response {:result true})))
  (POST "/mine" [] (response
                    (do
                      (core/mine!)
                      {:msg "mine success!"
                       :result true})))
  (GET "/chain" [] (response
                    @core/chain))
  (GET "/peers" [] (response
                    @core/peers))
  (POST "/peers/new" {{:strs [address]} :body}
        (do
          (core/add-peer! address)
          (response
           {:result true})))
  (POST "/sync" []
        (do
          (core/sync-chain!)
          (response
           {:result true})))
  (route/not-found "Not Found"))

(def api
  (-> (handler/api app)
      (middleware/wrap-json-body)
      (middleware/wrap-json-response)))

(defonce server (atom nil))

(defn -main
  [port]
  (let [s (adpater/run-jetty api {:port port :join? false})]
    (reset! server s)
    (println (format "server running at port:%s." port))))

(defn stop
  []
  (.stop @server))

;;(-main 2021)
;;(stop)
;;curl -H "Content-Type: application/json" -X GET  http://localhost:2021/chain
;;curl -H "Content-Type: application/json" -X POST -d '{"from":"aaa", "to":"bbb", "amount":100}' http://localhost:2021/transactions/new
;;curl -H "Content-Type: application/json" -X POST  http://localhost:2021/mine
