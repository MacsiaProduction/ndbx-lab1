#!/usr/bin/env bash
set -euo pipefail

mongosh --host mongos:27017 --quiet --eval '
  const shardNames = () => db.adminCommand({ listShards: 1 }).shards.map((shard) => shard._id)
  if (!shardNames().includes("shard1")) {
    sh.addShard("shard1/shard1-primary:27018,shard1-secondary1:27018,shard1-secondary2:27018")
  }
  if (!shardNames().includes("shard2")) {
    sh.addShard("shard2/shard2-primary:27018,shard2-secondary1:27018,shard2-secondary2:27018")
  }
  try {
    sh.enableSharding("eventhub")
  } catch (error) {
  }

  const eventhub = db.getSiblingDB("eventhub")
  eventhub.users.createIndex({ username: 1 }, { unique: true })
  eventhub.users.createIndex({ full_name: 1 })
  eventhub.events.createIndex({ title: 1 })
  eventhub.events.createIndex({ created_by: "hashed" })
  eventhub.events.createIndex({ title: 1, created_by: 1 })
  eventhub.events.createIndex({ category: 1 })
  eventhub.events.createIndex({ price: 1 })
  eventhub.events.createIndex({ "location.city": 1 })
  eventhub.events.createIndex({ started_at: 1 })

  try {
    sh.shardCollection("eventhub.events", { created_by: "hashed" })
  } catch (error) {
  }
'
