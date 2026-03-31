#!/bin/bash
set -e

# Wait for master to be ready
until PGPASSWORD=$POSTGRES_PASSWORD pg_isready -h postgres-master -p 5432 -U $POSTGRES_USER; do
  echo "Waiting for master..."
  sleep 2
done

# Stop PostgreSQL to reconfigure as slave
pg_ctl -D "$PGDATA" -m fast -w stop || true

# Clean data directory
rm -rf "$PGDATA"/*

# Create base backup from master
PGPASSWORD=replicator123 pg_basebackup -h postgres-master -D "$PGDATA" -U replicator -v -P --wal-method=stream -S slave_slot

# Configure as standby
cat > "$PGDATA/postgresql.auto.conf" <<EOF
primary_conninfo = 'host=postgres-master port=5432 user=replicator password=replicator123'
primary_slot_name = 'slave_slot'
EOF

touch "$PGDATA/standby.signal"

# Restart PostgreSQL
pg_ctl -D "$PGDATA" -w start
