#include <iostream>
#include "getComs.hpp"

using namespace std;

/* 	[-]: All done, [x]: Testing in progress, [o]: Ready for testing, [~]: Writing in progress, [ ]: Not started

	0: DC			|[-] Offset
	1: Pulse		|[-] vInitial	|[-] vOn		|[-] tDelay		|[x] tRise		|[x] tFall		|[x] tOn		|[x] Period		|[o] nCycles
	2: Sine 		|[-] vOffset	|[-] vAmp		|[-] freq		|[-] tDelay		|[-] theta		|[o] phi		|[o] nCycles
	3: Exp 			|[ ] vInitial	|[ ] vPulse		|[ ] rDelay		|[ ] rTau		|[ ] fDelay		|[ ] fTau
	4: LFFM 		|[ ] dcOffset	|[ ] vAmp		|[ ] fCarrier	|[ ] mIndex		|[ ] fSignal	|[ ] tDelay
	5: PWL 			|[ ] t			|[ ] v			|[ ] tDelay
	6: PWL File 	|[ ] «File» 	|[ ] tDelay
	7: AM 			|[ ] aSignal	|[ ] fCarrier	|[ ] fMod		|[ ] cOffset	|[ ] tDelay

*/


int main(){
	vector<double> args{1,3,1e4, 1e-3, 1e3};
	Source _1 = Source(false,2,args);
	for(double i = 0; i<10e-3; i+=1e-6){
		cout<<i<<","<<_1.waveform(i)<<endl;
	}
}
