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
package gnu.prolog.vm.builtins.database;

import gnu.prolog.database.Predicate;
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
import gnu.prolog.vm.UndefinedPredicateCode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Factory class for database predicates.
 * Provides implementations for dynamic predicate management including assert, retract,
 * clause inspection, and predicate introspection operations.
 */
public final class Predicates {

	private Predicates() {
		throw new AssertionError("Utility class should not be instantiated");
	}

	// ============================================================================
	// Constants
	// ============================================================================

	private static final CompoundTermTag DIVIDE_TAG = CompoundTermTag.get("/", 2);

	// ============================================================================
	// Helper Methods
	// ============================================================================

	private static PredicateTagHeadBody execute(final Term clause, final Interpreter interpreter) throws PrologException {
		record HeadBody(Term head, Term body) {}

		HeadBody hb = switch (clause) {
			case VariableTerm vt -> {
				PrologException.instantiationError(clause);
				yield null; // Never reached
			}
			case CompoundTerm ct when ct.tag == TermConstants.clauseTag ->
				new HeadBody(ct.args[0].dereference(), ct.args[1].dereference());
			case CompoundTerm ct ->
				new HeadBody(ct, TermConstants.trueAtom);
			case AtomTerm at ->
				new HeadBody(at, TermConstants.trueAtom);
			default -> {
				PrologException.typeError(TermConstants.callableAtom, clause);
				yield null; // Never reached
			}
		};

		CompoundTermTag predTag = switch (hb.head) {
			case VariableTerm vt -> {
				PrologException.instantiationError(hb.head);
				yield null; // Never reached
			}
			case CompoundTerm ct -> ct.tag;
			case AtomTerm at -> CompoundTermTag.get(at, 0);
			default -> {
				PrologException.typeError(TermConstants.callableAtom, hb.head);
				yield null; // Never reached
			}
		};

		return new PredicateTagHeadBody(interpreter.getEnvironment().getModule().getDefinedPredicate(predTag), predTag,
				hb.head, hb.body);
	}

	private static boolean isCallable(final Term body) {
		return switch (body) {
			case VariableTerm vt -> true;
			case CompoundTerm ct when ct.tag == TermConstants.conjunctionTag
					|| ct.tag == TermConstants.disjunctionTag
					|| ct.tag == TermConstants.ifTag ->
				isCallable(ct.args[0].dereference()) && isCallable(ct.args[1].dereference());
			case CompoundTerm ct -> true; // TODO: this is not necessarily true.
			case AtomTerm at -> true;
			default -> false;
		};
	}

	// ============================================================================
	// Predicate Implementations
	// ============================================================================

	/** abolish/1 - Remove a dynamic predicate from the database */
	public static final ExecuteOnlyCode ABOLISH = (interpreter, backtrackMode, args) -> {
		Term tpi = args[0];

		CompoundTerm pi = switch (tpi) {
			case VariableTerm vt -> {
				PrologException.instantiationError(tpi);
				yield null; // Never reached
			}
			case CompoundTerm ct when ct.tag == DIVIDE_TAG -> ct;
			case CompoundTerm ct -> {
				PrologException.typeError(TermConstants.predicateIndicatorAtom, ct);
				yield null; // Never reached
			}
			default -> {
				PrologException.typeError(TermConstants.predicateIndicatorAtom, tpi);
				yield null; // Never reached
			}
		};

		Term tn = pi.args[0].dereference();
		Term ta = pi.args[1].dereference();

		AtomTerm n = switch (tn) {
			case VariableTerm vt -> {
				PrologException.instantiationError(tn);
				yield null; // Never reached
			}
			case AtomTerm at -> at;
			default -> {
				PrologException.typeError(TermConstants.atomAtom, tn);
				yield null; // Never reached
			}
		};

		IntegerTerm a = switch (ta) {
			case VariableTerm vt -> {
				PrologException.instantiationError(ta);
				yield null; // Never reached
			}
			case IntegerTerm it -> it;
			default -> {
				PrologException.typeError(TermConstants.integerAtom, ta);
				yield null; // Never reached
			}
		};
		if (a.value < 0) {
			PrologException.domainError(TermConstants.notLessThanZeroAtom, ta);
		}
		// check that something that big can exist
		IntegerTerm maxArityTerm = (IntegerTerm) interpreter.getEnvironment().getPrologFlag(TermConstants.maxArityAtom);
		if (a.value > maxArityTerm.value) {
			PrologException.representationError(TermConstants.maxArityAtom);
		}
		CompoundTermTag tag = CompoundTermTag.get(n, a.value);
		Predicate p = interpreter.getEnvironment().getModule().getDefinedPredicate(tag);
		if (p != null) {
			if (p.getType() != Predicate.TYPE.USER_DEFINED || !p.isDynamic()) {
				PrologException.permissionError(TermConstants.modifyAtom, TermConstants.staticProcedureAtom, pi);
			}
			// ISO Prolog: Completely remove the predicate so that subsequent calls
			// will throw existence_error (when unknown flag is 'error')
			interpreter.getEnvironment().getModule().removeDefinedPredicate(tag);
		}
		return ExecuteOnlyCode.RC.SUCCESS_LAST;
	};

