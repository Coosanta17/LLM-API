#!/bin/bash

LOG_DIR="./logs"
LOG_FILE="$LOG_DIR/latest.log"
TIMESTAMP=$(date +"%Y-%m-%d_%H-%M-%S")
ARCHIVE_FILE="$LOG_DIR/logs_$TIMESTAMP.tar.gz"

mkdir -p "$LOG_DIR"

if [[ -f "$LOG_FILE" ]]; then
    tar -czf "$ARCHIVE_FILE" "$LOG_FILE" && rm "$LOG_FILE"
    echo "Archived old log to $ARCHIVE_FILE"
fi

exec mvn spring-boot:run 2>&1 | tee -a "$LOG_FILE"
