/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package icsetlv.vm;

import icsetlv.common.exception.IcsetlvException;
import sav.common.core.SavException;
import sav.strategies.vm.VMConfiguration;
import sav.strategies.vm.VMListener;

import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;

/**
 * @author LLT
 *
 */
public class SimpleDebugger {

	/**
	 * using scenario Target VM attaches to previously-running debugger.
	 * @throws SavException 
	 */
	public VirtualMachine run(VMConfiguration config) throws IcsetlvException, SavException {
		VMListener listener = new VMListener();
		listener.startListening(config);
		try {
			Process process = VMRunner.startJVM(config);
			if (process != null) {
				return listener.connect(process);
			}
		} catch (IllegalConnectorArgumentsException e) {
			IcsetlvException.rethrow(e);
		} finally {
			listener.stopListening();
		}
		return null;
	}
}