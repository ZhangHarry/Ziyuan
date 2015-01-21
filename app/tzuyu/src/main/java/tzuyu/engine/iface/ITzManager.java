/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package tzuyu.engine.iface;

import sav.common.core.Logger;
import sav.common.core.iface.IPrintStream;
import sav.strategies.gentest.ISubTypesScanner;
import tester.ITCGStrategy;
import tzuyu.engine.algorithm.iface.Refiner;
import tzuyu.engine.algorithm.iface.Teacher;
import tzuyu.engine.algorithm.iface.Tester;
import tzuyu.engine.model.dfa.Alphabet;

/**
 * @author LLT
 *
 */
public interface ITzManager<A extends Alphabet<?>> {

	Tester getTester();

	Refiner<A> getRefiner();

	Teacher<A> getTeacher();

	ITCGStrategy getTCGStrategy();

	ISubTypesScanner getRefAnalyzer();

	IPrintStream getOutStream();
	
	Logger<?> getLogger();

	void checkProgress() throws InterruptedException;
}
