/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package icsetlv.common.utils;

import java.util.Collection;

/**
 * @author LLT
 * 
 */
public class CollectionBuilder<E, T extends Collection<E>> {
	private T orgCol;
	
	public static <E, T extends Collection<E>> CollectionBuilder<E, Collection<E>> init(
			T orgCol) {
		return new CollectionBuilder<E, Collection<E>>(orgCol);
	}

	public CollectionBuilder(T orgCol) {
		this.orgCol = orgCol;
	}

	public CollectionBuilder<E, T> add(E newVal) {
		orgCol.add(newVal);
		return this;
	}
	
	@SuppressWarnings("unchecked")
	public <R extends Collection<?>>R getResult() {
		return (R) orgCol;
	}
}