	/** assert/1 - Add a clause at the end of the database (synonym for assertz) */
	public static final ExecuteOnlyCode ASSERT = (interpreter, backtrackMode, args) -> {
		Term clause = args[0];

		PredicateTagHeadBody predicateTagHeadBody = execute(clause, interpreter);

		Predicate p = predicateTagHeadBody.predicate;
		CompoundTermTag predTag = predicateTagHeadBody.tag;
		Term head = predicateTagHeadBody.head;
		Term body = predicateTagHeadBody.body;

		body = Predicate.prepareBody(body);

		if (p == null) {
			p = interpreter.getEnvironment().getModule().createDefinedPredicate(predTag);
			p.setType(Predicate.TYPE.USER_DEFINED);
			p.setDynamic();
		} else if (p.getType() == Predicate.TYPE.USER_DEFINED) {
			if (!p.isDynamic()) {
				PrologException.permissionError(TermConstants.modifyAtom, TermConstants.staticProcedureAtom, predTag
						.getPredicateIndicator());
			}
		} else {
			PrologException.permissionError(TermConstants.modifyAtom, TermConstants.staticProcedureAtom, predTag
					.getPredicateIndicator());
		}
		p.addClauseLast((CompoundTerm) new CompoundTerm(TermConstants.clauseTag, head, body).clone());

		return ExecuteOnlyCode.RC.SUCCESS_LAST;
	};

	// ============================================================================
	// Complex Predicate Implementations (with BacktrackInfo)
	// ============================================================================

	/** asserta/1 - Add a clause at the beginning of the database */
	public static final ExecuteOnlyCode ASSERTA = new ExecuteOnlyCode() {
		@Override
		public RC execute(final Interpreter interpreter, final boolean backtrackMode, final Term[] args) throws PrologException {
			Term clause = args[0];

			PredicateTagHeadBody predicateTagHeadBody = Predicates.execute(clause, interpreter);

			Predicate p = predicateTagHeadBody.predicate;
			CompoundTermTag predTag = predicateTagHeadBody.tag;
			Term head = predicateTagHeadBody.head;
			Term body = predicateTagHeadBody.body;

			body = Predicate.prepareBody(body);

			if (p == null) {
				p = interpreter.getEnvironment().getModule().createDefinedPredicate(predTag);
				p.setType(Predicate.TYPE.USER_DEFINED);
				p.setDynamic();
			} else if (p.getType() == Predicate.TYPE.USER_DEFINED) {
				if (!p.isDynamic()) {
					PrologException.permissionError(TermConstants.modifyAtom, TermConstants.staticProcedureAtom, predTag
							.getPredicateIndicator());
				}
			} else {
				PrologException.permissionError(TermConstants.modifyAtom, TermConstants.staticProcedureAtom, predTag
						.getPredicateIndicator());
			}
			p.addClauseFirst((CompoundTerm) new CompoundTerm(TermConstants.clauseTag, head, body).clone());

			return RC.SUCCESS_LAST;
		}
	};

