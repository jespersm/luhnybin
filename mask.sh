#!/bin/sh

# Short and sweet build toolchain:
if [ luhn.c -nt luhn ]; then
  gcc -O4 luhn.c -o luhn
fi

# Go ahead, run the thing
./luhn

