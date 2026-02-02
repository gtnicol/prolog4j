/* GNU Prolog for Java
 * Copyright (C) 2025
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA  02111-1307, USA. The text of license can be also found
 * at http://www.gnu.org/copyleft/lgpl.html
 */
package gnu.prolog.vm.builtins.dcg;

import gnu.prolog.term.AtomTerm;
import gnu.prolog.term.CompoundTerm;
import gnu.prolog.term.Term;
import gnu.prolog.term.VariableTerm;
import gnu.prolog.vm.ExecuteOnlyCode;
import gnu.prolog.vm.Interpreter;
import gnu.prolog.vm.PrologException;
import gnu.prolog.vm.TermConstants;

/**
 * Factory class for DCG (Definite Clause Grammar) predicates.
 * Provides implementations for phrase/2 and phrase/3.
 */
public final class Predicates {

	private Predicates() {
		throw new AssertionError("Utility class should not be instantiated");
	}

	/**
	 * phrase/2 - phrase(+RuleSet, ?List)
	 * Succeeds if List can be parsed by the DCG RuleSet.
	 * Equivalent to: RuleSet(List, [])
	 */
	public static final ExecuteOnlyCode PHRASE_2 = (interpreter, backtrackMode, args) -> {
		Term ruleSet = args[0];
		Term list = args[1];

		// phrase(RuleSet, List) is equivalent to call(RuleSet, List, [])
		Term emptyList = TermConstants.emptyListAtom;
		Term goal = expandDCGGoal(ruleSet, list, emptyList);

		// Execute the expanded goal using Call.staticExecute
		// This properly handles backtracking and nested goals
		return gnu.prolog.vm.interpreter.Call.staticExecute(interpreter, backtrackMode, goal);
	};

	/**
	 * phrase/3 - phrase(+RuleSet, ?List, ?Rest)
	 * Succeeds if List can be parsed by the DCG RuleSet with Rest remaining.
	 * Equivalent to: RuleSet(List, Rest)
	 */
	public static final ExecuteOnlyCode PHRASE_3 = (interpreter, backtrackMode, args) -> {
		Term ruleSet = args[0];
		Term list = args[1];
		Term rest = args[2];

		// phrase(RuleSet, List, Rest) is equivalent to call(RuleSet, List, Rest)
		Term goal = expandDCGGoal(ruleSet, list, rest);

		// Execute the expanded goal using Call.staticExecute
		// This properly handles backtracking and nested goals
		return gnu.prolog.vm.interpreter.Call.staticExecute(interpreter, backtrackMode, goal);
	};

	/**
	 * Expand a DCG goal by adding difference list arguments.
	 *
	 * @param ruleSet the DCG rule (atom or compound term)
	 * @param s0 the input list
	 * @param s the output/rest list
	 * @return the expanded goal with difference list arguments
	 * @throws PrologException if ruleSet is not callable
	 */
	private static Term expandDCGGoal(final Term ruleSet, final Term s0, final Term s) throws PrologException {
		Term rs = ruleSet.dereference();

		return switch (rs) {
			case AtomTerm at -> {
				// atom(S0, S)
				yield new CompoundTerm(at, new Term[]{s0, s});
			}
			case CompoundTerm ct -> {
				// functor(arg1, ..., argN, S0, S)
				Term[] oldArgs = ct.args;
				Term[] newArgs = new Term[oldArgs.length + 2];
				System.arraycopy(oldArgs, 0, newArgs, 0, oldArgs.length);
				newArgs[oldArgs.length] = s0;
				newArgs[oldArgs.length + 1] = s;
				yield new CompoundTerm(ct.tag.functor, newArgs);
			}
			case VariableTerm vt -> {
				PrologException.instantiationError(ruleSet);
				yield null; // Never reached
			}
			default -> {
				PrologException.typeError(TermConstants.callableAtom, ruleSet);
				yield null; // Never reached
			}
		};
	}
}
