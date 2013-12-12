package tzuyu.engine.experiment;

import java.util.HashMap;
import java.util.Map;

import tzuyu.engine.model.Sequence;
import tzuyu.engine.model.Statement;
import tzuyu.engine.model.Variable;


/**
 * Copywright @ Randoop Team
 *
 */
class VariableRenamer {

  /**
   * The sequence in which every variable will be renamed
   * */
  public final Sequence sequence;
  
  /**
   * A map storing the variable id to its name (after renaming)
   * */
  public final Map<Integer, String> name_mapping;
  
  public VariableRenamer(Sequence sequence) {
    assert sequence != null : "The given sequence to rename can not be null";
    this.sequence = sequence;
    this.name_mapping = this.renameVarsInSequence();
  }
  
  /**
   * Gets the name for the index-th variable (output by the i-th statement)
   * */
  public String getRenamedVar(int stmtIdx, int varIdx) {
    if (varIdx == -1) {
      //the return value of the statement in stmtIdx
      String name = this.name_mapping.get(stmtIdx);
      if (name == null) {
        assert sequence.getStatement(stmtIdx).getOutputType().equals(void.class) :
            "The index: " + stmtIdx + "-th output should be void.";
        throw new Error("Error in TzuYu, please report it.");
      }
      return name;
    } else {
      // Other out reference parameters (including the receiver object)
      // other than the return value
      Variable var = this.sequence.getInputs(stmtIdx).get(varIdx);
      String name = getRenamedVar(var.stmtIdx, var.argIdx);
      if (name == null) {
        throw new Error ("Error in TzuYu, please report it.");
      }
      return name;
    }
  }
  
  /**
   * The map storing the occurrence number of the same class. The key is the 
   * class name, and the value is the number of variables with the given type. 
   * This field is only used in <code>rename</code> method.
   * */
  private Map<String, Integer> nameCounterMap = new HashMap<String, Integer>();
  
  private Map<Integer, String> renameVarsInSequence() {
    Map<Integer, String> index_var_map = new HashMap<Integer, String>();
    for (int i = 0; i < this.sequence.size(); i++) {
      Statement stmt = this.sequence.getStatement(i);
      Class<?> outputType = stmt.getOutputType();
      
      if (outputType.equals(void.class)) {
        continue;
      }
      
      String rename = getVariableName(outputType);
      if (!nameCounterMap.containsKey(rename)) {
        index_var_map.put(new Integer(i), rename + "0");
        //update and increase the counting in name map
        nameCounterMap.put(rename, 1);
      } else {
        int num = nameCounterMap.get(rename);
        index_var_map.put(new Integer(i), rename + num);
        //update and increase the counting in name map
        nameCounterMap.put(rename, num + 1);
      }
    }
    return index_var_map;
  }
  
  /**
   * Heuristically transforms variables to better names based on its type name.
   * Here are some examples:
   * int var0 = 1 will be transformed to int i0 = 1
   * ClassName var0 = new ClassName() will be transformed to 
   * ClassName className = new ClassName()
   * Class var0 = null will be transformed to Class clazz = null
   * */
  private static String getVariableName(Class<?> clz) {
    assert !clz.equals(void.class) : "The given variable type can not be void!";
    // renaming for array type
    if (clz.isArray()) {
      while (clz.isArray()) {
        clz = clz.getComponentType();
      }
      return getVariableName(clz) + "_array";
    }
    //for object, string, class types
    if (clz.equals(Object.class)) {
      return "obj";
    } else if (clz.equals(String.class)) {
      return "str";
    } else if (clz.equals(Class.class)) {
      return "clazz";
    } else if (clz.equals(int.class) || clz.equals(Integer.class)) {
    //for primtivie types (including boxing or unboxing types
      return "i";
    } else if (clz.equals(double.class) || clz.equals(Double.class)) {
      return "d";
    } else if (clz.equals(float.class) || clz.equals(Float.class)) {
      return "f";
    } else if (clz.equals(short.class) || clz.equals(Short.class)) {
      return "s";
    } else if (clz.equals(boolean.class) || clz.equals(Boolean.class)) {
      return "b";
    } else if (clz.equals(char.class) || clz.equals(Character.class)) {
      return "char";
    } else if (clz.equals(long.class) || clz.equals(Long.class)) {
      return "long";
    } else if (clz.equals(byte.class) || clz.equals(Byte.class)) {
      return "byte";
    } else {
      //for other object types
      String name = clz.getSimpleName();
      if (Character.isUpperCase(name.charAt(0))) {
        return name.substring(0, 1).toLowerCase() + name.substring(1);
      } else {
        return name + "_instance";
      }
    }
  }
}