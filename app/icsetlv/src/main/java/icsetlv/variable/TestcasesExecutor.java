/*
 * Copyright (C) 2013 by SUTD (Singapore)
 * All rights reserved.
 *
 * 	Author: SUTD
 *  Version:  $Revision: 1 $
 */

package icsetlv.variable;

import icsetlv.common.dto.ArrayValue;
import icsetlv.common.dto.BreakpointValue;
import icsetlv.common.dto.ExecValue;
import icsetlv.common.dto.PrimitiveValue;
import icsetlv.common.dto.ReferenceValue;
import icsetlv.common.dto.TcExecResult;
import icsetlv.common.exception.IcsetlvException;
import icsetlv.common.utils.PrimitiveUtils;
import icsetlv.common.utils.VariableUtils;
import icsetlv.vm.SimpleDebugger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import sav.common.core.Logger;
import sav.common.core.SavException;
import sav.common.core.utils.BreakpointUtils;
import sav.common.core.utils.CollectionUtils;
import sav.strategies.dto.BreakPoint;
import sav.strategies.dto.BreakPoint.Variable;
import sav.strategies.junit.JunitRunner.JunitRunnerProgramArgBuilder;
import sav.strategies.vm.VMConfiguration;

import com.sun.jdi.AbsentInformationException;
import com.sun.jdi.ArrayReference;
import com.sun.jdi.ArrayType;
import com.sun.jdi.ClassType;
import com.sun.jdi.Field;
import com.sun.jdi.IncompatibleThreadStateException;
import com.sun.jdi.LocalVariable;
import com.sun.jdi.Location;
import com.sun.jdi.Method;
import com.sun.jdi.ObjectReference;
import com.sun.jdi.PrimitiveType;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.StackFrame;
import com.sun.jdi.ThreadReference;
import com.sun.jdi.Type;
import com.sun.jdi.Value;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.event.BreakpointEvent;
import com.sun.jdi.event.ClassPrepareEvent;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventQueue;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.event.LocatableEvent;
import com.sun.jdi.event.VMDeathEvent;
import com.sun.jdi.event.VMDisconnectEvent;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.ClassPrepareRequest;
import com.sun.jdi.request.EventRequestManager;

/**
 * @author LLT
 * 
 */
public class TestcasesExecutor {
	private Logger<?> log = Logger.getDefaultLogger();
	private static final String TO_STRING_SIGN= "()Ljava/lang/String;";
	private static final String TO_STRING_NAME= "toString"; 
	private SimpleDebugger debugger;
	private VMConfiguration config;
	// class and its breakpoints
	private Map<String, List<BreakPoint>> brkpsMap;
	private int valRetrieveLevel;

	public TestcasesExecutor(VMConfiguration config, int valRetrieveLevel) {
		this.config = config;
		debugger = new SimpleDebugger();
		this.valRetrieveLevel = valRetrieveLevel;
	}

	public TcExecResult execute(List<String> passTestcases, List<String> failTestcases,
			List<BreakPoint> brkps) throws IcsetlvException, SavException {
		this.brkpsMap = BreakpointUtils.initBrkpsMap(brkps);
		Map<String, BreakPoint> locBrpMap = new HashMap<String, BreakPoint>();
		List<BreakpointValue> passVals = executeJunitTests(locBrpMap, passTestcases);
		List<BreakpointValue> failVals = executeJunitTests(locBrpMap, failTestcases);
		return new TcExecResult(passVals, failVals);
	}

	private List<BreakpointValue> executeJunitTests(
			Map<String, BreakPoint> locBrpMap, List<String> tcs) throws IcsetlvException, SavException {
		List<BreakpointValue> result = new ArrayList<BreakpointValue>();
		List<String> args = new JunitRunnerProgramArgBuilder().methods(tcs).build();
		config.setProgramArgs(args);
		VirtualMachine vm = debugger.run(config);
		if (vm == null) {
			throw new IcsetlvException("cannot start jvm!");
		}
		addClassWatch(vm);
		// process events
		EventQueue eventQueue = vm.eventQueue();
		boolean stop = false;
		while (!stop) {
			EventSet eventSet;
			try {
				eventSet = eventQueue.remove(1000);
			} catch (InterruptedException e) {
				// do nothing, just return.
				break;
			}
			if (eventSet == null) {
				break;
				// TODO LLT: try to get event from queue again until time
				// out is reached.
			}
			for (Event event : eventSet) {
				if (event instanceof VMDeathEvent
						|| event instanceof VMDisconnectEvent) {
					stop = true;
					break;
				} else if (event instanceof ClassPrepareEvent) {
					// watch field on loaded class
					ClassPrepareEvent classPrepEvent = (ClassPrepareEvent) event;
					ReferenceType refType = classPrepEvent.referenceType();
					// breakpoint
					addBreakpointWatch(vm, refType, locBrpMap);
				} else if (event instanceof BreakpointEvent) {
					BreakpointEvent bkpEvent = (BreakpointEvent) event;
					try {
						BreakpointValue bkpVal = extractValuesAtLocation(bkpEvent.location(), bkpEvent,
								locBrpMap);
						CollectionUtils.addIfNotNull(result, bkpVal);
					} catch (IncompatibleThreadStateException e) {
						log.error((Object[])e.getStackTrace());
					} catch (AbsentInformationException e) {
						log.error((Object[])e.getStackTrace());
					}
				}
			}
			eventSet.resume();
		}
		vm.resume();
		return result;
	}

