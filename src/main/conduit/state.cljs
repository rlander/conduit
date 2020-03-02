(ns conduit.state
  (:require [hoplon.storage-atom :refer [local-storage]]
            [javelin.core        :refer [cell cell=]
                                 :include-macros true]))

;; Common

(def page-size         10)
(defonce selected-page (cell {:name nil :view nil}))
(def user              (-> (cell {}) (local-storage :user)))
(def logged-in?        (cell= (seq user)))
(def anonymous?        (cell= (not logged-in?)))
(def loading?          (cell {}))


;; Home

(def tags             (cell []))
(def feed             (cell []))
(def feed-total-count (cell nil))


;; Article

(def article          (cell {}))
(def article-comments (cell []))
(def is-owner?        (cell= (= (:username user) (-> article :author :username))))


;; Profile

(def profile  (cell {}))
(def is-user? (cell= (= (:username profile) (:username user))))
