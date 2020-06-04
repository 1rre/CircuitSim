#include <iostream>
#include <armadillo>
#include <cassert>
#include <cmath>
#include "component.hpp"

using namespace std;
using namespace arma;

mat getZ(mat mxPre1, mat mxPre2, Sim _, double time){
	mat rtn = mat(mxPre1.n_rows,1);
	int nCnt = _.nodes.size();
	for(auto iS : _.sources){
		double val;
		if(time>=0){
			 val = iS.waveform(time);
		}
		else{
			val = iS.DCOffset;
		}
		if(iS.cName == 'I'){ //Current Source
			rtn[iS.pos->ID] += val;
			rtn[iS.neg->ID] -= val;
		}
		else{ //Voltage Source
			rtn[nCnt + iS.id] = val;
		}
	}
	for(auto dS : _.dSources){
		double val;
		if(time>=0){
			 val = dS.waveform(time - _.timeStep, mxPre1, time - 2 * _.timeStep, mxPre2);
		}
		else{
			val = dS.DCOffset;
		}
		if(dS.cName == 'I' || dS.cName == 'C'){ //Current Source
			rtn[dS.pos->ID] += val;
			rtn[dS.neg->ID] -= val;
		}
		else{ //Voltage Source
			rtn[nCnt + dS.id] = val;
		}
	}
	return rtn;
}
