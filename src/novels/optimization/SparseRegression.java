package novels.optimization;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import edu.stanford.math.primitivelib.autogen.matrix.DoubleSparseVector;
import edu.stanford.math.primitivelib.autogen.matrix.DoubleVectorEntry;
import edu.stanford.nlp.optimization.DiffFunction;
import edu.stanford.nlp.optimization.QNMinimizer;

import java.util.Iterator;

public class SparseRegression {

	int length;
	double lambda;
	double threshold=1e-5;
	int memory=15;
	ArrayList<DoubleSparseVector> x;
	ArrayList<Integer> y;
	double[] current;
	double L2Lambda;
	double L1Lambda;
	
	public void setBiasParameters(HashSet<Integer> biases) {
		OWLQN.biasParameters=biases;

	}
	public double[] regress() {
		LogisticDiffFunction diff=new LogisticDiffFunction();
		double[] initial=new double[length];
		OWLQN qn = new OWLQN();
		//QNMinimizer qn2 = new QNMinimizer(30, true);
		qn.setQuiet(true);
		System.out.println("regressing with " + x.size() + " training samples of length " + length);
	
		double[] gradient=qn.minimize(diff,initial,L1Lambda,threshold,memory);
		//double[] gradient=qn2.minimize(diff,threshold,initial,500);
		
		//QNMinimizer qn2 = new QNMinimizer(30, true);
		//qn2.shutUp();
	//	double[] gradient=qn2.minimize(diff,threshold,initial,500);
		return gradient;
	}
	/*public SparseLogisticRegression(double[][] x, double[] y, double L2Lambda) {
		this.x=x;
		this.y=y;
		length=x[0].length;
		current=new double[x.length];
		this.L2Lambda=L2Lambda;
	}*/
	
	public SparseRegression(ArrayList<DoubleSparseVector> x, ArrayList<Integer> y, double L1Lambda, double L2Lambda, int length) {
		this.x=x;
		this.y=y;
		this.length=length;
		current=new double[x.size()];
		this.L2Lambda=L2Lambda;
		this.L1Lambda=L1Lambda;
	}
	
	public void setCurrent(double[] weights) {
		DoubleSparseVector grad=new DoubleSparseVector(weights);

		for (int i=0; i<x.size(); i++) {
			DoubleSparseVector vec=x.get(i);
			double dot=vec.innerProduct(grad);
			//double one=1.0/(1+Math.exp(-1 * dot));
			current[i]=dot;//one;
			
		}
	}
	public class LogisticDiffFunction implements DiffFunction {

		public double[] derivativeAt(double[] arg0) {
			
			setCurrent(arg0);
			//DoubleSparseVector grad=new DoubleSparseVector(arg0);
			
			int length=arg0.length;
			double[] gradient=new double[length];
			
			for (int i=0; i<x.size(); i++) {
				DoubleSparseVector vec=x.get(i);
				double dot=current[i];
				//for (int j=0; j<length; j++) {
					//dot+=vec.innerProduct(grad);
						//vec.get(j) * arg0[j];
				//}
				double cval=1.0/(1+Math.exp(-1 * dot));
				double diff=y.get(i)-cval;
				double thisy=y.get(i);
				if (thisy == 0) {
					thisy=-1;
				}
				            
				
				for (Iterator<DoubleVectorEntry> iterator = vec.iterator(); iterator.hasNext(); ) {
					DoubleVectorEntry entry=iterator.next();
					int key=entry.getIndex();
					double value = entry.getValue();
					gradient[key]-=diff*value;
				}
			
			}		
			if (L2Lambda > 0) {
				for (int i=0; i<arg0.length-1; i++) {
					gradient[i]+=2*L2Lambda*(arg0[i]);
				}
			}

			return gradient;
		}

		public int domainDimension() {
			return length;
		}

		public double valueAt(double[] arg0) {
			double loss=0;
			setCurrent(arg0);
			
			for (int i=0; i<x.size(); i++) {
				double dot=current[i];
				double cval=1.0/(1+Math.exp(-1 * dot));
				if (y.get(i) == 1) {
					loss-=Math.log(cval);
				} else {
					loss-=Math.log(1-cval);
				}
				
			}
			if (L2Lambda > 0) {
				for (int i=0; i<arg0.length-1; i++) {
					loss+=(L2Lambda*(arg0[i])*(arg0[i]));
				}
			}
			
			return loss;
		}

	}
	

}
