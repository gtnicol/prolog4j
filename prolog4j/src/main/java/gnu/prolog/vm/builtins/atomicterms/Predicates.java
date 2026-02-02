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
package gnu.prolog.vm.builtins.atomicterms;

import gnu.prolog.database.Pair;
import gnu.prolog.io.ParseException;
import gnu.prolog.io.TermReader;
import gnu.prolog.io.TermWriter;
import gnu.prolog.term.AtomTerm;
import gnu.prolog.term.CompoundTerm;
import gnu.prolog.term.DecimalTerm;
import gnu.prolog.term.FloatTerm;
import gnu.prolog.term.IntegerTerm;
import gnu.prolog.term.Term;
import gnu.prolog.term.VariableTerm;
import gnu.prolog.vm.BacktrackInfo;
import gnu.prolog.vm.ExecuteOnlyCode;
import gnu.prolog.vm.Interpreter;
import gnu.prolog.vm.PrologException;
import gnu.prolog.vm.TermConstants;

/**
 * Factory class for atomic term predicates (ISO Prolog 8.16).
 * Provides implementations for atom_chars, atom_codes, atom_concat, atom_length, char_code, number_chars, number_codes, and sub_atom.
 */
public final class Predicates {

	private Predicates() {
		throw new AssertionError("Utility class should not be instantiated");
	}

	// ============================================================================
	// Helper Methods (public for backward compatibility)
	// ============================================================================

	/**
	 * Helper method used by number_chars and number_codes predicates.
	 * @see CompoundTerm#getInstantiatedHeadBody
	 * @param term the term
	 * @param numberIsVariable whether number is variable
	 * @return a (head,body) Pair or may be null if numberIsVariable is false
	 * @throws PrologException in the event of type or instantiation errors
	 */
	public static Pair<Term, Term> getInstantiatedHeadBody(final Term term, final boolean numberIsVariable) throws PrologException {
		switch (term) {
			case VariableTerm vt -> {
				if (numberIsVariable) {
					PrologException.instantiationError(term);
				}
				return null;
			}
			default -> {}
		}
		if (!CompoundTerm.isListPair(term)) {
			PrologException.typeError(TermConstants.listAtom, term);
		}
		CompoundTerm ct = (CompoundTerm) term;
		Term head = ct.args[0].dereference();
		Term body = ct.args[1].dereference();
		switch (head) {
			case VariableTerm vt -> {
				if (numberIsVariable) {
					PrologException.instantiationError(head);
				}
				return null;
			}
			default -> {}
		}
		return new Pair<>(head, body);
	}

	// ============================================================================
	// Predicate Implementations
	// ============================================================================

	/** atom_chars/2 - Convert between atom and list of characters */
	public static final ExecuteOnlyCode ATOM_CHARS = (interpreter, backtrackMode, args) -> {
		Term ta = args[0];
		Term tl = args[1];
		return switch (ta) {
			case VariableTerm va -> {
				StringBuilder bu = new StringBuilder();
				Term cur = tl;
				while (cur != TermConstants.emptyListAtom) {
					switch (cur) {
						case VariableTerm vt -> {
							PrologException.instantiationError(cur);
						}
						default -> {}
					}
					CompoundTerm ct = switch (cur) {
						case CompoundTerm c -> c;
						default -> {
							PrologException.typeError(TermConstants.listAtom, tl);
							yield null; // Never reached
						}
					};
					if (ct.tag != TermConstants.listTag) {
						PrologException.typeError(TermConstants.listAtom, tl);
					}
					Term head = ct.args[0].dereference();
					cur = ct.args[1].dereference();
					switch (head) {
						case VariableTerm vt -> {
							PrologException.instantiationError(head);
						}
						default -> {}
					}
					AtomTerm e = switch (head) {
						case AtomTerm at -> at;
						default -> {
							PrologException.typeError(TermConstants.characterAtom, head);
							yield null; // Never reached
						}
					};
					if (e.value.length() != 1) {
						PrologException.typeError(TermConstants.characterAtom, head);
					}
					bu.append(e.value.charAt(0));
				}
				interpreter.addVariableUndo(va);
				va.value = AtomTerm.get(bu.toString());
				yield ExecuteOnlyCode.RC.SUCCESS_LAST;
			}
			case AtomTerm a -> {
				Term list = TermConstants.emptyListAtom;
				for (int i = a.value.length() - 1; i >= 0; i--) {
					StringBuilder bu = new StringBuilder(1);
					bu.append(a.value.charAt(i));
					list = CompoundTerm.getList(AtomTerm.get(bu.toString()), list);
				}
				yield interpreter.unify(list, tl);
			}
			default -> {
				PrologException.typeError(TermConstants.atomAtom, ta);
				yield ExecuteOnlyCode.RC.FAIL; // Never reached
			}
		};
	};

