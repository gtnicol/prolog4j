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
package gnu.prolog.vm.builtins.termcomparsion;

import gnu.prolog.term.*;
import gnu.prolog.vm.ExecuteOnlyCode;
import gnu.prolog.vm.PrologException;
import gnu.prolog.vm.TermConstants;

/**
 * Factory class for term comparison predicates (ISO Prolog 8.4).
 * Provides implementations for {@code @<}/2, {@code @=<}/2, {@code @>}/2, {@code @>=}/2, {@code ==}/2, {@code \==}/2, {@code compare}/3.
 */
public final class Predicates {

	private Predicates() {
		throw new AssertionError("Utility class should not be instantiated");
	}

	private static final AtomTerm EQ_ATOM = AtomTerm.get("=");
	private static final AtomTerm LT_ATOM = AtomTerm.get("<");
	private static final AtomTerm GT_ATOM = AtomTerm.get(">");

	/** {@code @<}/2 - Standard order less than: Term1 {@code @<} Term2 */
	public static final ExecuteOnlyCode TERM_LESS_THAN = (interpreter, backtrackMode, args) -> {
		TermComparator cmp = new TermComparator();
		return cmp.compare(args[0], args[1]) < 0
			? ExecuteOnlyCode.RC.SUCCESS_LAST
			: ExecuteOnlyCode.RC.FAIL;
	};

	/** {@code @=<}/2 - Standard order less than or equal: Term1 {@code @=<} Term2 */
	public static final ExecuteOnlyCode TERM_LESS_THAN_OR_EQUAL = (interpreter, backtrackMode, args) -> {
		TermComparator cmp = new TermComparator();
		return cmp.compare(args[0], args[1]) <= 0
			? ExecuteOnlyCode.RC.SUCCESS_LAST
			: ExecuteOnlyCode.RC.FAIL;
	};

	/** {@code @>}/2 - Standard order greater than: Term1 {@code @>} Term2 */
	public static final ExecuteOnlyCode TERM_GREATER_THAN = (interpreter, backtrackMode, args) -> {
		TermComparator cmp = new TermComparator();
		return cmp.compare(args[0], args[1]) > 0
			? ExecuteOnlyCode.RC.SUCCESS_LAST
			: ExecuteOnlyCode.RC.FAIL;
	};

	/** {@code @>=}/2 - Standard order greater than or equal: Term1 {@code @>=} Term2 */
	public static final ExecuteOnlyCode TERM_GREATER_THAN_OR_EQUAL = (interpreter, backtrackMode, args) -> {
		TermComparator cmp = new TermComparator();
		return cmp.compare(args[0], args[1]) >= 0
			? ExecuteOnlyCode.RC.SUCCESS_LAST
			: ExecuteOnlyCode.RC.FAIL;
	};

	/** ==/2 - Term identical: Term1 == Term2 */
	public static final ExecuteOnlyCode TERM_IDENTICAL = (interpreter, backtrackMode, args) -> {
		TermComparator cmp = new TermComparator();
		return cmp.compare(args[0], args[1]) == 0
			? ExecuteOnlyCode.RC.SUCCESS_LAST
			: ExecuteOnlyCode.RC.FAIL;
	};

	/** \==/2 - Term not identical: Term1 \== Term2 */
	public static final ExecuteOnlyCode TERM_NOT_IDENTICAL = (interpreter, backtrackMode, args) -> {
		TermComparator cmp = new TermComparator();
		return cmp.compare(args[0], args[1]) != 0
			? ExecuteOnlyCode.RC.SUCCESS_LAST
			: ExecuteOnlyCode.RC.FAIL;
	};

	/** compare/3 - Compare two terms and unify with order: compare(?Order, +Term1, +Term2) */
	public static final ExecuteOnlyCode COMPARE = (interpreter, backtrackMode, args) -> {
		Term order = args[0];
		switch (order) {
			case VariableTerm vt -> {} // Valid type
			case AtomTerm at -> {
				if ((order != EQ_ATOM) & (order != LT_ATOM) & (order != GT_ATOM)) {
					PrologException.domainError(TermConstants.orderAtom, order);
				}
			}
			default -> {
				PrologException.typeError(TermConstants.atomAtom, order);
			}
		}
		TermComparator comp = new TermComparator();
		int rc = comp.compare(args[1], args[2]);
		if (rc == 0) {
			return interpreter.unify(EQ_ATOM, order);
		} else if (rc > 0) {
			return interpreter.unify(GT_ATOM, order);
		} else if (rc < 0) {
			return interpreter.unify(LT_ATOM, order);
		}
		return ExecuteOnlyCode.RC.FAIL;
	};
}
