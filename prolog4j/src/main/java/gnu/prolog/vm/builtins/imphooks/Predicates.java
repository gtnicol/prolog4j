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
package gnu.prolog.vm.builtins.imphooks;

import gnu.prolog.term.AtomTerm;
import gnu.prolog.term.IntegerTerm;
import gnu.prolog.term.Term;
import gnu.prolog.term.VariableTerm;
import gnu.prolog.vm.BacktrackInfo;
import gnu.prolog.vm.ExecuteOnlyCode;
import gnu.prolog.vm.Interpreter;
import gnu.prolog.vm.PrologException;
import gnu.prolog.vm.PrologHalt;
import gnu.prolog.vm.TermConstants;

import java.util.Iterator;
import java.util.Map;

/**
 * Factory class for implementation-defined hook predicates (ISO Prolog 8.17).
 * Provides implementations for halt/1, set_prolog_flag/2, and current_prolog_flag/2.
 */
public final class Predicates {

	private Predicates() {
		throw new AssertionError("Utility class should not be instantiated");
	}

	// ============================================================================
	// BacktrackInfo classes
	// ============================================================================

	private static class CurrentPrologFlagBacktrackInfo extends BacktrackInfo {
		Map<AtomTerm, Term> map;
		Iterator<AtomTerm> keys;
		int startUndoPosition;
		Term flag;
		Term value;

		CurrentPrologFlagBacktrackInfo() {
			super(-1, -1);
		}
	}

	/** halt/1 - Halt execution with exit code */
	public static final ExecuteOnlyCode HALT = (interpreter, backtrackMode, args) -> {
		IntegerTerm code = switch (args[0]) {
			case VariableTerm vt -> {
				PrologException.instantiationError(args[0]);
				yield null; // Never reached
			}
			case IntegerTerm it -> it;
			default -> {
				PrologException.typeError(TermConstants.integerAtom, args[0]);
				yield null; // Never reached
			}
		};
		throw new PrologHalt(code.value);
	};

	/** set_prolog_flag/2 - Set a Prolog flag value */
	public static final ExecuteOnlyCode SET_PROLOG_FLAG = (interpreter, backtrackMode, args) -> {
		Term flag = args[0];
		Term value = args[1];
		switch (flag) {
			case VariableTerm vt -> {
				PrologException.instantiationError();
			}
			case AtomTerm at -> {}
			default -> {
				PrologException.typeError(TermConstants.atomAtom, flag);
			}
		}
		switch (value) {
			case VariableTerm vt -> {
				PrologException.instantiationError();
			}
			default -> {}
		}
		interpreter.getEnvironment().setPrologFlag((AtomTerm) flag, value);
		return ExecuteOnlyCode.RC.SUCCESS_LAST;
	};

	/** current_prolog_flag/2 - Query Prolog flags (backtracking) */
	public static final ExecuteOnlyCode CURRENT_PROLOG_FLAG = new ExecuteOnlyCode() {
		@Override
		public RC execute(final Interpreter interpreter, final boolean backtrackMode, final Term[] args)
				throws PrologException {
			if (backtrackMode) {
				CurrentPrologFlagBacktrackInfo bi = (CurrentPrologFlagBacktrackInfo) interpreter.popBacktrackInfo();
				interpreter.undo(bi.startUndoPosition);
				return nextSolution(interpreter, bi);
			} else {
				Term flag = args[0];
				Term value = args[1];
				switch (flag) {
					case AtomTerm at -> {
						Term val = interpreter.getEnvironment().getPrologFlag(at);
						if (val == null) {
							PrologException.domainError(TermConstants.prologFlagAtom, flag);
						}
						return interpreter.unify(value, val.dereference());
					}
					case VariableTerm vt -> {} // Valid, will backtrack through all flags
					default -> {
						PrologException.typeError(TermConstants.atomAtom, flag);
					}
				}
				CurrentPrologFlagBacktrackInfo bi = new CurrentPrologFlagBacktrackInfo();
				bi.map = interpreter.getEnvironment().getPrologFlags();
				bi.keys = bi.map.keySet().iterator();
				bi.startUndoPosition = interpreter.getUndoPosition();
				bi.flag = flag;
				bi.value = value;
				return nextSolution(interpreter, bi);
			}
		}

		private RC nextSolution(final Interpreter interpreter, final CurrentPrologFlagBacktrackInfo bi)
				throws PrologException {
			while (bi.keys.hasNext()) {
				AtomTerm f = bi.keys.next();
				Term v = bi.map.get(f);
				RC rc = interpreter.simpleUnify(f, bi.flag);
				if (rc == RC.FAIL) {
					interpreter.undo(bi.startUndoPosition);
					continue;
				}
				rc = interpreter.simpleUnify(v, bi.value);
				if (rc == RC.FAIL) {
					interpreter.undo(bi.startUndoPosition);
					continue;
				}
				interpreter.pushBacktrackInfo(bi);
				return RC.SUCCESS;
			}
			return RC.FAIL;
		}
	};
}