	/** atom_codes/2 - Convert between atom and list of character codes */
	public static final ExecuteOnlyCode ATOM_CODES = (interpreter, backtrackMode, args) -> {
		Term ta = args[0];
		Term tl = args[1];
		return switch (ta) {
			case VariableTerm va -> {
				StringBuilder bu = new StringBuilder();
				IntegerTerm maxCharacterCodeTerm = (IntegerTerm) interpreter.getEnvironment()
						.getPrologFlag(TermConstants.maxCharacterCodeAtom);
				int maxCharacterCode = maxCharacterCodeTerm.value;
				Term cur = tl;
				while (cur != TermConstants.emptyListAtom) {
					switch (cur) {
						case VariableTerm vt -> {
							PrologException.instantiationError(cur);
						}
						default -> {}
					}
					CompoundTerm ct = switch (cur) {
						case CompoundTerm c -> c;
						default -> {
							PrologException.typeError(TermConstants.listAtom, tl);
							yield null; // Never reached
						}
					};
					if (ct.tag != TermConstants.listTag) {
						PrologException.typeError(TermConstants.listAtom, tl);
					}
					Term head = ct.args[0].dereference();
					cur = ct.args[1].dereference();
					switch (head) {
						case VariableTerm vt -> {
							PrologException.instantiationError(head);
						}
						default -> {}
					}
					IntegerTerm e = switch (head) {
						case IntegerTerm it -> it;
						default -> {
							PrologException.representationError(TermConstants.characterCodeAtom);
							yield null; // Never reached
						}
					};
					if (e.value < 0 || maxCharacterCode < e.value) {
						PrologException.representationError(TermConstants.characterCodeAtom);
					}
					bu.append((char) e.value);
				}
				interpreter.addVariableUndo(va);
				va.value = AtomTerm.get(bu.toString());
				yield ExecuteOnlyCode.RC.SUCCESS_LAST;
			}
			case AtomTerm a -> {
				Term list = TermConstants.emptyListAtom;
				for (int i = a.value.length() - 1; i >= 0; i--) {
					list = CompoundTerm.getList(IntegerTerm.get(a.value.charAt(i)), list);
				}
				yield interpreter.unify(list, tl);
			}
			default -> {
				PrologException.typeError(TermConstants.atomAtom, ta);
				yield ExecuteOnlyCode.RC.FAIL; // Never reached
			}
		};
	};

	/** atom_length/2 - Get the length of an atom */
	public static final ExecuteOnlyCode ATOM_LENGTH = (interpreter, backtrackMode, args) -> {
		Term tatom = args[0];
		Term tlength = args[1];
		switch (tatom) {
			case VariableTerm vt -> {
				PrologException.instantiationError(tatom);
			}
			default -> {}
		}
		AtomTerm atom = switch (tatom) {
			case AtomTerm at -> at;
			default -> {
				PrologException.typeError(TermConstants.atomAtom, tatom);
				yield null; // Never reached
			}
		};
		return switch (tlength) {
			case VariableTerm vlength -> {
				IntegerTerm ilength = IntegerTerm.get(atom.value.length());
				interpreter.addVariableUndo(vlength);
				vlength.value = ilength;
				yield ExecuteOnlyCode.RC.SUCCESS_LAST;
			}
			case IntegerTerm ilength -> {
				if (ilength.value < 0) {
					PrologException.domainError(TermConstants.notLessThanZeroAtom, tlength);
				}
				yield ilength.value == atom.value.length() ? ExecuteOnlyCode.RC.SUCCESS_LAST : ExecuteOnlyCode.RC.FAIL;
			}
			default -> {
				PrologException.typeError(TermConstants.integerAtom, tlength);
				yield ExecuteOnlyCode.RC.FAIL; // Never reached
			}
		};
	};

