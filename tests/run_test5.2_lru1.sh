#!/bin/bash

# ROOT_DIR="tmp/server"
# mkdir -p "$ROOT_DIR"
# cd "$ROOT_DIR" || { echo "Failed to navigate to $ROOT_DIR"; exit 1; }
# for letter in {A..H}; do
#     dd if=/dev/zero of=${letter} bs=1 count=1000002 2>/dev/null
# done
# cd ../../

export proxyport15440=10000
export pin15440=000000000

gcc -o ../tools/test5.2 ../tools/test5.2.c

sleep 5
LD_PRELOAD=../lib/lib440lib.so ../tools/test5.2

echo "All commands completed."