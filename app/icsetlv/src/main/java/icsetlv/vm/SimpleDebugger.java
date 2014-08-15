/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package icsetlv.vm;

import icsetlv.common.exception.IcsetlvException;

import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;

/**
 * @author LLT
 *
 */
public class SimpleDebugger {

	public VirtualMachine run(VMConfiguration config) throws IcsetlvException {
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