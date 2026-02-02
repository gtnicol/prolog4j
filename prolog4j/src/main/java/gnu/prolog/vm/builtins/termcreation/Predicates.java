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
package gnu.prolog.vm.builtins.termcreation;

import gnu.prolog.term.*;
import gnu.prolog.vm.ExecuteOnlyCode;
import gnu.prolog.vm.Interpreter;
import gnu.prolog.vm.PrologException;
import gnu.prolog.vm.TermConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory class for term creation predicates (ISO Prolog 8.5).
 * Provides implementations for functor/3, arg/3, =../2, copy_term/2.
 */
public final class Predicates {

	private Predicates() {
		throw new AssertionError("Utility class should not be instantiated");
	}

	private static final IntegerTerm ZERO = IntegerTerm.get(0);
	private static final Term[] TERM_ARRAY_TYPE = new Term[0];

	/** functor/3 - functor(+Term, ?Name, ?Arity) */
	public static final ExecuteOnlyCode FUNCTOR = (interpreter, backtrackMode, args) -> {
		int undoPos = interpreter.getUndoPosition();
		Term term = args[0];
		Term name = args[1];
		Term arity = args[2];
		ExecuteOnlyCode.RC rc;

		return switch (term) {
			case AtomicTerm at -> {
				rc = interpreter.unify(term, name);
				if (rc == ExecuteOnlyCode.RC.FAIL) {
					interpreter.undo(undoPos);
					yield ExecuteOnlyCode.RC.FAIL;
				}
				rc = interpreter.unify(arity, ZERO);
				if (rc == ExecuteOnlyCode.RC.FAIL) {
					interpreter.undo(undoPos);
					yield ExecuteOnlyCode.RC.FAIL;
				}
				yield ExecuteOnlyCode.RC.SUCCESS_LAST;
			}
			case CompoundTerm ct -> {
				CompoundTermTag tag = ct.tag;
				IntegerTerm tarity = IntegerTerm.get(tag.arity);
				rc = interpreter.unify(tag.functor, name);
				if (rc == ExecuteOnlyCode.RC.FAIL) {
					interpreter.undo(undoPos);
					yield ExecuteOnlyCode.RC.FAIL;
				}
				rc = interpreter.unify(tarity, arity);
				if (rc == ExecuteOnlyCode.RC.FAIL) {
					interpreter.undo(undoPos);
					yield ExecuteOnlyCode.RC.FAIL;
				}
				yield ExecuteOnlyCode.RC.SUCCESS_LAST;
			}
			case VariableTerm vt -> {
				switch (arity) {
					case VariableTerm av -> {
						PrologException.instantiationError(arity);
					}
					default -> {}
				}
				switch (name) {
					case VariableTerm nv -> {
						PrologException.instantiationError(name);
					}
					default -> {}
				}
				switch (name) {
					case AtomicTerm at -> {}
					default -> {
						PrologException.typeError(TermConstants.atomicAtom, name);
					}
				}
				IntegerTerm iarity = switch (arity) {
					case IntegerTerm it -> it;
					default -> {
						PrologException.typeError(TermConstants.integerAtom, arity);
						yield null; // Never reached
					}
				};
				if (iarity.value > 0) {
					AtomTerm functor = switch (name) {
						case AtomTerm at -> at;
						default -> {
							PrologException.typeError(TermConstants.atomAtom, name);
							yield null; // Never reached
						}
					};
					int n = iarity.value;
					// check that we can make something that big
					IntegerTerm maxArityTerm = (IntegerTerm) interpreter.getEnvironment()
						.getPrologFlag(TermConstants.maxArityAtom);
					if (n > maxArityTerm.value) {
						PrologException.representationError(TermConstants.maxArityAtom);
					}

					Term[] targs = new Term[n];
					for (int i = 0; i < n; i++) {
						targs[i] = new VariableTerm();
					}
					rc = interpreter.unify(term, new CompoundTerm(functor, targs));
					if (rc == ExecuteOnlyCode.RC.FAIL) {
						interpreter.undo(undoPos);
						yield ExecuteOnlyCode.RC.FAIL;
					}
					yield ExecuteOnlyCode.RC.SUCCESS_LAST;
				}
				if (iarity.value < 0) {
					PrologException.domainError(TermConstants.notLessThanZeroAtom, arity);
				}
				rc = interpreter.unify(term, name);
				if (rc == ExecuteOnlyCode.RC.FAIL) {
					interpreter.undo(undoPos);
					yield ExecuteOnlyCode.RC.FAIL;
				}
				yield ExecuteOnlyCode.RC.SUCCESS_LAST;
			}
			default -> ExecuteOnlyCode.RC.FAIL; // unreachable but required for exhaustiveness
		};
	};

