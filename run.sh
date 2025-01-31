#!/bin/bash

# Example script to run the API with logs and handle crashes.

LOG_DIR="./logs"
LOG_FILE="$LOG_DIR/latest.log"
TIMESTAMP=$(date +"%Y-%m-%d_%H-%M-%S")
ARCHIVE_FILE="$LOG_DIR/logs_$TIMESTAMP.tar.gz"

mkdir -p "$LOG_DIR"

# Archive and remove the old log if it exists
if [[ -f "$LOG_FILE" ]]; then
    tar -czf "$ARCHIVE_FILE" "$LOG_FILE" && rm "$LOG_FILE"
    echo "Archived old log to $ARCHIVE_FILE"
fi

while true; do
    echo "Starting process at $(date)" | tee -a "$LOG_FILE"
    mvn spring-boot:run 2>&1 | tee -a "$LOG_FILE"
    echo "Process crashed or exited at $(date), restarting..." | tee -a "$LOG_FILE"
    sleep 1  # Optional delay before restarting
done
