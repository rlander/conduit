(ns conduit.actions
  (:require [ajax.core            :refer [DELETE GET POST PUT]]
            [javelin.core         :refer [cell cell= dosync] :include-macros true]
            [reitit.frontend.easy :as easy]
            [conduit.state        :as state]))

(def base-url "https://conduit.productionready.io/api")

(defn api [meth url opts]
  (meth (str base-url url)
    (merge {:format          :json
            :response-format :json
            :keywords?       true
            :headers         (when @state/logged-in?
                               {:Authorization (str "Token " (:token @state/user))})}
          opts)))

(defn fetch-feed
  [{:keys [user-feed? offset tag author favorited]}]
  (let [url (str "/articles"
                 (when user-feed? (str "/feed"))
                 (str "?limit=" state/page-size)
                 (str "&offset=" (* offset state/page-size))
                 (when tag (str "&tag=" tag))
                 (when author (str "&author=" author))
                 (when favorited (str "&favorited=" favorited)))]
    (dosync (reset! state/feed [])
            (swap! state/loading? assoc :feed true))
    (api GET url
      {:handler (fn [res]
                  (dosync (reset! state/feed-total-count (:articlesCount res))
                          (reset! state/feed (:articles res))))
       :finally #(swap! state/loading? dissoc :feed)})))

(defn fetch-tags []
  (api GET "/tags" {:handler #(reset! state/tags (:tags %))}))

(defn fetch-article [slug]
  (api GET (str "/articles/" slug)
    {:handler (fn [res]
                (reset! state/article (:article res)))}))

(defn fetch-profile [username]
  (api GET (str "/profiles/" username)
    {:handler (fn [res]
                (reset! state/profile (:profile res)))}))

(defn fetch-comments [slug]
  (api GET (str "/articles/" slug "/comments")
    {:handler (fn [res]
                (reset! state/article-comments (:comments res)))}))

(defn fetch-user []
  (api GET "/user"
    {:handler (fn [res]
                (reset! state/user (:user res)))}))

(defn action-signup! [form-errors user-data]
  (api POST "/users"
    {:handler         (fn [res]
                        (easy/push-state :home)
                        (reset! state/user (:user res)))
     :error-handler   (fn [errors]
                        (reset! form-errors (-> errors :response :errors)))
     :params          {:user user-data}}))

(defn action-signin [form-errors user-data]
  (api POST "/users/login"
    {:handler         (fn [res]
                        (easy/push-state :home)
                        (reset! state/user (:user res)))
     :error-handler   (fn [errors]
                        (reset! form-errors (-> errors :response :errors)))
     :params          {:user user-data}}))

(defn toggle-follow! [username following?]
  (let [ACTION (if following? DELETE POST)]
    (api ACTION (str "/profiles/" username "/follow") {})))

(defn submit-article! [article-data success-cell errors-cell]
  (api POST "/articles"
    {:handler         (fn [res]
                        (easy/push-state :article {:slug (-> res :article :slug)}))
     :error-handler   (fn [errors]
                        (reset! errors-cell (-> errors :response :errors)))
     :params          {:article article-data}}))

(defn update-profile! [user-data success-cell errors-cell]
  (api PUT "/user"
   {:handler         (fn [res]
                       (reset! success-cell (-> res :user))
                       (easy/push-state :profile {:username (-> res :user :username)}))
    :error-handler   (fn [errors]
                       (reset! errors-cell (-> errors :response :errors)))
    :params          {:user user-data}}))

(defn toggle-favorite! [slug favorited?]
  (let [ACTION (if favorited? DELETE POST)]
    (api ACTION (str "/articles/" slug "/favorite")
     {:handler (fn [res]
                 (reset! state/article (:article res)))})))

(defn toggle-favorite-feed! [slug favorited?]
  (let [ACTION (if favorited? DELETE POST)]
    (api ACTION (str "/articles/" slug "/favorite")
     {:handler (fn [res]
                 (reset! state/feed
                   (mapv (fn [article]
                           (if (= (:slug article) slug)
                             (:article res)
                             article))
                         @state/feed)))})))

(defn add-comment! [comment-data slug]
  (api POST (str "/articles/" slug "/comments")
    {:handler (fn [res]
                (swap! state/article-comments #(conj % (-> res :comment))))
     :params  {:comment comment-data}}))

(defn delete-article! [article]
  (api DELETE (str "/articles/" (:slug article))
    {:handler (fn [_res]
                (easy/push-state :home))}))

(defn update-article! [slug article-data]
  (api PUT (str "/articles/" slug)
    {:handler (fn [res]
                (easy/push-state :article {:slug (-> res :article :slug)}))
     :params   {:article article-data}}))

(def init-home #(do (fetch-tags)
                    (fetch-feed {})))

(defn init-article [slug]
  (do (fetch-article slug)
      (fetch-comments slug)))

(defn init-profile [username]
  (do (dosync (reset! state/profile {})
              (reset! state/feed []))
    (fetch-profile username)
    (fetch-feed {:author username})))
