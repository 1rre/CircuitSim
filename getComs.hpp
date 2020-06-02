#ifndef getcoms_hpp
#define getcoms_hpp

#include <iostream>
#include <regex>
//#include <armadillo>
//#include "Matrix.hpp"
#include "component.hpp"
#include <fstream>

using namespace std;

double getVal(string num);

Sim getComs(){
	int cCnt = 0;
	int sCnt = 0;
	int rCnt = 0;
	const regex value(" (([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?)?)");
    const regex comment("([*].*)"); //* followed by anything, ie a comment (haha meta)
    const regex tranEx("([.]tran 0 [0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?s 0 [0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?s)"); //A transient simulation command. Interestingly this has units unlike the others.
    const regex dc("(DC (([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?)?))"); //A DC source
    const regex sine("(SINE [(](([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?)?) (([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?)?) (([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?)?) (([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?)?) (([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?)?) (([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?)?) (([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?)?))[)]"); //An AC source with SINE input
	const regex pulse("(PULSE [(](([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?)?) (([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?)?) (([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?)?) (([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?)?) (([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?)?) (([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?)?) (([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?)?) (([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?)?)[)])");
	const regex exp("(EXP [(](([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?)?) (([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?)?) (([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?)?) (([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?)?) (([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?)?) (([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?)?)[)])");
	const regex sffm("(SFFM [(](([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?)?) (([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?)?) (([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?)?) (([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?)?) (([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?)?) (([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?)?)[)])");
	const regex pwlVals("((PWL) ((REPEAT FOR)(( [0-9]+)|(EVER)))? [(]((([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?) ([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?))*)|(file[=](.*))[)] (ENDREPEAT)? (TRIGGER V([(]N[0-9][0-9][0-9])[)](([>])|([=])|([<]))(([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?)?))?)");
	const regex am("(AM [(](([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?)?) (([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?)?) (([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?)?) (([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?)?) (([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?)?)[)])");
	const regex node("(( 0)|( N[0-9][0-9][0-9]))"); //A node: 0 or N followed by 3 digits.
    const regex vCom("((R|L|C)(([0-9]+)|([A-z]+))+)"); //A named resistor
    const regex src("(V|I)(([0-9]+)|([A-z]+))+"); //A named voltage source
    const regex vComEx("(R(([0-9]+)|([A-z]+))+ ((N[0-9][0-9][0-9])|0) ((N[0-9][0-9][0-9])|0) [0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?)"); //A full line in the CIR file for any resistor
    const regex srcEx("((V|I)(([0-9]+)|([A-z]+))+ ((N[0-9][0-9][0-9])|0) ((N[0-9][0-9][0-9])|0) ())");//A full line in the CIR file for any type of voltage source, either AC or DC
    Sim rtn; //This will be our sim. There are many like it but this one is ours. Our sim is our best friend. It is our life. We must master it as we master our lives. Without us, our sim is useless. Without our sim, we are useless. We must run our sim true. We must simulate faster than the programs who are trying to simulate us. We must simulate them before they simulate us. Our sim and us know what counts in simulation is not the circuits you simulate, the current sources we approximate, nor the resistors we model. We know that it is the voltages we calculate that count. Our sim is human, even as us, because it is our life. Thus, we will learn it as a brother. We will learn its weaknesses, its strengths, its functions, its objects, its variables and its bugs. We will keep our sim well commented and optimised. We will become part of each other. Before Dave Thomas, we swear this creed. Our sim and us are the simulators of SPICE circuits. We are the masters of current sources. We are the simulators of life. So it be, until the circuit has been simulated and there are no more current sources, but comma separated values.
	rtn.nodes.push_back(Node());
	vector<string> lines; //The vector of strings read from cin. Used so that the user can input lines without having to wait for them to parse.
    while(cin){ //While data is being inputted
		string line=""; //Create a blank string to store the line in
		getline(cin, line); //Add the next line to the string
		lines.push_back(line); //Add the line to the vector of lines.
	}
	for(string l:lines){
		if(regex_match(l,vComEx)){ //Resistors, capacitors and inductors
			vComponent v;
			smatch m;
			regex_search(l, m, vCom);
			v.uName = m.str(0); //2 of 6
			v.cName = v.uName[0]; //3 of 6
			string _l = m.suffix();
			regex_search(_l,m,node);
			int nd;
			if(m.str(0) == " 0"){
				nd = 0;
			}
			else{
				nd = stoi(m.str(0).substr(2));
			}
			if(nd >= rtn.nodes.size()){
				for(int i = rtn.nodes.size(); i<=nd;i++){
					rtn.nodes.push_back(Node(i));
				}
			}
			v.pos = &rtn.nodes[nd]; //4 of 6
			_l = m.suffix();
			regex_search(_l,m,node);
			if(m.str(0) == " 0"){
				nd = 0;
			}
			else{
				nd = stoi(m.str(0).substr(2));
			}
			if(nd >= rtn.nodes.size()){
				for(int i = rtn.nodes.size(); i<=nd;i++){
					rtn.nodes.push_back(Node(i));
				}
			}
			v.neg = &rtn.nodes[nd]; //5 of 6
			v.val = getVal(string(m.suffix()).substr(1));
			if(v.cName == 'R'){
				v.id = rCnt; //1 of 6
				rCnt++;
				rtn.resistors.push_back(v);
			}
			else{
				v.id = cCnt; //1 of 6
				cCnt++;
				rtn.reactComs.push_back(v);
			}
		}
		else if(regex_match(l,srcEx)){ //Sources
			/*	 IMPORTANT
				PWL TIMES CAN BE DEFINED AS +1 ON THE PREVIOUS TIME
				PWL ALLOWS REPEAT FOR 5
				PWLFILE NEEDS TO BE COPIED TO PWL
			*/

		}
		else if(regex_match(l,tranEx)){ //Transient

		}
		else if(l==".op"){
			rtn.DC(); //Set the number of steps to 0. This is how we will decide something is a bias point check.
		}
		else if(l==".end"){
			break; //break out of the for loop
		}
		else if(!regex_match(l,comment)){ //If the line isn't a command or a comment
			cerr<<"Exception in netlist: Invalid format '"<<l<<"' found. Exiting."<<endl;
			exit(1); //Exit with error code 1: Invalid format in netlist //TODO: make list of error codes and what they mean
		}
	}
    return rtn;
}

