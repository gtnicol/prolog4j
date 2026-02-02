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
package gnu.prolog.database;

import gnu.prolog.term.AtomTerm;
import gnu.prolog.term.CompoundTerm;
import gnu.prolog.term.CompoundTermTag;
import gnu.prolog.term.Term;
import gnu.prolog.term.VariableTerm;
import gnu.prolog.vm.TermConstants;

/**
 * Transforms DCG (Definite Clause Grammar) rules into regular Prolog clauses.
 *
 * DCG rules use the --> operator and are transformed to add difference list arguments.
 * For example:
 * - s --> [a]. becomes s(S0, S) :- S0=[a|S].
 * - s --> np, vp. becomes s(S0, S) :- np(S0, S1), vp(S1, S).
 * - s --> {write(hello)}, [a]. becomes s(S0, S) :- write(hello), S0=[a|S].
 */
public final class DCGTransformer {

	private DCGTransformer() {
		throw new AssertionError("Utility class should not be instantiated");
	}

	// Thread-local counter for generating unique variable names within a transformation
	private static final ThreadLocal<Integer> varCounter = ThreadLocal.withInitial(() -> 0);

	/**
	 * Check if a term is a DCG rule (uses --> operator).
	 *
	 * @param term the term to check
	 * @return true if term is a DCG rule
	 */
	public static boolean isDCGRule(final Term term) {
		if (!(term instanceof CompoundTerm ct)) {
			return false;
		}
		return ct.tag.equals(TermConstants.dcgRuleTag);
	}

	/**
	 * Transform a DCG rule into a regular Prolog clause.
	 *
	 * @param rule the DCG rule (head --> body)
	 * @return the transformed clause (head(S0,S) :- transformedBody)
	 */
	public static Term transform(final Term rule) {
		if (!(rule instanceof CompoundTerm ct) || !ct.tag.equals(TermConstants.dcgRuleTag)) {
			throw new IllegalArgumentException("Not a DCG rule: " + rule);
		}

		// Reset counter for this transformation
		varCounter.set(0);

		Term head = ct.args[0];
		Term body = ct.args[1];

		// Create fresh variables for the difference list
		VariableTerm s0 = nextVar();
		VariableTerm s = nextVar();

		// Transform the head to add difference list arguments
		Term transformedHead = expandHead(head, s0, s);

		// Transform the body
		TransformResult result = transformBody(body, s0, s);

		// Create the clause: transformedHead :- transformedBody
		Term bodyGoal;
		if (result.goal == null || result.goal.equals(TermConstants.trueAtom)) {
			// No body, just unify the difference list: head(S0, S) :- S0 = S.
			bodyGoal = new CompoundTerm(TermConstants.unifyTag, new Term[]{s0, result.output});
		} else {
			// Check if we need to add final unification
			// If result.output != s, we need to unify them
			if (!result.output.equals(s)) {
				Term unify = new CompoundTerm(TermConstants.unifyTag, new Term[]{result.output, s});
				bodyGoal = new CompoundTerm(TermConstants.conjunctionTag, new Term[]{result.goal, unify});
			} else {
				bodyGoal = result.goal;
			}
		}
		return new CompoundTerm(TermConstants.clauseTag, new Term[]{transformedHead, bodyGoal});
	}

	/**
	 * Generate the next unique variable for this transformation.
	 */
	private static VariableTerm nextVar() {
		int count = varCounter.get();
		varCounter.set(count + 1);
		return new VariableTerm("S_" + count);
	}

	/**
	 * Expand a DCG head to include difference list arguments.
	 *
	 * @param head the original head
	 * @param s0 the input difference list variable
	 * @param s the output difference list variable
	 * @return the expanded head
	 */
	private static Term expandHead(final Term head, final Term s0, final Term s) {
		Term h = head.dereference();

		if (h instanceof AtomTerm at) {
			// atom --> ... becomes atom(S0, S) :- ...
			return new CompoundTerm(at, new Term[]{s0, s});
		} else if (h instanceof CompoundTerm ct) {
			// functor(args) --> ... becomes functor(args, S0, S) :- ...
			Term[] oldArgs = ct.args;
			Term[] newArgs = new Term[oldArgs.length + 2];
			System.arraycopy(oldArgs, 0, newArgs, 0, oldArgs.length);
			newArgs[oldArgs.length] = s0;
			newArgs[oldArgs.length + 1] = s;
			return new CompoundTerm(ct.tag.functor, newArgs);
		} else {
			throw new IllegalArgumentException("Invalid DCG head: " + head);
		}
	}

	/**
	 * Result of transforming a DCG body.
	 */
	private static class TransformResult {
		final Term goal;    // The transformed goal (may be null for empty body)
		final Term output;  // The output difference list variable

		TransformResult(final Term goal, final Term output) {
			this.goal = goal;
			this.output = output;
		}
	}

