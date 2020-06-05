#include <iostream>
#include <armadillo>
#include <cassert>
#include <cmath>
#include "component.hpp"

using namespace std;
using namespace arma;

mat getZ(mat mxPre1, mat mxPre2, Sim _, double time){
	mat rtn = mat(mxPre1.n_rows,1).fill(0);
	int nCnt = _.nodes.size() - 1;
	for(auto iS : _.sources){
		double val;
		if(time>=0){
			val = iS.waveform(time);
		}
		else{
			val = iS.DCOffset;
		}
		if(iS.cName == 'I'){ //Current Source
			rtn(iS.pos->ID - 1,0) += val;
			rtn(iS.neg->ID - 1,0) -= val;
		}
		else{ //Voltage Source
			rtn(nCnt + iS.id,0) = val;
		}
	}
	for(auto dS : _.dSources){
		double val;
		if(time>=0){
			val = dS.waveform(mxPre1,mxPre2,_.timeStep);
		}
		else{
			val = dS.DCOffset;
		}
		if(dS.cName == 'I' || dS.cName == 'C'){ //Current Source
			rtn(dS.pos->ID - 1,0) += val;
			rtn(dS.neg->ID - 1,0) -= val;
		}
		else{ //Voltage Source
			rtn[nCnt + dS.id] = val;
		}
	}
	return rtn;
}
