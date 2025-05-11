#!/bin/bash

export proxyport15440=12345
export pin15440=123456789

ROOT_DIR="tmp/server"
mkdir -p "$ROOT_DIR"
cd "$ROOT_DIR" || { echo "Failed to navigate to $ROOT_DIR"; exit 1; }
dd if=/dev/zero of=unlinked bs=1 count=200 2>/dev/null
cd ../../

gcc -o ../tools/test10 ../tools/test10.c

LD_PRELOAD=../lib/lib440lib.so ../tools/test10

echo "All commands completed."