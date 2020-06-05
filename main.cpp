#include "component.hpp"
#include "Matrix.hpp"
#include "ConstructA.hpp"

using namespace std;
using namespace arma;


int main(){
	Sim _;
	mat ma = GetA(_);
	mat maQ = mat(ma.n_rows, ma.n_cols).fill(0);
	mat maR = mat(ma.n_rows, ma.n_cols).fill(0);
	qr(maQ, maR, ma);
	maQ = maQ.t(); //TODO: Compare performance to other solving methods.
	string header;
	if(_.start != _.end && _.end != 0){
		header += ",time";
	}
	for(int i = 1; i<_.nodes.size(); i++){
		header += (",V(N" + to_string(i) + ")");
	}
	for(auto iS : _.sources){
		if(iS.cName == 'V'){
			header += (",I(" + iS.uName + ")");
		}
	}
	for(auto dS : _.dSources){
		if(dS.cName != 'I' && dS.cName != 'C'){
			header += (",I(" + dS.uName + ")");
		}
	}
	for(auto r : _.resistors){
		header += (",I(" + r.uName + ")");
	}
	cout<<header.substr(1)<<endl;
	mat mxPre1 = mat(ma.n_rows,0).fill(0);
	mat mxPre2 = mat(ma.n_rows,0).fill(0);
	mat mz = getZ(mxPre1, mxPre2, _, -1); //Run DC
	mat my = maQ * mz;
	solve(mxPre1,maR,my);
	mxPre2 = mxPre1;
	for(int nd = 0; nd<_.nodes.size();nd++){
		_.nodes[nd].voltage = mxPre1(nd,0);
	}
	_.nodes[0].voltage = 0;
	for(Node &nd : _.nodes){
		if(nd.ID > 0){
			nd.voltage = mxPre1(nd.ID - 1,0);
		}
	}
	if(_.start == _.end && _.end == 0){
		string s = "";
		for(double pt : mxPre1.col(0)){
			s += ",";
			s += to_string(pt);
		}
		for(auto r : _.resistors){
			s += ("," + to_string(r.findCur()));
		}
		s+='\n';
		cout<<s.substr(1);
	}
	for(double time = 0; time<_.end; time+=_.timeStep){
		mz = getZ(mxPre1, mxPre2, _, time);
		my = maQ * mz;
		mxPre2 = mxPre1;
		solve(mxPre1,maR,my);
		_.nodes[0].voltage = 0;
		for(Node &nd : _.nodes){
			if(nd.ID > 0){
				nd.voltage = mxPre1(nd.ID - 1,0);
			}
		}
		if(time > _.start){
			string s = to_string(time);
			for(double pt : mxPre1.col(0)){
				s += ",";
				s += to_string(pt);
			}
			for(auto r : _.resistors){
				s += ("," + to_string(r.findCur()));
			}
			s+='\n';
			cout<<s;
		}
	}
}
