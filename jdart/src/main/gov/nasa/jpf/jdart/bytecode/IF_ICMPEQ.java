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

import gov.nasa.jpf.constraints.expressions.NumericComparator;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.ThreadInfo;

public class IF_ICMPEQ extends gov.nasa.jpf.jvm.bytecode.IF_ICMPEQ{

  public IF_ICMPEQ(int targetPosition) {
    super(targetPosition);
  }

	@Override
	public Instruction execute (ThreadInfo ti) {
	  Instruction res = IF_ICMPHelper.execute(this, NumericComparator.EQ, ti);
	  if(res != null) {
      conditionValue = (res == this.target);
	    return res;
    }
	  return super.execute(ti);        
	}
}
