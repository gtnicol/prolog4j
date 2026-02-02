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
package gnu.prolog.vm.builtins.list;

import gnu.prolog.term.AtomTerm;
import gnu.prolog.term.CompoundTerm;
import gnu.prolog.term.CompoundTermTag;
import gnu.prolog.term.IntegerTerm;
import gnu.prolog.term.Term;
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
import java.util.List;

/**
 * Factory class for list manipulation predicates.
 */
public final class Predicates {

	private Predicates() {
		throw new AssertionError("Utility class should not be instantiated");
	}

	private static final class SortException extends RuntimeException {
		private static final long serialVersionUID = 1L;
		private final PrologException error;

		private SortException(final PrologException error) {
			this.error = error;
		}
	}

	// ============================================================================
	// Predicate Implementations
	// ============================================================================

	/** is_list/1 - Check if term is a list */
	public static final ExecuteOnlyCode IS_LIST = (interpreter, backtrackMode, args) -> {
		Term lst = args[0];
		while (lst != null) {
			if (TermConstants.emptyListAtom.equals(lst)) {
				return ExecuteOnlyCode.RC.SUCCESS_LAST;
			}
			if (lst instanceof CompoundTerm ct && CompoundTerm.isListPair(ct)) {
				lst = ct.args[1].dereference();
			} else {
				return ExecuteOnlyCode.RC.FAIL;
			}
		}
		return ExecuteOnlyCode.RC.FAIL;
	};

	/** is_proper_list/1 - Check if term is a proper list (no tail variable) */
	public static final ExecuteOnlyCode IS_PROPER_LIST = (interpreter, backtrackMode, args) -> {
		Term lst = args[0];
		while (lst != null) {
			if (TermConstants.emptyListAtom.equals(lst)) {
				return ExecuteOnlyCode.RC.SUCCESS_LAST;
			}
			if (lst instanceof VariableTerm) {
				return ExecuteOnlyCode.RC.FAIL;
			}
			if (lst instanceof CompoundTerm ct && CompoundTerm.isListPair(ct)) {
				lst = ct.args[1].dereference();
			} else {
				return ExecuteOnlyCode.RC.FAIL;
			}
		}
		return ExecuteOnlyCode.RC.FAIL;
	};

	/** length/2 - Get or check list length */
	public static final ExecuteOnlyCode LENGTH = new ExecuteOnlyCode() {
		@Override
		public RC execute(final Interpreter interpreter, final boolean backtrackMode, final Term[] args) throws PrologException {
			Term listTerm = args[0];
			Term lengthTerm = args[1];

			// Type check lengthTerm
			if (!(lengthTerm instanceof VariableTerm || lengthTerm instanceof IntegerTerm)) {
				PrologException.typeError(TermConstants.integerAtom, lengthTerm);
			}

			if (backtrackMode) {
				LengthBacktrackInfo bi = (LengthBacktrackInfo) interpreter.popBacktrackInfo();
				interpreter.undo(bi.startUndoPosition);
				return nextLength(interpreter, bi, args);
			}

			// If list is bound, count its length
			if (CompoundTerm.isListPair(listTerm) || TermConstants.emptyListAtom.equals(listTerm)) {
				int length = 0;
				Term lst = listTerm;
				while (lst != null) {
					if (TermConstants.emptyListAtom.equals(lst)) {
						break;
					}
					if (lst instanceof VariableTerm vt) {
						vt.value = TermConstants.emptyListAtom;
						break;
					}
					if (lst instanceof CompoundTerm ct && CompoundTerm.isListPair(ct) && ct.args.length == 2) {
						++length;
						lst = ct.args[1].dereference();
					} else {
						return RC.FAIL;
					}
				}
				return interpreter.unify(lengthTerm, IntegerTerm.get(length));
			} else if (listTerm instanceof VariableTerm) {
				// List is unbound
				final var derefLength = lengthTerm.dereference();
				if (derefLength instanceof VariableTerm) {
					// Both unbound - enable backtracking to generate lists of increasing length
					final var bi = new LengthBacktrackInfo();
					bi.startUndoPosition = interpreter.getUndoPosition();
					bi.currentLength = 0;
					return nextLength(interpreter, bi, args);
				}
				if (!(derefLength instanceof IntegerTerm it)) {
					PrologException.typeError(TermConstants.integerAtom, lengthTerm);
					return RC.FAIL; // Never reached
				}
				if (it.value < 0) {
					return RC.FAIL;
				}
				// Generate list of specified length
				final var genList = new ArrayList<Term>();
				for (int i = 0; i < it.value; i++) {
					genList.add(new VariableTerm());
				}
				return interpreter.unify(listTerm, CompoundTerm.getList(genList));
			} else {
				PrologException.typeError(TermConstants.listAtom, listTerm);
			}
			return RC.SUCCESS_LAST;
		}

		private RC nextLength(final Interpreter interpreter, final LengthBacktrackInfo bi, final Term[] args) throws PrologException {
			// Check if we've exceeded maximum length
			if (bi.currentLength >= LengthBacktrackInfo.MAX_LENGTH) {
				return RC.FAIL;
			}

			// Generate list of currentLength with fresh variables
			List<Term> genList = new ArrayList<>();
			for (int i = 0; i < bi.currentLength; i++) {
				genList.add(new VariableTerm());
			}
			Term listTerm = CompoundTerm.getList(genList);
			Term lengthTerm = IntegerTerm.get(bi.currentLength);

			// Try to unify with original arguments (args[0] and args[1])
			RC rc1 = interpreter.unify(args[0], listTerm);
			if (rc1 == RC.FAIL) {
				return RC.FAIL;
			}
			RC rc2 = interpreter.unify(args[1], lengthTerm);
			if (rc2 == RC.FAIL) {
				interpreter.undo(bi.startUndoPosition);
				return RC.FAIL;
			}

			// Increment for next backtrack (but keep same startUndoPosition)
			bi.currentLength++;
			interpreter.pushBacktrackInfo(bi);
			return RC.SUCCESS;
		}
	};

