#include <iostream>
#include <armadillo>
#include <cassert>
#include <cmath>
#include "component.hpp"

using namespace std;
using namespace arma;

mat getZ(mat mxPre1, mat mxPre2, Sim _, double time){ //The function we call when we need a z matrix. The parameters are the sim, current time and the 2 most recent x matrices.
	mat rtn = mat(mxPre1.n_rows,1).fill(0);
	const int nCnt = _.nodes.size() - 1; //A handy variable telling us the number of nodes
	for(auto iS : _.sources){  //for each independent source iS
		double val;
		if(time>=0){
			val = iS.waveform(time); //if the time is 0 or greater, we are doing an ac simulation, so get the waveform at the current time.
		}
		else{
			val = iS.DCOffset; //if the time is less than 0 (ie -1), we are doing a dc simulation, so return the dc offset.
		}
		if(iS.cName == 'I'){ //Current Source
			//update the z matrix such that the value of the current source is subtracted from the current into the node at negative pin and added to the current into the node at the positive pin
			rtn(iS.pos->ID - 1,0) += val;
			rtn(iS.neg->ID - 1,0) -= val;
		}
		else{ //Voltage Source
			rtn(nCnt + iS.id,0) = val; //Update the z matrix such that the index representing the voltage source is set appropriately
		}
	}
	for(auto dS : _.dSources){ //for each dependent source
		double val;
		if(time>=0){ //ac simulation
			val = dS.waveform(mxPre1,mxPre2,_.timeStep); //calculate the waveform from the previous 2 matrices and the timestep. the current time isn't needed as we're calculating dv/dt
		}
		else{ //dc simulation
			val = dS.DCOffset; //Set the value to the dc offset
		}
		if(dS.cName == 'I' || dS.cName == 'C'){ //Current Source7
			//update the z matrix such that the value of the current source is subtracted from the current into the node at negative pin and added to the current into the node at the positive pin
			rtn(dS.pos->ID - 1,0) += val;
			rtn(dS.neg->ID - 1,0) -= val;
		}
		else{ //Voltage Source
			rtn[nCnt + dS.id] = val; //Update the z matrix such that the index representing the voltage source is set appropriately
		}
	}
	return rtn;
}
