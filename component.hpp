#ifndef component_hpp
#define component_hpp

#include <string>
#include <functional>
#include <vector>
#include <cmath>
#include <limits>
#include <iostream>
#include <map>
#include <fstream>
#include <regex>
#include <cstdio>
#include <armadillo>
#include <tuple>


using namespace std;
using namespace arma;

double getVal(string num){ //The function that a number with a unit prefix (ie 1μ, 13.6k etc.)
	double val; //double to store the result in
	switch(num.back()){
		case 'p': //Pico
		val = stod(num.substr(0, num.length()-1))*1e-12; //Value is the numerical part of the input string by 10⁻¹²
		break;
		case 'n': //Nano
		val = stod(num.substr(0, num.length()-1))*1e-9; //Value is the numerical part of the input string by 10⁻⁹
		break;
		case 'u': //Micro
		val = stod(num.substr(0, num.length()-1))*1e-6; //Value is the numerical part of the input string by 10⁻⁶
		break;
		/*		case 'μ':
		val = stod(num.substr(0, num.length()-1))*1e-6; //Value is the numerical part of the input string by 10⁻⁶ //μ cannot be stored as a char so disabled for the time being
		break;*/
		case 'm': //milli
		val = stod(num.substr(0, num.length()-1))*1e-3; //Value is the numerical part of the input string by 10⁻³
		break;
		case 'k': //kilo
		val = stod(num.substr(0, num.length()-1))*1e3; //Value is the numerical part of the input string by 10³
		break;
		case 'g': //mega - g as mega is used as MEG
		val = stod(num.substr(0, num.length()-3))*1e6; //Value is the numerical part of the input string by 10⁶
		break;
		case 'G': //giga
		val = stod(num.substr(0, num.length()-1))*1e9; //Value is the numerical part of the input string multiplied by 10⁹
		break;
		default: //If there is no unit prefix. Safeguards are not required here as this function is only called after regex_search or regex_match, which identifies the value as correctly formed.
		val = stod(num); //Value is the input string
	}
	return val;
}

