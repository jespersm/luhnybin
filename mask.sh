#!/bin/sh

# Short and sweet build toolchain:
if [ LuhnFilter.java -nt LuhnFilter.class ]; then
  javac LuhnFilter.java
fi

# Go ahead, run the thing
java LuhnFilter

