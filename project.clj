;; Copyright (c) Eric Normand. All rights reserved.
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 [1] which can be found in the file
;; epl-v10.html at the root of this distribution.  By using this
;; software in any fashion, you are agreeing to be bound by the terms
;; of this license.
;;
;; [1]: http://opensource.org/licenses/eclipse-1.0.php
;;
;; You must not remove this notice, or any other, from this software.
(defproject clj-peg "0.6.0"
  :description "clj-peg is a PEG parser written in Clojure.  It can
  parse any sequence of data types, including lists and vectors."
  
  :dependencies     [[org.clojure/clojure  "[1.2.0,)"]]
  
  :dev-dependencies [[leiningen/lein-swank "[1.1.0,)"]
                     [swank-clojure        "[1.2.0,)"]
                     [lein-marginalia      "0.6.0"]]

  :aot [clj-peg.example-genclass])