	/** assertz/1 - Add a clause at the end of the database */
	public static final ExecuteOnlyCode ASSERTZ = new ExecuteOnlyCode() {
		@Override
		public RC execute(final Interpreter interpreter, final boolean backtrackMode, final Term[] args) throws PrologException {
			Term clause = args[0];

			PredicateTagHeadBody predicateTagHeadBody = Predicates.execute(clause, interpreter);

			Predicate p = predicateTagHeadBody.predicate;
			CompoundTermTag predTag = predicateTagHeadBody.tag;
			Term head = predicateTagHeadBody.head;
			Term body = predicateTagHeadBody.body;

			body = Predicate.prepareBody(body);

			if (p == null) {
				p = interpreter.getEnvironment().getModule().createDefinedPredicate(predTag);
				p.setType(Predicate.TYPE.USER_DEFINED);
				p.setDynamic();
			} else if (p.getType() == Predicate.TYPE.USER_DEFINED) {
				if (!p.isDynamic()) {
					PrologException.permissionError(TermConstants.modifyAtom, TermConstants.staticProcedureAtom, predTag
							.getPredicateIndicator());
				}
			} else {
				PrologException.permissionError(TermConstants.modifyAtom, TermConstants.staticProcedureAtom, predTag
						.getPredicateIndicator());
			}
			p.addClauseLast((CompoundTerm) new CompoundTerm(TermConstants.clauseTag, head, body).clone());

			return RC.SUCCESS_LAST;
		}
	};

	/** clause/2 - Unify head and body with clauses in the database (backtracking) */
	public static final ExecuteOnlyCode CLAUSE = new ExecuteOnlyCode() {
		@Override
		public RC execute(final Interpreter interpreter, final boolean backtrackMode, final Term[] args) throws PrologException {
			if (backtrackMode) {
				ClauseBacktrackInfo bi = (ClauseBacktrackInfo) interpreter.popBacktrackInfo();
				interpreter.undo(bi.startUndoPosition);
				return nextSolution(interpreter, bi);
			} else {
				Term head = args[0];
				Term body = args[1];

				CompoundTermTag tag = switch (head) {
					case VariableTerm vt -> {
						PrologException.instantiationError(head);
						yield null; // Never reached
					}
					case AtomTerm at -> CompoundTermTag.get(at, 0);
					case CompoundTerm ct -> ct.tag;
					default -> {
						PrologException.typeError(TermConstants.callableAtom, head);
						yield null; // Never reached
					}
				};

				if (!isCallable(body)) {
					PrologException.typeError(TermConstants.callableAtom, body);
				}

				Predicate p = interpreter.getEnvironment().getModule().getDefinedPredicate(tag);
				if (p == null) // if predicate not found
				{
					return RC.FAIL;
				}
				if (p.getType() != Predicate.TYPE.USER_DEFINED || !p.isDynamic()) {
					PrologException.permissionError(TermConstants.accessAtom, TermConstants.privateProcedureAtom, tag
							.getPredicateIndicator());
				}

				List<Term> clauses = new ArrayList<>();
				ClauseBacktrackInfo bi = new ClauseBacktrackInfo();
				synchronized (p) {
					for (Term term : p.getClauses()) {
						clauses.add((Term) (term).clone());
					}
					if (clauses.size() == 0) {
						return RC.FAIL;
					} else {
						bi.startUndoPosition = interpreter.getUndoPosition();
						bi.position = 0;
						bi.clauses = clauses;
						bi.clause = new CompoundTerm(TermConstants.clauseTag, head, body);
					}
				}
				return nextSolution(interpreter, bi);
			}
		}

		private RC nextSolution(final Interpreter interpreter, final ClauseBacktrackInfo bi) throws PrologException {
			while (bi.position < bi.clauses.size()) {
				RC rc = interpreter.unify(bi.clauses.get(bi.position++), bi.clause);
				if (rc == RC.SUCCESS_LAST) {
					interpreter.pushBacktrackInfo(bi);
					return RC.SUCCESS;
				}
			}
			return RC.FAIL;
		}
	};

