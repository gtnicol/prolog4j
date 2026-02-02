/* GNU Prolog for Java
 * Copyright (C) 1997-1999  Constantine Plotnikov
 * Copyright (C) 2009       Michiel Hendriks
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
package gnu.prolog.vm.builtins.misc;

import gnu.prolog.database.Predicate;
import gnu.prolog.io.PrologStream;
import gnu.prolog.io.WriteOptions;
import gnu.prolog.term.CompoundTerm;
import gnu.prolog.term.CompoundTermTag;
import gnu.prolog.term.Term;
import gnu.prolog.vm.ExecuteOnlyCode;
import gnu.prolog.vm.TermConstants;

/**
 * Factory class for miscellaneous predicates.
 * Provides implementations for stacktrace/1 and listing/0,1.
 */
public final class Predicates {

	private Predicates() {
		throw new AssertionError("Utility class should not be instantiated");
	}

	/** stacktrace/1 - Get the current call stack as a list */
	public static final ExecuteOnlyCode STACKTRACE = (interpreter, backtrackMode, args) -> {
		Term res = TermConstants.emptyListAtom;
		Term prev = TermConstants.emptyListAtom;
		for (CompoundTermTag tag : interpreter.getTracer().getCallStack()) {
			prev = res;
			res = CompoundTerm.getList(tag.getPredicateIndicator(), res);
		}
		return interpreter.unify(args[0], prev);
	};

	/** listing/0,1 - List predicate definitions */
	public static final ExecuteOnlyCode LISTING = (interpreter, backtrackMode, args) -> {
		CompoundTermTag filter = null;
		if (args.length >= 1) {
			filter = gnu.prolog.vm.builtins.debug.Predicates.getTag(args[0]);
		}
		WriteOptions options = new WriteOptions(interpreter.getEnvironment().getOperatorSet(), true, true, true);
		PrologStream stream = interpreter.getEnvironment().getCurrentOutput();
		for (CompoundTermTag tag : interpreter.getEnvironment().getModule().getPredicateTags()) {
			if (filter != null) {
				if (!tag.functor.equals(filter.functor)) {
					continue;
				}
				if (filter.arity != -1) {
					if (tag.arity != filter.arity) {
						continue;
					}
				}
			}
			Predicate p = interpreter.getEnvironment().getModule().getDefinedPredicate(tag);
			if (p.getType() != Predicate.TYPE.USER_DEFINED) {
				stream.putCodeSequence(null, interpreter, "% Foreign: ");
				stream.writeTerm(null, interpreter, options, tag.getPredicateIndicator());
				stream.putCode(null, interpreter, '\n');
				stream.putCode(null, interpreter, '\n');
			} else {
				synchronized (p) {
					for (Term t : p.getClauses()) {
						stream.writeTerm(null, interpreter, options, t);
						stream.putCode(null, interpreter, '.');
						stream.putCode(null, interpreter, '\n');
					}
					stream.putCode(null, interpreter, '\n');
				}
			}
		}
		stream.flushOutput(null);
		return ExecuteOnlyCode.RC.SUCCESS_LAST;
	};
}