	private BreakpointValue extractValuesAtLocation(Location location, LocatableEvent event,
			Map<String, BreakPoint> locBrpMap)
			throws IncompatibleThreadStateException,
			AbsentInformationException, IcsetlvException {
		BreakPoint bkp = locBrpMap.get(location.toString());
		if (bkp == null) {
			return null;
		}
		
		BreakpointValue bkVal = new BreakpointValue(bkp.getId());
		ThreadReference thread = event.thread();
		synchronized (thread) {
			if (!thread.frames().isEmpty()) {
				// LLT: get correct frame, not the first one.
				StackFrame frame = thread.frames().get(0);
				Method method = frame.location().method();
				ReferenceType refType;
				ObjectReference objRef = null;
				if (method.isStatic()) {
					refType = method.declaringType();
				} else {
					objRef = frame.thisObject();
					refType = objRef.referenceType();
				}
				Map<String, Value> allVariables = new HashMap<String, Value>();
				// class fields (static & non static)
				for (Field field : refType.allFields()) {
					Variable inVar = VariableUtils.lookupVarInBreakpoint(field,
							bkp);
					if (inVar != null) {
						if (field.isStatic()) {
							allVariables.put(inVar.getCode(), refType.getValue(field));
						} else if (objRef != null) {
							allVariables.put(inVar.getCode(), objRef.getValue(field));
						}
					}
				}
				// local variables (includes method args)
				for (LocalVariable var : frame.visibleVariables()) {
					Variable inVar = VariableUtils.lookupVarInBreakpoint(var,
							bkp);
					if (inVar != null) {
						allVariables.put(inVar.getCode(), frame.getValue(var));
					}
				}
				if (!allVariables.isEmpty()) {
					for (Entry<String, Value> entry : allVariables.entrySet()) {
						appendVarVal(bkVal, entry.getKey(), entry.getValue(), 1, thread);
					}
				}
			}
		}
		return bkVal;
	}

	private void appendVarVal(ExecValue parent, String varId,
			Value value, int level, ThreadReference thread) {
		if (level == valRetrieveLevel || varId.endsWith("serialVersionUID")
				|| value == null) {
			return;
		}
		level++;
		Type type = value.type();
		if (type instanceof PrimitiveType) {
			parent.add(new PrimitiveValue(varId, value.toString()));
		} else if (type instanceof ClassType && PrimitiveUtils.isPrimitiveTypeOrString(type.name())) {
			parent.add(new PrimitiveValue(varId, toPrimitiveValue((ClassType) type, (ObjectReference)value, thread)));
		} else if (type instanceof ArrayType) {
			appendArrVarVal(parent, varId, (ArrayReference)value, level, thread);
		} else if (type instanceof ClassType) {
			appendClassVarVal(parent, varId, (ObjectReference) value, level, thread);
		}
	}

	private synchronized String toPrimitiveValue(ClassType type, ObjectReference value,
			ThreadReference thread) {
		Method method = type.concreteMethodByName(TO_STRING_NAME,
				TO_STRING_SIGN);
		if (method != null) {
			try {
				if (thread.isSuspended()) {
					Value toStringValue = value.invokeMethod(thread, method,
							new ArrayList<Value>(),
							ObjectReference.INVOKE_SINGLE_THREADED);
					return toStringValue.toString();
				}
			} catch (Exception e) {
				// ignore.
				log.warn((Object[])e.getStackTrace());
			}
		}
		return null;
	}

	private void appendClassVarVal(ExecValue parent, String varId,
			ObjectReference value, int level, ThreadReference thread) {
		ReferenceValue val = new ReferenceValue(varId);
		ClassType type = (ClassType) value.type();
		for (Field field : type.allFields()) {
			appendVarVal(val, val.getChildId(field.name()),
					value.getValue(field), level, thread);
		}
		parent.add(val);
	}

	private void appendArrVarVal(ExecValue parent, String varId,
			ArrayReference value, int level, ThreadReference thread) {
		ArrayValue val = new ArrayValue(varId);
		val.setLength(value.length());
		for (int i = 0; i < value.length(); i++) {
			appendVarVal(val, val.getChildId(i), value.getValue(i), level, thread);
		}
		parent.add(val);
	}

	private void addBreakpointWatch(VirtualMachine vm, ReferenceType refType,
			Map<String, BreakPoint> locBrpMap) {
		EventRequestManager erm = vm.eventRequestManager();
		for (BreakPoint brkp : brkpsMap.get(refType.name())) {
			// The brkp.lineNo can be a line where a breakpoint cannot be added.
			// We try to do that in later lines then.
			boolean added = false;
			int lineNumber = brkp.getLineNo();
			while (!added) {
				List<Location> locations;
				try {
					locations = refType.locationsOfLine(lineNumber);
				} catch (AbsentInformationException e) {
					log.warn((Object[]) e.getStackTrace());
					continue;
				}
				if (!locations.isEmpty()) {
					Location location = locations.get(0);
					BreakpointRequest breakpointRequest = erm.createBreakpointRequest(location);
					breakpointRequest.setEnabled(true);
					locBrpMap.put(location.toString(), brkp);
					added = true;
				} else {
					lineNumber++;
				}
			}
		}
	}

	private void addClassWatch(VirtualMachine vm) {
		final EventRequestManager erm = vm.eventRequestManager();
		for (String className : brkpsMap.keySet()) {
			registerClassRequest(erm, className);
		}
	}

	private void registerClassRequest(EventRequestManager erm, String className) {
		final ClassPrepareRequest classPrepareRequest = erm.createClassPrepareRequest();
		classPrepareRequest.addClassFilter(className);
		classPrepareRequest.setEnabled(true);
	}

}
