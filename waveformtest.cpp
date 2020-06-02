#include <iostream>
#include "getComs.hpp"
#include <fstream>

using namespace std;

/* 	[-]: All done, [x]: Testing in progress, [o]: Ready for testing, [+]: Writing in progress, [ ]: Not started

	0: DC			|[-] Offset																														|[-] regex
	1: Pulse		|[-] vInitial	|[-] vOn		|[-] tDelay		|[x] tRise		|[x] tFall		|[x] tOn		|[x] Period		|[o] nCycles	|[o] regex
	2: Sine 		|[-] vOffset	|[-] vAmp		|[-] freq		|[-] tDelay		|[-] theta		|[o] phi		|[o] nCycles					|[o] regex
	3: Exp 			|[-] vInitial	|[-] vPulse		|[-] rDelay		|[-] rTau		|[-] fDelay		|[-] fTau										|[o] regex
	4: SFFM 		|[-] vOffset	|[-] vAmp		|[-] fCarrier	|[-] mIndex		|[-] fSignal	|[-] tDelay										|[o] regex
	5: PWL 			|[-] t			|[-] v																											|[o] regex
	a:PWL Trigger	|[ ] «Trigger»																													|[o] regex
	b:PWL File 		|[ ] «File»																														|[o] regex
	c:PWL Repeatn	|[ ] «Number»																													|[o] regex
	d:PWL Repeat*	|[ ] «Repeat»																													|[o] regex
	e:PWL TSF		|[ ] «Time»																														|[o] regex
	f:PWL VSF		|[ ] «Value»																													|[o] regex
	6: AM 			|[-] aSignal	|[-] fCarrier	|[-] fMod		|[-] cOffset	|[-] tDelay														|[o] regex

*/


int main(){
	vector<double> args{3,30,0.5,1.6,numeric_limits<double>::infinity(),3,5,9,1};
	Source _1;
	_1.srcFunc(false,5,args);
	double stop = 7;
	for(double i = 0; i<stop; i+=stop/10000){
		cout<<i<<","<<_1.waveform(i)<<endl;
	}
}