	/** char_code/2 - Convert between character and character code */
	public static final ExecuteOnlyCode CHAR_CODE = (interpreter, backtrackMode, args) -> {
		Term tchar = args[0];
		Term tcode = args[1];
		return switch (tchar) {
			case VariableTerm vchar -> {
				switch (tcode) {
					case VariableTerm vt -> {
						PrologException.instantiationError(tcode);
					}
					default -> {}
				}
				IntegerTerm icode = switch (tcode) {
					case IntegerTerm it -> it;
					default -> {
						PrologException.typeError(TermConstants.integerAtom, tcode);
						yield null; // Never reached
					}
				};
				if (icode.value < 0 || 0xffff < icode.value) {
					PrologException.representationError(TermConstants.characterCodeAtom);
				}
				StringBuilder bu = new StringBuilder(1);
				bu.append((char) icode.value);
				interpreter.addVariableUndo(vchar);
				vchar.value = AtomTerm.get(bu.toString());
				yield ExecuteOnlyCode.RC.SUCCESS_LAST;
			}
			case AtomTerm achar -> {
				if (achar.value.length() != 1) {
					PrologException.typeError(TermConstants.characterAtom, achar);
				}
				IntegerTerm code = IntegerTerm.get(achar.value.charAt(0));
				switch (tcode) {
					case IntegerTerm it -> {}
					case VariableTerm vt -> {}
					default -> {
						PrologException.typeError(TermConstants.integerAtom, tcode);
					}
				}
				yield interpreter.unify(code, tcode);
			}
			default -> {
				PrologException.typeError(TermConstants.characterAtom, tchar);
				yield ExecuteOnlyCode.RC.FAIL; // Never reached
			}
		};
	};

