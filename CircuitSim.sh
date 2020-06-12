#!/bin/bash
if test -f "$1"
then
	./run/wv <"$1" | java -jar run/results.jar
else
	./run/wv <"Sample Netlists/test_netlist_10.cir" | java -jar run/results.jar
fi
