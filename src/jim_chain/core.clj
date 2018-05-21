(ns jim-chain.core
  (:require [digest])
  (:import [java.util UUID]))

(def curr-node-addr (UUID/randomUUID))

;;所有（其它）节点
(def nodes (atom []))

(defn hash
  "计算区块的hash值"
  [block]
  (let [{:keys [index timestamp transactions previous-hash]}  block]
    (digest/sha-256 (str index timestamp transactions previous-hash))))
(comment
  (hash genesis-block))

;;创世块
(def genesis-block
  {:index 0
   :timestamp 0
   :transactions []
   :hash (hash genesis-block)
   :previous-hash 0})

;;提交到本节点，等待被写入的交易列表
(def waitting-transactions (atom []))

(defn add-transaction!
  "添加等待写入的交易"
  [sender recipient amount]
  (swap! waitting-transactions conj {:sender sender
                                     :recipient recipient
                                     :amount amount}))
