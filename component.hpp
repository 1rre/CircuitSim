#ifndef component_hpp
#define component_hpp

#include <string>
#include <functional>
#include <vector>

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
    string cName; //The component name ie "Resistor", "Capacitor" etc.
    string uName; //The name of the component as in the CIR file ie "R1", "Vin" etc.
    int id; //The unique (between components of the same type) identifier for the component.
    Node* pos; //The node to the "right" of this component. This is the cathode/positive pin of polar components.
    Node* neg; //The node to the "left" of this component. This is the anode/negative pin of polar components.
};
struct vComponent:Component{ //A linear component such as a resistor, capacitor, inductor or non-dependant source
	double val; //the value of the component in SI units. In sources this is the DC offset.
};
struct Source:Component{ //Only voltage sources here, I heard that current kills
	bool vORc;
	double DCOffset;
	function<double(double)> waveform; //use 'waveform(time);' to run function
	Source(function<double(double)> f, double offset, bool b);
	Source(double offset, bool b);
};
Source::Source(function<double(double)> f, double offset, bool b){
	this->DCOffset = offset;
	this->waveform = f;
	this->vORc = b;
}
Source::Source(double offset, bool b){
	this->DCOffset = offset;
	this->waveform = [offset](double d) {return offset;};
	this->vORc = b;
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