double getVal(string num){ //The function that a number with a unit prefix (ie 1μ, 13.6k etc.)
	double val; //double to store the result in
	switch(num.back()){
		case 'p': //Pico
		val = stof(num.substr(0, num.length()-1))*1e-12; //Value is the numerical part of the input string by 10⁻¹²
		break;
		case 'n': //Nano
		val = stof(num.substr(0, num.length()-1))*1e-9; //Value is the numerical part of the input string by 10⁻⁹
		break;
		case 'u': //Micro
		val = stof(num.substr(0, num.length()-1))*1e-6; //Value is the numerical part of the input string by 10⁻⁶
		break;
		/*		case 'μ':
		val = stof(num.substr(0, num.length()-1))*1e-6; //Value is the numerical part of the input string by 10⁻⁶ //μ cannot be stored as a char so disabled for the time being
		break;*/
		case 'm': //milli
		val = stof(num.substr(0, num.length()-1))*1e-3; //Value is the numerical part of the input string by 10⁻³
		break;
		case 'k': //kilo
		val = stof(num.substr(0, num.length()-1))*1e3; //Value is the numerical part of the input string by 10³
		break;
		case 'g': //mega - g as mega is used as MEG
		val = stof(num.substr(0, num.length()-3))*1e6; //Value is the numerical part of the input string by 10⁶
		break;
		case 'G': //giga
		val = stof(num.substr(0, num.length()-1))*1e9; //Value is the numerical part of the input string multiplied by 10⁹
		break;
		default: //If there is no unit prefix. Safeguards are not required here as this function is only called after regex_search or regex_match, which identifies the value as correctly formed.
		val = stof(num); //Value is the input string
	}
	return val;
}

#endif