	/** number_chars/2 - Convert between number and list of characters */
	public static final ExecuteOnlyCode NUMBER_CHARS = (interpreter, backtrackMode, args) -> {
		Term number = args[0];
		Term list = args[1];

		return switch (number) {
			case VariableTerm vn -> {
				// Convert list of chars to number
				String numStr = getNumberStringFromChars(list, true);
				if (numStr != null) {
					Term res = null;
					try {
						res = TermReader.stringToTerm(numStr, interpreter.getEnvironment());
					} catch (ParseException ex) {// TODO there is useful debug information here which we are discarding
						PrologException.syntaxError(ex);
					}
					switch (res) {
						case IntegerTerm it -> {}
						case FloatTerm ft -> {}
						case DecimalTerm dt -> {}
						default -> {
							PrologException.syntaxError(TermConstants.numberExpectedAtom);
						}
					}
					yield interpreter.unify(res, number);
				} else {
					// Should not reach here if getNumberStringFromChars returns null with numberIsVariable=true
					yield ExecuteOnlyCode.RC.FAIL;
				}
			}
			case IntegerTerm it -> {
				// Convert number to list of chars
				String numStr = TermWriter.toString(number);
				Term res = TermConstants.emptyListAtom;
				for (int i = numStr.length() - 1; i >= 0; i--) {
					res = CompoundTerm.getList(AtomTerm.get(numStr.charAt(i)), res);
				}
				yield interpreter.unify(list, res);
			}
			case FloatTerm ft -> {
				// Try to parse the list first to check for alternative representations (e.g., scientific notation)
				String inputStr = getNumberStringFromChars(list, false);
				if (inputStr != null) {
					try {
						Term parsed = TermReader.stringToTerm(inputStr, interpreter.getEnvironment());
						if (parsed instanceof FloatTerm parsedFloat) {
							// Check if the parsed number equals the input number (handles 3.3 == 3.3E+0)
							if (Math.abs(ft.value - parsedFloat.value) < 1e-15) {
								yield ExecuteOnlyCode.RC.SUCCESS_LAST;
							}
						} else if (parsed instanceof IntegerTerm parsedInt) {
							// Check if integer representation equals float (e.g., 3.0 == 3)
							if (Math.abs(ft.value - parsedInt.value) < 1e-15) {
								yield ExecuteOnlyCode.RC.SUCCESS_LAST;
							}
						}
					} catch (ParseException ex) {
						// Fall through to default conversion
					}
				}

				// Convert number to list of chars (default representation)
				String numStr = TermWriter.toString(number);
				Term res = TermConstants.emptyListAtom;
				for (int i = numStr.length() - 1; i >= 0; i--) {
					res = CompoundTerm.getList(AtomTerm.get(numStr.charAt(i)), res);
				}
				yield interpreter.unify(list, res);
			}
			case DecimalTerm dt -> {
				// Try to parse the list first to check for alternative representations
				String inputStr = getNumberStringFromChars(list, false);
				if (inputStr != null) {
					try {
						Term parsed = TermReader.stringToTerm(inputStr, interpreter.getEnvironment());
						if (parsed instanceof DecimalTerm parsedDecimal) {
							// Check if the parsed number equals the input number
							if (dt.value.compareTo(parsedDecimal.value) == 0) {
								yield ExecuteOnlyCode.RC.SUCCESS_LAST;
							}
						} else if (parsed instanceof IntegerTerm parsedInt) {
							// Check if integer representation equals decimal (e.g., 3.0 == 3)
							if (dt.value.compareTo(java.math.BigDecimal.valueOf(parsedInt.value)) == 0) {
								yield ExecuteOnlyCode.RC.SUCCESS_LAST;
							}
						}
					} catch (ParseException ex) {
						// Fall through to default conversion
					}
				}

				// Convert number to list of chars (default representation)
				String numStr = TermWriter.toString(number);
				Term res = TermConstants.emptyListAtom;
				for (int i = numStr.length() - 1; i >= 0; i--) {
					res = CompoundTerm.getList(AtomTerm.get(numStr.charAt(i)), res);
				}
				yield interpreter.unify(list, res);
			}
			default -> {
				PrologException.typeError(TermConstants.numberAtom, number);
				yield ExecuteOnlyCode.RC.FAIL; // Never reached
			}
		};
	};

