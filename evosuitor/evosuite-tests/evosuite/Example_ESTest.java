/*
 * This file was automatically generated by EvoSuite
 * Tue Oct 17 04:17:21 GMT 2017
 */

package evosuite;

import org.junit.Test;
import static org.junit.Assert.*;
import evosuite.Example;
import org.evosuite.runtime.EvoRunner;
import org.evosuite.runtime.EvoRunnerParameters;
import org.junit.runner.RunWith;

@RunWith(EvoRunner.class) @EvoRunnerParameters(mockJVMNonDeterminism = true, useVFS = true, useVNET = true, resetStaticState = true, separateClassLoader = true, useJEE = true) 
public class Example_ESTest extends Example_ESTest_scaffolding {

  @Test(timeout = 4000)
  public void test0()  throws Throwable  {
      String[] stringArray0 = new String[4];
      Example.main(stringArray0);
  }
}