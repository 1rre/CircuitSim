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

  for (size_t i = 0; i < s.resistors.size(); i++)
{ // for loop that cycles through each resistor
    double pos = s.resistors[i].pos->ID-1; // positive node. (-1) so that the matrix fills from 0
    double neg = s.resistors[i].neg->ID-1; // negative node. the reference node becomes -1
    if (pos>=0 && neg>=0)
{ // if the resistor is not connected to the reference node (node -1)
      Gmatrix(pos,neg) += -1/s.resistors[i].val;
      Gmatrix(neg,pos) += -1/s.resistors[i].val;
      Gmatrix(neg,neg) += 1/s.resistors[i].val;
      Gmatrix(pos,pos) += 1/s.resistors[i].val; // add or subtract the conductance of the resistor to the relevant spaces in the G matrix
    }
    if(pos==-1)
{//if the positive node is the reference node
      Gmatrix(neg,neg) += (1/s.resistors[i].val);
    }
    else if (neg==-1)
{//if the negative node is the reference node.
      Gmatrix(pos,pos) += (1/s.resistors[i].val);
    }
  }
  return Gmatrix; //return the completed G matrix
}

Mat<double> MatrixB(Sim s)
{ //Calculates Matrix B and stores the result in MatrixB
  double M = 0; //number of voltage sources
  for (size_t i = 0; i < s.sources.size(); i++)
{ // finds the number of voltage sources and inductors in the netlist
    if (s.sources[i].cName=='V')
{
      M+=1;

    }
  }
  for (size_t i = 0; i < s.dSources.size(); i++)
{
    M+=1;
  }

  double N = s.nodes.size()-1; // number of nodes
  double cnt=0;
  Mat<double> MatrixB(N,M,fill::zeros); // creating a matrix of the correct size initialised with zeros
  for(int i = 0; i < s.sources.size(); i++)
{ //cycling through the sources vector
    if (s.sources[i].cName=='V')
{ // choosing the voltage sources and inductors
      double pos = s.sources[i].pos->ID-1;//finding the positive node
      double neg = s.sources[i].neg->ID-1;//finding the negative node

      if(pos>=0 && neg>=0) //if the source isnt connected to the reference node
      {
        MatrixB(pos,cnt)=1; //add the positive terminal
        MatrixB(neg,cnt)=-1; //add the negative terminal
      }
      if (pos== -1) //if the positive terminal connects to the reference node
      {
        MatrixB(neg,cnt)=-1;
      }
      if (neg== -1)//if the negative termainal connects to the reference node
      {
        MatrixB(pos,cnt)=1;
      }
      cnt+=1;
    }
  }
//  cout<<MatrixB<<endl;

  for(int i = 0; i < s.dSources.size(); i++)
{ //cycling through the sources vector
    double pos = s.dSources[i].pos->ID-1;//finding the positive node
    double neg = s.dSources[i].neg->ID-1;//finding the negative node
    if(pos>=0 && neg>=0) //if the source isnt connected to the reference node
    	{
      MatrixB(pos,cnt)=1; //add the positive terminal
    	MatrixB(neg,cnt)=-1; //add the negative terminal
      }
    if (pos== -1) //if the positive terminal connects to the reference node
    {
      MatrixB(neg,cnt)=-1;
  	}
    if (neg== -1)//if the negative termainal connects to the reference node
    {
      MatrixB(pos,cnt)=1;
    }
    cnt+=1;
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
  double M = 0;
  for (size_t i = 0; i < s.sources.size(); i++)
{
    if (s.sources[i].cName=='V')
{
      M+=1;
    }
  }
  for (size_t i = 0; i < s.dSources.size(); i++)
{
  	M+=1;
  }
  Mat<double> D(M,M,fill::zeros);
  return D;
}

Mat<double> GetA(Sim s)
{
  double N = s.nodes.size()-1;
  double M = 0;
  for (size_t i = 0; i < s.sources.size(); i++)
{
    if (s.sources[i].cName=='V')
{
      M+=1;
    }
  }
  for (size_t i = 0; i < s.dSources.size(); i++)
{
    M+=1;
  }
  Mat<double> A((N+M),(N+M),fill::zeros);
  Mat<double> G = MatrixG(s);
  Mat<double> B = MatrixB(s);
  Mat<double> C = MatrixC(s);
  Mat<double> D = MatrixD(s);
  A(span(0,N-1),span(0,N-1)) = G;
  A(span(0,N-1),span(N,N+M-1)) = B;
  A(span(N,N+M-1),span(0,N-1)) = C;
  A(span(N,N+M-1),span(N,N+M-1)) = D;
  return A;
}
