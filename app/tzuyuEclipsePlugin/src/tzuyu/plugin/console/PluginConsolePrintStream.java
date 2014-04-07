/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package tzuyu.plugin.console;

import java.io.IOException;

import org.eclipse.ui.console.IOConsoleOutputStream;

import tzuyu.engine.iface.AbstractPrintStream;
import tzuyu.engine.iface.IPrintStream;
import tzuyu.engine.utils.StringUtils;

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
			// TODO Auto-generated catch block
			e.printStackTrace();
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
