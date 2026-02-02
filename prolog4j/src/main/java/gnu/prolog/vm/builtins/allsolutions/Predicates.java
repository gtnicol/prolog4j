/* GNU Prolog for Java
 * Copyright (C) 1997-1999  Constantine Plotnikov
 * Copyright (C) 2009       Michiel Hendriks
 * Copyright (C) 2010       Daniel Thomas
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
package gnu.prolog.vm.builtins.allsolutions;

import gnu.prolog.term.CompoundTerm;
import gnu.prolog.term.CompoundTermTag;
import gnu.prolog.term.Term;
import gnu.prolog.term.TermComparator;
import gnu.prolog.term.TermUtils;
import gnu.prolog.term.VariableTerm;
import gnu.prolog.vm.BacktrackInfo;
import gnu.prolog.vm.ExecuteOnlyCode;
import gnu.prolog.vm.Interpreter;
import gnu.prolog.vm.PrologCode;
import gnu.prolog.vm.PrologException;
import gnu.prolog.vm.TermConstants;
import gnu.prolog.vm.interpreter.Call;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

/**
 * Factory class for all-solutions predicates.
 * Provides implementations for ISO Prolog all-solutions predicates that collect
 * all solutions to a goal: bagof/3, findall/3, and setof/3.
 * These predicates use backtracking to find and collect multiple solutions.
 */
public final class Predicates {

	private Predicates() {
		throw new AssertionError("Utility class should not be instantiated");
	}

	// ============================================================================
	// Constants
	// ============================================================================

	private static final CompoundTermTag PLUS_TAG = CompoundTermTag.get("+", 2);

	// ============================================================================
	// Helper Methods
	// ============================================================================

	/**
	 * Check that list is a valid Prolog list (including an uninstantiated variable)
	 *
	 * @param list the term to check to see if it is a list
	 * @throws PrologException for the various errors when it is not a list.
	 */
	private static void checkList(final Term list) throws PrologException {
		Term exArg = list.dereference();
		Term current = exArg;
		while (current != TermConstants.emptyListAtom) {
			switch (current) {
				case VariableTerm vt -> {
					return;
				}
				case CompoundTerm ct -> {
					if (ct.tag != TermConstants.listTag) {
						PrologException.typeError(TermConstants.listAtom, exArg);
					}
					current = ct.args[1].dereference();
				}
				default -> {
					PrologException.typeError(TermConstants.listAtom, exArg);
				}
			}
		}
	}

	/**
	 * Execute findall operation to collect all solutions
	 *
	 * @param interpreter interpreter in which context code is executed
	 * @param backtrackMode true if predicate is called on backtracking and false otherwise
	 * @param template the term to collect for each solution
	 * @param goal the goal to find solutions for
	 * @param list the list to populate with solutions
	 * @return either {@link PrologCode.RC#SUCCESS_LAST} or {@link PrologCode.RC#FAIL}
	 * @throws PrologException on execution errors
	 */
	private static ExecuteOnlyCode.RC findall(final Interpreter interpreter, final boolean backtrackMode,
			final Term template, final Term goal, final List<Term> list) throws PrologException {
		int startUndoPosition = interpreter.getUndoPosition();
		BacktrackInfo startBi = interpreter.peekBacktrackInfo();
		try {
			try {
				boolean callBacktrackMode = false;
				ExecuteOnlyCode.RC rc;
				do {
					rc = Call.staticExecute(interpreter, callBacktrackMode, goal);
					callBacktrackMode = true;
					if (rc != ExecuteOnlyCode.RC.FAIL) {
						list.add((Term) template.clone());
					}
				} while (rc == ExecuteOnlyCode.RC.SUCCESS);
				if (rc == ExecuteOnlyCode.RC.SUCCESS_LAST) {
					interpreter.undo(startUndoPosition);
				}
				return ExecuteOnlyCode.RC.SUCCESS_LAST;
			} catch (RuntimeException rex) {
				PrologException.systemError(rex);
				return ExecuteOnlyCode.RC.FAIL; // fake return
			}
		} catch (PrologException ex) {
			interpreter.popBacktrackInfoUntil(startBi);
			interpreter.undo(startUndoPosition);
			throw ex;
		}
	}

