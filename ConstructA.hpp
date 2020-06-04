#include <iostream>
#include <armadillo>
#include <cassert>
#include <cmath>
#include "component.hpp"

using namespace std;
using namespace arma;

Mat<double> GetA()
{
;
}
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
  double N = s.nodes.size();
	Mat<double> MatrixB(N,M,fill::zeros);
	for(int i = 0; i < s.sources.size(); i++){
    if ((s.sources[i].cName=='V') || (s.sources[i].cName=='L')) {
    	double pos = -1;//s.sources[i].pos->ID-1;
			double neg = 0;//s.sources[i].neg->ID-1;
			//cout<<"neg: "<<neg<<endl;
			//cout<<"pos: "<<pos<<endl;
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
	double M = 0;
	for (size_t i = 0; i < s.sources.size(); i++)
		{
		if ((s.sources[i].cName=='V') || (s.sources[i].cName=='L')) {
			M+=1;
		}
	Mat<double> D(M,M,fill::zeros);
	return D;
}
}