	/** append/3 - append(?HeadList, ?TailList, ?List) */
	public static final ExecuteOnlyCode APPEND = new ExecuteOnlyCode() {
		@Override
		public RC execute(final Interpreter interpreter, final boolean backtrackMode, final Term[] args) throws PrologException {
			assert (args.length == 3);
			Term headList = args[0];
			Term tailList = args[1];
			Term list = args[2];
			if (list instanceof VariableTerm) {
				return interpreter.unify(CompoundTerm.getList(headList, tailList), list);
			}
			if (backtrackMode) {
				AppendBacktrackInfo bi = (AppendBacktrackInfo) interpreter.popBacktrackInfo();
				interpreter.undo(bi.startUndoPosition);
				return nextSolution(interpreter, bi);
			} else {
				AppendBacktrackInfo bi = new AppendBacktrackInfo();
				bi.startUndoPosition = interpreter.getUndoPosition();
				bi.item = args[0];
				if (args[1] instanceof VariableTerm) {
					bi.list = new VariableTerm();
					bi.listExpand = true;
					bi.listDest = args[1];
				} else {
					bi.list = args[1];
				}
				return nextSolution(interpreter, bi);
			}
		}

		private RC nextSolution(final Interpreter interpreter, final AppendBacktrackInfo bi) throws PrologException {
			while (!TermConstants.emptyListAtom.equals(bi.list)) {
				if (bi.listExpand) {
					Term tmp = CompoundTerm.getList(bi.item, bi.list);
					interpreter.unify(bi.listDest, tmp);
					bi.item = new VariableTerm();
					bi.list = tmp;
				}
				Term head = ((CompoundTerm) bi.list).args[0].dereference();
				if (!bi.listExpand) {
					bi.list = ((CompoundTerm) bi.list).args[1].dereference();
				}
				if (bi.list instanceof VariableTerm) {
					bi.listDest = bi.list;
					bi.list = new VariableTerm();
					bi.listExpand = true;
				} else if (!CompoundTerm.isListPair(bi.list) && !TermConstants.emptyListAtom.equals(bi.list)) {
					return RC.FAIL;
				}
				if (interpreter.unify(bi.item, head) == RC.FAIL) {
					interpreter.undo(bi.startUndoPosition);
					continue;
				}
				interpreter.pushBacktrackInfo(bi);
				return RC.SUCCESS;
			}
			return interpreter.unify(bi.item, TermConstants.emptyListAtom);
		}
	};

