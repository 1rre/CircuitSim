#include <iostream>
#include <armadillo>
#include <cassert>
#include <cmath>
#include "component.hpp"

using namespace std;
using namespace arma;


Mat<double> MatrixG(Sim s) // vector of resistors
{
  double N = s.nodes.size();
	Mat<double> Gmatrix(N-1,N-1, fill::zeros);


	for (size_t i = 0; i < s.resistors.size(); i++) {
		double pos = s.resistors[i].pos->ID-1; // left node
	    double neg = s.resistors[i].neg->ID-1; // right node
	    if (pos>=0 && neg>=0) { // if there is no connection to the reference node
			Gmatrix(pos,neg) += -1/s.resistors[i].val;
			Gmatrix(neg,pos) += -1/s.resistors[i].val;
			Gmatrix(neg,neg) += 1/s.resistors[i].val;
			Gmatrix(pos,pos) += 1/s.resistors[i].val;
		}
		if(pos==-1){
			Gmatrix(neg,neg) += (1/s.resistors[i].val);
		}
		else if (neg==-1){
			Gmatrix(pos,pos) += (1/s.resistors[i].val);
		}
	}
	return Gmatrix;
}

Mat<double> MatrixB(Sim s){ //Calculates Matrix B and stores the result in matrixB
	double M = 0;
	for (size_t i = 0; i < s.sources.size(); i++) {
		if ((s.sources[i].cName=='V') || (s.sources[i].cName=='L')) {
			M+=1;
		}
	}
  double N = s.nodes.size()-1;
	Mat<double> MatrixB(N,M,fill::zeros);
	for(int i = 0; i < s.sources.size(); i++){
    if ((s.sources[i].cName=='V') || (s.sources[i].cName=='L')) {
    	double pos = s.sources[i].pos->ID-1;
			double neg = s.sources[i].neg->ID-1;
			cout<<"neg: "<<neg<<endl;
			cout<<"pos: "<<pos<<endl;
			if(pos>=0 && neg>=0)
			{
				MatrixB(i,pos)=1;
				MatrixB(i,neg)=-1;
			}
			if (pos== -1)
			{
				MatrixB(i,neg)=-1;
			}
			if (neg== -1)
			{
				MatrixB(i,pos)=1;
			}
    }

	}
	return MatrixB;
}

Mat<double> MatrixC(Sim s)
{
	Mat<double> C = trans(MatrixB(s));
	return C;
}

Mat<double> MatrixD(Sim s)
{
	double M = 1;
	for (size_t i = 0; i < s.sources.size(); i++)
		{
		if ((s.sources[i].cName=='V') || (s.sources[i].cName=='L')) {
			M+=1;
		}
	Mat<double> D(M,M,fill::zeros);
	return D;
}
}

Mat<double> GetA(Sim s)
{
double N = s.nodes.size()-1;
double M = 0;
for (size_t i = 0; i < s.sources.size(); i++) {
	if ((s.sources[i].cName=='V') || (s.sources[i].cName=='L')) {
		M+=1;
	}
}
Mat<double> A((N+M),(N+M),fill::zeros);
//cout<<A<<endl;
Mat<double> G = MatrixG(s);
//cout<<G<<endl;
Mat<double> B = MatrixB(s);
//cout<<B<<endl;
Mat<double> C = MatrixC(s);
//cout<<C<<endl;
Mat<double> D = MatrixD(s);
//cout<<D<<endl;
//cout<<"N"<<N<<endl;
//cout<<"M"<<M<<endl;
A(span(0,N-1),span(0,N-1)) = G;
//cout<<"G"<<endl;
//cout<<A<<endl;
A(span(0,N-1),span(N,N+M-1)) = B;
//cout<<"B"<<endl;
A(span(N,N+M-1),span(0,N-1)) = C;
//cout<<"C"<<endl;
A(span(N,N+M-1),span(N,N+M-1)) = D;
//cout<<"D"<<endl;
//cout<<A<<endl;
return A;
}
