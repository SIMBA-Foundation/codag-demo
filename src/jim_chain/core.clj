(ns jim-chain.core
  (:require [digest])
  (:import [java.util UUID]))

(def curr-node-addr (UUID/randomUUID))

;;所有（其它）节点
(def nodes (atom []))

(defn add-node!
  "添加节点"
  [address]
  (swap! nodes conj address))

(defn block-hash
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
   :hash (block-hash genesis-block)
   :previous-hash 0
   :proof 68867})

;;chaindata
(def chain (atom [genesis-block]))

;;提交到本节点，等待被写入的交易列表
(def waitting-transactions (atom []))

(defn add-transaction!
  "添加等待写入的交易"
  [sender recipient amount]
  (swap! waitting-transactions conj {:sender sender
                                     :recipient recipient
                                     :amount amount}))


;;##############POW###############
(defn valid-proof?
  "是否可用的工作量证明值"
  [previous-block proof]
  (.startsWith (digest/sha-256 (str (:hash previous-block) proof)) "0000"))

(defn calculate-proof
  "计算工作量证明值"
  [previous-block]
  (loop [p 0]
    (if-not (valid-proof? previous-block p)
      (recur (inc p))
      p)))
(comment
  (proof-of-work genesis-block))

(defn add-block!
  [proof]
  (let [previous-block (last @chain)
        block {:index (inc (:index previous-block))
               :timestamp (System/nanoTime)
               :transactions @waitting-transactions
               :previous-hash (:hash previous-block)
               :proof proof}
        block (assoc block :hash (block-hash block))]
    (swap! chain conj block)
    (reset! waitting-transactions [])))

(defn mine!
  []
  (let [proof (calculate-proof (last @chain))]
    ;;矿工奖励
    (add-transaction! nil curr-node-addr 1)
    (add-block! proof)))

(defn sync-chain!
  "和所有节点同步数据，找到最长的正确的链"
  []
  ;;TODO
  )
