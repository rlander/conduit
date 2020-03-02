(ns conduit.views
  (:require [hoplon.core          :refer [a button div fieldset footer form h1 h2 h4 hr
                                          i img input li nav p span ul text textarea
                                          case-tpl cond-tpl defelem for-tpl if-tpl]
                                  :include-macros true]
            [javelin.core         :refer [cell cell= dosync]
                                  :include-macros true]
            [reitit.frontend.easy :as easy]
            [conduit.actions      :as actions]
            [conduit.state        :as state]))

;; Utils

(defn path-cell [c path]
  (cell= (get-in c path) (partial swap! c assoc-in path)))

(def nbsp (.fromCharCode js/String 160))
(def amp (.fromCharCode js/String 38))

(defn format-date
  [date]
  (.toDateString (js/Date. date)))

(defn fmt-error-msgs
  [errors]
  (cell=
    (mapcat (fn [[field msgs]]
              (map #(str (name field) " " %)  msgs))
        errors)))


;; Components

(defelem btn-follow [{:keys [toggle username following?]} _]
  (button
    :toggle (cell= (or toggle true))
    :click #(do (actions/toggle-follow! @username @following?)
                (swap! following? not))
    :class (cell= {:action-btn            true
                   :btn                   true
                   :btn-sm                true
                   :btn-secondary         true
                   :btn-outline-secondary (not following?)})
    (i :class "ion-plus-round")
    (cell=
      (str nbsp
        (if following? "Unfollow " "Follow ") username))))

(defelem nav-item [{:keys [link page toggle]} kids]
  (li :class "nav-item" :toggle toggle
    (a :href (or link (easy/href page))
       :class (cell= {:nav-link true :active (= page (:name state/selected-page))})
      kids)))

(defelem header []
  (nav :class "navbar navbar-light"
    (div :class "container"
        (a :class "navbar-brand" :href (easy/href :home) "conduit")
      (ul :class "nav navbar-nav pull-xs-right"
        (nav-item :page :home     :toggle true             "Home")
        (nav-item :page :editor   :toggle state/logged-in? (i :class "ion-compose"
                                                             (str nbsp "New Article")))
        (nav-item :page :settings :toggle state/logged-in? (i :class "ion-gear-a"
                                                             (str nbsp "Settings")))
        (nav-item :page :signin   :toggle state/anonymous? "Sign in")
        (nav-item :page :signup   :toggle state/anonymous? "Sign up")
        (nav-item
          :page   :profile
          :link   (cell= (easy/href :profile {:username (:username state/user)}))
          :toggle state/logged-in?
          (img :class "user-pic" :src (cell= (:image state/user)))
          (cell= (:username state/user)))))))

(defelem footer_ []
  (footer
    (div :class "container"
      (a :href (easy/href :home) :class "logo-font" "conduit")
      (span :class "attribution"
        "An interactive learning project from "
        (a :href "https://thinkster.io" "Thinkster")
        (str ". Code " amp " design licensed under MIT.")))))

;; Home Page ------------------------------------------------------------------

(defn article-preview [{:keys [author createdAt favorited favoritesCount description slug tagList title]}]
  (let [profile-link (easy/href :profile {:username (:username author)})]
    (div :class "article-preview"
      (div :class "article-meta"
        (a :href profile-link
          (img :src (:image author)))
        (div :class "info"
          (a :class "author" :href profile-link (:username author))
          (span :class "date" (format-date createdAt)))
        (button
          :click #(actions/toggle-favorite-feed! slug favorited)
          :class {:btn true :btn-sm true :pull-xs-right true :btn-primary true :btn-outline-primary (not favorited)}
          (i :class "ion-heart" (str " " favoritesCount))))
      (a :class "preview-link" :href (easy/href :article {:slug slug})
        (h1 title)
        (p description)
        (span "Read more...")
        (ul :class "tag-list"
          (for [tag tagList]
            (li :class "tag-default tag-pill tag-outline" tag)))))))

(defn sidebar [tags selected-tag selected-feed]
  (div :class "sidebar"
    (p "Popular Tags")
    (div :class "tag-list"
      (p :toggle (cell= (empty? tags)) "Loading tags...")
      (for-tpl [tag tags]
        (a :click (fn [e]
                    (.preventDefault e)
                    (actions/fetch-feed {:tag @tag})
                    (dosync
                      (reset! selected-tag tag)
                      (reset! selected-feed :tag-feed)))
           :class "tag-pill tag-default" :href "" tag)))))

(defelem feed-content [{:keys [feed loading?]} _kids]
  (div
    (p :toggle (cell= loading?) :class "article-preview" "Loading articles...")
    (p :toggle (cell= (and (not loading?) (empty? feed))) :class "article-preview" "No articles here... yet.")
    (for-tpl [article feed]
      (div
        (cell= (article-preview article))))))

(defelem feed-pagination [{:keys [toggle total-count page-size feed-params]} _]
  (ul :class "pagination" :toggle toggle
    (cell=
      (let [current-offset (cell 0)]
        (for [offset (range (/ total-count page-size))]
          (li :class (cell= {:page-item true :active (= offset current-offset)})
            (a :click #(do (.preventDefault %)
                           (actions/fetch-feed (assoc feed-params :offset offset))
                           (reset! current-offset offset))
               :href ""
               :class "page-link" (+ offset 1))))))))

