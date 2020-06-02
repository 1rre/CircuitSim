#include <iostream>
#include "getComs.hpp"
#include <fstream>

using namespace std;

/* 	[-]: All done, [x]: Testing in progress, [o]: Ready for testing, [~]: Writing in progress, [ ]: Not started

	0: DC			|[-] Offset
	1: Pulse		|[-] vInitial	|[-] vOn		|[-] tDelay		|[x] tRise		|[x] tFall		|[x] tOn		|[x] Period		|[o] nCycles
	2: Sine 		|[-] vOffset	|[-] vAmp		|[-] freq		|[-] tDelay		|[-] theta		|[o] phi		|[o] nCycles
	3: Exp 			|[-] vInitial	|[-] vPulse		|[-] rDelay		|[-] rTau		|[-] fDelay		|[-] fTau
	4: SFFM 		|[~] vOffset	|[~] vAmp		|[~] fCarrier	|[~] mIndex		|[~] fSignal	|[~] tDelay
	5: PWL 			|[-] t			|[-] v
	6: PWL File 	|[-] «File»
	7: AM 			|[~] aSignal	|[~] fCarrier	|[~] fMod		|[~] cOffset	|[~] tDelay

*/


int main(){
//	vector<double> args{99, 59};
	vector<double> args;
	for(char c:"points.csv"){
		args.push_back(c);
	}
	Source _1 = Source(false,6,args);
	double stop = 20;
	for(double i = 0; i<stop; i+=stop/10000){
		cout<<i<<","<<_1.waveform(i)<<endl;
	}
}
