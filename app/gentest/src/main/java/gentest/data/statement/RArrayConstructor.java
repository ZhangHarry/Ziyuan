/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */
package gentest.data.statement;

/**
 * 
 * @author Nguyen Phuoc Nguong Phuc
 *
 */
public class RArrayConstructor extends Statement {
	private int[] sizes;
	private Class<?> outputType; // Type of the array itself
	private Class<?> contentType; // Type of the array content

	public RArrayConstructor(int[] sizes, Class<?> outputType,
			Class<?> contentType) {
		super(RStatementKind.ARRAY_CONSTRUCTOR);
		this.outputType = outputType;
		this.contentType = contentType;
		this.sizes = sizes;
	}

	@Override
	public boolean hasOutputVar() {
		return true;
	}

	@Override
	public void accept(StatementVisitor visitor) throws Throwable {
		visitor.visit(this);
	}

	public Class<?> getOutputType() {
		return outputType;
	}

	public Class<?> getContentType() {
		return contentType;
	}

	public int[] getSizes() {
		return sizes;
	}

}
