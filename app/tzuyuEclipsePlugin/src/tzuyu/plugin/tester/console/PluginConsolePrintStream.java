/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package tzuyu.plugin.tester.console;

import java.io.IOException;

import org.eclipse.ui.console.IOConsoleOutputStream;
import org.eclipse.ui.statushandlers.StatusManager;

import sav.common.core.AbstractPrintStream;
import sav.common.core.iface.IPrintStream;
import sav.common.core.utils.StringUtils;
import tzuyu.plugin.commons.utils.IStatusUtils;
import tzuyu.plugin.tester.reporter.PluginLogger;

/**
 * @author LLT
 * 
 */
public class PluginConsolePrintStream extends AbstractPrintStream implements
		IPrintStream {
	private IOConsoleOutputStream out;
	
	public PluginConsolePrintStream() {
		out = TzConsole.getOutputStream();
	}

	@Override
	public void print(byte b) {
		print(Byte.toString(b));
	}

	@Override
	public void print(char c) {
		print(Character.toString(c));
	}

	@Override
	public void print(double d) {
		print(Double.toString(d));
	}

	@Override
	public void print(String s) {
		try {
			out.write(s);
		} catch (IOException e) {
			PluginLogger.getLogger().logEx(e);
			StatusManager.getManager().handle(
					IStatusUtils.error(e.getMessage()), StatusManager.BLOCK);
		}
	}

	@Override
	public void println(String s) {
		print(StringUtils.nullToEmpty(s) + "\n");
	}

	@Override
	public void println(Object[] ar) {
		for (Object e : ar) {
			println(StringUtils.toStringNullToEmpty(e));
		}
	}

}
