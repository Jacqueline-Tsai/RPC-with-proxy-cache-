#!/bin/bash
export proxyport15440=12345
export pin15440=123456789

gcc -o ../tools/test1.1 ../tools/test1.1.c
gcc -o ../tools/test1.2 ../tools/test1.2.c
gcc -o ../tools/test1.3 ../tools/test1.3.c
LD_PRELOAD=../lib/lib440lib.so ../tools/test1.3 &
LD_PRELOAD=../lib/lib440lib.so ../tools/test1.2 &
LD_PRELOAD=../lib/lib440lib.so ../tools/test1.1 &

wait

echo "All commands completed."