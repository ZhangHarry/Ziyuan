package tester;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;



import tzuyu.engine.model.ExecutionOutcome;
import tzuyu.engine.model.Sequence;
import tzuyu.engine.model.Statement;
import tzuyu.engine.model.TzuYuAction;
import tzuyu.engine.model.Variable;
import tzuyu.engine.runtime.NormalExecution;
import tzuyu.engine.utils.SimpleList;

/**
 * This class acts as the executor for a guarded statement and a sequence.
 * 
 * @author Spencer Xiao
 * 
 */
public final class RuntimeExecutor {
  // The default message printer
  private static PrintStream out = System.out;

  private RuntimeExecutor() {

  }

  /**
   * Execute the sequence to generate the runtime value for all the variables
   * generated or referenced in this sequence. The sequence is self-contained,
   * that is all the parameters referenced are generated by the sequence itself.
   * The execution may not succeed due to the randomness of the parameter
   * generation process, in this case null will be returned.
   * 
   * @param sequence
   *          the sequence must be self-contained.
   * @return the execution result with runtime values of the given
   *         <code> sequence</code>.
   */
  public static SequenceRuntime executeSequence(Sequence sequence) {
    // The runtime values stored
    List<List<Object>> runtimeValues = new ArrayList<List<Object>>();
    List<Object> retVals = new ArrayList<Object>();
    SimpleList<Statement> statements = sequence.getStatementsWithInputs();
    for (int index = 0; index < statements.size(); index++) {
      Statement stmt = statements.get(index);
      List<Variable> inputs = sequence.getInputs(index);
      List<Object> inputVals = new ArrayList<Object>(inputs.size());
      for (Variable var : inputs) {
        if (var.argIdx == -1) {
          inputVals.add(retVals.get(var.stmtIdx));
        } else {
          Object value = runtimeValues.get(var.stmtIdx).get(var.argIdx);
          inputVals.add(value);
        }
      }

      ExecutionOutcome outcome = stmt.statement.getAction().execute(
          inputVals.toArray(), out);

      if (outcome instanceof NormalExecution) {
        Object retVal = ((NormalExecution) outcome).getRetunValue();
        retVals.add(retVal);
        Object[] outVals = ((NormalExecution) outcome).getOutValues();
        runtimeValues.add(Arrays.asList(outVals));
      } else {
        // If any of the statement in this self-contained sequence fail,
        // the execution of this sequence fail. We return null.
        return new SequenceRuntime(false, sequence, retVals, runtimeValues);
      }
    }

    return new SequenceRuntime(true, sequence, retVals, runtimeValues);
  }

  /**
   * Evaluate the guard condition to generate all the runtime values for the
   * input parameters and return them.
   * 
   * @param stmt
   * @param inputVars
   *          the input variables. All variables refer to the same sequence.
   *          This is an very important precondition.
   * @return
   */
  public static ExecutionResult executeGuard(
      TzuYuAction stmt, List<Variable> inputVars) {

    List<Object> inputVals = new ArrayList<Object>(inputVars.size());

    // If the size of the input variables is zero, we known that
    // the guard must be true; And the statement which the guard
    // condition is for must be a static method without any other
    // normal parameters. So we just return true and empty list.
    if (inputVars.size() == 0) {
      return new ExecutionResult(true, new ArrayList<Object>());
    }

    // Since all the input variables refers to the same sequence,
    // we only need to execute one sequence and then we can get
    // all the runtime values.
    SequenceRuntime runtime = executeSequence(inputVars.get(0).owner);
    if (!runtime.isSuccessful()) {
      return new ExecutionResult(false, inputVals);
    }
    // Then get all the value for parameters
    for (Variable inputVar : inputVars) {
      Object value = runtime.getValue(inputVar);
      inputVals.add(value);
    }
    // Execute the guard condition to check whether these parameters can
    // pass the guard condition testing.
    boolean passing = true;
    try{
      passing = stmt.getGuard().evaluate(inputVals.toArray());
    } catch (NullPointerException e) {
      //The evaluation of the guard may throw null pointer exception when
      //the referenced fields is null, thus we catch NullPointerException 
      //and return false for this evaluation.
      //e.printStackTrace();
      passing = false;
    }

    return new ExecutionResult(passing, inputVals);
  }

  /**
   * Execute the real action(statement) in the guarded statement with the given
   * input variables on the precondition that the guard condition in the guarded
   * statement evaluates to true under the give input variables.
   * 
   * @param stmt
   *          the guarded statement to be executed
   * @param inputVars
   *          the input variables which makes the guard condition in the
   *          <code>stmt</code> to be true.
   * @return
   */
  public static boolean executeStatement(
      TzuYuAction stmt, List<Object> inputVals) {
    // Instrument to get the states of the objects
    ExecutionOutcome outcome = stmt.getAction().execute(inputVals.toArray(),
        out);

    if (outcome instanceof NormalExecution) {
      // Normal execution
      return true;
    } else {
      // Exception execution
      return false;
    }
  }

  public static boolean execute(TzuYuAction stmt, List<Variable> inputVars) {
    ExecutionResult result = executeGuard(stmt, inputVars);

    if (result.isPassing()) {
      List<Object> runtimeValues = result.getRuntime();
      boolean normal = executeStatement(stmt, runtimeValues);
      return normal;
    } else {
      return false;
    }
  }

  public static void setPrintStream(PrintStream ps) {
    out = ps;
  }
}
