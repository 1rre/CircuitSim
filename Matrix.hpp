
#include <iostream>
#include <armadillo>
#include <cassert>
#include <cmath>
#include "component.hpp"

using namespace std;
using namespace arma;

//Mat<double> GetNext_A(Mat<double A, Mat<double> X, Mat<double> Y, vector<LinComponent> source, vector<LinComponent> resistor, double time,)

double GetVoltage(double &F)
{
  double F1=F*0.5;
  return F1;
}
double GetCurrent(double &F)
{
  double F1=F+0.5;
  return F1;
}


Mat<double> SolveX(Mat<double> A, Mat<double> Z)
{
  Mat<double> A_in = inv(A);//inverting the A matrix
//  cout<<A_in<<endl;
  Mat<double> X_n = A_in*Z; //multiplying the inverse by Z to get X
  return X_n;//return the x matrix
}

Mat<double> GetNext(Mat<double> &A, Mat<double> &Z,double siz, double Vn, vector<double> vlt)
{
  for (size_t i = siz-Vn-1; i < siz; i++) {
    if (vlt[i]==1) { // if the component is a voltage source that changes (capacitor)
      double F=1;
      Z(i,0)*=GetVoltage(F);
      cout<<Z(i,0)<<endl;
    }

    if (vlt[i]==2) { // if the component is a current source that changes (inductor)
      double F=1;
      Z(i,0)*=GetCurrent(F);
      cout<<Z(i,0)<<endl;
    }
  }
return Z;
}
