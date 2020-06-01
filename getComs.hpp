#ifndef getcoms_hpp
#define getcoms_hpp

#include <iostream>
#include <regex>
#include <any>
#include <string>
#include <armadillo>
#include "Matrix.hpp"
#include "component.hpp"

using namespace std;

void GetComs(){
    const regex comment("([*].*)"); //* followed by anything, ie a comment (haha meta)
    const regex cEnd("([.]end)"); //an end instruction, implying anything beyond that point needn't be parsed
    const regex op("([.]op)"); //a dc bias check command
    const regex dc("DC [0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?"); //A DC source
    const regex sine("SINE ([(][0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)? [0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)? [0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?[)])"); //An AC source with SINE input
    const regex node("(( 0)|( N[0-9][0-9][0-9]))"); //A node: 0 or N followed by 3 digits.
    const regex res("(R(([0-9]+)|([A-z]+))+)"); //A named resistor
    const regex vSrc("V(([0-9]+)|([A-z]+))+"); //A named voltage source
    const regex cSrc("I(([0-9]+)|([A-z]+))+"); //A named current source
    const regex value(" ([0-9]+)(([.][0-9]+)?)((p|n|u|µ|m|k|(Meg)|G)?)"); //A double value including the unit prefix
    const regex tranEx("([.]tran 0 [0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?s 0 [0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?s)"); //A transient simulation command. Interestingly this has units unlike the others.
    const regex cSrcEx("(I(([0-9]+)|([A-z]+))+ ((N[0-9][0-9][0-9])|0) ((N[0-9][0-9][0-9])|0) ((SINE)|(DC))(( [0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?)|([(][0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)? [0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)? [0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?[)])))"); //A full line in the CIR file for any type of current source, either AC or DC
    const regex resEx("(R(([0-9]+)|([A-z]+))+ ((N[0-9][0-9][0-9])|0) ((N[0-9][0-9][0-9])|0) [0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?)"); //A full line in the CIR file for any resistor
    const regex vSrcEx("(V(([0-9]+)|([A-z]+))+ ((N[0-9][0-9][0-9])|0) ((N[0-9][0-9][0-9])|0) ((SINE)|(DC))(( [0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?)|([(][0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)? [0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)? [0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?[)])))");//A full line in the CIR file for any type of voltage source, either AC or DC
    const regex indEx("(L(([0-9]+)|([A-z]+))+ ((N[0-9][0-9][0-9])|0) ((N[0-9][0-9][0-9])|0) [0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?)"); //A full line in the CIR file for any resistor
    const regex capEx("(C(([0-9]+)|([A-z]+))+ ((N[0-9][0-9][0-9])|0) ((N[0-9][0-9][0-9])|0) [0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?)"); //A full line in the CIR file for any resistor
}


#endif