	/** number_codes/2 - Convert between number and list of character codes */
	public static final ExecuteOnlyCode NUMBER_CODES = (interpreter, backtrackMode, args) -> {
		Term number = args[0];
		Term list = args[1];

		return switch (number) {
			case VariableTerm vn -> {
				// Convert list of codes to number
				String numStr = getNumberStringFromCodes(list, true);
				if (numStr != null) {
					Term res = null;
					try {
						res = TermReader.stringToTerm(numStr, interpreter.getEnvironment());
					} catch (ParseException ex) {// TODO there is useful debug information here which we are discarding
						PrologException.syntaxError(ex);
					}
					switch (res) {
						case IntegerTerm it -> {}
						case FloatTerm ft -> {}
						case DecimalTerm dt -> {}
						default -> {
							PrologException.syntaxError(TermConstants.numberExpectedAtom);
						}
					}
					yield interpreter.unify(res, number);
				} else {
					// Should not reach here if getNumberStringFromCodes returns null with numberIsVariable=true
					yield ExecuteOnlyCode.RC.FAIL;
				}
			}
			case IntegerTerm it -> {
				// Convert number to list of codes
				String numStr = TermWriter.toString(number);
				Term res = TermConstants.emptyListAtom;
				for (int i = numStr.length() - 1; i >= 0; i--) {
					res = CompoundTerm.getList(IntegerTerm.get(numStr.charAt(i)), res);
				}
				yield interpreter.unify(list, res);
			}
			case FloatTerm ft -> {
				// Try to parse the list first to check for alternative representations (e.g., scientific notation)
				String inputStr = getNumberStringFromCodes(list, false);
				if (inputStr != null) {
					try {
						Term parsed = TermReader.stringToTerm(inputStr, interpreter.getEnvironment());
						if (parsed instanceof FloatTerm parsedFloat) {
							// Check if the parsed number equals the input number (handles 33.0 == 3.3E+01)
							if (Math.abs(ft.value - parsedFloat.value) < 1e-15) {
								yield ExecuteOnlyCode.RC.SUCCESS_LAST;
							}
						} else if (parsed instanceof IntegerTerm parsedInt) {
							// Check if integer representation equals float (e.g., 3.0 == 3)
							if (Math.abs(ft.value - parsedInt.value) < 1e-15) {
								yield ExecuteOnlyCode.RC.SUCCESS_LAST;
							}
						}
					} catch (ParseException ex) {
						// Fall through to default conversion
					}
				}

				// Convert number to list of codes (default representation)
				String numStr = TermWriter.toString(number);
				Term res = TermConstants.emptyListAtom;
				for (int i = numStr.length() - 1; i >= 0; i--) {
					res = CompoundTerm.getList(IntegerTerm.get(numStr.charAt(i)), res);
				}
				yield interpreter.unify(list, res);
			}
			case DecimalTerm dt -> {
				// Try to parse the list first to check for alternative representations
				String inputStr = getNumberStringFromCodes(list, false);
				if (inputStr != null) {
					try {
						Term parsed = TermReader.stringToTerm(inputStr, interpreter.getEnvironment());
						if (parsed instanceof DecimalTerm parsedDecimal) {
							// Check if the parsed number equals the input number
							if (dt.value.compareTo(parsedDecimal.value) == 0) {
								yield ExecuteOnlyCode.RC.SUCCESS_LAST;
							}
						} else if (parsed instanceof IntegerTerm parsedInt) {
							// Check if integer representation equals decimal (e.g., 3.0 == 3)
							if (dt.value.compareTo(java.math.BigDecimal.valueOf(parsedInt.value)) == 0) {
								yield ExecuteOnlyCode.RC.SUCCESS_LAST;
							}
						}
					} catch (ParseException ex) {
						// Fall through to default conversion
					}
				}

				// Convert number to list of codes (default representation)
				String numStr = TermWriter.toString(number);
				Term res = TermConstants.emptyListAtom;
				for (int i = numStr.length() - 1; i >= 0; i--) {
					res = CompoundTerm.getList(IntegerTerm.get(numStr.charAt(i)), res);
				}
				yield interpreter.unify(list, res);
			}
			default -> {
				PrologException.typeError(TermConstants.numberAtom, number);
				yield ExecuteOnlyCode.RC.FAIL; // Never reached
			}
		};
	};

	// ============================================================================
	// Complex Predicate Implementations (with BacktrackInfo)
	// ============================================================================

	private static final AtomTerm nullAtom = AtomTerm.get("");

