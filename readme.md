# Team 1 Circuit Simulator

## Files

* Source code is located in [src](src)
* Executables are located in [run](run)
* Some simple reference netlists can be found in [Sample Netlists](Sample%20Netlists)

## Running

To run the program, run `./CircuitSim.sh [CIR FILE]`, or to use the [default netlist](Sample%20Netlists/test_netlist_10.cir) run `./CircuitSim.sh`

## Prerequisites

* You must have JDK version 8 installed to run the program. This can be installed on debian via `sudo apt-get install openjdk-8-jre`. The [C++ binary](run/CircuitSim) can be run without any prerequisites installed.
* To compile the [source code](src), you must have "sbt" and "Armadillo" installed on your system. These can be installed with:
```
    sudo apt-get install liblapack-dev  
    sudo apt-get install libblas-dev  
    sudo apt-get install libboost-dev  
    sudo apt-get install libarmadillo-dev  
    sudo apt-get install sbt  
    sudo apt-get install openjdk-8-jre
```
