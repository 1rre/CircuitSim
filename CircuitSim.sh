#!/bin/bash
if test -f "$1"
then
	./run/CircuitSim <"$1" | java -jar run/results.jar
else
	./run/CircuitSim <"Sample Netlists/test_netlist_10.cir" | java -jar run/results.jar
fi