(defelem home-page []
  (let [selected-feed (cell :global-feed)
        show-tag-tab? (cell= (= selected-feed :tag-feed))
        selected-tag  (cell nil)]

    (div :class "home-page"
      (div :class "banner"
        (div :class "container"
          (h1 :class "logo-font" "conduit")
          (p "A place to share your knowledge.")))

      (div :class "container page"
        (div :class "row"
          (div :class "col-md-9"
            (div :class "feed-toggle"
              (ul :class "nav nav-pills outline-active"
                (li :class "nav-item" :toggle (cell= state/logged-in?)
                  (a :click (fn [e]
                              (.preventDefault e)
                              (actions/fetch-feed {:user-feed? true})
                              (reset! selected-feed :your-feed))
                     :class (cell= {:nav-link true :active (= selected-feed :your-feed)}) :href "" "Your Feed"))
                (li :class "nav-item"
                  (a :click (fn [e]
                              (.preventDefault e)
                              (actions/fetch-feed {})
                              (reset! selected-feed :global-feed))
                     :class (cell= {:nav-link true :active (= selected-feed :global-feed)}) :href "" "Global Feed"))
                (li :class "nav-item" :toggle show-tag-tab?
                  (a :class (cell= {:nav-link true :active show-tag-tab?}) :href "" (cell= (text "# ~{selected-tag}"))))))

            (feed-content
              :feed     state/feed
              :loading? (cell= (:feed state/loading?)))
            (feed-pagination
              :toggle      (cell= (not (:feed state/loading?)))
              :total-count state/feed-total-count
              :page-size   state/page-size
              :feed-params (cell= {:user-feed? (= selected-feed :your-feed)
                                   :tag        (when (= selected-feed :tag-feed)
                                                 @selected-tag)})))

          (div :class "col-md-3"
            (sidebar state/tags selected-tag selected-feed)))))))

;; Sign-in Page ---------------------------------------------------------------

