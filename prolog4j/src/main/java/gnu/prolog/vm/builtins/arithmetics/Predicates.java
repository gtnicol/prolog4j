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
package gnu.prolog.vm.builtins.arithmetics;

import gnu.prolog.term.*;
import gnu.prolog.vm.Evaluate;
import gnu.prolog.vm.ExecuteOnlyCode;

import java.math.BigDecimal;
import gnu.prolog.vm.PrologException;

/**
 * Factory class for arithmetic predicates (ISO Prolog 9.3).
 * Provides implementations for {@code is}/2, {@code =:=}/2, {@code =\=}/2, {@code <}/2, {@code =<}/2, {@code >}/2, {@code >=}/2.
 */
public final class Predicates {

	private Predicates() {
		throw new AssertionError("Utility class should not be instantiated");
	}

	/** is/2 - Arithmetic evaluation and unification: X is Expr */
	public static final ExecuteOnlyCode IS = (interpreter, backtrackMode, args) ->
		interpreter.unify(args[0], Evaluate.evaluate(args[1]));

	/** =:=/2 - Arithmetic equality: Expr1 =:= Expr2 */
	public static final ExecuteOnlyCode EQUAL = (interpreter, backtrackMode, args) -> {
		Term t1 = Evaluate.evaluate(args[0]);
		Term t2 = Evaluate.evaluate(args[1]);
		return compareNumeric(t1, t2) == 0
			? ExecuteOnlyCode.RC.SUCCESS_LAST
			: ExecuteOnlyCode.RC.FAIL;
	};

	/** =\=/2 - Arithmetic inequality: Expr1 =\= Expr2 */
	public static final ExecuteOnlyCode NOT_EQUAL = (interpreter, backtrackMode, args) -> {
		Term t1 = Evaluate.evaluate(args[0]);
		Term t2 = Evaluate.evaluate(args[1]);
		return compareNumeric(t1, t2) != 0
			? ExecuteOnlyCode.RC.SUCCESS_LAST
			: ExecuteOnlyCode.RC.FAIL;
	};

	/** {@code <}/2 - Arithmetic less than: Expr1 {@code <} Expr2 */
	public static final ExecuteOnlyCode LESS_THAN = (interpreter, backtrackMode, args) -> {
		Term t1 = Evaluate.evaluate(args[0]);
		Term t2 = Evaluate.evaluate(args[1]);
		return compareNumeric(t1, t2) < 0
			? ExecuteOnlyCode.RC.SUCCESS_LAST
			: ExecuteOnlyCode.RC.FAIL;
	};

	/** {@code =<}/2 - Arithmetic less than or equal: Expr1 {@code =<} Expr2 */
	public static final ExecuteOnlyCode LESS_THAN_OR_EQUAL = (interpreter, backtrackMode, args) -> {
		Term t1 = Evaluate.evaluate(args[0]);
		Term t2 = Evaluate.evaluate(args[1]);
		return compareNumeric(t1, t2) <= 0
			? ExecuteOnlyCode.RC.SUCCESS_LAST
			: ExecuteOnlyCode.RC.FAIL;
	};

	/** >/2 - Arithmetic greater than: Expr1 > Expr2 */
	public static final ExecuteOnlyCode GREATER_THAN = (interpreter, backtrackMode, args) -> {
		Term t1 = Evaluate.evaluate(args[0]);
		Term t2 = Evaluate.evaluate(args[1]);
		return compareNumeric(t1, t2) > 0
			? ExecuteOnlyCode.RC.SUCCESS_LAST
			: ExecuteOnlyCode.RC.FAIL;
	};

	/** >=/2 - Arithmetic greater than or equal: Expr1 >= Expr2 */
	public static final ExecuteOnlyCode GREATER_THAN_OR_EQUAL = (interpreter, backtrackMode, args) -> {
		Term t1 = Evaluate.evaluate(args[0]);
		Term t2 = Evaluate.evaluate(args[1]);
		return compareNumeric(t1, t2) >= 0
			? ExecuteOnlyCode.RC.SUCCESS_LAST
			: ExecuteOnlyCode.RC.FAIL;
	};

	/**
	 * Helper method to compare two numeric terms.
	 *
	 * @param t1 first term (must be numeric)
	 * @param t2 second term (must be numeric)
	 * @return negative if t1 < t2, zero if equal, positive if t1 > t2
	 */
	private static int compareNumeric(final Term t1, final Term t2) {
		// If either term is DecimalTerm, use BigDecimal comparison
		if (t1 instanceof DecimalTerm || t2 instanceof DecimalTerm) {
			BigDecimal bd1 = switch (t1) {
				case IntegerTerm it -> BigDecimal.valueOf(it.value);
				case FloatTerm ft -> BigDecimal.valueOf(ft.value);
				case DecimalTerm dt -> dt.value;
				default -> throw new IllegalArgumentException("Expected numeric term");
			};
			BigDecimal bd2 = switch (t2) {
				case IntegerTerm it -> BigDecimal.valueOf(it.value);
				case FloatTerm ft -> BigDecimal.valueOf(ft.value);
				case DecimalTerm dt -> dt.value;
				default -> throw new IllegalArgumentException("Expected numeric term");
			};
			return bd1.compareTo(bd2);
		}

		// Nested switch for type-specific comparison (no DecimalTerm)
		return switch (t1) {
			case IntegerTerm it1 -> switch (t2) {
				case IntegerTerm it2 -> Integer.compare(it1.value, it2.value);
				case FloatTerm ft2 -> Double.compare(it1.value, ft2.value);
				default -> throw new IllegalArgumentException("Expected numeric term");
			};
			case FloatTerm ft1 -> {
				double v2 = switch (t2) {
					case IntegerTerm it -> it.value;
					case FloatTerm ft -> ft.value;
					default -> throw new IllegalArgumentException("Expected numeric term");
				};
				yield Double.compare(ft1.value, v2);
			}
			default -> throw new IllegalArgumentException("Expected numeric term");
		};
	}
}