	/** atom_concat/3 - Concatenate atoms (backtracking when both Atom1 and Atom2 are variables) */
	public static final ExecuteOnlyCode ATOM_CONCAT = new ExecuteOnlyCode() {
		@Override
		public RC execute(final Interpreter interpreter, final boolean backtrackMode, final Term[] args) throws PrologException {
			if (backtrackMode) {
				AtomConcatBacktrackInfo acbi = (AtomConcatBacktrackInfo) interpreter.popBacktrackInfo();
				interpreter.undo(acbi.startUndoPosition);
				int al = acbi.atom.length();
				int pos = acbi.atomPosition;
				VariableTerm v1 = (VariableTerm) args[0];
				VariableTerm v2 = (VariableTerm) args[1];
				if (acbi.atomPosition == al) {
					interpreter.addVariableUndo(v1);
					v1.value = args[2];
					interpreter.addVariableUndo(v2);
					v2.value = nullAtom;
					return RC.SUCCESS_LAST;
				}
				interpreter.addVariableUndo(v1);
				v1.value = AtomTerm.get(acbi.atom.substring(0, pos));
				interpreter.addVariableUndo(v2);
				v2.value = AtomTerm.get(acbi.atom.substring(pos, al));
				acbi.atomPosition++;
				interpreter.pushBacktrackInfo(acbi);
				return RC.SUCCESS;
			} else {
				Term t1 = args[0];
				Term t2 = args[1];
				Term t12 = args[2];
				int startUndoPosition = interpreter.getUndoPosition();

				switch (t1) {
					case VariableTerm vt -> {}
					case AtomTerm at -> {}
					default -> {
						PrologException.typeError(TermConstants.atomAtom, t1);
					}
				}
				switch (t2) {
					case VariableTerm vt -> {}
					case AtomTerm at -> {}
					default -> {
						PrologException.typeError(TermConstants.atomAtom, t2);
					}
				}
				switch (t12) {
					case VariableTerm vt -> {}
					case AtomTerm at -> {}
					default -> {
						PrologException.typeError(TermConstants.atomAtom, t12);
					}
				}
				return switch (t12) {
					case VariableTerm v12 -> {
						switch (t1) {
							case VariableTerm vt -> {
								PrologException.instantiationError(t1);
							}
							default -> {}
						}
						switch (t2) {
							case VariableTerm vt -> {
								PrologException.instantiationError(t2);
							}
							default -> {}
						}
						AtomTerm a1 = (AtomTerm) t1;
						AtomTerm a2 = (AtomTerm) t2;
						AtomTerm a3 = AtomTerm.get(a1.value + a2.value);
						interpreter.addVariableUndo(v12);
						v12.value = a3;
						yield RC.SUCCESS_LAST;
					}
					case AtomTerm a12 -> {
						String s12 = a12.value;
						yield switch (t1) {
							case VariableTerm v1 when t2 instanceof VariableTerm -> {
								VariableTerm v2 = (VariableTerm) t2;
								if (s12.length() == 0) {
									interpreter.addVariableUndo(v1);
									v1.value = a12;
									interpreter.addVariableUndo(v2);
									v2.value = a12;
									yield RC.SUCCESS_LAST;
								}
								interpreter.addVariableUndo(v1);
								v1.value = nullAtom;
								interpreter.addVariableUndo(v2);
								v2.value = a12;
								interpreter.pushBacktrackInfo(new AtomConcatBacktrackInfo(1, startUndoPosition, s12));
								yield RC.SUCCESS;
							}
							case VariableTerm v1 -> {
								AtomTerm a2 = (AtomTerm) t2;
								String s2 = a2.value;
								if (s12.endsWith(s2)) {
									interpreter.addVariableUndo(v1);
									v1.value = AtomTerm.get(s12.substring(0, s12.length() - s2.length()));
									yield RC.SUCCESS_LAST;
								} else {
									yield RC.FAIL;
								}
							}
							case AtomTerm a1 when t2 instanceof VariableTerm -> {
								VariableTerm v2 = (VariableTerm) t2;
								String s1 = a1.value;
								if (s12.startsWith(s1)) {
									interpreter.addVariableUndo(v2);
									int l1 = s1.length();
									int l12 = s12.length();
									v2.value = AtomTerm.get(s12.substring(l1, l12));
									yield RC.SUCCESS_LAST;
								} else {
									yield RC.FAIL;
								}
							}
							default -> {
								AtomTerm a1 = (AtomTerm) t1;
								AtomTerm a2 = (AtomTerm) t2;
								String s1 = a1.value;
								String s2 = a2.value;
								yield s12.equals(s1 + s2) ? RC.SUCCESS_LAST : RC.FAIL;
							}
						};
					}
					default -> RC.FAIL; // Never reached
				};
			}
		}
	};