	/** member/2 - member(?Elem, ?List) */
	public static final ExecuteOnlyCode MEMBER = new ExecuteOnlyCode() {
		@Override
		public RC execute(final Interpreter interpreter, final boolean backtrackMode, final Term[] args) throws PrologException {
			if (backtrackMode) {
				MemberBacktrackInfo bi = (MemberBacktrackInfo) interpreter.popBacktrackInfo();
				interpreter.undo(bi.startUndoPosition);
				return nextSolution(interpreter, bi);
			} else {
				MemberBacktrackInfo bi = new MemberBacktrackInfo();
				bi.startUndoPosition = interpreter.getUndoPosition();
				bi.item = args[0];
				if (args[1] instanceof VariableTerm) {
					bi.list = new VariableTerm();
					bi.listExpand = true;
					bi.listDest = args[1];
				} else {
					bi.list = args[1];
				}
				return nextSolution(interpreter, bi);
			}
		}

		private RC nextSolution(final Interpreter interpreter, final MemberBacktrackInfo bi) throws PrologException {
			while (!TermConstants.emptyListAtom.equals(bi.list)) {
				if (bi.listExpand) {
					Term tmp = CompoundTerm.getList(bi.item, bi.list);
					interpreter.unify(bi.listDest, tmp);
					bi.item = new VariableTerm();
					bi.list = tmp;
				}
				Term head = ((CompoundTerm) bi.list).args[0].dereference();
				if (!bi.listExpand) {
					bi.list = ((CompoundTerm) bi.list).args[1].dereference();
				}
				if (bi.list instanceof VariableTerm) {
					bi.listDest = bi.list;
					bi.list = new VariableTerm();
					bi.listExpand = true;
				} else if (!CompoundTerm.isListPair(bi.list) && !TermConstants.emptyListAtom.equals(bi.list)) {
					return RC.FAIL;
				}
				if (interpreter.unify(bi.item, head) == RC.FAIL) {
					interpreter.undo(bi.startUndoPosition);
					continue;
				}
				interpreter.pushBacktrackInfo(bi);
				return RC.SUCCESS;
			}
			return RC.FAIL;
		}
	};

	/** reverse/2 - reverse(?List, ?Reversed) */
	public static final ExecuteOnlyCode REVERSE = (interpreter, backtrackMode, args) -> {
		final var list = args[0].dereference();
		final var reversed = args[1].dereference();

		// Determine which direction to reverse based on which argument is bound
		final var source = !(list instanceof VariableTerm) ? list
			: !(reversed instanceof VariableTerm) ? reversed : null;
		final var target = source == list ? reversed : list;

		if (source == null) {
			PrologException.instantiationError(list);
			return ExecuteOnlyCode.RC.FAIL; // Never reached
		}

		final var elements = new ArrayList<Term>();
		Term current = source;
		while (!TermConstants.emptyListAtom.equals(current)) {
			if (current instanceof CompoundTerm ct && CompoundTerm.isListPair(ct)) {
				elements.add(ct.args[0].dereference());
				current = ct.args[1].dereference();
			} else {
				PrologException.typeError(TermConstants.listAtom, source);
			}
		}
		Collections.reverse(elements);
		return interpreter.unify(target, CompoundTerm.getList(elements));
	};

	/** nth/3 - nth(?N, ?List, ?Elem) - 1-indexed */
	public static final ExecuteOnlyCode NTH = (interpreter, backtrackMode, args) -> {
		final var nTerm = args[0].dereference();
		final var list = args[1].dereference();
		final var elem = args[2];

		if (!(nTerm instanceof IntegerTerm it)) {
			PrologException.typeError(TermConstants.integerAtom, nTerm);
			return ExecuteOnlyCode.RC.FAIL; // Never reached
		}

		if (it.value < 1) {
			return ExecuteOnlyCode.RC.FAIL;
		}

		Term current = list;
		for (int i = 1; i < it.value; i++) {
			if (current instanceof CompoundTerm ct && CompoundTerm.isListPair(ct)) {
				current = ct.args[1].dereference();
			} else {
				return ExecuteOnlyCode.RC.FAIL;
			}
		}

		if (current instanceof CompoundTerm ct && CompoundTerm.isListPair(ct)) {
			return interpreter.unify(elem, ct.args[0].dereference());
		}
		return ExecuteOnlyCode.RC.FAIL;
	};

