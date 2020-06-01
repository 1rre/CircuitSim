#ifndef getcoms_hpp
#define getcoms_hpp

using namespace std;

static const regex comment("([*].*)"); //* followed by anything, ie a comment (haha meta)
static const regex cEnd("([.]end)"); //an end instruction, implying anything beyond that point needn't be parsed
static const regex op("([.]op)"); //a dc bias check command
static const regex dc("DC [0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?"); //A DC source
static const regex sine("SINE ([(][0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)? [0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)? [0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?[)])"); //An AC source with SINE input
static const regex node("(( 0)|( N[0-9][0-9][0-9]))"); //A node: 0 or N followed by 3 digits.
static const regex res("(R(([0-9]+)|([A-z]+))+)"); //A named resistor
static const regex vSrc("V(([0-9]+)|([A-z]+))+"); //A named voltage source
static const regex cSrc("I(([0-9]+)|([A-z]+))+"); //A named current source
static const regex value(" ([0-9]+)(([.][0-9]+)?)((p|n|u|µ|m|k|(Meg)|G)?)"); //A double value including the unit prefix
static const regex tranEx("([.]tran 0 [0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?s 0 [0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?s)"); //A transient simulation command. Interestingly this has units unlike the others.
static const regex cSrcEx("(I(([0-9]+)|([A-z]+))+ ((N[0-9][0-9][0-9])|0) ((N[0-9][0-9][0-9])|0) ((SINE)|(DC))(( [0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?)|([(][0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)? [0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)? [0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?[)])))"); //A full line in the CIR file for any type of current source, either AC or DC
static const regex resEx("(R(([0-9]+)|([A-z]+))+ ((N[0-9][0-9][0-9])|0) ((N[0-9][0-9][0-9])|0) [0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?)"); //A full line in the CIR file for any resistor
static const regex vSrcEx("(V(([0-9]+)|([A-z]+))+ ((N[0-9][0-9][0-9])|0) ((N[0-9][0-9][0-9])|0) ((SINE)|(DC))(( [0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?)|([(][0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)? [0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)? [0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?[)])))");//A full line in the CIR file for any type of voltage source, either AC or DC
static const regex indEx("(L(([0-9]+)|([A-z]+))+ ((N[0-9][0-9][0-9])|0) ((N[0-9][0-9][0-9])|0) [0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?)"); //A full line in the CIR file for any resistor
static const regex capEx("(C(([0-9]+)|([A-z]+))+ ((N[0-9][0-9][0-9])|0) ((N[0-9][0-9][0-9])|0) [0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?)"); //A full line in the CIR file for any resistor


#endif
