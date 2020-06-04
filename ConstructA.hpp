#include <iostream>
#include <armadillo>
#include <cassert>
#include <cmath>
#include "component.hpp"

using namespace std;
using namespace arma;


Mat<double> MatrixG(Sim s) // this function will return the G matrix, which shows the positions and vlues of all the resistors.
{
  double N = s.nodes.size(); //number of nodes
  Mat<double> Gmatrix(N-1,N-1, fill::zeros); //constructing a matrix of the correct size for G


  for (size_t i = 0; i < s.resistors.size(); i++) { // for loop that cycles through each resistor
    double pos = s.resistors[i].pos->ID-1; // positive node. (-1) so that the matrix fills from 0
    double neg = s.resistors[i].neg->ID-1; // negative node. the reference node becomes -1
    if (pos>=0 && neg>=0) { // if the resistor is not connected to the reference node (node -1)
      Gmatrix(pos,neg) += -1/s.resistors[i].val;
      Gmatrix(neg,pos) += -1/s.resistors[i].val;
      Gmatrix(neg,neg) += 1/s.resistors[i].val;
      Gmatrix(pos,pos) += 1/s.resistors[i].val; // add or subtract the conductance of the resistor to the relevant spaces in the G matrix
    }
    if(pos==-1){//if the positive node is the reference node
      Gmatrix(neg,neg) += (1/s.resistors[i].val);
    }
    else if (neg==-1){//if the negative node is the reference node.
      Gmatrix(pos,pos) += (1/s.resistors[i].val);
    }
  }
  return Gmatrix; //return the completed G matrix
}

Mat<double> MatrixB(Sim s){ //Calculates Matrix B and stores the result in MatrixB
  double M = 0; //number of voltage sources
  for (size_t i = 0; i < s.sources.size(); i++) { // finds the number of voltage sources and inductors in the netlist
    if ((s.sources[i].cName=='V') || (s.sources[i].cName=='L')) {
      M+=1;
    }
  }
  double N = s.nodes.size()-1; // number of nodes
  Mat<double> MatrixB(N,M,fill::zeros); // creating a matrix of the correct size initialised with zeros
  for(int i = 0; i < s.sources.size(); i++){ //cycling through the sources vector
    if ((s.sources[i].cName=='V') || (s.sources[i].cName=='L')) { // choosing the voltage sources and inductors
      double pos = s.sources[i].pos->ID-1;//finding the positive node
      double neg = s.sources[i].neg->ID-1;//finding the negative node
      cout<<s.sources[i].uName<<endl;
      cout<<"neg: "<<neg<<endl;
      cout<<"pos: "<<pos<<endl;
      if(pos>=0 && neg>=0) //if the source isnt connected to the reference node
      {
        MatrixB(i,pos)=1; //add the positive terminal
        MatrixB(i,neg)=-1; //add the negative terminal
      }
      if (pos== -1) //if the positive terminal connects to the reference node
      {
        MatrixB(i,neg)=-1;
      }
      if (neg== -1)//if the negative termainal connects to the reference node
      {
        MatrixB(i,pos)=1;
      }
    }

  }
  return MatrixB;// return the completed B matrix
}

Mat<double> MatrixC(Sim s) // construct the
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
  }
  Mat<double> D(M,M,fill::zeros);
  return D;

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