	/** sub_atom/5 - Extract subatoms (backtracking) */
	public static final ExecuteOnlyCode SUB_ATOM = new ExecuteOnlyCode() {
		@Override
		public RC execute(final Interpreter interpreter, final boolean backtrackMode, final Term[] args) throws PrologException {
			if (backtrackMode) {
				SubAtomBacktrackInfo bi = (SubAtomBacktrackInfo) interpreter.popBacktrackInfo();
				interpreter.undo(bi.startUndoPosition);
				return nextSolution(interpreter, bi);
			} else {
				SubAtomBacktrackInfo bi = new SubAtomBacktrackInfo();
				bi.startUndoPosition = interpreter.getUndoPosition();

				Term tatom = args[0];
				Term tbefore = args[1];
				Term tlength = args[2];
				Term tafter = args[3];
				Term tsub_atom = args[4];

				switch (tatom) {
					case VariableTerm vt -> {
						PrologException.instantiationError(tatom);
					}
					default -> {}
				}
				bi.atom = switch (tatom) {
					case AtomTerm at -> at;
					default -> {
						PrologException.typeError(TermConstants.atomAtom, tatom);
						yield null; // Never reached
					}
				};
				bi.atomLen = bi.atom.value.length();
				bi.currentPos = 0;
				bi.currentLen = 0;
				switch (tbefore) {
					case VariableTerm vb -> {
						bi.beforeFixed = false;
						bi.varBefore = vb;
					}
					case IntegerTerm ib -> {
						bi.beforeFixed = true;
						bi.before = ib.value;
						if (bi.before < 0) {
							PrologException.domainError(TermConstants.notLessThanZeroAtom, tbefore);
						}
					}
					default -> {
						PrologException.typeError(TermConstants.integerAtom, tbefore);
					}
				}
				switch (tlength) {
					case VariableTerm vl -> {
						bi.lengthFixed = false;
						bi.varLength = vl;
					}
					case IntegerTerm il -> {
						bi.lengthFixed = true;
						bi.length = il.value;
						if (bi.length < 0) {
							PrologException.domainError(TermConstants.notLessThanZeroAtom, tlength);
						}
						if (bi.length > bi.atomLen) {
							return RC.FAIL;
						}
					}
					default -> {
						PrologException.typeError(TermConstants.integerAtom, tlength);
					}
				}
				switch (tafter) {
					case VariableTerm va -> {
						bi.afterFixed = false;
						bi.varAfter = va;
					}
					case IntegerTerm ia -> {
						bi.afterFixed = true;
						bi.after = ia.value;
						if (bi.after < 0) {
							PrologException.domainError(TermConstants.notLessThanZeroAtom, tafter);
						}
					}
					default -> {
						PrologException.typeError(TermConstants.integerAtom, tafter);
					}
				}

				switch (tsub_atom) {
					case VariableTerm vs -> {
						bi.subAtomFixed = false;
						bi.varSubAtom = vs;
					}
					case AtomTerm a -> {
						if (bi.lengthFixed) {
							if (bi.length != a.value.length()) {
								return RC.FAIL;
							}
						} else {
							bi.lengthFixed = true;
							bi.length = a.value.length();
							if (bi.length > bi.atomLen) {
								return RC.FAIL;
							}
						}
						bi.subAtomFixed = true;
						bi.subAtom = a;
					}
					default -> {
						PrologException.typeError(TermConstants.atomAtom, tsub_atom);
					}
				}
				return nextSolution(interpreter, bi);
			}
		}

		private RC nextSolution(final Interpreter interpreter, final SubAtomBacktrackInfo bi) {
			while (true) {
				if (bi.currentLen > bi.atomLen - bi.currentPos) {
					bi.currentLen = 0;
					bi.currentPos++;
					if (bi.currentPos > bi.atomLen) {
						return RC.FAIL;
					}
				}

				int len = bi.currentLen;
				int pos = bi.currentPos;
				bi.currentLen++;

				if (bi.beforeFixed && pos != bi.before) {
					continue;
				}
				if (bi.lengthFixed && len != bi.length) {
					continue;
				}
				if (bi.afterFixed && bi.atomLen - (pos + len) != bi.after) {
					continue;
				}
				if (bi.subAtomFixed && !bi.atom.value.regionMatches(pos, bi.subAtom.value, 0, len)) {
					continue;
				}
				// unify
				if (bi.varBefore != null) {
					interpreter.addVariableUndo(bi.varBefore);
					bi.varBefore.value = IntegerTerm.get(pos);
				}
				if (bi.varLength != null) {
					interpreter.addVariableUndo(bi.varLength);
					bi.varLength.value = IntegerTerm.get(len);
				}
				if (bi.varAfter != null) {
					interpreter.addVariableUndo(bi.varAfter);
					bi.varAfter.value = IntegerTerm.get(bi.atomLen - (pos + len));
				}
				if (bi.varSubAtom != null) {
					interpreter.addVariableUndo(bi.varSubAtom);
					bi.varSubAtom.value = AtomTerm.get(bi.atom.value.substring(pos, pos + len));
				}
				interpreter.pushBacktrackInfo(bi);
				return RC.SUCCESS;
			}
		}
	};