	// ============================================================================
	// Predicate Implementations
	// ============================================================================

	/** findall/3 - Find all solutions to a goal */
	public static final ExecuteOnlyCode FINDALL = new ExecuteOnlyCode() {
		@Override
		public RC execute(final Interpreter interpreter, final boolean backtrackMode, final Term[] args)
				throws PrologException {
			List<Term> list = new ArrayList<>();
			checkList(args[2]);
			RC rc = findall(interpreter, backtrackMode, args[0], args[1], list);
			if (rc == RC.SUCCESS_LAST) {
				return interpreter.unify(args[2], CompoundTerm.getList(list));
			}
			return RC.FAIL;
		}
	};

	/** bagof/3 - Collect solutions to a goal with free variables (backtracking) */
	public static final ExecuteOnlyCode BAGOF = new ExecuteOnlyCode() {
		@Override
		public RC execute(final Interpreter interpreter, final boolean backtrackMode, final Term[] args)
				throws PrologException {
			if (backtrackMode) {
				BagofBacktrackInfo bi = (BagofBacktrackInfo) interpreter.popBacktrackInfo();
				interpreter.undo(bi.startUndoPosition);
				return nextSolution(interpreter, bi);
			} else {
				Term ptemplate = args[0];
				Term pgoal = args[1];
				Term pinstances = args[2];
				checkList(pinstances);
				Set<Term> wset = new HashSet<>();
				Term findallGoal = TermUtils.getFreeVariableSet(pgoal, ptemplate, wset);
				Term witness = TermUtils.getWitness(wset);
				CompoundTerm findallTemplate = new CompoundTerm(PLUS_TAG, witness, ptemplate);
				List<Term> list = new ArrayList<>();
				RC rc = findall(interpreter, false, findallTemplate, findallGoal, list);
				if (rc == RC.FAIL || list.size() == 0) {
					return RC.FAIL;
				}
				BagofBacktrackInfo bi = new BagofBacktrackInfo();
				bi.startUndoPosition = interpreter.getUndoPosition();
				bi.solutionList = list;
				bi.witness = witness;
				bi.instances = pinstances;
				return nextSolution(interpreter, bi);
			}
		}

		private RC nextSolution(final Interpreter interpreter, final BagofBacktrackInfo bi) throws PrologException {
			List<Term> curTList = new ArrayList<>();
			int undoPos = interpreter.getUndoPosition();
			while (bi.solutionList.size() != 0) {
				CompoundTerm curInstance = (CompoundTerm) (bi.solutionList.remove(0)).dereference();
				Term curWitness = curInstance.args[0].dereference();
				RC rc = interpreter.simpleUnify(bi.witness, curWitness);
				if (rc == RC.FAIL) {
					throw new IllegalStateException("unexpected unify fail");
				}
				curTList.add(curInstance.args[1].dereference());
				ListIterator<Term> isol = bi.solutionList.listIterator();
				while (isol.hasNext()) {
					CompoundTerm ct = (CompoundTerm) isol.next();
					Term w = ct.args[0].dereference();
					if (TermUtils.isVariant(curWitness, w)) {
						rc = interpreter.simpleUnify(bi.witness, w);
						if (rc == RC.FAIL) {
							throw new IllegalStateException("unexpected unify fail");
						}
						curTList.add(ct.args[1].dereference());
						isol.remove();
					}
				}
				processList(curTList, false);
				rc = interpreter.unify(CompoundTerm.getList(curTList), bi.instances.dereference());
				if (rc == RC.SUCCESS_LAST) {
					if (bi.solutionList.size() != 0) {
						interpreter.pushBacktrackInfo(bi);
						return RC.SUCCESS;
					} else {
						return RC.SUCCESS_LAST;
					}
				}
				interpreter.undo(undoPos);
				curTList.clear();
			}
			return RC.FAIL;
		}
	};

