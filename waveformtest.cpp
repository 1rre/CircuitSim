#include <iostream>
#include "getComs.hpp"
#include <fstream>

using namespace std;

/* 	[-]: All done, [x]: Testing in progress, [o]: Ready for testing, [+]: Writing in progress, [ ]: Not started

	0: DC			|[-] Offset																														|[-] regex
	1: Pulse		|[-] vInitial	|[-] vOn		|[-] tDelay		|[-] tRise		|[-] tFall		|[-] tOn		|[-] Period		|[-] nCycles	|[-] regex
	2: Sine 		|[-] vOffset	|[-] vAmp		|[-] freq		|[-] tDelay		|[-] theta		|[-] phi		|[-] nCycles					|[-] regex
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
	cout<<"a,b"<<endl;
	Sim s = getComs();
	double start = 0;
	double finish = 5e-3;
	for(auto x : s.sources){
		for(double i = 0; i<finish; i+=(finish-start)/1e3){
			if(i>=start){
				cout<<i<<","<<x.waveform(i)<<endl;
			}
		}
	}
}

/*
Problems:
V1 N001 0 SINE (1 5 1k 1m 3k 1.57 2) - jumps to ~46 after ~2.9ms
Offset = 1
Amplitude = 5
Freq = 1k
Time delay = 1m
Damping Factor = 3k
Phase difference = pi/2
NCycles = 2;
*/
