/* GNU Prolog for Java
 * Copyright (C) 1997-1999  Constantine Plotnikov
 * Copyright (C) 2009       Michiel Hendriks
 * Copyright (C) 2010       Daniel Thomas
 * Copyright (C) 2011       Matt Lilley
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
package gnu.prolog.vm.builtins.meta;

import gnu.prolog.term.AtomTerm;
import gnu.prolog.term.CompoundTerm;
import gnu.prolog.term.CompoundTermTag;
import gnu.prolog.term.Term;
import gnu.prolog.vm.BacktrackInfo;
import gnu.prolog.vm.BacktrackInfoWithCleanup;
import gnu.prolog.vm.ExecuteOnlyCode;
import gnu.prolog.vm.Interpreter;
import gnu.prolog.vm.PrologException;
import gnu.prolog.vm.interpreter.Call;

/**
 * Factory class for meta predicates.
 * Provides implementations for meta-predicates such as setup_call_catcher_cleanup/4
 * which handles exception handling and cleanup in Prolog programs.
 *
 * @author Matt Lilley
 * @author Constantine Plotnikov
 * @author Michiel Hendriks
 * @author Daniel Thomas
 */
public final class Predicates {

	private Predicates() {
		throw new AssertionError("Utility class should not be instantiated");
	}

	// ============================================================================
	// Predicate Implementations
	// ============================================================================

	/**
	 * setup_call_catcher_cleanup/4 - Meta-predicate for exception handling and cleanup.
	 *
	 * This predicate implements the ISO Prolog setup_call_catcher_cleanup/4 which provides
	 * deterministic cleanup semantics. The setup goal is called once, then the call goal
	 * is executed. The catcher is unified with the exit status (exit, fail, !, or exception(E))
	 * and the cleanup goal is executed when the call goal completes, fails, is cut, or throws
	 * an exception.
	 */
	public static final ExecuteOnlyCode SETUP_CALL_CATCHER_CLEANUP = new ExecuteOnlyCode() {
		@Override
		public RC execute(final Interpreter interpreter, final boolean backtrackMode, final Term[] args) throws PrologException {
			RC rc = RC.SUCCESS;
			Term setup = args[0];
			Term call = args[1];
			Term catcher = args[2];
			Term cleanup = args[3];

			// Only call setup the first time
			if (!backtrackMode) {
				rc = Call.staticExecute(interpreter, false, setup);
			}
			if (rc == RC.SUCCESS || rc == RC.SUCCESS_LAST) {
				try {
					rc = Call.staticExecute(interpreter, backtrackMode, call);
				} catch (PrologException q) {
					rc = interpreter.unify(catcher,
							new CompoundTerm(CompoundTermTag.get(AtomTerm.get("exception"), 1), q.getTerm()));
					if (rc == RC.SUCCESS || rc == RC.SUCCESS_LAST) {
						return Call.staticExecute(interpreter, false, cleanup);
						// re-throw exception if unification fails
					} else {
						throw (q);
					}
				}
				if (rc != RC.SUCCESS) {
					// Call cleanup if the 2nd arg fails, has an exception or is finished
					// But first, unify the port with catcher
					RC unifyRC = RC.SUCCESS_LAST;
					if (rc == RC.FAIL) {
						unifyRC = interpreter.unify(catcher, AtomTerm.get("fail"));
					} else if (rc == RC.SUCCESS_LAST) {
						unifyRC = interpreter.unify(catcher, AtomTerm.get("exit"));
					}
					if (unifyRC == RC.SUCCESS || unifyRC == RC.SUCCESS_LAST) {
						RC cleanupRC = RC.SUCCESS;
						// Save state so the cleanup leaves no choicepoints
						BacktrackInfo bi = interpreter.peekBacktrackInfo();
						cleanupRC = Call.staticExecute(interpreter, false, cleanup);
						interpreter.popBacktrackInfoUntil(bi);
						if (cleanupRC == RC.SUCCESS || cleanupRC == RC.SUCCESS_LAST) {
							return rc;
						} else {
							return cleanupRC;
						}
					} else {
						return rc;
					}
				} else {
					// Choicepoint has been left. Inject a cleanup here
					interpreter.pushBacktrackInfo(new BacktrackInfoWithCatcherCleanup(catcher, cleanup));
					return rc;
				}
			} else {
				return rc;
			}
		}
	};

	// ============================================================================
	// BacktrackInfo Classes
	// ============================================================================

	/**
	 * BacktrackInfo for setup_call_catcher_cleanup/4 predicate.
	 * Extends BacktrackInfoWithCleanup to add catcher unification support.
	 */
	private static class BacktrackInfoWithCatcherCleanup extends BacktrackInfoWithCleanup {
		Term catcher;

		BacktrackInfoWithCatcherCleanup(final Term catcher, final Term cleanup) {
			super(cleanup);
			this.catcher = catcher;
		}

		@Override
		public void cleanup(final Interpreter interpreter) {
			try {
				if (interpreter.unify(catcher, AtomTerm.get("!")) == ExecuteOnlyCode.RC.SUCCESS_LAST) {
					super.cleanup(interpreter);
				}
			} catch (PrologException e) {
				/* Ignore exceptions and return status for cleanup */
				// TODO(drt24) Log a warning
			}
		}
	}
}
