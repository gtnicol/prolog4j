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
package gnu.prolog.vm.builtins.typetesting;

import gnu.prolog.term.*;
import gnu.prolog.vm.ExecuteOnlyCode;

/**
 * Factory class for type-testing predicates (ISO Prolog 8.3).
 * Provides implementations for var/1, nonvar/1, atom/1, number/1, etc.
 */
public final class Predicates {

	private Predicates() {
		throw new AssertionError("Utility class should not be instantiated");
	}

	/** var/1 - Tests if term is an unbound variable */
	public static final ExecuteOnlyCode VAR = (interpreter, backtrackMode, args) ->
		switch (args[0].dereference()) {
			case VariableTerm vt -> ExecuteOnlyCode.RC.SUCCESS_LAST;
			default -> ExecuteOnlyCode.RC.FAIL;
		};

	/** nonvar/1 - Tests if term is not an unbound variable */
	public static final ExecuteOnlyCode NONVAR = (interpreter, backtrackMode, args) ->
		switch (args[0].dereference()) {
			case VariableTerm vt -> ExecuteOnlyCode.RC.FAIL;
			default -> ExecuteOnlyCode.RC.SUCCESS_LAST;
		};

	/** atom/1 - Tests if term is an atom */
	public static final ExecuteOnlyCode ATOM = (interpreter, backtrackMode, args) ->
		switch (args[0].dereference()) {
			case AtomTerm at -> ExecuteOnlyCode.RC.SUCCESS_LAST;
			default -> ExecuteOnlyCode.RC.FAIL;
		};

	/** number/1 - Tests if term is a number (integer or float) */
	public static final ExecuteOnlyCode NUMBER = (interpreter, backtrackMode, args) ->
		switch (args[0].dereference()) {
			case NumericTerm nt -> ExecuteOnlyCode.RC.SUCCESS_LAST;
			default -> ExecuteOnlyCode.RC.FAIL;
		};

	/** integer/1 - Tests if term is an integer */
	public static final ExecuteOnlyCode INTEGER = (interpreter, backtrackMode, args) ->
		switch (args[0].dereference()) {
			case IntegerTerm it -> ExecuteOnlyCode.RC.SUCCESS_LAST;
			default -> ExecuteOnlyCode.RC.FAIL;
		};

	/** float/1 - Tests if term is a float (includes DecimalTerm for precise floats) */
	public static final ExecuteOnlyCode FLOAT = (interpreter, backtrackMode, args) ->
		switch (args[0].dereference()) {
			case FloatTerm ft -> ExecuteOnlyCode.RC.SUCCESS_LAST;
			case DecimalTerm dt -> ExecuteOnlyCode.RC.SUCCESS_LAST;
			default -> ExecuteOnlyCode.RC.FAIL;
		};

	/** decimal/1 - Tests if term is a decimal (arbitrary precision) */
	public static final ExecuteOnlyCode DECIMAL = (interpreter, backtrackMode, args) ->
		switch (args[0].dereference()) {
			case DecimalTerm dt -> ExecuteOnlyCode.RC.SUCCESS_LAST;
			default -> ExecuteOnlyCode.RC.FAIL;
		};

	/** atomic/1 - Tests if term is atomic (atom, number, etc.) */
	public static final ExecuteOnlyCode ATOMIC = (interpreter, backtrackMode, args) ->
		switch (args[0].dereference()) {
			case AtomicTerm at -> ExecuteOnlyCode.RC.SUCCESS_LAST;
			default -> ExecuteOnlyCode.RC.FAIL;
		};

	/** compound/1 - Tests if term is a compound term */
	public static final ExecuteOnlyCode COMPOUND = (interpreter, backtrackMode, args) ->
		switch (args[0].dereference()) {
			case CompoundTerm ct -> ExecuteOnlyCode.RC.SUCCESS_LAST;
			default -> ExecuteOnlyCode.RC.FAIL;
		};

	/** callable/1 - Tests if term is callable (atom or compound) */
	public static final ExecuteOnlyCode CALLABLE = (interpreter, backtrackMode, args) -> {
		Term term = args[0].dereference();
		return switch (term) {
			case AtomTerm at -> ExecuteOnlyCode.RC.SUCCESS_LAST;
			case CompoundTerm ct -> ExecuteOnlyCode.RC.SUCCESS_LAST;
			default -> ExecuteOnlyCode.RC.FAIL;
		};
	};

	/** ground/1 - Tests if term contains no unbound variables */
	public static final ExecuteOnlyCode GROUND = (interpreter, backtrackMode, args) ->
		isGround(args[0].dereference())
			? ExecuteOnlyCode.RC.SUCCESS_LAST
			: ExecuteOnlyCode.RC.FAIL;

	/**
	 * Helper method to check if a term is ground (contains no variables).
	 */
	private static boolean isGround(final Term term) {
		return switch (term.dereference()) {
			case VariableTerm vt -> false;
			case CompoundTerm ct -> {
				for (Term arg : ct.args) {
					if (!isGround(arg)) {
						yield false;
					}
				}
				yield true;
			}
			default -> true; // Atoms, numbers, etc. are ground
		};
	}

	/** java_object/1 - Tests if term is a Java object */
	public static final ExecuteOnlyCode JAVA_OBJECT = (interpreter, backtrackMode, args) ->
		switch (args[0].dereference()) {
			case JavaObjectTerm jot -> ExecuteOnlyCode.RC.SUCCESS_LAST;
			default -> ExecuteOnlyCode.RC.FAIL;
		};
}
