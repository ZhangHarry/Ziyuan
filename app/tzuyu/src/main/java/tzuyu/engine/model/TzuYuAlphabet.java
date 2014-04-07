package tzuyu.engine.model;

import java.lang.reflect.Modifier;

import tzuyu.engine.TzClass;
import tzuyu.engine.bool.AndFormula;
import tzuyu.engine.bool.NotFormula;
import tzuyu.engine.bool.Simplifier;
import tzuyu.engine.iface.IPrintStream;
import tzuyu.engine.model.dfa.Alphabet;
import tzuyu.engine.model.exception.TzRuntimeException;

public class TzuYuAlphabet extends Alphabet {
	private TzClass project;
	private IPrintStream out;
	
	private TzuYuAlphabet(TzuYuAlphabet from) {
		super();
		this.project = from.project;
	}
	
	public static TzuYuAlphabet forClass(TzClass project) {
		return new TzuYuAlphabet(project, null);
	}
	
	public static TzuYuAlphabet forStaticGroup(TzClass project) {
		return new TzuYuAlphabet(project, true);
	}
	
	public static TzuYuAlphabet forNonStaticGroup(TzClass project) {
		return new TzuYuAlphabet(project, false);
	}

	/**
	 * TODO LLT: still need to decide which solution
	 * separate the static and non-static methods or not.
	 */
	private TzuYuAlphabet(TzClass project, Boolean staticGroup) {
		super();
		this.project = project;
		// add constructors as the initial alphabet

		/*
		 * ConstructorInfo[] ctors = Analytics.getTargetClassInfo().getConstructors(); 
		 * for (ConstructorInfo ctor : ctors) { 
		 * 		TzuYuAction action = TzuYuAction.fromCtor(ctor); 
		 * 		sigma.addSymbol(action); 
		 * }
		 */

		// add methods as the initial alphabet
		MethodInfo[] methods = project.getTargetClassInfo().getMethods(project.getConfiguration());

		for (MethodInfo method : methods) {
			if (staticGroup == null || Modifier.isStatic(method.getModifiers()) == staticGroup) {
				TzuYuAction action = TzuYuAction.fromMethod(method, project.getConfiguration());
				if (action == null) {
					throw new TzRuntimeException("Cannot create the initial alphabet");
				}
				addSymbol(action);
			}
		}
	}

	/**
	 * Incrementally refine the old alphabet by combining the two new alphabet
	 * symbols with the old alphabet and remove the old alphabet symbol which
	 * has the same statement as the two new alphabet symbols.
	 * 
	 * @param action
	 * @param fAction
	 *            the positive action which is the divider.
	 * @param tAction
	 *            the negative action which is the negation of divider.
	 * @return the new alphabet which contains the two alphabets.
	 */
	public TzuYuAlphabet incrementalRefine(Formula divider, Action action) {

		if (!(action instanceof TzuYuAction)) {
			throw new TzRuntimeException("The action is not an TzuYu alphabet");
		}

		TzuYuAction tzuyuAction = (TzuYuAction) action;

		StatementKind stmt = tzuyuAction.getAction();

		Formula trueDivider = divider;

		Formula falseDivider = new NotFormula(divider);

		TzuYuAlphabet newSigma = new TzuYuAlphabet(this);

		for (int index = 1; index < this.getSize(); index++) {
			Action symbol = this.getAction(index);

			TzuYuAction act = (TzuYuAction) symbol;

			if (tzuyuAction.equals(act)) {
				Formula newTrueFormula = new AndFormula(act.getGuard(),
						trueDivider);
				newTrueFormula = Simplifier.simplify(newTrueFormula);

				Formula newFalseFormula = new AndFormula(act.getGuard(),
						falseDivider);
				newFalseFormula = Simplifier.simplify(newFalseFormula);
				if (newTrueFormula.equals(Formula.FALSE)) {
					out.println("new guard evaluates to false.");
				} else {
					TzuYuAction trueAction = new TzuYuAction(newTrueFormula,
							stmt);
					newSigma.addSymbol(trueAction);
				}
				if (newFalseFormula.equals(Formula.FALSE)) {
					out.println("new guard evaluates to false.");
				} else {
					TzuYuAction falseAction = new TzuYuAction(newFalseFormula,
							stmt);
					newSigma.addSymbol(falseAction);
				}
			} else {
				newSigma.addSymbol(symbol);
			}
		}

		return newSigma;
	}

	/**
	 * Refine the alphabet with the newly discovered alphabet to substitute the
	 * old alphabet such that the old actions with the guard and the negation of
	 * guard are removed and add tow new alphabet symbols with the new guard and
	 * its negation to the alphabet.
	 * 
	 * @param divider
	 * @param action
	 * @return
	 */
	public TzuYuAlphabet refine(Formula divider, Action action) {
		if (!(action instanceof TzuYuAction)) {
			throw new TzRuntimeException("The action is not an TzuYu alphabet");
		}

		TzuYuAction positiveAction = (TzuYuAction) action;

		StatementKind stmt = positiveAction.getAction();

		Formula negativeGuard = new NotFormula(positiveAction.getGuard());
		negativeGuard = Simplifier.simplify(negativeGuard);

		TzuYuAction negativeAction = new TzuYuAction(negativeGuard, stmt);

		Formula trueDivider = divider;

		Formula falseDivider = new NotFormula(divider);

		TzuYuAlphabet newSigma = new TzuYuAlphabet(this);

		for (int index = 1; index < this.getSize(); index++) {
			Action symbol = this.getAction(index);

			TzuYuAction act = (TzuYuAction) symbol;

			// Remove the alphabet symbol and add the two new alphabet symbol.
			if (positiveAction.equals(act)) {
				Formula newTrueFormula = new AndFormula(act.getGuard(),
						trueDivider);
				newTrueFormula = Simplifier.simplify(newTrueFormula);

				if (newTrueFormula.equals(Formula.FALSE)) {
					out.println("new guard evaluates to false.");
				} else {
					TzuYuAction trueAction = new TzuYuAction(newTrueFormula,
							stmt);
					newSigma.addSymbol(trueAction);
				}

				Formula newFalseFormula = new AndFormula(act.getGuard(),
						falseDivider);
				newFalseFormula = Simplifier.simplify(newFalseFormula);

				if (newFalseFormula.equals(Formula.FALSE)) {
					out.println("new guard evaluates to false.");
				} else {
					TzuYuAction falseAction = new TzuYuAction(newFalseFormula,
							stmt);
					newSigma.addSymbol(falseAction);
				}
			} else if (!negativeAction.equals(act)) { 
				// we also need to remove the negation of the original alphabet
				// symbol.
				newSigma.addSymbol(symbol);
			}
		}

		return newSigma;
	}

	public TzClass getProject() {
		return project;
	}

	public void setOutStream(IPrintStream outStream) {
		this.out = outStream;
	}
}