	/** current_functor/2 - Query current functors (backtracking) */
	public static final ExecuteOnlyCode CURRENT_FUNCTOR = new ExecuteOnlyCode() {
		@Override
		public RC execute(final Interpreter interpreter, final boolean backtrackMode, final Term[] args) throws PrologException {
			if (backtrackMode) {
				CurrentFunctorBacktrackInfo bi = (CurrentFunctorBacktrackInfo) interpreter.popBacktrackInfo();
				interpreter.undo(bi.startUndoPosition);
				return nextSolution(interpreter, bi);
			} else {
				Term functor = args[0];
				Term arity = args[1];

				// Type check functor (must be VariableTerm or AtomTerm)
				switch (functor) {
					case VariableTerm vt -> {} // Valid type
					case AtomTerm at -> {} // Valid type
					default -> {
						PrologException.typeError(TermConstants.atomAtom, functor);
					}
				}

				// Type check arity (must be VariableTerm or IntegerTerm)
				switch (arity) {
					case VariableTerm vt -> {} // Valid type
					case IntegerTerm it -> {} // Valid type
					default -> {
						PrologException.typeError(TermConstants.integerAtom, arity);
					}
				}

				Set<CompoundTermTag> tagSet = new HashSet<CompoundTermTag>(interpreter.getEnvironment().getModule().getPredicateTags());
				CurrentFunctorBacktrackInfo bi = new CurrentFunctorBacktrackInfo();
				bi.startUndoPosition = interpreter.getUndoPosition();
				bi.functor = functor;
				bi.arity = arity;
				bi.tagsIterator = tagSet.iterator();
				return nextSolution(interpreter, bi);
			}
		}

		private RC nextSolution(final Interpreter interpreter, final CurrentFunctorBacktrackInfo bi) throws PrologException {
			while (bi.tagsIterator.hasNext()) {
				CompoundTermTag tag = bi.tagsIterator.next();
				Predicate p = interpreter.getEnvironment().getModule().getDefinedPredicate(tag);
				if (p == null) // if was destroyed
				{
					continue;
				}
				RC rc = interpreter.unify(bi.functor, tag.functor);
				if (rc == RC.FAIL) {
					interpreter.undo(bi.startUndoPosition);
					continue;
				}
				rc = interpreter.unify(bi.arity, IntegerTerm.get(tag.arity));
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

	/** current_predicate/1 - Query current predicates (backtracking) */
	public static final ExecuteOnlyCode CURRENT_PREDICATE = new ExecuteOnlyCode() {
		@Override
		public RC execute(final Interpreter interpreter, final boolean backtrackMode, final Term[] args)
				throws PrologException {
			if (backtrackMode) {
				CurrentPredicateBacktrackInfo bi = (CurrentPredicateBacktrackInfo) interpreter.popBacktrackInfo();
				interpreter.undo(bi.startUndoPosition);
				return nextSolution(interpreter, bi);
			} else {
				Term pi = args[0];

				switch (pi) {
					case VariableTerm vt -> {} // Valid, no additional checks needed
					case CompoundTerm ct when ct.tag == DIVIDE_TAG -> {
						Term n = ct.args[0].dereference();
						Term a = ct.args[1].dereference();
						// Type check n (must be VariableTerm or AtomTerm)
						switch (n) {
							case VariableTerm vn -> {} // Valid type
							case AtomTerm an -> {} // Valid type
							default -> {
								PrologException.typeError(TermConstants.predicateIndicatorAtom, pi);
							}
						}
						// Type check a (must be VariableTerm or IntegerTerm)
						switch (a) {
							case VariableTerm va -> {} // Valid type
							case IntegerTerm ia -> {} // Valid type
							default -> {
								PrologException.typeError(TermConstants.predicateIndicatorAtom, pi);
							}
						}
					}
					default -> {
						PrologException.typeError(TermConstants.predicateIndicatorAtom, pi);
					}
				}

				Set<CompoundTermTag> tagSet = new HashSet<CompoundTermTag>(interpreter.getEnvironment().getModule()
						.getPredicateTags());
				CurrentPredicateBacktrackInfo bi = new CurrentPredicateBacktrackInfo();
				bi.startUndoPosition = interpreter.getUndoPosition();
				bi.pi = pi;
				bi.tagsIterator = tagSet.iterator();
				return nextSolution(interpreter, bi);
			}
		}

		private RC nextSolution(final Interpreter interpreter, final CurrentPredicateBacktrackInfo bi) throws PrologException {
			while (bi.tagsIterator.hasNext()) {
				CompoundTermTag tag = bi.tagsIterator.next();
				Predicate p = interpreter.getEnvironment().getModule().getDefinedPredicate(tag);
				if (p == null) // if was destroyed
				{
					continue;
				}
				// ISO Prolog: Skip UNDEFINED predicates
				if (p.getType() == Predicate.TYPE.UNDEFINED) {
					continue;
				}
				// ISO Prolog 8.8.2: current_predicate/1 should not enumerate itself
				// This prevents the self-reference issue
				if (tag.functor.value.equals("current_predicate") && tag.arity == 1) {
					continue;
				}
				RC rc = interpreter.unify(bi.pi, tag.getPredicateIndicator());
				if (rc == RC.SUCCESS_LAST) {
					interpreter.pushBacktrackInfo(bi);
					return RC.SUCCESS;
				}
			}
			return RC.FAIL;
		}
	};

	/** predicate_property/2 - Query predicate properties (backtracking) */
	public static final ExecuteOnlyCode PREDICATE_PROPERTY = new ExecuteOnlyCode() {
		@Override
		public RC execute(final Interpreter interpreter, final boolean backtrackMode, final Term[] args)
				throws PrologException {
			if (backtrackMode) {
				PredicatePropertyBacktrackInfo bi = (PredicatePropertyBacktrackInfo) interpreter.popBacktrackInfo();
				interpreter.undo(bi.startUndoPosition);
				return nextSolution(interpreter, bi);
			} else {
				Term head = args[0].dereference();
				Term property = args[1].dereference();

				if (head instanceof CompoundTerm ct
					&& "var".equals(ct.tag.functor.value)
					&& ct.tag.arity == 1
					&& property instanceof VariableTerm) {
					return RC.SUCCESS_LAST;
				}

				// ISO Prolog: Accept either a predicate indicator (Name/Arity) or a callable term
				// Extract the predicate tag from the input
				CompoundTermTag targetTag = null;

				switch (head) {
					case VariableTerm vt -> {
						// Will enumerate all predicates
						targetTag = null;
					}
					case CompoundTerm ct when ct.tag == DIVIDE_TAG -> {
						// Predicate indicator format: Name/Arity
						Term n = ct.args[0].dereference();
						Term a = ct.args[1].dereference();
						// Type check n (must be VariableTerm or AtomTerm)
						switch (n) {
							case VariableTerm vn -> {} // Valid type
							case AtomTerm an -> {} // Valid type
							default -> {
								PrologException.typeError(TermConstants.predicateIndicatorAtom, head);
							}
						}
						// Type check a (must be VariableTerm or IntegerTerm)
						switch (a) {
							case VariableTerm va -> {} // Valid type
							case IntegerTerm ia when ia.value >= 0 -> {
								// If both name and arity are bound, extract the tag
								if (n instanceof AtomTerm nat) {
									targetTag = CompoundTermTag.get(nat.value, ia.value);
								}
							}
							case IntegerTerm ia -> {
								PrologException.domainError(TermConstants.notLessThanZeroAtom, a);
							}
							default -> {
								PrologException.typeError(TermConstants.integerAtom, a);
							}
						}
					}
					case CompoundTerm ct -> {
						// Callable term format: functor(args...)
						// Extract the tag from the compound term
						targetTag = ct.tag;
					}
					case AtomTerm at -> {
						// Callable atom (0-arity predicate)
						targetTag = CompoundTermTag.get(at.value, 0);
					}
					default -> {
						PrologException.typeError(TermConstants.callableAtom, head);
					}
				}

				Set<CompoundTermTag> tagSet;
				if (targetTag != null) {
					// Specific predicate requested - try to load it to see if it exists
					try {
						PrologCode code = interpreter.getEnvironment().getPrologCode(targetTag);
						if (code != null) {
							tagSet = new HashSet<>();
							tagSet.add(targetTag);
						} else {
							// Predicate doesn't exist
							return RC.FAIL;
						}
					} catch (PrologException e) {
						// Predicate doesn't exist or error loading it
						return RC.FAIL;
					}
				} else {
					// Enumerate all predicates in the module
					tagSet = new HashSet<>(interpreter.getEnvironment().getModule().getPredicateTags());
				}

				PredicatePropertyBacktrackInfo bi = new PredicatePropertyBacktrackInfo();
				bi.startUndoPosition = interpreter.getUndoPosition();
				bi.head = head;
				bi.property = property;
				bi.tagsIterator = tagSet.iterator();
				bi.currentTag = null;
				bi.propertyIndex = 0;
				return nextSolution(interpreter, bi);
			}
		}

		private RC nextSolution(final Interpreter interpreter, final PredicatePropertyBacktrackInfo bi) throws PrologException {
			while (true) {
				// If we're still enumerating properties for the current predicate
				if (bi.currentTag != null) {
					List<Term> properties = getPredicateProperties(interpreter, bi.currentTag);
					if (properties != null && !properties.isEmpty()) {
						// Try to unify the next property
						while (bi.propertyIndex < properties.size()) {
							Term prop = properties.get(bi.propertyIndex);
							bi.propertyIndex++;

							RC rc = interpreter.unify(bi.property, prop);
							if (rc == RC.SUCCESS_LAST) {
								// Unified successfully - push backtrack info and return
								interpreter.pushBacktrackInfo(bi);
								return RC.SUCCESS;
							} else {
								// Failed to unify - undo and try next property
								interpreter.undo(bi.startUndoPosition);
							}
						}
					}
					// Done with this predicate, move to next
					bi.currentTag = null;
					bi.propertyIndex = 0;
				}

				// Move to next predicate
				if (!bi.tagsIterator.hasNext()) {
					return RC.FAIL;
				}

				CompoundTermTag tag = bi.tagsIterator.next();

				// Check if this predicate actually exists (either in module or as built-in)
				if (!predicateExists(interpreter, tag)) {
					continue;
				}

				// Try to unify the head with this predicate indicator
				// If head is a callable term (not a predicate indicator), convert it to a predicate indicator
				Term headToUnify;
				Term headDereferenced = bi.head.dereference();
				if (headDereferenced instanceof CompoundTerm ct && ct.tag != DIVIDE_TAG) {
					// Callable compound term - use its predicate indicator
					headToUnify = ct.tag.getPredicateIndicator();
				} else if (headDereferenced instanceof AtomTerm at) {
					// Callable atom - use its predicate indicator (Name/0)
					headToUnify = CompoundTermTag.get(at.value, 0).getPredicateIndicator();
				} else {
					// Already a predicate indicator or variable - use as is
					headToUnify = bi.head;
				}

				RC rc = interpreter.unify(headToUnify, tag.getPredicateIndicator());
				if (rc != RC.SUCCESS_LAST) {
					interpreter.undo(bi.startUndoPosition);
					continue;
				}

				// Set up to enumerate properties for this predicate
				bi.currentTag = tag;
				bi.propertyIndex = 0;
				// Loop will continue to enumerate properties
			}
		}

		private boolean predicateExists(final Interpreter interpreter, final CompoundTermTag tag) {
			// Check if it's a user-defined or external predicate in the module
			Predicate p = interpreter.getEnvironment().getModule().getDefinedPredicate(tag);
			if (p != null && p.getType() != Predicate.TYPE.UNDEFINED) {
				return true;
			}

			// Check if it's a built-in predicate
			try {
				PrologCode code = interpreter.getEnvironment().getPrologCode(tag);
				// If we got code and it's not UndefinedPredicateCode, the predicate exists
				return code != null && !(code instanceof UndefinedPredicateCode);
			} catch (PrologException e) {
				return false;
			}
		}

		private List<Term> getPredicateProperties(final Interpreter interpreter, final CompoundTermTag tag) {
			List<Term> properties = new ArrayList<>();

			// First check if it's a user-defined or external predicate
			Predicate p = interpreter.getEnvironment().getModule().getDefinedPredicate(tag);
			if (p != null && p.getType() != Predicate.TYPE.UNDEFINED) {
				// User-defined or external predicate
				switch (p.getType()) {
					case BUILD_IN:
					case CONTROL:
						properties.add(AtomTerm.get("built_in"));
						properties.add(AtomTerm.get("static"));
						break;
					case USER_DEFINED:
					case EXTERNAL:
						// ISO Prolog: Standard library predicates should be marked as built_in
						if (p.isStandardLibrary()) {
							properties.add(AtomTerm.get("built_in"));
						}
						if (p.isDynamic()) {
							properties.add(AtomTerm.get("dynamic"));
						} else {
							properties.add(AtomTerm.get("static"));
						}
						break;
					case UNDEFINED:
						// No properties
						break;
				}
			} else {
				// Check if it's a built-in predicate
				try {
					PrologCode code = interpreter.getEnvironment().getPrologCode(tag);
					if (code != null && !(code instanceof UndefinedPredicateCode)) {
						// It's a built-in predicate
						properties.add(AtomTerm.get("built_in"));
						properties.add(AtomTerm.get("static"));
					}
				} catch (PrologException e) {
					// Predicate doesn't exist
				}
			}

			return properties;
		}
	};

	/** retract/1 - Remove a clause from the database (backtracking) */
	public static final ExecuteOnlyCode RETRACT = new ExecuteOnlyCode() {
		@Override
		public RC execute(final Interpreter interpreter, final boolean backtrackMode, final Term[] args) throws PrologException {
			if (backtrackMode) {
				RetractBacktrackInfo bi = (RetractBacktrackInfo) interpreter.popBacktrackInfo();
				interpreter.undo(bi.startUndoPosition);
				return nextSolution(interpreter, bi);
			} else {
				Term clause = args[0];

				PredicateTagHeadBody predicateTagHeadBody = Predicates.execute(clause, interpreter);

				Predicate p = predicateTagHeadBody.predicate;
				Term head = predicateTagHeadBody.head;
				Term body = predicateTagHeadBody.body;
				CompoundTermTag predTag = predicateTagHeadBody.tag;

				if (p == null) {
					return RC.FAIL;
				} else if (p.getType() == Predicate.TYPE.USER_DEFINED) {
					if (!p.isDynamic()) {
						PrologException.permissionError(TermConstants.modifyAtom, TermConstants.staticProcedureAtom, predTag
								.getPredicateIndicator());
					}
				} else {
					PrologException.permissionError(TermConstants.modifyAtom, TermConstants.staticProcedureAtom, predTag
							.getPredicateIndicator());
				}
				Map<Term, Term> map = new HashMap<>();
				RetractBacktrackInfo bi = new RetractBacktrackInfo();
				synchronized (p) {
					List<Term> clauses = p.getClauses();
					List<Term> list = new ArrayList<Term>(clauses.size());
					for (Term term : clauses) {
						Term cl = term;
						Term cp = (Term) cl.clone();
						map.put(cp, cl);
						list.add(cp);
					}

					bi.iclauses = list.iterator();
					bi.clauseMap = map;
					bi.startUndoPosition = interpreter.getUndoPosition();
					bi.clause = new CompoundTerm(TermConstants.clauseTag, head, body);
					bi.pred = p;
					bi.tag = predTag;
				}
				return nextSolution(interpreter, bi);
			}
		}

		private RC nextSolution(final Interpreter interpreter, final RetractBacktrackInfo bi) throws PrologException {
			while (bi.iclauses.hasNext()) {
				Term term = bi.iclauses.next();
				RC rc = interpreter.unify(bi.clause, term);
				if (rc == RC.SUCCESS_LAST) {
					Term toRemove = bi.clauseMap.get(term);
					bi.pred.removeClause(toRemove);
					interpreter.pushBacktrackInfo(bi);
					return RC.SUCCESS;
				}
			}
			return RC.FAIL;
		}
	};

	// ============================================================================
	// BacktrackInfo Classes
	// ============================================================================

	private static class ClauseBacktrackInfo extends BacktrackInfo {
		List<Term> clauses;
		int position;
		int startUndoPosition;
		Term clause;

		ClauseBacktrackInfo() {
			super(-1, -1);
		}
	}

	private static class CurrentFunctorBacktrackInfo extends BacktrackInfo {
		int startUndoPosition;
		Iterator<CompoundTermTag> tagsIterator;
		Term functor;
		Term arity;

		CurrentFunctorBacktrackInfo() {
			super(-1, -1);
		}
	}

	private static class CurrentPredicateBacktrackInfo extends BacktrackInfo {
		int startUndoPosition;
		Iterator<CompoundTermTag> tagsIterator;
		Term pi;

		CurrentPredicateBacktrackInfo() {
			super(-1, -1);
		}
	}

	private static class PredicatePropertyBacktrackInfo extends BacktrackInfo {
		int startUndoPosition;
		Iterator<CompoundTermTag> tagsIterator;
		Term head;
		Term property;
		CompoundTermTag currentTag;
		int propertyIndex;

		PredicatePropertyBacktrackInfo() {
			super(-1, -1);
		}
	}

	private static class RetractBacktrackInfo extends BacktrackInfo {
		Iterator<Term> iclauses;
		Map<Term, Term> clauseMap;
		int startUndoPosition;
		Term clause;
		Predicate pred;
		CompoundTermTag tag;

		RetractBacktrackInfo() {
			super(-1, -1);
		}
	}

	// ============================================================================
	// Helper Classes
	// ============================================================================

	private static class PredicateTagHeadBody {
		public final Predicate predicate;
		public final CompoundTermTag tag;
		public final Term head;
		public final Term body;

		public PredicateTagHeadBody(final Predicate predicate, final CompoundTermTag predicateTag, final Term head, final Term body) {
			this.predicate = predicate;
			tag = predicateTag;
			this.head = head;
			this.body = body;
		}
	}
}