	// ============================================================================
	// Private Helper Methods
	// ============================================================================

	/** Helper for number_chars - returns null if illegal character sequence */
	private static String getNumberStringFromChars(final Term list, final boolean numberIsVariable) throws PrologException {
		StringBuilder bu = new StringBuilder();
		Term cur = list;
		while (cur != TermConstants.emptyListAtom) {
			Pair<Term, Term> headBodyPair = getInstantiatedHeadBody(cur, numberIsVariable);
			if (headBodyPair == null) {
				return null;
			}
			Term head = headBodyPair.left();
			cur = headBodyPair.right();

			switch (head) {
				case AtomTerm ch when ch.value.length() == 1 ->
					bu.append(ch.value.charAt(0));
				case AtomTerm ch -> {
					PrologException.typeError(TermConstants.characterAtom, head);
				}
				default -> {
					PrologException.typeError(TermConstants.characterAtom, head);
				}
			}
		}
		return bu.toString();
	}

	/** Helper for number_codes - returns null if illegal character sequence */
	private static String getNumberStringFromCodes(final Term list, final boolean numberIsVariable) throws PrologException {
		StringBuilder bu = new StringBuilder();
		Term cur = list;
		while (cur != TermConstants.emptyListAtom) {
			Pair<Term, Term> headBodyPair = getInstantiatedHeadBody(cur, numberIsVariable);
			if (headBodyPair == null) {
				return null;
			}
			Term head = headBodyPair.left();
			cur = headBodyPair.right();

			switch (head) {
				case IntegerTerm ch when ch.value >= 0 && ch.value <= 0xffff ->
					bu.append((char) ch.value);
				case IntegerTerm ch -> {
					PrologException.representationError(TermConstants.characterCodeAtom);
				}
				default -> {
					PrologException.representationError(TermConstants.characterCodeAtom);
				}
			}
		}
		return bu.toString();
	}

	// ============================================================================
	// BacktrackInfo Classes
	// ============================================================================

	private static class AtomConcatBacktrackInfo extends BacktrackInfo {
		int atomPosition;
		int startUndoPosition;
		String atom;

		AtomConcatBacktrackInfo(final int atomPosition, final int startUndoPosition, final String atom) {
			super(-1, -1);
			this.atomPosition = atomPosition;
			this.startUndoPosition = startUndoPosition;
			this.atom = atom;
		}
	}

	private static class SubAtomBacktrackInfo extends BacktrackInfo {
		int startUndoPosition;
		AtomTerm atom;
		boolean beforeFixed;
		int before;
		VariableTerm varBefore;
		boolean lengthFixed;
		int length;
		VariableTerm varLength;
		boolean afterFixed;
		int after;
		VariableTerm varAfter;
		boolean subAtomFixed;
		AtomTerm subAtom;
		VariableTerm varSubAtom;

		int currentPos;
		int currentLen;
		int atomLen;

		SubAtomBacktrackInfo() {
			super(-1, -1);
		}
	}
}