	/** setof/3 - Collect sorted unique solutions to a goal (backtracking) */
	public static final ExecuteOnlyCode SETOF = new ExecuteOnlyCode() {
		@Override
		public RC execute(final Interpreter interpreter, final boolean backtrackMode, final Term[] args)
				throws PrologException {
			if (backtrackMode) {
				SetofBacktrackInfo bi = (SetofBacktrackInfo) interpreter.popBacktrackInfo();
				interpreter.undo(bi.startUndoPosition);
				return nextSolution(interpreter, bi);
			} else {
				Term ptemplate = args[0];
				Term pgoal = args[1];
				Term pinstances = args[2];
				checkList(pinstances);
				Set<Term> wset = new HashSet<>();
				Term findallGoal = TermUtils.getFreeVariableSet(pgoal, ptemplate, wset);
				Term witness = TermUtils.getWitness(wset);
				CompoundTerm findallTemplate = new CompoundTerm(PLUS_TAG, witness, ptemplate);
				List<Term> list = new ArrayList<>();
				RC rc = findall(interpreter, false, findallTemplate, findallGoal, list);
				if (rc == RC.FAIL || list.size() == 0) {
					return RC.FAIL;
				}
				SetofBacktrackInfo bi = new SetofBacktrackInfo();
				bi.startUndoPosition = interpreter.getUndoPosition();
				bi.solutionList = list;
				bi.witness = witness;
				bi.instances = pinstances;
				return nextSolution(interpreter, bi);
			}
		}

		private RC nextSolution(final Interpreter interpreter, final SetofBacktrackInfo bi) throws PrologException {
			List<Term> curTList = new ArrayList<>();
			int undoPos = interpreter.getUndoPosition();
			while (bi.solutionList.size() != 0) {
				CompoundTerm curInstance = (CompoundTerm) (bi.solutionList.remove(0)).dereference();
				Term curWitness = curInstance.args[0].dereference();
				RC rc = interpreter.simpleUnify(bi.witness, curWitness);
				if (rc == RC.FAIL) {
					throw new IllegalStateException("unexpected unify fail");
				}
				curTList.add(curInstance.args[1].dereference());
				ListIterator<Term> isol = bi.solutionList.listIterator();
				while (isol.hasNext()) {
					CompoundTerm ct = (CompoundTerm) isol.next();
					Term w = ct.args[0].dereference();
					if (TermUtils.isVariant(curWitness, w)) {
						rc = interpreter.simpleUnify(bi.witness, w);
						if (rc == RC.FAIL) {
							throw new IllegalStateException("unexpected unify fail");
						}
						curTList.add(ct.args[1].dereference());
						isol.remove();
					}
				}
				processList(curTList, true);
				rc = interpreter.unify(CompoundTerm.getList(curTList), bi.instances.dereference());
				if (rc == RC.SUCCESS_LAST) {
					if (bi.solutionList.size() != 0) {
						interpreter.pushBacktrackInfo(bi);
						return RC.SUCCESS;
					} else {
						return RC.SUCCESS_LAST;
					}
				}
				interpreter.undo(undoPos);
				curTList.clear();
			}
			return RC.FAIL;
		}
	};

	/**
	 * Process the list for setof - sorts and removes duplicates
	 *
	 * @param curTList the list to process
	 * @param sortAndDeduplicate if true, sort and remove duplicates (for setof); if false, do nothing (for bagof)
	 */
	private static void processList(final List<Term> curTList, final boolean sortAndDeduplicate) {
		if (!sortAndDeduplicate) {
			return;
		}
		TermComparator tc = new TermComparator();
		Collections.sort(curTList, tc);
		// remove duplicates
		Iterator<Term> it = curTList.iterator();
		Term prev = null; // initially there is no "previous element"
		while (it.hasNext()) {
			Term cur = it.next();
			if (prev != null && tc.compare(prev, cur) == 0) {
				it.remove(); // only safe way to remove list element while iterating
			} else {
				prev = cur;
			}
		}
	}

	// ============================================================================
	// BacktrackInfo Classes
	// ============================================================================

	private static class BagofBacktrackInfo extends BacktrackInfo {
		int startUndoPosition;
		List<Term> solutionList;
		Term witness;
		Term instances;

		BagofBacktrackInfo() {
			super(-1, -1);
		}
	}

	private static class SetofBacktrackInfo extends BacktrackInfo {
		int startUndoPosition;
		List<Term> solutionList;
		Term witness;
		Term instances;

		SetofBacktrackInfo() {
			super(-1, -1);
		}
	}
}