class Node{ //A node. As the nodes are numbered 0 or from N001 to N999 we can give them a unique integer ID directly from the CIR file
public:
	int ID = -1; //Used as the key for the right and left component maps
	double voltage;
	Node(int id);
};
Node::Node(int id){ //Constructor for a node where there is a nonzero voltage, ie not the reference node
	this->ID = id;
	this->voltage = double(0);
}
struct Component{
    char cName; //The component name ie "Resistor", "Capacitor" etc.
    string uName; //The name of the component as in the CIR file ie "R1", "Vin" etc.
    int id; //The unique (between components of the same type) identifier for the component.
    Node* pos; //The node to the "right" of this component. This is the cathode/positive pin of polar components.
    Node* neg; //The node to the "left" of this component. This is the anode/negative pin of polar components.
};
struct Resistor:Component{ //A linear component such as a resistor, capacitor, inductor or non-dependant source
	double val; //the value of the component in SI units. In sources this is the DC offset.
	double findCur(){
		auto x = (*this).pos;
		auto y = *(x);
		return(((*this).pos->voltage - this->neg->voltage)/this->val);
	}
};
struct Source:Component{ //Only voltage sources here, I heard that current kills
	double DCOffset;
	function<double(double)> waveform; //use 'waveform(time);' to run function
	void srcFunc(int id, vector<double> args){
		switch(id){
			case 0: //DC
				this->DCOffset = args[0];
				this->waveform = [args](double time){ return args[0]; };
			break;
			case 1:{ //Pulse
				double vInitial = 0, vOn = 0, tDelay = 0, tRise = 0, tFall = 0, tOn = 0, tPeriod = numeric_limits<double>::infinity(), nCycles = numeric_limits<double>::infinity();
				switch(args.size()){
					case 1:{ //Vinitial
						vInitial = args[0];
						break;}
					case 2:{ //Prevs & Von
						vInitial = args[0];
						vOn = args[1];
						break;}
					case 3:{ //Prevs & Tdelay
						vInitial = args[0];
						vOn = args[1];
						tDelay = args[2];
						break;}
					case 4:{ //Prevs & Trise
						vInitial = args[0];
						vOn = args[1];
						tDelay = args[2];
						tRise = args[3];
						break;}
					case 5:{ //Prevs & Tfall
						vInitial = args[0];
						vOn = args[1];
						tDelay = args[2];
						tRise = args[3];
						tFall = args[4];
						break;}
					case 6:{ //Prevs & Ton
						vInitial = args[0];
						vOn = args[1];
						tDelay = args[2];
						tRise = args[3];
						tFall = args[4];
						tOn = args[5];
						break;}
					case 7:{ //Prevs & Tperiod
						vInitial = args[0];
						vOn = args[1];
						tDelay = args[2];
						tRise = args[3];
						tFall = args[4];
						tOn = args[5];
						tPeriod = args[6];
						if(tOn != 0 && tPeriod < tRise + tFall + tOn){
							cerr<<"Source period does not match with active time. Exiting."<<endl;
							exit(3);
						}
						else if(tPeriod < tRise + tFall){
							double disp = tPeriod/(tRise + tFall);
							vOn *= disp;
							tRise *= disp;
							tFall *= disp;
						}
						break;}
					case 8:{ //Prevs & Ncycles
						vInitial = args[0];
						vOn = args[1];
						tDelay = args[2];
						tRise = args[3];
						tFall = args[4];
						tOn = args[5];
						tPeriod = args[6];
						nCycles = args[7];
						if(tOn != 0 && tPeriod < tRise + tFall + tOn){
							cerr<<"Source period does not match with active time. Exiting."<<endl;
							exit(3);
						}
						else if(tPeriod < tRise + tFall){
							double disp = tPeriod/(tRise + tFall);
							vOn *= disp;
							tRise *= disp;
							tFall *= disp;
						}
						break;}
				}
				this->DCOffset = vInitial;
				this->waveform = [vInitial, vOn, tDelay, tRise, tFall, tOn, tPeriod, nCycles](double time){
					if(time <= tDelay || time > tPeriod * nCycles + tDelay){
						return vInitial;
					}
					const double effTime = fmod(time - tDelay, tPeriod);
					if(effTime <= tRise){
						return vInitial + (vOn - vInitial) * (effTime / tRise);
					}
					else if(effTime <= tRise + tOn){
						return vOn;
					}
					else if(effTime <= tRise + tOn + tFall){
						return vOn - (vOn - vInitial) * (effTime - tOn - tRise) / tFall;
					}
					return vInitial;
				};
				break;}
			case 2:{ //Sine
				double vOffset = 0, vAmp = 0, freq = 0, tDelay = 0, theta = 0, phi = 0, nCycles = numeric_limits<double>::infinity();
				switch(args.size()){
					case 1:{ //Voffset
						vOffset = args[0];
						break;}
					case 2:{ //Prevs & Vamp
						vOffset = args[0];
						vAmp = args[1];
						break;}
					case 3:{ //Prevs & Freq
						vOffset = args[0];
						vAmp = args[1];
						freq = args[2];
						break;}
					case 4:{ //Prevs & Tdelay
						vOffset = args[0];
						vAmp = args[1];
						freq = args[2];
						tDelay = args[3];
						break;}
					case 5:{ //Prevs & Theta (damping)
						vOffset = args[0];
						vAmp = args[1];
						freq = args[2];
						tDelay = args[3];
						theta = args[4];
						break;}
					case 6:{ //Prevs & Phi (phase)
						vOffset = args[0];
						vAmp = args[1];
						freq = args[2];
						tDelay = args[3];
						theta = args[4];
						phi = args[5];
						break;}
					case 7:{ //Prevs & Ncycles
						vOffset = args[0];
						vAmp = args[1];
						freq = args[2];
						tDelay = args[3];
						theta = args[4];
						phi = args[5];
						nCycles = args[6];
						break;}
				}
				this->DCOffset = vOffset;
				this->waveform = [vOffset, vAmp, freq, tDelay, theta, phi, nCycles](double time){
					double effTime = tDelay - time;
					if(time < tDelay){
						return vOffset + vAmp * sin(phi);
					}
					else if(time > nCycles / (freq) + tDelay){
						effTime = -nCycles / (freq);
					}
					return vOffset + vAmp * exp(theta * effTime) * sin(2 * M_PI * freq * effTime + phi);
				};
				break;}
			case 3:{ //Exp
				double vInitial = 0, vPulse = 0, rDelay = 0, rTau = 1, fDelay = numeric_limits<double>::infinity(), fTau = numeric_limits<double>::infinity();
				switch(args.size()){
					case 1:{ //Vinitial (DC Offset)
						vInitial = args[0];
						break;}
					case 2:{ //Prevs & Vpulsed
						vInitial = args[0];
						vPulse = args[1];
						break;}
					case 3:{ //Prevs & Rise Delay
						vInitial = args[0];
						vPulse = args[1];
						rDelay = args[2];
						break;}
					case 4:{ //Prevs & Rise Tau
						vInitial = args[0];
						vPulse = args[1];
						rDelay = args[2];
						rTau = args[3];
						break;}
					case 5:{ //Prevs & Fall Delay
						vInitial = args[0];
						vPulse = args[1];
						rDelay = args[2];
						rTau = args[3];
						fDelay = args[4];
						break;}
					case 6:{ //Prevs & Fall Tau
						vInitial = args[0];
						vPulse = args[1];
						rDelay = args[2];
						rTau = args[3];
						fDelay = args[4];
						fTau = args[5];
						break;}
				}
				this->DCOffset = vInitial;
				this->waveform = [vInitial,vPulse,rDelay,rTau,fDelay,fTau](double time){
					double rtn = vInitial;
					if(time > rDelay){
						rtn += (vPulse - vInitial) * (1 - exp((rDelay - time) / rTau));
					}
					if(time>fDelay){
						rtn += (vInitial - vPulse) * (1 - exp((fDelay - time) / fTau));
					}
					return rtn;
				};
				break;
			}
			case 4:{ //Sffm
				double vOffset = 0, vAmp = 0, fCarrier = 0, mIndex = 1, fSignal = 0, tDelay = 0;
				switch(args.size()){
					case 1:{
						vOffset = args[0];
						break;}
					case 2:{
						vOffset = args[0];
						vAmp = args[1];
						break;}
					case 3:{
						vOffset = args[0];
						vAmp = args[1];
						fCarrier = args[2];
						break;}
					case 4:{
						vOffset = args[0];
						vAmp = args[1];
						fCarrier = args[2];
						mIndex = args[3];
						break;}
					case 5:{
						vOffset = args[0];
						vAmp = args[1];
						fCarrier = args[2];
						mIndex = args[3];
						fSignal = args[4];
						break;}
					case 6:{
						vOffset = args[0];
						vAmp = args[1];
						fCarrier = args[2];
						mIndex = args[3];
						fSignal = args[4];
						tDelay = args[5];
						break;}
				}
				this->DCOffset = vOffset;
				this->waveform = [vOffset, vAmp, fCarrier, mIndex, fSignal,tDelay](double time){
					if(time<tDelay){
						return vOffset;
					}
					const double effTime = time - tDelay;
					return vOffset + vAmp * (sin(2 * M_PI * fCarrier * effTime + mIndex * sin(2 * M_PI * fSignal * effTime)));
				};
				break;}
			case 5:{ //Pwl //TODO: implement trigger
				map<double,double> points;
				int end = 0;
				for(int i = 0; args[i]!=numeric_limits<double>::infinity(); i+=2){
					points[args[i]] = args[i+1];
					end = i;
				}
				args = vector<double>(args.begin() + end + 3, args.end());
				end = 0;
				bool repeat_ = args[end]; //True if the PWL repeats forever
				this->DCOffset = args[1];
				this->waveform = [points,repeat_](double time){
					double effTime = fmod(time,(*prev(points.end())).first);
					if(time < (*points.begin()).first || (repeat_ && effTime < (*points.begin()).first)){
						return (*points.begin()).second;
					}
					else if(time > (*prev(points.end())).first && !repeat_){
						return (*prev(points.end())).second;
					}
					pair<double,double> t1 = (*points.lower_bound(effTime));
					pair<double,double> t2 = (*prev(points.lower_bound(effTime)));
					return ((t2.second - t1.second) / (t2.first - t1.first)) * (effTime - t1.first) + t1.second;
				};
				break;}
			/* USE IN GETCOMS:
			case 6:{ //Pwl File
				ifstream file;
				file.open("input.pwl");
				if(!file){
					cerr<<"File does not exist"<<endl;
					exit(4);
				}
				const regex com("[,]");
				map<double,double> points;
				smatch m;
				string s = "";
				while(file>>s){
					regex_search(s,m,com);
					points[stod(m.prefix())] = stod(m.suffix());
				}
				file.close();
				remove("input.pwl");
				this->waveform = [points](double time){
					if(time < (*points.begin()).first){
						return (*points.begin()).second;
					}
					else if(time > (*prev(points.end())).first){
						return (*prev(points.end())).second;
					}
					pair<double,double> t1 = (*points.lower_bound(time));
					pair<double,double> t2 = (*prev(points.lower_bound(time)));
					return ((t2.second - t1.second) / (t2.first - t1.first)) * (time - t1.first) + t1.second;
				};
				break;}*/
			case 6:{ //AM
				double aSignal = 0, fCarrier = 0, fMod = 0, cOffset = 0, tDelay = 0;
				switch(args.size()){
					case 1:{
						aSignal = args[0];
						break;}
					case 2:{
						aSignal = args[0];
						fCarrier = args[1];
						break;}
					case 3:{
						aSignal = args[0];
						fCarrier = args[1];
						fMod = args[2];
						break;}
					case 4:{
						aSignal = args[0];
						fCarrier = args[1];
						fMod = args[2];
						cOffset = args[3];
						break;}
					case 5:{
						aSignal = args[0];
						fCarrier = args[1];
						fMod = args[2];
						cOffset = args[3];
						tDelay = args[4];
						break;}
				}
				this->DCOffset = 0;
				this->waveform = [aSignal, fCarrier, fMod, cOffset, tDelay](double time){
					if(time<tDelay){
						return double(0);
					}
					double effTime = time - tDelay;
					return aSignal * (cOffset + sin(2 * M_PI * fMod * effTime)) * sin(2 * M_PI * fCarrier * effTime);
				};
				break;}
		}
	}
};
struct DepSource:Source{
	function<double(Mat<double>,Mat<double>,double)> waveform;
	void srcFunc(int id, vector<double> args){
		switch(id){
			case 0:{ //Inductor
					double lValue = args[0], posNode = args[1], negNode = args[2];
					this->waveform = [posNode, negNode](Mat<double> mxPre1, Mat<double> mxPre2, double ts){
						const double vPre1 = mxPre1(posNode-1,0) - mxPre1(negNode-1,0); //Voltage across inductor at t-timestep
						const double vPre2 = mxPre2(posNode-1,0) - mxPre2(negNode-1,0); //Voltage across inductor at t-2·timestep
						return 2 * vPre1 - vPre2;
					};
				break;}
			case 1:{ //Capacitor
				double cValue = args[0], posNode = args[1], negNode = args[2];
				this->waveform = [cValue, posNode, negNode](Mat<double> mxPre1, Mat<double> mxPre2, double ts){
					const double vPre1 = mxPre1(posNode - 1,0) - mxPre1(negNode - 1,0); //Voltage across inductor at t-timestep
					const double vPre2 = mxPre2(posNode - 1,0) - mxPre2(negNode - 1,0); //Voltage across inductor at t-2·timestep
					cerr<<"V1:"<<mxPre1(posNode - 1,0)-mxPre1(negNode - 1,0)<<endl;
					cerr<<"V2:"<<mxPre2(posNode - 1,0) - mxPre2(negNode - 1,0)<<endl;
					cerr<<"TS:"<<ts<<endl;
					double dV = vPre1-vPre2;
					cerr<<"DV:"<<dV<<endl;
					double dVdT = dV/(ts);
					cerr<<cValue<<","<<dVdT<<endl;
					double rtn = cValue * dVdT;
					cerr<<"Return:"<<rtn<<endl;
					return rtn;
				};
				break;}
			case 2:{ //Voltage Trigger

				break;}
			case 3:{ //Current Trigger

				break;}
			case 4:{ //Voltage Dependant

				break;}
			case 5:{ //Current Dependant

			}
		}
	}
};
class Sim{
public:
	void getComs();
	vector<Source> sources; //Independent voltage & current sources ie DC 5v, SINE 5v amplitude / 3v dc offset etc.
	vector<Resistor> resistors; //Resistors ie R1 between nodes 2 & 3 with value 3.4kΩ
	vector<DepSource> dSources; //Dependent voltage & current sources (including capacitors and inductors) ie DC 3V if(V(node 1) > 1.5V), 1V otherwise
	vector<Node> nodes; //The wires between the components.
	double timeStep;
	double start;
	double end;
	int steps;
	void DC(){
		this->start = 0;
		this->end = 0;
		this->timeStep = 0;
		this->steps = 0;
	}
	void Tran(double start, double end, double timeStep){
	    this->start = start;
	    this->end = end;
	    this->timeStep = timeStep;
	    this->steps = ((start-end)/timeStep);
	}
	void Tran(double start, double end, int steps){
	    this->start = start;
		this->end = end;
		this->steps = steps;
		this->timeStep = (end-start)/steps;
	}
	Sim();
};
Sim::Sim(){
	this->sources = vector<Source>{};
	this->dSources = vector<DepSource>{};
	this->resistors = vector<Resistor>{};
	this->nodes = vector<Node>{Node(0)};
	int iCnt = 0;
	int vCnt = 0;
	int rCnt = 0;
	//TODO: Add dSource regex.
	const regex value("([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?)");//(([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?)?)
    const regex comment("([*].*)"); //* followed by anything, ie a comment (haha meta)
    const regex tranEx("([.]tran 0 [0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?s?( [0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?s?)*( [0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?s?)*)"); //A transient simulation command. Interestingly this has units unlike the others.
    const regex dc("(DC (([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?)?))"); //A DC source
    const regex sine("(SINE[ ]?[(]?( ?)(([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)? )?)(([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)? )?)(([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)? )?)(([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)? )?)(([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)? )?)(([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)? )?)(([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?)?)[)])"); //An AC source with SINE input
	const regex pulse("(PULSE[ ]?[(]?( ?)(([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)? )?)(([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)? )?)(([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)? )?)(([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)? )?)(([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)? )?)(([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)? )?)(([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)? )?)(([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?)?)[)])");
	const regex exp("(EXP[ ]?[(]?( ?)(([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)? )?)(([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)? )?)(([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)? )?)(([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)? )?)(([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)? )?)(([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?)?)[)])");
	const regex sffm("(SFFM[ ]?[(]?( ?)(([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)? )?)(([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)? )?)(([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)? )?)(([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)? )?)(([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)? )?)(([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?)?)[)])");
	const regex pwl("((PWL) ((VALUE[_]SCALE[_]FACTOR[=](([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?)?))?( )?(TIME[_]SCALE[_]FACTOR[=](([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?)?))( )?)?(((REPEAT FOR)(( [0-9]+)|(EVER)))? [(]((([+]?[0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?) ([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?))*)|(file[=](.*))[)] (ENDREPEAT)?( )?)+ (TRIGGER V([(]N[0-9][0-9][0-9])[)](([>])|([=])|([<]))(([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?)?))?)");
	const regex am("(AM[ ]?[(]?(([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)? )?)(([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)? )?)(([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)? )?)(([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)? )?)(([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?)?)[)])");
	const regex node("(( 0)|( N[0-9][0-9][0-9]))"); //A node: 0 or N followed by 3 digits.
    const regex vCom("((R|L|C)(([0-9]+)|([A-z]+))+)"); //A named resistor
    const regex src("(V|I)(([0-9]+)|([A-z]+))+"); //A named voltage source
    const regex vComEx("((R|L|C)(([0-9]+)|([A-z]+))+ ((N[0-9][0-9][0-9])|0) ((N[0-9][0-9][0-9])|0) [0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?)"); //A full line in the CIR file for any resistor
    const regex srcEx("((V|I)(([0-9]+)|([A-z]+))+ ((N[0-9][0-9][0-9])|0) ((N[0-9][0-9][0-9])|0) ((DC (([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?)?))|(SINE[ ]?[(]?( ?)(([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)? )?)(([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)? )?)(([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)? )?)(([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)? )?)(([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)? )?)(([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)? )?)(([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?)?)[)])|(PULSE[ ]?[(]?( ?)(([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)? )?)(([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)? )?)(([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)? )?)(([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)? )?)(([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)? )?)(([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)? )?)(([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)? )?)(([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?)?)[)])|(EXP[ ]?[(]?( ?)(([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)? )?)(([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)? )?)(([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)? )?)(([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)? )?)(([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)? )?)(([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?)?)[)])|(SFFM[ ]?[(]?( ?)(([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)? )?)(([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)? )?)(([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)? )?)(([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)? )?)(([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)? )?)(([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?)?)[)])|((PWL)  ((VALUE[_]SCALE[_]FACTOR[=](([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?)?))?( )?(TIME[_]SCALE[_]FACTOR[=](([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?)?))( )?)?(((REPEAT FOR)(( [0-9]+)|(EVER)))? [(]((([+]?[0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?) ([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?))*)|(file[=](.*))[)] (ENDREPEAT)?( )?)+ (TRIGGER V([(]N[0-9][0-9][0-9])[)](([>])|([=])|([<]))(([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?)?))?)|(AM[ ]?[(]?(([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)? )?)(([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)? )?)(([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)? )?)(([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)? )?)(([0-9]+([.][0-9]+)?(p|n|u|µ|m|k|(Meg)|G)?)?)[)])))"); //A full line in the CIR file for any type of voltage or current source, either AC or DC
    //Sim rtn; //This will be our sim. There are many like it but this one is ours. Our sim is our best friend. It is our life. We must master it as we master our lives. Without us, our sim is useless. Without our sim, we are useless. We must run our sim true. We must simulate faster than the programs who are trying to simulate us. We must simulate them before they simulate us. Our sim and us know what counts in simulation is not the circuits you simulate, the current sources we approximate, nor the resistors we model. We know that it is the voltages we calculate that count. Our sim is human, even as us, because it is our life. Thus, we will learn it as a brother. We will learn its weaknesses, its strengths, its functions, its objects, its variables and its bugs. We will keep our sim well commented and optimised. We will become part of each other. Before Dave Thomas, we swear this creed. Our sim and us are the simulators of SPICE circuits. We are the masters of current sources. We are the simulators of life. So it be, until the circuit has been simulated and there are no more current sources, but comma separated values.
	vector<string> lines; //The vector of strings read from cin. Used so that the user can input lines without having to wait for them to parse.
	while(cin){ //While data is being inputted
		string line=""; //Create a blank string to store the line in
		getline(cin, line); //Add the next line to the string
		string _l = line;
		smatch m;
		while(regex_search(_l,m,node)){
			int pnd = 0;
			if(m.str(0) == " 0"){
				pnd = 0;
			}
			else{
				pnd = stoi(m.str(0).substr(2));
			}
			if(pnd >= this->nodes.size()){
				for(int i = this->nodes.size(); i<=pnd;i++){
					this->nodes.push_back(Node(i));
				}
			}
			_l = m.suffix();
		}
		lines.push_back(line); //Add the line to the vector of lines.
	}
	for(string l:lines){
		if(regex_match(l,vComEx)){ //Resistors, capacitors and inductors
			Resistor v;
			smatch m;
			regex_search(l, m, vCom);
			v.uName = m.str(0); //2 of 6
			v.cName = v.uName[0]; //3 of 6
			string _l = m.suffix();
			regex_search(_l,m,node);
			int pnd = 0;
			if(m.str(0) == " 0"){
				pnd = 0;
			}
			else{
				pnd = stoi(m.str(0).substr(2));
			}
			v.pos = new Node(0);
			v.pos = &this->nodes[pnd];
			_l = m.suffix();
			regex_search(_l,m,node);
			int nnd = 0;
			if(m.str(0) == " 0"){
				nnd = 0;
			}
			else{
				nnd = stoi(m.str(0).substr(2));
			}
			v.neg = new Node(0);
			v.neg = &this->nodes[nnd]; //5 of 6
			v.val = getVal(string(m.suffix()).substr(1));
			if(v.cName == 'R'){ //Resistor
				v.id = rCnt; //1 of 6
				rCnt++;
				this->resistors.push_back(v);
			}
			else{ //Inductor or Capacitor
				DepSource dS;
				dS.cName = v.cName;
				dS.uName = v.uName;
				dS.DCOffset = 0;
				dS.pos = new Node(0);
				dS.neg = new Node(0);
				dS.pos = v.pos;
				dS.neg = v.neg;
				if(dS.cName == 'C'){
					dS.id = iCnt;
					iCnt++;
				}
				else{
					dS.id = vCnt;
					vCnt++;
				}
				vector<double> args{v.val,double(dS.pos->ID),double(dS.neg->ID)};
				dS.srcFunc(dS.cName=='C',args);
				this->dSources.push_back(dS);
			}
		}
		else if(regex_match(l,srcEx)){ //Sources
            Source aS;
            smatch m;
            regex_search(l,m,src);
            aS.uName = m.str(0);
            aS.cName = aS.uName[0];
            string _l = m.suffix();
            regex_search(_l,m,node);
			int pnd = 0;
			if(m.str(0) == " 0"){
				pnd = 0;
			}
			else{
				pnd = stoi(m.str(0).substr(2));
			}
			aS.pos = new Node(0);
			*aS.pos = (*this).nodes[pnd];
			int nnd = 0;
			_l = m.suffix();
			regex_search(_l,m,node);
			if(m.str(0) == " 0"){
				nnd = 0;
			}
			else{
				nnd = stoi(m.str(0).substr(2));
			}
			aS.neg = new Node(0);
			*aS.neg = (*this).nodes[nnd]; //5 of 6
			if(aS.cName != 'c'){
				aS.id = vCnt;
	            vCnt++;
			}
			_l = string(m.suffix()).substr(1);
            if(regex_search(_l,m,dc)){
				_l = string(m.str(0)).substr(3);
				vector<double> args{getVal(_l)};
				aS.srcFunc(0, args);
			}
			else if(regex_search(_l,m,pulse)){
				vector<double> args;
				while(regex_search(_l,m,value)){
					args.push_back(getVal(m.str(0)));
					_l = m.suffix();
				}
				aS.srcFunc(1, args);
			}
			else if(regex_search(_l,m,sine)){
				vector<double> args;
				while(regex_search(_l,m,value)){
					args.push_back(getVal(m.str(0)));
					_l = m.suffix();
				}
				aS.srcFunc(2, args);
			}
			else if(regex_search(_l,m,exp)){
				vector<double> args;
				while(regex_search(_l,m,value)){
					args.push_back(getVal(m.str(0)));
					_l = m.suffix();
				}
				aS.srcFunc(3, args);
			}
			else if(regex_search(_l,m,sffm)){
				vector<double> args;
				while(regex_search(_l,m,value)){
					args.push_back(getVal(m.str(0)));
					_l = m.suffix();
				}
				aS.srcFunc(4, args);
			}
			else if(regex_search(_l,m,pwl)){
				//TODO: This mess
				cerr<<"PWLs are scary yo, check back later"<<endl;
				exit(5);
			}
			else if(regex_search(_l,m,am)){
				vector<double> args;
				while(regex_search(_l,m,value)){
					args.push_back(getVal(m.str(0)));
					_l = m.suffix();
				}
				aS.srcFunc(6, args);
			}
			this->sources.push_back(aS);
		}
		else if(regex_match(l,tranEx)){ //Transients be like 0 [tstop] [tstart] [timestep]
			vector<double> params;
			string _l = l;
			smatch m;
			while(regex_search(_l,m,value)){
				params.push_back(getVal(m.str(0)));
				_l = m.suffix();
			}
			switch(params.size()){
				case 2:{ //Just stop time
					this->Tran(double(0), params[1], 1000);
					break;}
				case 3:{ //Start and stop time
					this->Tran(params[2], params[1], 1000);
					break;}
				case 4:{
					this->Tran(params[2], params[1], params[3]);
					break;}
			}
		}
		else if(l==".op"){
			this->DC(); //Set the number of steps to 0. This is how we will decide something is a bias point check.
		}
		else if(l==".end"){
			break; //break out of the for loop
		}
		else if(!regex_match(l,comment)){ //If the line isn't a command or a comment
			cerr<<"Exception in netlist: Invalid format '"<<l<<"' found. Exiting."<<endl;
			exit(1); //Exit with error code 1: Invalid format in netlist //TODO: make list of error codes and what they mean
		}
	}
}

#endif