	/** arg/3 - arg(+N, +Term, ?Arg) */
	public static final ExecuteOnlyCode ARG = (interpreter, backtrackMode, args) -> {
		Term n = args[0];
		Term term = args[1];
		Term arg = args[2];

		IntegerTerm in = switch (n) {
			case VariableTerm vt -> {
				PrologException.instantiationError(n);
				yield null; // Never reached
			}
			case IntegerTerm it -> it;
			default -> {
				PrologException.typeError(TermConstants.integerAtom, n);
				yield null; // Never reached
			}
		};

		// ISO Prolog: Check term is compound before checking N validity
		CompoundTerm ct = switch (term) {
			case VariableTerm vt -> {
				PrologException.instantiationError(term);
				yield null; // Never reached
			}
			case CompoundTerm cterm -> cterm;
			default -> {
				PrologException.typeError(TermConstants.compoundAtom, term);
				yield null; // Never reached
			}
		};

		// ISO Prolog: Check N is not less than zero
		if (in.value < 0) {
			PrologException.domainError(TermConstants.notLessThanZeroAtom, n);
		}

		// ISO Prolog: arg/3 fails for N=0 or N > arity
		if (in.value == 0 || ct.tag.arity < in.value) {
			return ExecuteOnlyCode.RC.FAIL;
		}
		return interpreter.unify(ct.args[in.value - 1].dereference(), arg);
	};

	/** copy_term/2 - copy_term(+Term, ?Copy) */
	public static final ExecuteOnlyCode COPY_TERM = (interpreter, backtrackMode, args) ->
		interpreter.unify((Term) args[0].clone(), args[1]);

	/** term_variables/2 - term_variables(+Term, ?Vars) */
	public static final ExecuteOnlyCode TERM_VARIABLES = (interpreter, backtrackMode, args) -> {
		Term term = args[0];
		Term vars = args[1];
		List<VariableTerm> varList = new ArrayList<>();
		collectVariables(term, varList);

		// Build Prolog list from variables
		Term resultList = TermConstants.emptyListAtom;
		for (int i = varList.size() - 1; i >= 0; i--) {
			resultList = CompoundTerm.getList(varList.get(i), resultList);
		}

		return interpreter.unify(resultList, vars);
	};

	/**
	 * Helper method to collect all variables in a term.
	 * Variables are collected in left-to-right order, with duplicates included only once.
	 */
	private static void collectVariables(final Term term, final List<VariableTerm> vars) {
		Term t = term.dereference();
		switch (t) {
			case VariableTerm vt -> {
				if (!vars.contains(vt)) {
					vars.add(vt);
				}
			}
			case CompoundTerm ct -> {
				for (Term arg : ct.args) {
					collectVariables(arg, vars);
				}
			}
			default -> {} // Atoms, numbers, etc. have no variables
		}
	}

