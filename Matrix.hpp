
#include <iostream>
#include <armadillo>
#include <cassert>
#include <cmath>
#include "component.hpp"

using namespace std;
using namespace arma;

//Mat<double> GetNext_A(Mat<double A, Mat<double> X, Mat<double> Y, vector<LinComponent> source, vector<LinComponent> resistor, double time,)

Mat<double> SolveX(Mat<double> A, Mat<double> Z)
{
  Mat<double> A_in = inv(A);//inverting the A matrix
  cout<<A_in<<endl;
  Mat<double> X_n = A_in*Z; //multiplying the inverse by Z to get X
  return X_n;//return the x matrix
}

Mat<double>GetNext