	/**
	 * Transform a DCG body.
	 *
	 * @param body the DCG body to transform
	 * @param input the input difference list variable
	 * @param finalOutput the final output difference list variable
	 * @return the transform result
	 */
	private static TransformResult transformBody(final Term body, final Term input, final Term finalOutput) {
		Term b = body.dereference();

		// Handle terminals: [a, b, c]
		if (b.equals(TermConstants.emptyListAtom)) {
			// [] means no consumption, just unify input and output
			return new TransformResult(null, input);
		}

		// Check if it's a list of terminals
		if (b instanceof CompoundTerm ct && ct.tag.equals(TermConstants.listTag)) {
			// [items...] - build a list matching goal
			Term listGoal = buildListMatch(ct, input, finalOutput);
			return new TransformResult(listGoal, finalOutput);
		}

		// Handle embedded goals: {Goal} - uses CompoundTermTag.curly1
		if (b instanceof CompoundTerm ct && ct.tag.equals(CompoundTermTag.curly1)) {
			// {Goal} - execute Goal without consuming input
			Term embeddedGoal = ct.args[0];
			return new TransformResult(embeddedGoal, input);
		}

		// Handle cut: ! - should not consume input
		if (b.equals(TermConstants.cutAtom)) {
			// ! - execute cut without consuming input
			return new TransformResult(b, input);
		}

		// Handle conjunction: Body1, Body2
		if (b instanceof CompoundTerm ct && ct.tag.equals(TermConstants.conjunctionTag)) {
			Term body1 = ct.args[0];
			Term body2 = ct.args[1];

			// Create intermediate variable
			VariableTerm intermediate = nextVar();

			// Transform each part
			TransformResult result1 = transformBody(body1, input, intermediate);
			TransformResult result2 = transformBody(body2, result1.output, finalOutput);

			// Combine the goals
			Term combinedGoal;
			if (result1.goal == null || result1.goal.equals(TermConstants.trueAtom)) {
				combinedGoal = result2.goal;
			} else if (result2.goal == null || result2.goal.equals(TermConstants.trueAtom)) {
				combinedGoal = result1.goal;
			} else {
				combinedGoal = new CompoundTerm(TermConstants.conjunctionTag,
						new Term[]{result1.goal, result2.goal});
			}

			return new TransformResult(combinedGoal, result2.output);
		}

		// Handle disjunction: Body1 ; Body2
		if (b instanceof CompoundTerm ct && ct.tag.equals(TermConstants.disjunctionTag)) {
			Term body1 = ct.args[0];
			Term body2 = ct.args[1];

			// Transform each branch - both must end at finalOutput
			TransformResult result1 = transformBody(body1, input, finalOutput);
			TransformResult result2 = transformBody(body2, input, finalOutput);

			Term disjunction = new CompoundTerm(TermConstants.disjunctionTag,
					new Term[]{result1.goal, result2.goal});

			return new TransformResult(disjunction, finalOutput);
		}

		// Handle non-terminal: call the non-terminal with difference list arguments
		Term nonterminalCall = expandNonTerminal(b, input, finalOutput);
		return new TransformResult(nonterminalCall, finalOutput);
	}

	/**
	 * Build a list matching goal for terminal list.
	 *
	 * @param list the terminal list
	 * @param input the input variable
	 * @param output the output variable
	 * @return unification goal
	 */
	private static Term buildListMatch(final CompoundTerm list, final Term input, final Term output) {
		// Build the list with output at the end
		// [a,b,c] with input S0 and output S becomes: S0 = [a,b,c|S]
		Term reconstructed = reconstructListWithTail(list, output);
		return new CompoundTerm(TermConstants.unifyTag, new Term[]{input, reconstructed});
	}

	/**
	 * Reconstruct a list term with a new tail.
	 *
	 * @param list the original list
	 * @param tail the new tail
	 * @return the reconstructed list
	 */
	private static Term reconstructListWithTail(final CompoundTerm list, final Term tail) {
		if (list.tag.equals(TermConstants.listTag)) {
			Term head = list.args[0];
			Term listTail = list.args[1].dereference();

			if (listTail.equals(TermConstants.emptyListAtom)) {
				// Last element
				return new CompoundTerm(TermConstants.listTag, new Term[]{head, tail});
			} else if (listTail instanceof CompoundTerm ct && ct.tag.equals(TermConstants.listTag)) {
				// Recursive case
				Term newTail = reconstructListWithTail(ct, tail);
				return new CompoundTerm(TermConstants.listTag, new Term[]{head, newTail});
			} else {
				// Improper list? Just return with new tail
				return new CompoundTerm(TermConstants.listTag, new Term[]{head, tail});
			}
		}
		return list;
	}

	/**
	 * Expand a non-terminal to include difference list arguments.
	 *
	 * @param nonterminal the non-terminal
	 * @param input the input variable
	 * @param output the output variable
	 * @return the expanded call
	 */
	private static Term expandNonTerminal(final Term nonterminal, final Term input, final Term output) {
		Term nt = nonterminal.dereference();

		if (nt instanceof AtomTerm at) {
			// atom becomes atom(Input, Output)
			return new CompoundTerm(at, new Term[]{input, output});
		} else if (nt instanceof CompoundTerm ct) {
			// functor(args) becomes functor(args, Input, Output)
			Term[] oldArgs = ct.args;
			Term[] newArgs = new Term[oldArgs.length + 2];
			System.arraycopy(oldArgs, 0, newArgs, 0, oldArgs.length);
			newArgs[oldArgs.length] = input;
			newArgs[oldArgs.length + 1] = output;
			return new CompoundTerm(ct.tag.functor, newArgs);
		} else {
			throw new IllegalArgumentException("Invalid non-terminal: " + nonterminal);
		}
	}
}
