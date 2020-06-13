#include "component.hpp"
#include "Matrix.hpp"
#include "ConstructA.hpp"

using namespace std;
using namespace arma;


int main(){
	Sim _; //Declare the sim. As part of the initialisation, the components & simulation command are read and parsed from std in.
	mat ma = GetA(_); //Initialise the A matrix to the value returned by the GetA function
	mat maQ = mat(ma.n_rows, ma.n_cols).fill(0);
	mat maR = mat(ma.n_rows, ma.n_cols).fill(0);
	qr(maQ, maR, ma); //Run a QR decomposition on matrix A and store the resultant 'Q' & 'R' matrices in the blank matrices declared above
	maQ = maQ.t(); //Transpose the Q part of A's decomposition. This is the same as the inverse of Q due to it being orthagonal.
	string header;
	if(_.start != _.end && _.end != 0){ //If the simulation is a transient simulation
		header += ",time"; //Add a header for the time column in the csv
	}
	for(int i = 1; i<_.nodes.size(); i++){
		header += (",V(N" + to_string(i) + ")"); //Add a voltage header for each node
	}
	for(auto iS : _.sources){
		if(iS.cName == 'V'){
			header += (",I(" + iS.uName + ")"); //Add a current header for each source
		}
	}
	for(auto dS : _.dSources){
		if(dS.cName != 'I'){
			header += (",I(" + dS.uName + ")"); //Add a current header for each dependent source (currently just inductors and capcitors)
		}
	}
	for(auto r : _.resistors){
		header += (",I(" + r.uName + ")"); //Add a current header for each resistor
	}
	cout<<header.substr(1)<<endl; //Print the header minus the leading comma to std out
	mat mxPre1 = mat(ma.n_rows,0).fill(0);
	mat mxPre2 = mat(ma.n_rows,0).fill(0);
	mat mz = getZ(mxPre1, mxPre2, _, -1); //Get matrix Z at time -1
	mat my = maQ * mz; //Set a matrix y to the Q part of A's decomposition * the z matrix we just got
	solve(mxPre1,maR,my); //Solve matrix x = R part of A * y matrix
	mxPre2 = mxPre1; //Set mxPre1 and mxPre2 to be equal. Pre1 and Pre2 stand for 1 timestep previous and 2 timesteps previous.
	_.nodes[0].voltage = 0;
	for(Node &nd : _.nodes){
		if(nd.ID > 0){
			nd.voltage = mxPre1(nd.ID - 1,0); //Update the node voltages to the contents of the x matrix
		}
	}
	if(_.start == _.end && _.end == 0){ //If the simulation is a dc simulation
		string s = "";
		for(double pt : mxPre1.col(0)){
			s += ",";
			s += to_string(pt); //Add each point in the x matrix to a string
		}
		for(auto r : _.resistors){
			s += ("," + to_string(r.findCur())); //add the current of each resistor to the string
		}
		for(auto iS : _.sources){
			if(iS.cName == 'I'){
				s += ("," + to_string(iS.waveform(0))); //add the current of each current source to the string
			}
		}
		for(auto dS : _.dSources){
			s += ("," + to_string(dS.waveform(mxPre1,mxPre2,0))); //add the current of each capacitor to the string
		}
		cout<<s.substr(1)<<endl; //Print the voltages and currents to std out
	}
	for(double time = _.start; time<_.end; time+=_.timeStep){ //for each timestep from the start to the end
		mz = getZ(mxPre1, mxPre2, _, time); //get the Z matrix at the current time
		my = maQ * mz; //multiply it by the Q part of A to get my
		mxPre2 = mxPre1; //update mxPre2
		solve(mxPre1,maR,my); //Solve mxPre1 = the R part of A * my
		_.nodes[0].voltage = 0; //set the ground node's voltage to 0. This shouldn't be necessary but it didn't work without it.
		for(Node &nd : _.nodes){
			if(nd.ID > 0){
				nd.voltage = mxPre1(nd.ID - 1,0); //update the voltage of each non-reference node.
			}
		}
		if(time > _.start){
			string s = to_string(time);
			for(double pt : mxPre1.col(0)){
				s += ",";
				s += to_string(pt); //add each point in the x matrix to a string
			}
			for(auto r : _.resistors){
				s += ("," + to_string(r.findCur())); //add the current of each resistor to the string
			}
			for(auto iS : _.sources){
				if(iS.cName == 'I'){
					s += ("," + to_string(iS.waveform(time))); //add the current of each current source to the string
				}
			}
			for(auto dS : _.dSources){
				s += ("," + to_string(dS.waveform(mxPre1,mxPre2,time))); //add the current of each capacitor to the string
			}
			s+='\n'; //Add a newline to the string.
			cout<<s; //Print the voltages and currents to std out
		}
	}
}
