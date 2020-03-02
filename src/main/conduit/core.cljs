(ns conduit.core
  (:require
   ["jquery"             :as jq]
   [hoplon.jquery]
   [javelin.core         :refer [dosync] :include-macros true]
   [reitit.frontend      :as reitit]
   [reitit.frontend.easy :as easy]
   [conduit.actions      :as actions]
   [conduit.state        :as state]
   [conduit.views        :as views]))

(def routes
  ["/"

   [""
    {:name :home
     :view  views/home-page}]

   ["@"
    [":username"
     {:name :profile
      :view  views/profile-page}]]

   ["article"
    ["/:slug"
     {:name :article
      :view  views/article-page}]]

   ["editor"
    [""
     {:name :editor
      :view  views/editor}]
    ["/:slug"
     {:name :edit-article
      :view  views/edit-article-page}]]

   ["settings"
    [""
     {:name :settings
      :view  views/settings-page}]]

   ["signin"
    [""
     {:name :signin
      :view  views/signin-page}]]

   ["signup"
    [""
     {:name :signup
      :view  views/signup-page}]]])

(def router
  (reitit/router routes))

(defn on-navigate [{{:keys [path]} :parameters :as new-match}]
  (reset! state/selected-page (-> new-match :data))
  (case (-> new-match :data :name)
    :home         (actions/init-home)
    :article      (actions/init-article (:slug path))
    :profile      (actions/init-profile (:username path))
    :settings     (actions/fetch-user)
    :edit-article (actions/fetch-article (:slug path))
    (prn "default")))

;; Setup

(defn mount-components []
  (jq #(.html (jq "#app") (views/current-page))))

;; start is called by init and after code reloading finishes
(defn start []
  (easy/start!
    router
    on-navigate
    {:use-fragment true})
  (mount-components)
  (js/console.log "Starting..."))

;; this is called before any code is reloaded
(defn stop []
  (js/console.log "Stopping..."))

(defn ^:export init []
  ;; init is called ONCE when the page loads
  ;; this is called in the index.html and must be exported
  ;; so it is available even in :advanced release builds
  (js/console.log "Initializing...")
  (start))
