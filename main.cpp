#include "getComs.hpp"
#include "Matrix.hpp"

using namespace std;
using namespace arma;


int main(){
	Sim _ = getComs();
	mat ma;	//TODO: Get Matrix A.
	mat maQ;
	mat maR;
	qr(maQ, maR, ma);
	maQ = maQ.t(); //TODO: Compare performance to other solving methods.
	mat mxPre1 = mat(ma.n_rows,1);
	mat mxPre2 = mat(mxPre1);
	mat mz;
	for(int i = 1; i<_.nodes.size(); i++){
		if(i>1){
			cout<<",";
		}
		cout<<"V(N"<<i<<")";
	}
	for(auto iS : _.sources){
		if(iS.cName == 'V'){
			cout<<",I("<<iS.uName<<")";
		}
	}
	for(auto dS : _.dSources){
		if(dS.cName != 'I' && dS.cName != 'C'){
			cout<<",I"<<dS.uName<<")";
		}
	}
	cout<<endl;
	for(double time = 0; time<_.end; time+=_.timeStep){
		mz = getZ(mxPre1, mxPre2, _, time);
		mat my = maQ * mz;
		mxPre2 = mxPre1;
		solve(mxPre1,maR,my);

	}
}
