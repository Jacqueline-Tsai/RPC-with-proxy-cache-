#!/bin/bash

# Create directories and files
# ROOT_DIR="tmp/server"
# mkdir -p "$ROOT_DIR"
# cd "$ROOT_DIR" || { echo "Failed to navigate to $ROOT_DIR"; exit 1; }

# mkdir -p subdir/subdir
# mkdir -p subdir/subdir2

# dd if=/dev/zero of=huge_file bs=1 count=2000000 2>/dev/null
# dd if=/dev/zero of=subdir/huge_file bs=1 count=2000000 2>/dev/null
# dd if=/dev/zero of=subdir/subdir/huge_file bs=1 count=2000000 2>/dev/null
# dd if=/dev/zero of=subdir/subdir2/huge_file bs=1 count=2000000 2>/dev/null
# dd if=/dev/zero of=../hammer bs=1 count=17 2>/dev/null

# cd ../../

export proxyport15440=12345
export pin15440=123456789

gcc -o ../tools/test2 ../tools/test2.c

LD_PRELOAD=../lib/lib440lib.so ../tools/test2

echo "All commands completed."