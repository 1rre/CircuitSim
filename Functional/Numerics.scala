package re._1r.circuit

import org.apache.commons.math3.complex._
import org.apache.commons.math3.linear.Array2DRowFieldMatrix

object Numerics {
  implicit class ComplexImplicits(z: Complex){
    def +(a: Complex) = z.add(a)
    def +(a: Double) = z.add(a)
    def -(a: Complex) = z.subtract(a)
    def -(a: Double) = z.subtract(a)
    def *(a: Complex) = z.multiply(a)
    def *(a: Double) = z.multiply(a)
    def /(a: Complex) = z.divide(a)
    def /(a: Double) = z.divide(a)
    def unary_- = z * -1
  }
  implicit class DoubleComplexImplicits(i: Double){
    def +(a: Complex) = a + i
    def -(a: Complex) = -a + i
    def *(a: Complex) = a * i
    def /(a: Complex) = i * a.pow(-1)
  }
  implicit class FieldMatrixImplicits(m: Vector[Vector[Complex]]) {
    def toMatrix: Array2DRowFieldMatrix[Complex] = new Array2DRowFieldMatrix(m.map(_.toArray).toArray)
  }
  implicit class MeanVector(x: Vector[(Double, Double)]) {
    def mean = if(x.unzip._2.sum != 0) x.map(a => a._1 * a._2).sum / x.unzip._2.sum else 0
  }
    
}