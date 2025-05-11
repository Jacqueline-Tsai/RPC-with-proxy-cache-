#!/bin/bash

ROOT_DIR="tmp/server"
mkdir -p "$ROOT_DIR"
cd "$ROOT_DIR" || { echo "Failed to navigate to $ROOT_DIR"; exit 1; }
for letter in {A..H}; do
    dd if=/dev/zero of=${letter} bs=1 count=1000002 2>/dev/null
done
cd ../../