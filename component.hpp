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


using namespace std;

class Node{ //A node. As the nodes are numbered 0 or from N001 to N999 we can give them a unique integer ID directly from the CIR file
public:
	int ID; //Used as the key for the right and left component maps
	double voltage;
	Node(int id);
	Node();
};
Node::Node(int id){ //Constructor for a node where there is a nonzero voltage, ie not the reference node
	this->ID = id;
}
Node::Node(){ //Constructorfor either an empty node or the reference node
	this->ID=0;
}

struct Component{
    char cName; //The component name ie "Resistor", "Capacitor" etc.
    string uName; //The name of the component as in the CIR file ie "R1", "Vin" etc.
    int id; //The unique (between components of the same type) identifier for the component.
    Node* pos; //The node to the "right" of this component. This is the cathode/positive pin of polar components.
    Node* neg; //The node to the "left" of this component. This is the anode/negative pin of polar components.
};
struct vComponent:Component{ //A linear component such as a resistor, capacitor, inductor or non-dependant source
	double val; //the value of the component in SI units. In sources this is the DC offset.
};
struct Source:Component{ //Only voltage sources here, I heard that current kills
	bool cSource;
	double DCOffset;
	function<double(double)> waveform; //use 'waveform(time);' to run function
	Source(bool b, int id, vector<double> args);
};
Source::Source(bool b, int id, vector<double> args){
	this->cSource = b;
	switch(id){
		case 0: //DC
			this->DCOffset = args[0];
			this->waveform = [args](double time){ return args[0]; };
		break;
		case 1:{ //Pulse
			double vInitial = 0, vOn = 0, tDelay = 0, tRise = 0, tFall = 0, tOn = 0, tPeriod = 0, nCycles = numeric_limits<double>::infinity();
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
					tPeriod = tRise;
					break;}
				case 5:{ //Prevs & Tfall
					vInitial = args[0];
					vOn = args[1];
					tDelay = args[2];
					tRise = args[3];
					tFall = args[4];
					tPeriod = tRise + tFall;
					break;}
				case 6:{ //Prevs & Ton
					vInitial = args[0];
					vOn = args[1];
					tDelay = args[2];
					tRise = args[3];
					tFall = args[4];
					tOn = args[5];
					tPeriod = tRise + tFall + tOn;
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
			this->waveform = [vOffset, vAmp, freq, tDelay, theta, phi, nCycles](double time){
				if(time < tDelay){
					return vOffset + vAmp * sin(phi / (2 * M_PI));
				}
				else if(time > nCycles / freq + tDelay){
					return vOffset + vAmp * exp(theta * (nCycles/freq)) * sin(2 * M_PI * nCycles + phi / (2 * M_PI));
				}
				const double effTime = tDelay - time;
				return vOffset + vAmp * exp(theta * effTime) * sin(2 * M_PI * freq * effTime + phi / (2 * M_PI));
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
			this->waveform = [vInitial,vPulse,rDelay,rTau,fDelay,fTau](double time){
				double rtn = vInitial;
				if(time > rDelay){
					rtn += (vPulse - vInitial) * (1 - exp((rDelay - time) / rTau));
					if(time > rDelay + fDelay){
						rtn += (vInitial - vPulse) * (1 - exp((rDelay + fDelay - time) / fTau));
					}
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
			this->waveform = [vOffset, vAmp, fCarrier, mIndex, fSignal,tDelay](double time){
				if(time<tDelay){
					return vOffset;
				}
				const double effTime = time - tDelay;
				return vOffset + vAmp * (sin(2 * M_PI * fCarrier * effTime + mIndex * sin(2 * M_PI * fSignal * effTime)));
			};
			break;}
		case 5:{ //Pwl
			map<double,double> points;
			for(int i = 0; i<args.size() - 1; i+=2){
				points[args[i]] = args[i+1];
			}
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
			this->waveform = [aSignal, fCarrier, fMod, cOffset, tDelay](double time){


				return time;
			};
			break;}
	}
}

class Sim{ //Currently unused struct for toring the type of simulations. Potentially worth merging with SimParams. Structs DC and Tran inherit from this.
public:
	vector<Source> sources;
	vector<vComponent> resistors;
	vector<vComponent> reactComs;
	vector<Node> nodes;
	double timeStep;
	double start;
	double end;
	int steps;
	void DC(){
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
};

#endif
