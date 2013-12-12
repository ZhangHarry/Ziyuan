package tester;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import tzuyu.engine.model.Sequence;
import tzuyu.engine.model.StatementKind;
import tzuyu.engine.model.TzuYuAction;
import tzuyu.engine.model.Variable;
import tzuyu.engine.runtime.RArrayDeclaration;
import tzuyu.engine.runtime.RAssignment;
import tzuyu.engine.runtime.SeedSequences;
import tzuyu.engine.utils.ArrayListSimpleList;
import tzuyu.engine.utils.Options;
import tzuyu.engine.utils.Randomness;
import tzuyu.engine.utils.SimpleList;
import tzuyu.engine.utils.ReflectionUtils.Match;

public class HelperSequenceCreator {

  /**
   * Returns a sequence that creates an object of type compatible the given
   * class. Wraps the object in a list, and returns the list.
   * 
   * CURRENTLY, will return a sequence (i.e. a non-empty list) only if cls is an
   * array.
   */
  public static SimpleList<Sequence> createSequence(
      ComponentManager components, Class<?> cls) {

    // Class<?> cls = statement.getInputTypes().get(i);

    if (!cls.isArray()) {
      return new ArrayListSimpleList<Sequence>();
    }

    Sequence s = null;

    if (cls.getComponentType().isPrimitive()) {
      s = randPrimitiveArray(cls.getComponentType());
    } else {
      SimpleList<Sequence> candidates = components.getSequencesForType(
          cls.getComponentType(), false);
      if (candidates.isEmpty()) {
        if (Options.forbidNull()) {
          // No sequences that produce appropriate component values found, and
          // null forbidden.
          // Return the empty array.
          RArrayDeclaration decl = 
              new RArrayDeclaration(cls.getComponentType(), 0);
          s = new Sequence();
          TzuYuAction array = TzuYuAction.fromStatmentKind(decl);
          s = s.extend(array, Collections.<Variable> emptyList());
        } else {
          // No sequences that produce appropriate component values found, and
          // null allowed.
          // TODO: We should also randomly return the empty array--it's a
          // perfectly good case
          // even if null is allowed.
          // Return the array [ null ].
          RArrayDeclaration decl = 
              new RArrayDeclaration(cls.getComponentType(), 1);
          s = new Sequence();
          RAssignment assign = 
              RAssignment.nullOrZeroDecl(cls.getComponentType());
          s = s.extend(TzuYuAction.fromStatmentKind(assign),
              Collections.<Variable> emptyList());
          List<Variable> ins = new ArrayList<Variable>();
          ins.add(s.getVariable(0));
          s = s.extend(TzuYuAction.fromStatmentKind(decl), ins);
        }
      } else {
        // Return the array [ x ] where x is the last value in the sequence.
        RArrayDeclaration decl = 
            new RArrayDeclaration(cls.getComponentType(), 1);
        s = candidates.get(Randomness.nextRandomInt(candidates.size()));
        List<Variable> ins = new ArrayList<Variable>();
        // XXX IS THIS OLD COMMENT TRUE? : this assumes that last statement will
        // have such a var,
        // which I know is currently true because of SequenceCollection
        // implementation.
        ins.add(s.randomVarOfTypeLastStatement(cls.getComponentType(),
            Match.COMPATIBLE_TYPE));
        s = s.extend(TzuYuAction.fromStatmentKind(decl), ins);
      }
    }
    assert s != null;
    ArrayListSimpleList<Sequence> l = new ArrayListSimpleList<Sequence>();
    l.add(s);
    return l;
  }

  private static Sequence randPrimitiveArray(Class<?> componentType) {
    assert componentType.isPrimitive();
    Set<Object> potentialElts = SeedSequences.getSeeds(componentType);
    int length = Randomness.nextRandomInt(4);
    Sequence s = new Sequence();
    List<Variable> emptylist = new ArrayList<Variable>();
    for (int i = 0; i < length; i++) {
      Object elt = Randomness.randomSetMember(potentialElts);
      StatementKind assign = 
          RAssignment.statementForAssignment(componentType, elt);
      s = s.extend(TzuYuAction.fromStatmentKind(assign), emptylist);
    }
    List<Variable> inputs = new ArrayList<Variable>();
    for (int i = 0; i < length; i++) {
      inputs.add(s.getVariable(i));
    }
    RArrayDeclaration array = new RArrayDeclaration(componentType, length);
    s = s.extend(TzuYuAction.fromStatmentKind(array), inputs);
    return s;
  }

}