	/** =../2 - univ: Term =.. List */
	public static final ExecuteOnlyCode UNIV = (interpreter, backtrackMode, args) -> {
		int undoPos = interpreter.getUndoPosition();
		Term term = args[0];
		Term list = args[1];

		return switch (term) {
			case AtomicTerm at -> {
				checkList(list, false);
				switch (list) {
					case VariableTerm lvar -> {
						interpreter.addVariableUndo(lvar);
						lvar.value = CompoundTerm.getList(term, TermConstants.emptyListAtom);
						yield ExecuteOnlyCode.RC.SUCCESS_LAST;
					}
					case AtomTerm atl when atl == TermConstants.emptyListAtom -> {
						yield ExecuteOnlyCode.RC.FAIL;
					}
					case CompoundTerm ct -> {
						Term head = ct.args[0].dereference();
						Term tail = ct.args[1].dereference();
						switch (head) {
							case CompoundTerm h -> {
								PrologException.typeError(TermConstants.atomicAtom, head);
							}
							default -> {}
						}
						Term t = CompoundTerm.getList(term, TermConstants.emptyListAtom);
						yield interpreter.unify(t, list);
					}
					default -> {
						yield ExecuteOnlyCode.RC.FAIL;
					}
				}
			}
			case CompoundTerm ct -> {
				checkList(list, false);
				CompoundTermTag tag = ct.tag;
				AtomTerm functor = tag.functor;
				Term tmp = TermConstants.emptyListAtom;
				Term[] targs = ct.args;
				for (int i = tag.arity - 1; i >= 0; i--) {
					tmp = CompoundTerm.getList(targs[i].dereference(), tmp);
				}
				tmp = CompoundTerm.getList(functor, tmp);
				ExecuteOnlyCode.RC rc = interpreter.unify(tmp, list);
				if (rc == ExecuteOnlyCode.RC.FAIL) {
					interpreter.undo(undoPos);
				}
				yield rc;
			}
			case VariableTerm vt -> {
				checkList(list, true);
				if (list == TermConstants.emptyListAtom) {
					PrologException.domainError(TermConstants.nonEmptyListAtom, list);
				}
				CompoundTerm ct = (CompoundTerm) list;
				if (ct.tag != TermConstants.listTag) {
					PrologException.typeError(TermConstants.listAtom, list);
				}
				Term head = ct.args[0].dereference();
				Term tail = ct.args[1].dereference();
				switch (head) {
					case VariableTerm hv -> {
						PrologException.instantiationError(head);
					}
					default -> {}
				}
				if (tail == TermConstants.emptyListAtom) {
					interpreter.addVariableUndo(vt);
					vt.value = head;
					yield ExecuteOnlyCode.RC.SUCCESS_LAST;
				}
				AtomTerm functor = switch (head) {
					case AtomTerm at -> at;
					default -> {
						PrologException.typeError(TermConstants.atomAtom, head);
						yield null; // Never reached
					}
				};
				List<Term> argList = new ArrayList<>();
				do {
					ct = (CompoundTerm) tail;
					head = ct.args[0].dereference();
					tail = ct.args[1].dereference();
					argList.add(head);
				} while (tail != TermConstants.emptyListAtom);
				Term[] targsArr = argList.toArray(TERM_ARRAY_TYPE);
				interpreter.addVariableUndo(vt);
				vt.value = new CompoundTerm(functor, targsArr);
				yield ExecuteOnlyCode.RC.SUCCESS_LAST;
			}
			default -> ExecuteOnlyCode.RC.FAIL; // unreachable
		};
	};

	/**
	 * Helper method to validate list structure.
	 *
	 * @param list the list term to check
	 * @param nonPartial if true, require fully instantiated list
	 * @throws PrologException if list is invalid
	 */
	private static void checkList(final Term list, final boolean nonPartial) throws PrologException {
		Term exArg = list;

		if (list == TermConstants.emptyListAtom) {
			return;
		}
		switch (list) {
			case VariableTerm vt -> {
				if (nonPartial) {
					PrologException.instantiationError(list);
				} else {
					return;
				}
			}
			case CompoundTerm ct -> {
				if (ct.tag != TermConstants.listTag) {
					PrologException.typeError(TermConstants.listAtom, exArg);
				}
			}
			default -> {
				PrologException.typeError(TermConstants.listAtom, exArg);
			}
		}

		CompoundTerm ct = (CompoundTerm) list;
		Term head = ct.args[0].dereference();
		Term tail = ct.args[1].dereference();
		if (tail == TermConstants.emptyListAtom) {
			switch (head) {
				case CompoundTerm h -> {
					PrologException.typeError(TermConstants.atomicAtom, head);
				}
				default -> {}
			}
			return;
		} else {
			switch (head) {
				case VariableTerm vt -> {}
				case AtomicTerm at -> {}
				default -> {
					PrologException.typeError(TermConstants.atomAtom, head);
				}
			}
		}

		Term listIter = tail;
		while (true) {
			if (listIter == TermConstants.emptyListAtom) {
				return;
			}
			switch (listIter) {
				case VariableTerm vt -> {
					if (nonPartial) {
						PrologException.instantiationError();
					} else {
						return;
					}
				}
				case CompoundTerm c -> {
					ct = c;
					if (ct.tag != TermConstants.listTag) {
						PrologException.typeError(TermConstants.listAtom, exArg);
					}
				}
				default -> {
					PrologException.typeError(TermConstants.listAtom, exArg);
				}
			}
			listIter = ct.args[1].dereference();
		}
	}
}
