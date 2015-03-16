/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package libsvm.core;

/**
 * @author khanh
 *
 */
public class CoefficientProcessing {

	private static final int BOUND_MIN_COEFFICIENT = 100;
	private static final double MAX_DIFFERENCE_TO_NEAREST_INTEGER = Math.pow(10, -1);
	private static final double EPSILON = Math.pow(10, -9);
	
	public double[] process(Divider divider){
		double[] thetas = getFullThetas(divider);
		thetas = detectZeroCoefficient(thetas);
		thetas = pivotMinCoefficient(thetas);
		thetas = integerRound(thetas);
		
		return thetas;
	}

	private double[] getFullThetas(Divider divider) {
		double[] oldThetas = divider.getThetas();
		//the last element is the theta0
		double[] thetas = new double[oldThetas.length + 1];
		for(int i = 0; i < thetas.length - 1; i++){
			thetas[i] = oldThetas[i];
		}
		thetas[thetas.length - 1] = divider.getTheta0();
		return thetas;
	}
	

	/**
	 * Value less than EPSILON considered as zero
	 * But if after that having all zero, then just return original
	 * @param thetas
	 * @return
	 */
	private double[] detectZeroCoefficient(double[] thetas){
		double[] result = new double[thetas.length];
		int countZero = 0;
		for(int i = 0; i < thetas.length; i++){
			if(thetas[i] < EPSILON){
				result[i] = 0;
				countZero++;
			}
			else{
				result[i] = thetas[i];
			}
		}
		
		if(countZero == thetas.length){
			return thetas;
		}
		return result;
	}
	
	private double[] pivotMinCoefficient(double[] thetas) {
		double min = Double.MAX_VALUE;
		for(int i = 0; i < thetas.length; i++){
			double absCoefficient = Math.abs(thetas[i]);
			if(absCoefficient > 0 && absCoefficient < min){
				min = absCoefficient;
			}
		}
		
		double[] result = new double[thetas.length];
		for(int i = 0; i < thetas.length; i++){
			result[i] = thetas[i] / min;
		}
		
		return result;
	}
	
	/**
	 * Only make the coefficient of variables as integer
	 * Constant will rounded accordingly
	 * @param coefficients
	 * @return
	 */
	private double[] integerRound(double[] coefficients){
		double[] result = new double[coefficients.length];
		
		for(int i = 1; i <= BOUND_MIN_COEFFICIENT; i++){
			boolean allCoefficientsInteger = true;
			//try to make coefficient of variables as integer
			for(int j = 0; j < coefficients.length - 1; j++){
				double newCoefficient = coefficients[j] * i;
				if(isApproximateInteger(newCoefficient)){
					result[j] = Math.round(newCoefficient);
				}
				else{
					allCoefficientsInteger = false;
					break;
				}
			}
			
			if(allCoefficientsInteger){
				//update constant accordingly, we must take the floor because the divider for positive is in the form
				//ax+by >= c
				result[coefficients.length - 1] = Math.floor(coefficients[coefficients.length - 1] * i);
				return result;
			}
		}
		
		return coefficients;
	}
	
	private boolean isApproximateInteger(double number){
		long roundingInteger = Math.round(number);
		return Math.abs(number - roundingInteger) < MAX_DIFFERENCE_TO_NEAREST_INTEGER;
	}
}
