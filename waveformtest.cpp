#include "getComs.hpp"

using namespace std;

/* 	[-]: All done, [x]: Testing in progress, [o]: Ready for testing, [+]: Writing in progress, [ ]: Not started

	Independent:
	0: DC			|[-] Offset																														|[-] regex
	1: Pulse		|[-] vInitial	|[-] vOn		|[-] tDelay		|[-] tRise		|[-] tFall		|[-] tOn		|[-] Period		|[-] nCycles	|[-] regex
	2: Sine 		|[-] vOffset	|[-] vAmp		|[-] freq		|[-] tDelay		|[-] theta		|[-] phi		|[-] nCycles					|[-] regex
	3: Exp 			|[-] vInitial	|[-] vPulse		|[-] rDelay		|[-] rTau		|[-] fDelay		|[-] fTau										|[-] regex
	4: SFFM 		|[-] vOffset	|[-] vAmp		|[-] fCarrier	|[-] mIndex		|[-] fSignal	|[-] tDelay										|[-] regex
	5: PWL 			|[-] t			|[-] v																											|[o] regex
	b: PWL File 	|[ ] «File»																														|[ ] regex
	c: PWL Repeatn	|[ ] «Number»																													|[ ] regex
	d: PWL Repeat*	|[ ] «Repeat»																													|[ ] regex
	e: PWL TSF		|[ ] «Time»																														|[ ] regex
	f: PWL VSF		|[ ] «Value»																													|[ ] regex
	6: AM 			|[-] aSignal	|[-] fCarrier	|[-] fMod		|[-] cOffset	|[-] tDelay														|[-] regex

	Dependent:
	0: Capacitor	|[ ] cValue		|[ ] posNode	|[ ] negNode	|[ ] vPre1		|[ ] vPre2
	1: Inductor		|[ ] lValue		|[ ] posNode	|[ ] negNode	|[ ] vPre1		|[ ] vPre2
	2: vTrigger		|[ ] tNode		|[ ] tPoint		|[ ] PWLArgs	|[ ] vLast
	3: iTrigger		|[ ] tSrc		|[ ] tPoint		|[ ] PWLArgs	|[ ] iLast
	4: vDep			|[ ] dNode		|[ ] multiplier	|[ ] vLast
	5: iDep			|[ ] dSrc		|[ ] multiplier	|[ ] iLast
*/


int main(){
	int x = false;
	cout<<x<<endl;
/*	cout<<"a,b"<<endl;
	Sim s = getComs();
	double start = 0;
	double finish = 5e-3;
	for(auto x : s.sources){
		for(double i = 0; i<finish; i+=(finish-start)/1e4){
			if(i>=start){
				cout<<i<<","<<x.waveform(i)<<endl;
			}
		}
	}*/
}