	/** last/2 - last(?List, ?Last) */
	public static final ExecuteOnlyCode LAST = (interpreter, backtrackMode, args) -> {
		final var list = args[0].dereference();
		final var last = args[1];

		if (TermConstants.emptyListAtom.equals(list)) {
			return ExecuteOnlyCode.RC.FAIL;
		}

		Term current = list;
		Term result = null;
		while (!TermConstants.emptyListAtom.equals(current)) {
			if (current instanceof CompoundTerm ct && CompoundTerm.isListPair(ct)) {
				result = ct.args[0].dereference();
				current = ct.args[1].dereference();
			} else {
				PrologException.typeError(TermConstants.listAtom, list);
			}
		}

		return result != null ? interpreter.unify(last, result) : ExecuteOnlyCode.RC.FAIL;
	};

	/** msort/2 - sort list preserving duplicates */
	public static final ExecuteOnlyCode MSORT = (interpreter, backtrackMode, args) -> {
		Term list = args[0].dereference();
		Term sorted = args[1].dereference();

		// Check if list is a variable
		if (list instanceof VariableTerm) {
			PrologException.instantiationError(list);
		}

		// Check if sorted argument is valid (must be variable or list)
		if (!(sorted instanceof VariableTerm || TermConstants.emptyListAtom.equals(sorted) || CompoundTerm.isListPair(sorted))) {
			PrologException.typeError(TermConstants.listAtom, sorted);
		}

		List<Term> elements = new ArrayList<>();
		Term current = list;
		while (!TermConstants.emptyListAtom.equals(current)) {
			if (current instanceof VariableTerm) {
				PrologException.instantiationError(args[0]);
			}
			if (!CompoundTerm.isListPair(current)) {
				PrologException.typeError(TermConstants.listAtom, args[0]);
			}
			CompoundTerm ct = (CompoundTerm) current;
			Term element = ct.args[0].dereference();
			// Check if element is a variable (uninstantiated)
			if (element instanceof VariableTerm) {
				PrologException.instantiationError(args[0]);
			}
			elements.add(element);
			current = ct.args[1].dereference();
		}

		Collections.sort(elements, new gnu.prolog.term.TermComparator());
		return interpreter.unify(args[1], CompoundTerm.getList(elements));
	};

	/** sort/2 - sort list removing duplicates */
	public static final ExecuteOnlyCode SORT = (interpreter, backtrackMode, args) -> {
		Term list = args[0].dereference();
		Term sorted = args[1].dereference();

		// Check if list is a variable
		if (list instanceof VariableTerm) {
			PrologException.instantiationError(list);
		}

		// Check if sorted argument is valid (must be variable or list)
		if (!(sorted instanceof VariableTerm || TermConstants.emptyListAtom.equals(sorted) || CompoundTerm.isListPair(sorted))) {
			PrologException.typeError(TermConstants.listAtom, sorted);
		}

		List<Term> elements = new ArrayList<>();
		Term current = list;
		while (!TermConstants.emptyListAtom.equals(current)) {
			if (current instanceof VariableTerm) {
				PrologException.instantiationError(args[0]);
			}
			if (!CompoundTerm.isListPair(current)) {
				PrologException.typeError(TermConstants.listAtom, args[0]);
			}
			CompoundTerm ct = (CompoundTerm) current;
			Term element = ct.args[0].dereference();
			// Check if element is a variable (uninstantiated)
			if (element instanceof VariableTerm) {
				PrologException.instantiationError(args[0]);
			}
			elements.add(element);
			current = ct.args[1].dereference();
		}

		gnu.prolog.term.TermComparator comparator = new gnu.prolog.term.TermComparator();
		Collections.sort(elements, comparator);

		// Remove duplicates
		List<Term> unique = new ArrayList<>();
		Term prev = null;
		for (Term elem : elements) {
			if (prev == null || comparator.compare(prev, elem) != 0) {
				unique.add(elem);
				prev = elem;
			}
		}

		return interpreter.unify(args[1], CompoundTerm.getList(unique));
	};