(defelem signin-page []
  (let [form-data   (cell {})
        form-errors (cell {})]
    (div :class "auth-page"
      (div :class "container page"
        (div :class "row"
          (div :class "col-md-6 offset-md-3 col-xs-12"
            (h1 :class "text-xs-center" "Sign in")
            (p :class "text-xs-center"
              (a :href (easy/href :signup) "Need an account?"))

            (ul :toggle (cell= (not-empty form-errors)) :class "error-messages"
              (for-tpl [error (fmt-error-msgs form-errors)]
                (li (cell= error))))

            (form :submit (fn [e]
                            (actions/action-signin form-errors @form-data)
                            (.preventDefault e))
              (fieldset :class "form-group"
                (input
                  :change (fn [e] (swap! form-data #(assoc % :email @e)))
                  :class "form-control form-control-lg"
                  :type "text"
                  :placeholder "Email"))
              (fieldset :class "form-group"
                (input
                  :change (fn [e] (swap! form-data #(assoc % :password @e)))
                  :class "form-control form-control-lg"
                  :type "password"
                  :placeholder "Password"))
              (button :class "btn btn-lg btn-primary pull-xs-right" "Sign in"))))))))

;; Sign-up Page ---------------------------------------------------------------

(defelem signup-page []
  (let [form-data   (cell {})
        form-errors (cell {})]
    (div :class "auth-page"
      (div :class "container page"
        (div :class "row"
          (div :class "col-md-6 offset-md-3 col-xs-12"
            (h1 :class "text-xs-center" "Sign up")
            (p :class "text-xs-center"
              (a :href (easy/href :signin) "Have an account?"))

            (ul :toggle (cell= (not-empty form-errors)) :class "error-messages"
              (for-tpl [error (fmt-error-msgs form-errors)]
                (li (cell= error))))
            (form :submit (fn [e]
                            (actions/action-signup! form-errors @form-data)
                            (.preventDefault e))
              (fieldset :class "form-group"
                (input
                  :change (fn [e] (swap! form-data #(assoc % :username @e)))
                  :class "form-control form-control-lg"
                  :type "text"
                  :placeholder "Your name"))
              (fieldset :class "form-group"
                (input
                  :change (fn [e] (swap! form-data #(assoc % :email @e)))
                  :class "form-control form-control-lg"
                  :type "text"
                  :placeholder "Email"))
              (fieldset :class "form-group"
                (input
                  :change (fn [e] (swap! form-data #(assoc % :password @e)))
                  :class "form-control form-control-lg"
                  :type "password"
                  :placeholder "Password"))
              (button :class "btn btn-lg btn-primary pull-xs-right" "Sign up"))))))))

;; Profile Page ---------------------------------------------------------------

(defelem profile-page []
  (let [selected-feed (cell :articles-feed)]
    (div :class "profile-page"
      (div :class "user-info"
        (div :class "container"
          (div :class "row"
            (div :class "col-xs-12 col-md-10 offset-md-1"
              (img :class "user-img" :src (cell= (:image state/profile)))
              (h4 (cell= (:username state/profile)))
              (p (cell= (:bio state/profile)))

              (if-tpl state/is-user?
                (button
                  :click #(easy/push-state :settings)
                  :class "action-btn btn btn-sm btn-outline-secondary" "Edit Profile")
                (btn-follow
                  :toggle     state/logged-in?
                  :username   (cell= (:username state/profile))
                  :following? (path-cell state/profile [:following])))))))

      (div :class "container"
        (div :class "row"
          (div :class "col-xs-12 col-md-10 offset-md-1"
            (div :class "articles-toggle"
              (ul :class "nav nav-pills outline-active"
                (li :class "nav-item"
                  (a :click (fn [e]
                              (.preventDefault e)
                              (actions/fetch-feed {:author (:username @state/profile)})
                              (reset! selected-feed :articles-feed))
                    :class (cell= {:nav-link true :active (= selected-feed :articles-feed)})
                    :href "" "My Articles"))
                (li :class "nav-item"
                  (a :click (fn [e]
                              (.preventDefault e)
                              (actions/fetch-feed {:favorited (:username @state/profile)})
                              (reset! selected-feed :favorited-feed))
                     :class (cell= {:nav-link true :active (= selected-feed :favorited-feed)})
                     :href "" "Favorited Articles"))))

            (feed-content
              :feed     state/feed
              :loading? (cell= (:feed state/loading?)))
            (feed-pagination
              :toggle      (cell= (not (:feed state/loading?)))
              :total-count state/feed-total-count
              :page-size   state/page-size
              :feed-params (cell= {:author (when (= selected-feed :articles-feed)
                                             (:username state/profile))
                                   :favorited (when (= selected-feed :favorited-feed)
                                                (:username state/profile))}))))))))

;; Settings Page --------------------------------------------------------------

(defelem settings-page []
  (let [form-data   (cell {:image    (:image @state/user)
                           :username (:username @state/user)
                           :bio      (:bio @state/user)
                           :email    (:email @state/user)})
        form-errors (cell {})]

    (div :class "settings-page"
      (div :class "container-page"
        (div :class "row"

          (div :class "col-md-6 offset-md-3 col-xs-12"
            (h1 :class "text-xs-center" "Your Settings")

            (ul :toggle (cell= (not-empty form-errors)) :class "error-messages"
              (for-tpl [error (fmt-error-msgs form-errors)]
                (li (cell= error))))

            (form :submit (fn [e]
                            (actions/update-profile! @form-data state/user form-errors)
                            (.preventDefault e))
              (fieldset :class "form-group"
                (input
                  :change (fn [e] (swap! form-data #(assoc % :image @e)))
                  :value (cell= (:image state/user))
                  :class "form-control"
                  :type "text"
                  :placeholder "URL of profile picture"))
              (fieldset :class "form-group"
                (input
                  :change (fn [e] (swap! form-data #(assoc % :username @e)))
                  :value (cell= (:username state/user))
                  :class "form-control form-control-lg"
                  :type "text"
                  :placeholder "Your Name"))
              (fieldset :class "form-group"
                (textarea
                  :change (fn [e] (swap! form-data #(assoc % :bio @e)))
                  :value (cell= (:bio state/user))
                  :class "form-control form-control-lg"
                  :rows "8"
                  :placeholder "Short bio about you"))
              (fieldset :class "form-group"
                (input
                  :change (fn [e] (swap! form-data #(assoc % :email @e)))
                  :value (cell= (:email state/user))
                  :class "form-control form-control-lg"
                  :type "text"
                  :placeholder "Email"))
              (fieldset :class "form-group"
                (input
                  :change (fn [e] (swap! form-data #(assoc % :password @e)))
                  :class "form-control form-control-lg"
                  :type "password"
                  :placeholder "Password"))
              (button :class "btn btn-lg btn-primary pull-xs-right" "Update Settings")))

          (div :class "col-md-6 offset-md-3 col-xs-12"
            (hr)
            (button
              :click #(do (reset! state/user {})
                          (easy/push-state :home))
              :class "btn btn-outline-danger" "Or click here to logout")))))))

;; Edit Article Page ----------------------------------------------------------

(defelem editor [{:keys [editing?]} _]
  (let [form-data   (cell {})
        form-errors (cell {})]

    (div :class "editor-page"
      (div :class "container page"
        (div :class "row"
          (div :class "col-md-10 offset-md-1 col-xs-12"

            (ul :toggle (cell= (not-empty form-errors)) :class "error-messages"
              (for-tpl [error (fmt-error-msgs form-errors)]
                (li (cell= error))))

            (form :submit (fn [e]
                            (if editing?
                              (actions/update-article! (:slug @state/article) @form-data)
                              (actions/submit-article! @form-data {} form-errors))
                            (.preventDefault e))
              (fieldset
                (fieldset :class "form-group"
                  (input
                    :change (fn [e] (swap! form-data #(assoc % :title @e)))
                    :value (cell= (when editing? (:title state/article)))
                    :class "form-control form-control-lg"
                    :type "text"
                    :placeholder "Article Title"))
                (fieldset :class "form-group"
                  (input
                    :change (fn [e] (swap! form-data #(assoc % :description @e)))
                    :value (cell= (when editing? (:description state/article)))
                    :class "form-control form-control-lg"
                    :type "text"
                    :placeholder "What's this article about?"))
                (fieldset :class "form-group"
                  (textarea
                    :change (fn [e] (swap! form-data #(assoc % :body @e)))
                    :value (cell= (when editing? (:body state/article)))
                    :class "form-control form-control-lg"
                    :rows "8"
                    :placeholder "Write your article (in markdown)"))
                (fieldset :class "form-group"
                  (input
                    :change (fn [e]
                              (swap! form-data #(assoc % :tagList (clojure.string/split @e #" "))))
                    :value (cell= (when editing? (apply str (interpose " " (:tagList state/article)))))
                    :class "form-control"
                    :type "text"
                    :placeholder "Enter tags"
                    (div :class "tag-list")))
                (button :class "btn btn-lg pull-xs-right btn-primary"
                  (if editing?
                    "Update Article"
                    "Publish Article"))))))))))

(defn edit-article-page []
  (editor :editing? true))

;; Article Page ---------------------------------------------------------------

(defn article-comment [{:keys [body author createdAt]} user]
  (let [author-link (easy/href :profile {:username (:username author)})]
    (div :class "card"
      (div :class "card-block"
        (p :class "card-texr" body))
      (div :class "card-footer"
        (a :class "comment-author" :href author-link
          (img :class "comment-author-img" :src (:image author)))
        nbsp
        (a :class "comment-author" :href author-link (:username author))
        (span :class "date-posted" (format-date createdAt))
        (span :toggle (cell= (= (:username author) (:username user))) :class "mod-options"
          (i :class "ion-trash-a"))))))

(defn comment-editor [article user logged-in?]
  (let [comment-form  (cell {})]
    (form
      :toggle logged-in?
      :submit (fn [e]
                (actions/add-comment! @comment-form (:slug @article))
                (reset! comment-form {})
                (.preventDefault e))
      :class "card comment-form"
      (div :class "card-block"
        (textarea
          :change (fn [e] (swap! comment-form #(assoc % :body @e)))
          :value  (cell= (:body comment-form))
          :class "form-control"
          :rows "3"
          :placeholder "Write a comment..."))
      (div :class "card-footer"
        (img :class "comment-author-img" :src (cell= (:image user)))
        (button :class "btn btn-sm btn-primary" "Post Comment")))))

(defn article-meta-logged-in [article]
  (let [favorited?        (cell= (:favorited article))
        following-author? (path-cell article [:author :following])]
    (span
      (btn-follow
        :username   (cell= (-> article :author :username))
        :following? following-author?)
      (str nbsp nbsp)
      (button
        :click  #(actions/toggle-favorite! (:slug @article) @favorited?)
        :class  (cell= {:btn                 true
                        :btn-sm              true
                        :btn-primary         true
                        :btn-outline-primary (not favorited?)})
        (i :class "ion-heart")
        (str nbsp
          "Favorite Article ")
        (span :class "counter" (cell= (str "(" (:favoritesCount article) ")")))))))

(defn article-meta-owner [article]
  (span
    (button
      :click #(easy/push-state :edit-article {:slug (:slug @article)})
      :class "btn btn-sm btn-outline-secondary"
      (i :class "ion-edit")
      " Edit Article")
    (str nbsp nbsp)
    (button
      :click #(actions/delete-article! @article)
      :class "btn btn-sm btn-outline-danger"
      (i :class "ion-trash-a")
      " Delete Article")))

(defn article-meta [article is-owner? logged-in?]
  (let [author-link (cell= (easy/href :profile {:username (-> article :author :username)}))]
    (div :class "article-meta"
      (a :ḧref author-link
        (img :src (cell= (-> article :author :image))))
      (div :class "info"
        (a :class "author" :href author-link (cell= (-> article :author :username)))
        (span :class "date" (cell= (format-date (-> article :createdAt)))))
      (cond-tpl
        is-owner? (article-meta-owner article)
        logged-in? (article-meta-logged-in article)))))

(defelem article-page []
  (div :class "article-page"
    (div :class "banner"
      (div :class "container"
        (h1 (cell= (:title state/article)))
        (article-meta state/article state/is-owner? state/logged-in?)))

    (div :class "container page"
      (div :class "row article-content"
        (div :class "col-md-12"
          (p (cell= (:body state/article)))))

      (hr)

      (div :class "article-actions"
        (article-meta state/article state/is-owner? state/logged-in?))

      (div :clas "row"
        (div :class "col-xs-12 col-md-8 offset-md-2"
          (p  :toggle (cell= (not state/logged-in?))
            (a :href (easy/href :signin) "Sign in")
            " or "
            (a :href (easy/href :signup) "sign up")
            " to add comments on this article.")

          (comment-editor state/article state/user state/logged-in?)

          (for-tpl [c state/article-comments]
            (div (cell= (article-comment c state/user)))))))))

(defn current-page []
  (div
    (header)
    (cell=
      (when-let [view (:view state/selected-page)]
        (view)))
    (footer_)))
