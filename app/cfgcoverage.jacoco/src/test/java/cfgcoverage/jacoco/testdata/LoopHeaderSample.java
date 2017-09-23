/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package cfgcoverage.jacoco.testdata;

/**
 * @author LLT
 *
 */
public class LoopHeaderSample {

	public void multiLoopCond() {
		int x = 1;
		int y = 3;
		while (x < 4 
				&& y < 5) {
			x += y;
		}
		
		while (x < 4 && (x + y) < 10 &&
				(x * y) < 20
				&& y < 5) {
			x += y;
		}
	}
	
	public void multiLoopCondNeg() {
		int x = 1;
		int y = 3;
		while (!(x < 4) 
				&& y < 5) {
			x += y;
		}
	}
	
	public void singleLoopCond() {
		int x = 1;
		int y = 3;
		while (y < 5) {
			x += y;
		}
	}
	
	public void forLoop() {
		int x = 1;
		int y = 3;
		for (int i = 0; i < 10; i++) {
			x += y * i;
		}
		System.out.println(x);
	}
	
	public void doWhileMultiCond() {
		int x = 1;
		int y = 3;
		do {
			x += y;
		} while (x < 4 
				&& y < 5);
	}
	
	public void doWhileSingleCondWithInLoopCond() {
		int x = 1;
		int y = 3;
		do {
			x += y;
			if (x >= 4) {
				return;
			}
		} while (y < 5);
	}
}