	/** predsort/3 - sort list using a comparison predicate, removing duplicates */
	public static final ExecuteOnlyCode PREDSORT = new ExecuteOnlyCode() {
		@Override
		public RC execute(final Interpreter interpreter, final boolean backtrackMode, final Term[] args) throws PrologException {
			Term predTerm = args[0].dereference();
			Term list = args[1].dereference();
			Term sorted = args[2].dereference();

			// Check if list is a variable
			if (list instanceof VariableTerm) {
				PrologException.instantiationError(list);
			}

			// Check if sorted argument is valid (must be variable or list)
			if (!(sorted instanceof VariableTerm || TermConstants.emptyListAtom.equals(sorted) || CompoundTerm.isListPair(sorted))) {
				PrologException.typeError(TermConstants.listAtom, sorted);
			}

			// Collect elements from input list
			List<Term> elements = new ArrayList<>();
			Term current = list;
			while (!TermConstants.emptyListAtom.equals(current)) {
				if (current instanceof VariableTerm) {
					PrologException.instantiationError(args[1]);
				}
				if (!CompoundTerm.isListPair(current)) {
					PrologException.typeError(TermConstants.listAtom, args[1]);
				}
				CompoundTerm ct = (CompoundTerm) current;
				Term element = ct.args[0].dereference();
				// Check if element is a variable (uninstantiated)
				if (element instanceof VariableTerm) {
					PrologException.instantiationError(args[1]);
				}
				elements.add(element);
				current = ct.args[1].dereference();
			}

			// Sort using the provided predicate
			try {
				elements.sort((t1, t2) -> {
					try {
						return compareWithPredicate(interpreter, predTerm, t1, t2);
					} catch (PrologException e) {
						throw new SortException(e);
					}
				});
			} catch (SortException ex) {
				throw ex.error;
			}

			// Remove duplicates (elements that compare as =)
			List<Term> unique = new ArrayList<>();
			Term prev = null;
			for (Term elem : elements) {
				if (prev == null) {
					unique.add(elem);
					prev = elem;
				} else {
					try {
						int cmp = compareWithPredicate(interpreter, predTerm, prev, elem);
						if (cmp != 0) {
							unique.add(elem);
							prev = elem;
						}
					} catch (PrologException e) {
						throw e;
					}
				}
			}

			return interpreter.unify(args[2], CompoundTerm.getList(unique));
		}
	};

	private static int compareWithPredicate(final Interpreter interpreter, final Term predTerm, final Term left,
			final Term right) throws PrologException
	{
		Term pred = predTerm.dereference();
		if (pred instanceof VariableTerm)
		{
			PrologException.instantiationError(pred);
		}

		VariableTerm orderVar = new VariableTerm();
		Term goal;

			if (pred instanceof AtomTerm predAtom)
			{
				CompoundTermTag tag = CompoundTermTag.get(predAtom, 3);
				goal = new CompoundTerm(tag, orderVar, left, right);
			}
		else
		{
			PrologException.typeError(TermConstants.callableAtom, pred);
			return 0;
		}

		int undoPos = interpreter.getUndoPosition();
		BacktrackInfo startBi = interpreter.peekBacktrackInfo();
		PrologCode.RC rc;
		Term order;
		try
		{
			rc = Call.staticExecute(interpreter, false, goal);
			if (rc == PrologCode.RC.FAIL)
			{
				PrologException.domainError(TermConstants.orderAtom, TermConstants.failAtom);
			}
			order = orderVar.dereference();
		}
		finally
		{
			interpreter.popBacktrackInfoUntil(startBi);
			interpreter.undo(undoPos);
		}

			if (order instanceof AtomTerm orderAtom)
			{
				return switch (orderAtom.value)
				{
				case "<" -> -1;
				case "=" -> 0;
				case ">" -> 1;
				default -> {
					PrologException.domainError(TermConstants.orderAtom, order);
					yield 0;
				}
			};
		}
		PrologException.domainError(TermConstants.orderAtom, order);
		return 0;
	}

	// ============================================================================
	// BacktrackInfo Classes
	// ============================================================================

	private static class AppendBacktrackInfo extends BacktrackInfo {
		Term item;
		Term list;
		boolean listExpand;
		Term listDest;
		int startUndoPosition;

		AppendBacktrackInfo() {
			super(-1, -1);
		}
	}

	private static class MemberBacktrackInfo extends BacktrackInfo {
		Term item;
		Term list;
		boolean listExpand;
		Term listDest;
		int startUndoPosition;

		MemberBacktrackInfo() {
			super(-1, -1);
		}
	}

	private static class LengthBacktrackInfo extends BacktrackInfo {
		int currentLength;
		int startUndoPosition;
		static final int MAX_LENGTH = Integer.getInteger("gnu.prolog.length.max", 3);  // Prevent infinite generation

		LengthBacktrackInfo() {
			super(-1, -1);
		}
	}
}
