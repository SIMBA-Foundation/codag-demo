(ns codag-demo.core
  (:require [digest])
  (:import [java.util UUID]))

;;#############peer##################
(def curr-peer-addr (UUID/randomUUID))
(def peers (atom []))
(defn add-peer!
  [address]
  (swap! peers conj address))

(defn node-hash
  "计算node的hash值"
  [node]
  (let [{:keys [timestamp miner-addr transactions pre-level-hash-set proof]} node]
    (digest/sha-256 (str timestamp miner-addr transactions pre-level-hash-set proof))))
(comment
  (hash genesis-block))

;;##############POW###############
(defn valid-proof?
  "是否可用的工作量证明值"
  [pre-level-hash-set miner-addr timestamp proof]
  ;;难度是四个零
  (.startsWith (digest/sha-256 (str pre-level-hash-set miner-addr timestamp proof)) "0000"))

(defn calculate-proof
  "计算工作量证明值"
  [{:keys [pre-level-hash-set miner-addr timestamp]}]
  (loop [p 0]
    (if-not (valid-proof? pre-level-hash-set miner-addr timestamp p)
      (recur (inc p))
      p)))

(def K 3)

;;创世节点
(def genesis-node
  (->
   {:timestamp 0
    :miner-addr "ac45c4ec-b2ac-4b64-9bfb-59d5cbce5f6f" ;;创世区块矿工地址
    :transactions []
    ;;指向的上一个level的K个node的集合
    :pre-level-hash-set #{}}
   (#(assoc % :proof (calculate-proof %)))
   (#(assoc % :hash (node-hash %)))))

;;CoDAG chain
(def chain (atom [#{genesis-node}]))

;;提交到本节点，等待被写入的交易列表
(def waitting-transactions (atom []))

(defn add-transaction!
  "添加等待写入的交易"
  [from to amount]
  (swap! waitting-transactions conj
         {:from from
          :to to
          :amount amount}))

(defn last-level
  "最后一层"
  [chain]
  (if (= 1 (count chain)) ;第一个区块自成一层
    (last chain)
    (if (< (count (last chain)) K)
      (last (butlast chain)) ;不满K个，为上上层
      (last chain))))

(defn level-hash-set
  "指向的一层的hash集合"
  [level]
  (set
   (map :hash (take K level))))

(defn split
  "return [chain-head last-level curr-level]"
  [chain]
  (cond
    (= 1 (count chain)) [[] (last chain) #{}]
    (<= K (count (last chain))) [(subvec chain 0 (dec (count chain))) (last chain) #{}]
    (> K (count (last chain))) [(subvec chain 0 (- (count chain) 2))
                                (nth chain (- (count chain) 2))
                                (last chain)]))

(defn add-node!
  [chain-head last-level curr-level timestamp proof]
  (let [node (-> {:timestamp timestamp
                  :miner-addr curr-peer-addr
                  :transactions @waitting-transactions
                  :pre-level-hash-set (level-hash-set last-level)
                  :proof proof}
                 (#(assoc % :hash (node-hash %))))]
    (reset! chain (conj chain-head last-level (conj curr-level node)))
    (reset! waitting-transactions [])))

(defn mine!
  []
  (let [[chain-head last-level curr-level] (split @chain)
        _ (prn :level-hash-set (level-hash-set last-level))
        timestamp (System/nanoTime)
        proof (calculate-proof
               {:pre-level-hash-set (level-hash-set last-level)
                :miner-addr  curr-peer-addr
                :timestamp timestamp})]
    ;;矿工奖励
    (add-transaction! nil curr-peer-addr 1)
    (add-node! chain-head last-level curr-level timestamp proof)))

(defn sync-chain!
  "和所有节点同步数据，找到最长的正确的链"
  []
  ;;TODO
  )
