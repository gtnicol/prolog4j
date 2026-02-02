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
package gnu.prolog.vm.builtins.debug;

import gnu.prolog.term.AtomTerm;
import gnu.prolog.term.CompoundTerm;
import gnu.prolog.term.CompoundTermTag;
import gnu.prolog.term.IntegerTerm;
import gnu.prolog.term.Term;
import gnu.prolog.vm.ExecuteOnlyCode;
import gnu.prolog.vm.PrologException;
import gnu.prolog.vm.TermConstants;
import gnu.prolog.vm.interpreter.Tracer.TraceLevel;

import java.util.EnumSet;

/**
 * Factory class for debugging predicates.
 * Provides implementations for trace, notrace, spy, nospy, nospyall, debugging, tracing.
 */
public final class Predicates {

	private Predicates() {
		throw new AssertionError("Utility class should not be instantiated");
	}

	// ============================================================================
	// Helper Methods (public for backward compatibility)
	// ============================================================================

	/**
	 * Get trace level from term.
	 * @param term the term
	 * @return set of TraceLevels for the term
	 * @throws PrologException if term is not valid
	 */
	public static EnumSet<TraceLevel> getTraceLevel(final Term term) throws PrologException {
		return switch (term) {
			case AtomTerm at -> TraceLevel.fromString(at.value);
			default -> {
				PrologException.typeError(TermConstants.atomAtom, term);
				yield EnumSet.noneOf(TraceLevel.class); // Never reached
			}
		};
	}

	/**
	 * Get CompoundTermTag from term.
	 * @param term the term
	 * @return the CompoundTermTag for the term
	 * @throws PrologException if term is not valid
	 */
	public static CompoundTermTag getTag(final Term term) throws PrologException {
		String functor = "";
		int arity = -1;

		switch (term) {
			case AtomTerm at -> {
				functor = at.value;
				int idx = functor.indexOf('/');
				if (idx > -1) {
					try {
						arity = Integer.parseInt(functor.substring(idx + 1));
						functor = functor.substring(0, idx);
					} catch (NumberFormatException e) {
						// Keep arity as -1
					}
				}
			}
			case CompoundTerm ct -> {
				if (!ct.tag.toString().equals("//2")) {
					PrologException.typeError(TermConstants.compoundAtom, term);
				}
				functor = switch (ct.args[0]) {
					case AtomTerm at -> at.value;
					default -> {
						PrologException.typeError(TermConstants.atomAtom, ct.args[0]);
						yield ""; // Never reached
					}
				};
				arity = switch (ct.args[1]) {
					case IntegerTerm it -> it.value;
					default -> {
						PrologException.typeError(TermConstants.atomAtom, ct.args[1]);
						yield -1; // Never reached
					}
				};
			}
			default -> {
				PrologException.typeError(TermConstants.compoundAtom, term);
			}
		}
		return CompoundTermTag.get(functor, arity);
	}

	// ============================================================================
	// Predicate Implementations
	// ============================================================================

	/** trace/0 - Enable tracing */
	public static final ExecuteOnlyCode TRACE = (interpreter, backtrackMode, args) -> {
		interpreter.getTracer().setActive(true);
		return ExecuteOnlyCode.RC.SUCCESS_LAST;
	};

	/** notrace/0 - Disable tracing */
	public static final ExecuteOnlyCode NOTRACE = (interpreter, backtrackMode, args) -> {
		interpreter.getTracer().setActive(false);
		return ExecuteOnlyCode.RC.SUCCESS_LAST;
	};

	/** debugging/0 - Report debugging status */
	public static final ExecuteOnlyCode DEBUGGING = (interpreter, backtrackMode, args) -> {
		interpreter.getTracer().reportStatus();
		return ExecuteOnlyCode.RC.SUCCESS_LAST;
	};

	/** tracing/0 - Check if tracing is active */
	public static final ExecuteOnlyCode TRACING = (interpreter, backtrackMode, args) -> {
		if (interpreter.getTracer().isActive()) {
			return ExecuteOnlyCode.RC.SUCCESS_LAST;
		}
		return ExecuteOnlyCode.RC.FAIL;
	};

	/** spy/2 - Set a trace point */
	public static final ExecuteOnlyCode SPY = (interpreter, backtrackMode, args) -> {
		CompoundTermTag tag = getTag(args[0]);
		if (tag.arity == -1) {
			for (CompoundTermTag ptag : interpreter.getEnvironment().getModule().getPredicateTags()) {
				if (ptag.functor.equals(tag.functor)) {
					setSpyPoint(interpreter, ptag, args[1]);
				}
			}
		} else {
			setSpyPoint(interpreter, tag, args[1]);
		}
		return ExecuteOnlyCode.RC.SUCCESS_LAST;
	};

	/** nospy/1 - Remove a trace point */
	public static final ExecuteOnlyCode NOSPY = (interpreter, backtrackMode, args) -> {
		CompoundTermTag tag = getTag(args[0]);
		if (tag.arity == -1) {
			for (CompoundTermTag ptag : interpreter.getEnvironment().getModule().getPredicateTags()) {
				if (ptag.functor.equals(tag.functor)) {
					interpreter.getTracer().removeTrace(ptag);
				}
			}
		} else {
			interpreter.getTracer().removeTrace(tag);
		}
		return ExecuteOnlyCode.RC.SUCCESS_LAST;
	};

	/** nospyall/0 - Remove all trace points */
	public static final ExecuteOnlyCode NOSPYALL = (interpreter, backtrackMode, args) -> {
		interpreter.getTracer().removeAllTraces();
		return ExecuteOnlyCode.RC.SUCCESS_LAST;
	};

	// ============================================================================
	// Private Helper Methods
	// ============================================================================

	private static void setSpyPoint(final gnu.prolog.vm.Interpreter interpreter,
	                                 final CompoundTermTag tag,
	                                 final Term arg) throws PrologException {
		switch (arg) {
			case AtomTerm at -> {
				EnumSet<TraceLevel> lvl = getTraceLevel(arg);
				interpreter.getTracer().setTrace(tag, lvl);
			}
			case CompoundTerm ct -> {
				EnumSet<TraceLevel> lvl = getTraceLevel(ct.args[0]);
				if (ct.tag.toString().equals("+/1")) {
					interpreter.getTracer().addTrace(tag, lvl);
				} else if (ct.tag.toString().equals("-/1")) {
					interpreter.getTracer().removeTrace(tag, lvl);
				} else {
					PrologException.representationError(arg);
				}
			}
			default -> {} // No action for other types
		}
	}
}
