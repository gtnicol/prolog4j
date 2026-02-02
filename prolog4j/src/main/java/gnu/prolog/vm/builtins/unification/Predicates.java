/* GNU Prolog for Java
 * Copyright (C) 1997-1999  Constantine Plotnikov
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
package gnu.prolog.vm.builtins.unification;

import gnu.prolog.term.*;
import gnu.prolog.vm.ExecuteOnlyCode;
import gnu.prolog.vm.PrologCode;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory class for unification predicates (ISO Prolog 8.2).
 * Provides implementations for =/2, \=/2, unify_with_occurs_check/2.
 */
public final class Predicates {

	private Predicates() {
		throw new AssertionError("Utility class should not be instantiated");
	}

	/** =/2 - Unify two terms: Term1 = Term2 */
	public static final ExecuteOnlyCode UNIFY = (interpreter, backtrackMode, args) ->
		interpreter.unify(args[0], args[1]);

	/** \=/2 - Terms not unifiable: Term1 \= Term2 */
	public static final ExecuteOnlyCode NOT_UNIFIABLE = (interpreter, backtrackMode, args) -> {
		int undoPos = interpreter.getUndoPosition();
		ExecuteOnlyCode.RC rc = interpreter.unify(args[0], args[1]);
		if (rc == PrologCode.RC.SUCCESS_LAST) {
			interpreter.undo(undoPos);
			rc = ExecuteOnlyCode.RC.FAIL;
		} else {
			rc = ExecuteOnlyCode.RC.SUCCESS_LAST;
		}
		return rc;
	};

	/** unify_with_occurs_check/2 - Unify with occurs check: unify_with_occurs_check(+Term1, +Term2) */
	public static final ExecuteOnlyCode UNIFY_WITH_OCCURS_CHECK = (interpreter, backtrackMode, args) -> {
		List<Term> stack = new ArrayList<>(10);
		stack.add(args[0]);
		stack.add(args[1]);
		ExecuteOnlyCode.RC rc = ExecuteOnlyCode.RC.SUCCESS_LAST;
		unify_loop: while (!stack.isEmpty()) {
			Term t1 = stack.remove(stack.size() - 1).dereference();
			Term t2 = stack.remove(stack.size() - 1).dereference();
			if (t1 == t2) {
				// Same term, continue
				continue;
			}

			// Try to handle t1 as VariableTerm first
			switch (t1) {
				case VariableTerm vt1 -> {
					if (!occurCheck(vt1, t2)) {
						rc = ExecuteOnlyCode.RC.FAIL;
						break unify_loop;
					}
					interpreter.addVariableUndo(vt1);
					vt1.value = t2;
					continue unify_loop;
				}
				default -> {}
			}

			// Try to handle t2 as VariableTerm
			switch (t2) {
				case VariableTerm vt2 -> {
					if (!occurCheck(vt2, t1)) {
						rc = ExecuteOnlyCode.RC.FAIL;
						break unify_loop;
					}
					interpreter.addVariableUndo(vt2);
					vt2.value = t1;
					continue unify_loop;
				}
				default -> {}
			}

			// Class mismatch check
			if (t1.getClass() != t2.getClass()) {
				rc = ExecuteOnlyCode.RC.FAIL;
				break unify_loop;
			}

			// Handle same-class unification with switch
			// Note: t1.getClass() == t2.getClass() is guaranteed by check above
			switch (t1) {
				case CompoundTerm ct1 -> {
					CompoundTerm ct2 = (CompoundTerm) t2;
					if (ct1.tag != ct2.tag) {
						rc = ExecuteOnlyCode.RC.FAIL;
						break unify_loop;
					}
					Term[] args1 = ct1.args;
					Term[] args2 = ct2.args;
					for (int i = args2.length - 1; i >= 0; i--) {
						stack.add(args1[i].dereference());
						stack.add(args2[i].dereference());
					}
				}
				case FloatTerm ft1 -> {
					FloatTerm ft2 = (FloatTerm) t2;
					if (ft1.value != ft2.value) {
						rc = ExecuteOnlyCode.RC.FAIL;
						break unify_loop;
					}
				}
				case DecimalTerm dt1 -> {
					DecimalTerm dt2 = (DecimalTerm) t2;
					if (!dt1.equals(dt2)) {
						rc = ExecuteOnlyCode.RC.FAIL;
						break unify_loop;
					}
				}
				case IntegerTerm it1 -> {
					IntegerTerm it2 = (IntegerTerm) t2;
					if (it1.value != it2.value) {
						rc = ExecuteOnlyCode.RC.FAIL;
						break unify_loop;
					}
				}
				case AtomTerm at1 -> {
					AtomTerm at2 = (AtomTerm) t2;
					if (at1 != at2) {
						rc = ExecuteOnlyCode.RC.FAIL;
						break unify_loop;
					}
				}
				case JavaObjectTerm jot1 -> {
					JavaObjectTerm jot2 = (JavaObjectTerm) t2;
					if (jot1.value != jot2.value) {
						rc = ExecuteOnlyCode.RC.FAIL;
						break unify_loop;
					}
				}
				default -> {
					rc = PrologCode.RC.FAIL;
					break unify_loop;
				}
			}
		}
		return rc;
	};

	/**
	 * Perform occurs check on variable.
	 *
	 * @param variable the variable to check
	 * @param term the term to check against
	 * @return true if term does not contain variable
	 */
	private static boolean occurCheck(final VariableTerm variable, final Term term) {
		if (variable == term) {
			return false;
		}
		switch (term) {
			case CompoundTerm ct -> {
				Term[] args = ct.args;
				for (int i = args.length - 1; i >= 0; i--) {
					if (!occurCheck(variable, args[i].dereference())) {
						return false;
					}
				}
			}
			default -> {}
		}
		return true;
	}
}
