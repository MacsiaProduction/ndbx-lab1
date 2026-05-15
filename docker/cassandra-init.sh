#!/usr/bin/env bash
set -euo pipefail
HOST="${CASSANDRA_HOST:-cassandra-test}"
PORT="${CASSANDRA_PORT:-9042}"
KS="${CASSANDRA_KEYSPACE:?}"

cqlsh "$HOST" "$PORT" <<EOF
CREATE KEYSPACE IF NOT EXISTS ${KS} WITH replication = {'class': 'SimpleStrategy', 'replication_factor': 1};
CREATE TABLE IF NOT EXISTS ${KS}.event_reactions (
    event_id text,
    created_by text,
    like_value tinyint,
    created_at timestamp,
    PRIMARY KEY ((event_id), created_by)
);
CREATE INDEX IF NOT EXISTS event_reactions_like_value_idx ON ${KS}.event_reactions (like_value);
CREATE INDEX IF NOT EXISTS event_reactions_created_by_idx ON ${KS}.event_reactions (created_by);
EOF
