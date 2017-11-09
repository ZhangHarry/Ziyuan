/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package gentest.core.value.generator;

import gentest.core.data.statement.RArrayAssignment;
import gentest.core.data.statement.RArrayConstructor;
import gentest.core.data.type.IType;
import gentest.core.data.variable.GeneratedVariable;
import gentest.main.GentestConstants;
import sav.common.core.SavException;
import sav.common.core.utils.PrimitiveUtils;
import sav.common.core.utils.Randomness;

/**
 * @author Nguyen Phuoc Nguong Phuc
 */
public class ArrayValueGenerator extends ValueGenerator {
	private int dimension; // start from 1
	private IType lastContenType;
	
	public ArrayValueGenerator(IType type) {
		super(type);
		dimension = getArrayDimension();
		lastContenType = getLastContentType();
	}

	@Override
	public boolean doAppendVariable(GeneratedVariable variable, int level) throws SavException {
		// Generate the array
		int sizes[] = new int[dimension];
		for (int i = 0; i < dimension; i++) {
			sizes[i] = selectArraySize(i + 1, dimension, lastContenType.getRawType(), level);
		}
		final RArrayConstructor arrayConstructor = new RArrayConstructor(sizes,
				type.getRawType(), lastContenType.getRawType());
		variable.append(arrayConstructor);
		variable.commitReturnVarIdIfNotExist();
		// Generate the array content
		int[] location = next(null, arrayConstructor.getSizes()); // if arrayConstructor.getSizes()[i]>0, i.e. size[i]>0, location[i] = 0,otherwise null
		while (location != null) {
			/* keep level the same for better chance to generate a non-null value for content, this would be make
			 * more sense in case of an array.
			 */
			GeneratedVariable newVar = appendVariable(variable,
					level, lastContenType);
			int localVariableID = newVar.getReturnVarId(); // the last ID

			// get the variable
			RArrayAssignment arrayAssignment = new RArrayAssignment(
					arrayConstructor.getOutVarId(), location, localVariableID);
			variable.append(arrayAssignment);
			location = next(location, arrayConstructor.getSizes());
		}
		return true;
	}

	/**
	 * maxsize of the last dimension = 10 (if primitive/String) or 1 -> 3 (if not primitive), and is decreased by dimension.
	 * maxsize of other dimension not the last one is from 1 -> 2.
	 * */
	private int selectArraySize(int curLevel, int dimension, Class<?> contentType, int varLevel) {
		if ((curLevel != dimension && dimension > 1) || varLevel >= 4) {
			return Randomness.nextInt(1, 2);
		}
		
		String clazzName = contentType.getName();
		boolean isSimpleType = PrimitiveUtils.isPrimitive(clazzName) || PrimitiveUtils.isPrimitiveTypeOrString(clazzName);
		int maxSize = GentestConstants.VALUE_GENERATION_ARRAY_MAXLENGTH;
		
		if (!isSimpleType && maxSize > 5) {
			maxSize = getRandomness().weighedCoinFlip(0.7) ? 3 : 5;
		}
		if (curLevel == dimension && dimension > 1) {
			maxSize = maxSize - (dimension * 2) + 1;
			if (maxSize <= 0) {
				maxSize = 2;
			}
		} 
		return Randomness.nextInt(1, maxSize);
	}
	
	private IType getLastContentType() {
		IType contentType = type;
		while (contentType.isArray()) {
			contentType = contentType.getComponentType();
		}
		return contentType;
	}

	private int getArrayDimension() {
		int dimension = 0;
		IType arrayType = type;
		while (arrayType.isArray()) {
			arrayType = arrayType.getComponentType();
			dimension++;
		}
		return dimension;
	}

	private static int[] next(int[] array, int[] limit) {
		if (array == null) {
			final int[] result = new int[limit.length];
			for (int i = 0; i < result.length; i++) {
				if (limit[i] > 0) {
					result[i] = 0;
				} else {
					return null;
				}
			}
			return result;
		}
		int i = 0;
		while (i < array.length && array[i] >= limit[i] - 1) {
			i++;
		}
		if (i >= array.length) {
			return null;
		} else {
			array[i]++;
			if (i - 1 >= 0 && array[i - 1] == limit[i - 1] - 1) {
				for (int j = 0; j < i; j++) {
					array[j] = 0;
				}
			}
			return array;
		}
	}

}
