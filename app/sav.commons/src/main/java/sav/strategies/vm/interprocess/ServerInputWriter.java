/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package sav.strategies.vm.interprocess;

import java.io.OutputStream;

/**
 * @author LLT
 *
 */
public abstract class ServerInputWriter extends AbstractStatefulStream {

	public abstract void setOutputStream(OutputStream outputStream);

	public final void write() {
		synchronized (this) {
			if (!isReady()) {
				throw new IllegalStateException("ServerInputWriter is not ready!");
			}
			writeData();
			waiting(); // wait for new data
		}
	}
	
	protected abstract void writeData();

}
