#include <iostream>
#include <armadillo>
#include <cassert>
#include <cmath>
#include "component.hpp"

using namespace std;
using namespace arma;

/*

	This function will construct the I and E matrices simultaneously (spelling? haha) by updating a single matrix in the form:

	|I1|				|Z0|
	|I2|				|Z1|
	|I3|				|Z2|
	 		   ===
	|E0|				|Z3|
	|E1|				|Z4|

	Where I[n] is the sum of the current sources into minus the sum of the current sources out of the node with ID 'n'
	And E[x] is the voltage supplied by the voltage source with ID 'x'

	We will do this by first iterating through each independent source, then each dependent source.
	If the source is a current source, we will add c to I[n] where c is the current supplied by the source
	If the source is a voltage source, we will set E[x] to v where v is the voltage supplied by the source

*/

mat getZ(mat mxPre1, mat mxPre2, Sim _, double time){
	mat rtn = mat(mxPre1.n_rows,1).fill(0); //An empty matrix to fill up & return
	const int nCnt = _.nodes.size() - 1; //The number of nodes excluding the reference node
	for(auto iS : _.sources){  //for each independent current or voltage source "iS" in the vector of sources
		double val; //The value of the source - it will be different if we are doing an ac simulation vs an operating point check
		if(time>=0){//if the time is 0 or greater, we are doing an ac simulation
			val = iS.waveform(time); //Get the value of the source at the current time (this allows sin, pulse, etc. to have time variant voltages)
		}							 //and set the variable declared earlier to it.
		else{ //if the time is less than 0 (ie -1), we are doing an operating point check
			val = iS.DCOffset;  //set the variable we declared earlier as equal to the DC offset of the source (or just the value if the source is DC)
		}
		if(iS.cName == 'I'){ //If the source "iS" is a current Source
			rtn(iS.pos->ID - 1,0) += val; //Add the value of "iS" to index (x,0) in the I Matrix, where 'x' is the ID of the node attached to "iS"'s positive side
			rtn(iS.neg->ID - 1,0) -= val; //Sub the value of "iS" from index (y,0) in the I Matrix, where 'y' is the ID of the node attached to "iS"'s negative side
		}
		else{ //If the source "iS" is a voltage source
			rtn(nCnt + iS.id,0) = val; //Set the index (z,0) in the E Matrix as equal to the value of the voltage source, where 'z' is the ID of the voltage source
		}
	}

/*

	The Z matrix will now be partially complete, having been updated for all the *independent* sources in the circuit.
	We now need to add the values of the dependent sources, such as inductors and capacitors.

*/

	for(auto dS : _.dSources){ //for each dependent current or voltage source "dS" in the vector of dependent sources
		double val;
		if(time>=0){
			val = dS.waveform(mxPre1,mxPre2,_.timeStep); //We need the 2 previous "mx"es here, where "mx" is the X matrix which contains the voltages at each node.
		}												 //We use that to calculate dV/dt for the capacitor and inductor transient equations.
		else{ //From here this loop is written the same as the independent sources one.
			val = dS.DCOffset;
		}
		if(dS.cName == 'I' || dS.cName == 'C'){
			rtn(dS.pos->ID - 1,0) += val;
			rtn(dS.neg->ID - 1,0) -= val;
		}
		else{
			rtn[nCnt + dS.id] = val;
		}
	}
	return rtn; //Return the now filled matrix to the main program
}
