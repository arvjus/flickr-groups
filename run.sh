#!/bin/sh

for f in "$@"; do
	echo processing $f...
	java -jar ./flickrgroups-1.0.jar $f
done
