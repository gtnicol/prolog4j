/* GNU Prolog for Java
 * Copyright (C) 2009 Michiel Hendriks
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
package gnu.prolog.vm.builtins.java;

import gnu.prolog.term.AtomTerm;
import gnu.prolog.term.JavaObjectTerm;
import gnu.prolog.term.Term;
import gnu.prolog.vm.ExecuteOnlyCode;
import gnu.prolog.vm.PrologException;
import gnu.prolog.vm.TermConstants;

/**
 * Factory class for Java integration predicates.
 * Provides implementations for java_to_string/2 and java_classname/2.
 */
public final class Predicates {

	private Predicates() {
		throw new AssertionError("Utility class should not be instantiated");
	}

	/** java_to_string/2 - Get string representation of Java object */
	public static final ExecuteOnlyCode JAVA_TO_STRING = (interpreter, backtrackMode, args) -> {
		Object obj = switch (args[0]) {
			case JavaObjectTerm jot -> jot.value;
			default -> {
				PrologException.typeError(TermConstants.javaObjectAtom, args[0]);
				yield null; // Never reached
			}
		};
		Term val = AtomTerm.get(obj != null ? obj.toString() : "null");
		return interpreter.unify(args[1], val);
	};

	/** java_classname/2 - Get class name of Java object */
	public static final ExecuteOnlyCode JAVA_CLASSNAME = (interpreter, backtrackMode, args) -> {
		Object obj = switch (args[0]) {
			case JavaObjectTerm jot -> jot.value;
			default -> {
				PrologException.typeError(TermConstants.javaObjectAtom, args[0]);
				yield null; // Never reached
			}
		};
		Term val = AtomTerm.get(obj != null ? obj.getClass().getName() : "null");
		return interpreter.unify(args[1], val);
	};
}
