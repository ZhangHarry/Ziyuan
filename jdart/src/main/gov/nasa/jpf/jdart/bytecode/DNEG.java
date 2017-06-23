/*
 * Copyright (C) 2015, United States Government, as represented by the 
 * Administrator of the National Aeronautics and Space Administration.
 * All rights reserved.
 * 
 * The PSYCO: A Predicate-based Symbolic Compositional Reasoning environment 
 * platform is licensed under the Apache License, Version 2.0 (the "License"); you 
 * may not use this file except in compliance with the License. You may obtain a 
 * copy of the License at http://www.apache.org/licenses/LICENSE-2.0. 
 * 
 * Unless required by applicable law or agreed to in writing, software distributed 
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR 
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the 
 * specific language governing permissions and limitations under the License.
 */
package gov.nasa.jpf.jdart.bytecode;

import gov.nasa.jpf.constraints.expressions.UnaryMinus;
import gov.nasa.jpf.jdart.ConcolicInstructionFactory;
import gov.nasa.jpf.jdart.ConcolicUtil;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;

/**
 * Negate double ..., value => ..., result
 */
public class DNEG extends gov.nasa.jpf.jvm.bytecode.DNEG {

  @Override
	public Instruction execute(ThreadInfo ti) {
		StackFrame sf = ti.getTopFrame();
    
    if (sf.getLongOperandAttr() == null) {
      return super.execute(ti);
    }    
    
	  ConcolicUtil.Pair<Double> negated = ConcolicUtil.popDouble(sf);
    
    UnaryMinus<Double> symb = new UnaryMinus<Double>(negated.symb);    
    double conc = -negated.conc;    
    
    ConcolicUtil.Pair<Double> result = new ConcolicUtil.Pair<Double>(conc, symb);
    ConcolicUtil.pushDouble(result, sf);

    if (ConcolicInstructionFactory.DEBUG) ConcolicInstructionFactory.logger.finest("Execute DNEG: " + result);		
    return getNext(ti);         
 	}
}
