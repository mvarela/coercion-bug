(ns coercion.bug
  (:require [reitit.ring :as ring]
            [reitit.http :as http]
            [reitit.coercion.malli]
            [reitit.swagger :as swagger]
            [reitit.swagger-ui :as swagger-ui]
            [reitit.http.coercion :as coercion]
            [reitit.dev.pretty :as pretty]
            [reitit.interceptor.sieppari :as sieppari]
            [reitit.http.interceptors.parameters :as parameters]
            [reitit.http.interceptors.muuntaja :as muuntaja]
            [reitit.http.interceptors.exception :as exception]
            [reitit.http.interceptors.dev :as dev]
            [ring.adapter.jetty :as jetty]
            [muuntaja.core :as m]
            [clojure.java.io :as io]
            [malli.core :as malli]
            [clojure.spec.alpha :as spec]
            [clojure.string :as string]))



(defn ->ok [body]
  {:status 200
   :body body})

(def routes [["/swagger.json"
              {:get {:no-doc true
                     :swagger {:info {:title "my-api"
                                      :description "with reitit-http"}}
                     :handler (swagger/create-swagger-handler)}}]
             ["/ping" {:get {:summary "ping"
                             :parameters {}
                             :responses {200 {:body [:sequential string?]}}
                             :handler (fn [ctx] (->ok []))}}]

             ["/ping2" {:get {:summary "ping2"
                             :parameters {}
                             :responses {200 {:body [:sequential string?]}}
                             :handler (fn [ctx] (->ok ["some string"]))}}]])

(def routes-data {
                  :reitit.interceptor/transform dev/print-context-diffs ;; pretty context diffs
                  ;;:validate spec/validate ;; enable spec validation for route data
                  ;;:reitit.spec/wrap spell/closed ;; strict top-level validation
                  :exception pretty/exception
                  :data {:coercion reitit.coercion.malli/coercion
                         :muuntaja m/instance
                         :interceptors [ ;; swagger feature
                                        swagger/swagger-feature
                                        ;; query-params & form-params
                                        (parameters/parameters-interceptor)
                                        ;; content-negotiation
                                        (muuntaja/format-negotiate-interceptor)
                                        ;; encoding response body
                                        (muuntaja/format-response-interceptor)
                                        ;; exception handling
                                        (exception/exception-interceptor)
                                        ;; decoding request body
                                        (muuntaja/format-request-interceptor)
                                        ;; coercing response bodys
                                        (coercion/coerce-response-interceptor)
                                        ;; coercing request parameters
                                        (coercion/coerce-request-interceptor)]}})

(def app (http/ring-handler
           (http/router routes  routes-data)
           (ring/routes
            (swagger-ui/create-swagger-ui-handler
             {:path "/"
              :config {:validatorUrl nil
                       :operationsSorter "alpha"}})
            (ring/create-default-handler))
           {:executor sieppari/executor}))


(defn start []
  (def server (jetty/run-jetty #'app {:port 3001, :join? false, :async true}))
  (println "server running in port 3001"))

(start)

(comment
  (.stop server)

  )
