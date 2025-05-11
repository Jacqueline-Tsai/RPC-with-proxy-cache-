#!/bin/bash

export proxyport15440=12345
export pin15440=123456789

gcc -o ../tools/test8 ../tools/test8.c

LD_PRELOAD=../lib/lib440lib.so ../tools/test8

echo "All commands completed."