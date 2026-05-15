#!/usr/bin/env bash
set -euo pipefail

ensure_replica_set() {
  local rs_uri="$1"
  local rs_config="$2"

  mongosh --host "$rs_uri" --quiet --eval "
    const config = $rs_config
    let status
    try {
      status = rs.status()
    } catch (error) {
      status = { ok: 0 }
    }
    if (status.ok !== 1) {
      try {
        rs.initiate(config)
      } catch (error) {
        if (error.codeName !== 'AlreadyInitialized') {
          throw error
        }
      }
    }
    while (true) {
      try {
        const current = rs.status()
        if (current.ok === 1 && current.members.some((member) => member.stateStr === 'PRIMARY')) {
          break
        }
      } catch (error) {
      }
      sleep(1000)
    }
  "
}

ensure_replica_set "configsvr:27019" '{
  _id: "cfg",
  configsvr: true,
  members: [{ _id: 0, host: "configsvr:27019" }]
}'
ensure_replica_set "shard1-primary:27018" '{
  _id: "shard1",
  members: [
    { _id: 0, host: "shard1-primary:27018" },
    { _id: 1, host: "shard1-secondary1:27018" },
    { _id: 2, host: "shard1-secondary2:27018" }
  ]
}'
ensure_replica_set "shard2-primary:27018" '{
  _id: "shard2",
  members: [
    { _id: 0, host: "shard2-primary:27018" },
    { _id: 1, host: "shard2-secondary1:27018" },
    { _id: 2, host: "shard2-secondary2:27018" }
  ]
}'
