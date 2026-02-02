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
package gnu.prolog.vm.builtins.io;

import gnu.prolog.database.PrologTextLoaderError;
import gnu.prolog.database.PrologTextLoaderState;
import gnu.prolog.io.Operator;
import gnu.prolog.io.Operator.SPECIFIER;
import gnu.prolog.io.OperatorSet;
import gnu.prolog.io.PrologStream;
import gnu.prolog.io.ReadOptions;
import gnu.prolog.io.WriteOptions;
import gnu.prolog.term.AtomTerm;
import gnu.prolog.term.CompoundTerm;
import gnu.prolog.term.CompoundTermTag;
import gnu.prolog.term.IntegerTerm;
import gnu.prolog.term.JavaObjectTerm;
import gnu.prolog.term.Term;
import gnu.prolog.term.VariableTerm;
import gnu.prolog.vm.BacktrackInfo;
import gnu.prolog.vm.Environment;
import gnu.prolog.vm.ExecuteOnlyCode;
import gnu.prolog.vm.Interpreter;
import gnu.prolog.vm.PrologException;
import gnu.prolog.vm.TermConstants;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory class for I/O predicates.
 * Provides implementations for stream operations, character/byte I/O, term reading/writing,
 * and operator management predicates.
 */
public final class Predicates {

	private static final Logger logger = LoggerFactory.getLogger(Predicates.class);

	private Predicates() {
		throw new AssertionError("Utility class should not be instantiated");
	}

	// ============================================================================
	// Constants
	// ============================================================================

	private static final CompoundTermTag FORCE_TAG = CompoundTermTag.get("force", 1);
	private static final AtomTerm COMMA_ATOM = AtomTerm.get(",");

	// ============================================================================
	// Helper Methods
	// ============================================================================

	private static Term mapToList(final Map<String, VariableTerm> map) {
		List<Term> entries = new ArrayList<>(map.size());
		for (Entry<String, VariableTerm> entry : map.entrySet()) {
			entries.add(new CompoundTerm(TermConstants.unifyTag, AtomTerm.get(entry.getKey()), entry.getValue()));
		}
		return CompoundTerm.getList(entries);
	}

	private static void validateOp(final int priority, final SPECIFIER specifier, final AtomTerm opAtom, final OperatorSet opSet)
			throws PrologException {
		if (opAtom == COMMA_ATOM) {
			PrologException.permissionError(TermConstants.modifyAtom, TermConstants.operatorAtom, opAtom);
		}
		switch (specifier) {
			case FX:
			case FY:
				break;
			case XF:
			case YF: {
				Operator op = opSet.lookupXf(opAtom.value);
				if (op.specifier != SPECIFIER.YF && specifier != SPECIFIER.XF) {
					PrologException.permissionError(TermConstants.createAtom, TermConstants.operatorAtom, opAtom);
				}
				break;
			}
			case XFX:
			case XFY:
			case YFX: {
				Operator op = opSet.lookupXf(opAtom.value);
				if (op.specifier == SPECIFIER.YF || specifier == SPECIFIER.XF) {
					PrologException.permissionError(TermConstants.createAtom, TermConstants.operatorAtom, opAtom);
				}
				break;
			}
		}
	}

	// ============================================================================
	// Predicate Implementations
	// ============================================================================

	/** at_end_of_stream/1 - Check if stream is at end */
	public static final ExecuteOnlyCode AT_END_OF_STREAM = (interpreter, backtrackMode, args) -> {
		PrologStream stream = interpreter.getEnvironment().resolveStream(args[0]);
		Term st = stream.getEndOfStreamState();
		if (st == PrologStream.atAtom || st == PrologStream.pastAtom) {
			return ExecuteOnlyCode.RC.SUCCESS_LAST;
		} else {
			return ExecuteOnlyCode.RC.FAIL;
		}
	};

	/** char_conversion/2 - Set character conversion */
	public static final ExecuteOnlyCode CHAR_CONVERSION = (interpreter, backtrackMode, args) -> {
		Term from = args[0];
		Term to = args[1];
		AtomTerm afrom = switch (from) {
			case AtomTerm at -> {
				if (at.value.length() != 1) {
					PrologException.representationError(from);
				}
				yield at;
			}
			case VariableTerm vt -> {
				PrologException.instantiationError(from);
				yield null; // Never reached
			}
			default -> {
				PrologException.representationError(from);
				yield null; // Never reached
			}
		};
		AtomTerm ato = switch (to) {
			case AtomTerm at -> {
				if (at.value.length() != 1) {
					PrologException.representationError(to);
				}
				yield at;
			}
			case VariableTerm vt -> {
				PrologException.instantiationError(vt);
				yield null; // Never reached
			}
			default -> {
				PrologException.representationError(to);
				yield null; // Never reached
			}
		};
		char cfrom = afrom.value.charAt(0);
		char cto = ato.value.charAt(0);
		interpreter.getEnvironment().getConversionTable().setConversion(cfrom, cto);
		return ExecuteOnlyCode.RC.SUCCESS_LAST;
	};

	/** close/2 - Close a stream */
	public static final ExecuteOnlyCode CLOSE = (interpreter, backtrackMode, args) -> {
		Term cur = args[1];
		Term force = TermConstants.falseAtom;
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
					PrologException.typeError(TermConstants.listAtom, args[1]);
					yield null; // Never reached
				}
			};
			if (ct.tag != TermConstants.listTag) {
				PrologException.typeError(TermConstants.listAtom, args[1]);
			}
			Term head = ct.args[0].dereference();
			cur = ct.args[1].dereference();

			switch (head) {
				case VariableTerm vt -> {
					PrologException.instantiationError(head);
				}
				default -> {}
			}
			CompoundTerm e = switch (head) {
				case CompoundTerm c -> c;
				default -> {
					PrologException.domainError(TermConstants.closeOptionAtom, head);
					yield null; // Never reached
				}
			};
			if (e.tag != FORCE_TAG || e.args[0] != TermConstants.trueAtom && e.args[0] != TermConstants.falseAtom) {
				PrologException.domainError(TermConstants.closeOptionAtom, head);
			}
			force = e.args[0];
		}

		PrologStream stream = interpreter.getEnvironment().resolveStream(args[0]);
		if (stream == interpreter.getEnvironment().getUserInput()) {
			return ExecuteOnlyCode.RC.SUCCESS_LAST;
		}
		if (stream == interpreter.getEnvironment().getUserOutput()) {
			return ExecuteOnlyCode.RC.SUCCESS_LAST;
		}
		stream.close(force == TermConstants.trueAtom);
		return ExecuteOnlyCode.RC.SUCCESS_LAST;
	};

	/** current_input/1 - Get current input stream */
	public static final ExecuteOnlyCode CURRENT_INPUT = (interpreter, backtrackMode, args) -> {
		Term stream = args[0];
		switch (stream) {
			case VariableTerm vt -> {} // Valid type
			case JavaObjectTerm jt -> {
				switch (jt.value) {
					case PrologStream ps -> {} // Valid
					default -> {
						PrologException.domainError(TermConstants.streamAtom, stream);
					}
				}
			}
			default -> {
				PrologException.domainError(TermConstants.streamAtom, stream);
			}
		}
		return interpreter.unify(stream, interpreter.getEnvironment().getCurrentInput().getStreamTerm());
	};

	/** current_output/1 - Get current output stream */
	public static final ExecuteOnlyCode CURRENT_OUTPUT = (interpreter, backtrackMode, args) -> {
		Term stream = args[0];
		switch (stream) {
			case VariableTerm vt -> {} // Valid type
			case JavaObjectTerm jt -> {
				switch (jt.value) {
					case PrologStream ps -> {} // Valid
					default -> {
						PrologException.domainError(TermConstants.streamAtom, stream);
					}
				}
			}
			default -> {
				PrologException.domainError(TermConstants.streamAtom, stream);
			}
		}
		return interpreter.unify(stream, interpreter.getEnvironment().getCurrentOutput().getStreamTerm());
	};

	/** ensure_loaded/1 - Load Prolog file if not already loaded */
	public static final ExecuteOnlyCode ENSURE_LOADED = (interpreter, backtrackMode, args) -> {
		Environment environment = interpreter.getEnvironment();
		PrologTextLoaderState state = environment.getPrologTextLoaderState();

		state.ensureLoaded(args[0]);
		environment.runInitialization(interpreter);

		for (PrologTextLoaderError error : state.getErrors()) {
			logger.error("{}", error);
		}
		return ExecuteOnlyCode.RC.SUCCESS_LAST;
	};

	/** flush_output/1 - Flush output stream */
	public static final ExecuteOnlyCode FLUSH_OUTPUT = (interpreter, backtrackMode, args) -> {
		PrologStream stream = interpreter.getEnvironment().resolveStream(args[0]);
		if (stream.getMode() != TermConstants.outputAtom) {
			PrologException.permissionError(TermConstants.outputAtom, TermConstants.streamAtom, args[0]);
		}
		stream.flushOutput(args[0]);
		return ExecuteOnlyCode.RC.SUCCESS_LAST;
	};

	/** get_byte/2 - Read a byte from stream */
	public static final ExecuteOnlyCode GET_BYTE = (interpreter, backtrackMode, args) -> {
		PrologStream stream = interpreter.getEnvironment().resolveStream(args[0]);
		Term in_byte = args[1];
		switch (in_byte) {
			case VariableTerm vt -> {} // Valid type
			case IntegerTerm ch -> {
				int b = ch.value;
				if (b < -1 || 255 < b) {
					PrologException.typeError(TermConstants.inByteAtom, in_byte);
				}
			}
			default -> {
				PrologException.typeError(TermConstants.inByteAtom, in_byte);
			}
		}
		Term rc = IntegerTerm.get(stream.getByte(args[0], interpreter));
		return interpreter.unify(in_byte, rc);
	};

	/** get_char/2 - Read a character from stream */
	public static final ExecuteOnlyCode GET_CHAR = (interpreter, backtrackMode, args) -> {
		PrologStream stream = interpreter.getEnvironment().resolveStream(args[0]);
		Term inchar = args[1];
		switch (inchar) {
			case VariableTerm vt -> {} // Valid type
			case AtomTerm ch -> {
				if (!(ch == PrologStream.endOfFileAtom) & !(ch.value.length() == 1)) {
					PrologException.typeError(TermConstants.inCharacterAtom, inchar);
				}
			}
			default -> {
				PrologException.typeError(TermConstants.inCharacterAtom, inchar);
			}
		}
		int inch = stream.getCode(args[0], interpreter);
		Term rc;
		if (inch == -1) {
			rc = PrologStream.endOfFileAtom;
		} else {
			rc = AtomTerm.get((char) inch);
		}
		return interpreter.unify(inchar, rc);
	};

	/** op/3 - Define or remove operator */
	public static final ExecuteOnlyCode OP = (interpreter, backtrackMode, args) -> {
		Term tpriority = args[0];
		Term topspec = args[1];
		Term tops = args[2];

		int priority = 0;
		SPECIFIER opspec = SPECIFIER.NONE;
		Set<AtomTerm> ops = new HashSet<>();
		OperatorSet opSet = interpreter.getEnvironment().getOperatorSet();

		// parse priority
		switch (tpriority) {
			case VariableTerm vt -> {
				PrologException.instantiationError(tpriority);
			}
			default -> {}
		}
		IntegerTerm itpriority = switch (tpriority) {
			case IntegerTerm it -> it;
			default -> {
				PrologException.typeError(TermConstants.integerAtom, tpriority);
				yield null; // Never reached
			}
		};
		priority = itpriority.value;
		if (priority < 0 || 1200 < priority) {
			PrologException.domainError(TermConstants.operatorPriorityAtom, tpriority);
		}
		// parse specifier
		switch (topspec) {
			case VariableTerm vt -> {
				PrologException.instantiationError(topspec);
			}
			default -> {}
		}
		AtomTerm atomSpec = switch (topspec) {
			case AtomTerm at -> at;
			default -> {
				PrologException.typeError(TermConstants.atomAtom, topspec);
				yield null; // Never reached
			}
		};

		opspec = SPECIFIER.fromAtom(atomSpec);

		if (opspec == SPECIFIER.NONE) {
			PrologException.domainError(TermConstants.operatorSpecifierAtom, topspec);
		}
		// parse ops
		switch (tops) {
			case AtomTerm at when tops == TermConstants.emptyListAtom -> {
				// do nothing
			}
			case AtomTerm at -> {
				validateOp(priority, opspec, at, opSet);
				ops.add(at);
			}
			case CompoundTerm ct -> {
				Term cur = tops;
				while (cur != TermConstants.emptyListAtom) {
					switch (cur) {
						case VariableTerm vt -> {
							PrologException.instantiationError(cur);
						}
						default -> {}
					}
					CompoundTerm ct2 = switch (cur) {
						case CompoundTerm c -> c;
						default -> {
							PrologException.typeError(TermConstants.listAtom, tops);
							yield null; // Never reached
						}
					};
					if (ct2.tag != TermConstants.listTag) {
						PrologException.typeError(TermConstants.listAtom, tops);
					}
					Term head = ct2.args[0].dereference();
					cur = ct2.args[1].dereference();
					switch (head) {
						case VariableTerm vt -> {
							PrologException.instantiationError(head);
						}
						default -> {}
					}
					AtomTerm ahead = switch (head) {
						case AtomTerm a -> a;
						default -> {
							PrologException.typeError(TermConstants.atomAtom, head);
							yield null; // Never reached
						}
					};
					validateOp(priority, opspec, ahead, opSet);
					ops.add(ahead);
				}
			}
			default -> {
				PrologException.typeError(TermConstants.listAtom, tops);
			}
		}
		if (priority == 0) {
			Iterator<AtomTerm> i = ops.iterator();
			while (i.hasNext()) {
				AtomTerm op = i.next();
				opSet.remove(opspec, op.value);
			}
		} else {
			Iterator<AtomTerm> i = ops.iterator();
			while (i.hasNext()) {
				AtomTerm op = i.next();
				opSet.add(priority, opspec, op.value);
			}
		}
		return ExecuteOnlyCode.RC.SUCCESS_LAST;
	};

	/** open/4 - Open a file stream */
	public static final ExecuteOnlyCode OPEN = (interpreter, backtrackMode, args) -> {
		Term tsource_sink = args[0];
		Term tmode = args[1];
		Term tstream = args[2];
		Term optionsList = args[3];

		AtomTerm source_sink;
		AtomTerm mode;
		VariableTerm vstream;

		// check source/sink
		switch (tsource_sink) {
			case VariableTerm vt -> {
				PrologException.instantiationError(tsource_sink);
			}
			default -> {}
		}
		source_sink = switch (tsource_sink) {
			case AtomTerm at -> at;
			default -> {
				PrologException.domainError(TermConstants.sourceSinkAtom, tsource_sink);
				yield null; // Never reached
			}
		};

		// check mode
		switch (tmode) {
			case VariableTerm vt -> {
				PrologException.instantiationError(tmode);
			}
			default -> {}
		}
		mode = switch (tmode) {
			case AtomTerm at -> at;
			default -> {
				PrologException.typeError(TermConstants.atomAtom, tmode);
				yield null; // Never reached
			}
		};
		if (tmode != PrologStream.readAtom && tmode != PrologStream.writeAtom && tmode != PrologStream.appendAtom) {
			PrologException.domainError(TermConstants.ioModeAtom, tmode);
		}

		// check stream
		vstream = switch (tstream) {
			case VariableTerm vt -> vt;
			default -> {
				PrologException.typeError(TermConstants.variableAtom, tstream);
				yield null; // Never reached
			}
		};

		PrologStream.OpenOptions options = new PrologStream.OpenOptions(source_sink, mode, interpreter.getEnvironment());
		// parse options
		Term cur = optionsList;
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
					PrologException.typeError(TermConstants.listAtom, optionsList);
					yield null; // Never reached
				}
			};
			if (ct.tag != TermConstants.listTag) {
				PrologException.typeError(TermConstants.listAtom, optionsList);
			}
			Term head = ct.args[0].dereference();
			cur = ct.args[1].dereference();
			switch (head) {
				case VariableTerm vt -> {
					PrologException.instantiationError(head);
				}
				default -> {}
			}
			CompoundTerm op = switch (head) {
				case CompoundTerm c -> c;
				default -> {
					PrologException.domainError(TermConstants.streamOptionAtom, head);
					yield null; // Never reached
				}
			};
			if (op.tag == PrologStream.typeTag) {
				Term val = op.args[0].dereference();
				if (val != PrologStream.textAtom && val != PrologStream.binaryAtom) {
					PrologException.domainError(TermConstants.streamOptionAtom, op);
				}
				options.type = (AtomTerm) val;
			} else if (op.tag == PrologStream.repositionTag) {
				Term val = op.args[0].dereference();
				if (val != TermConstants.trueAtom && val != TermConstants.falseAtom) {
					PrologException.domainError(TermConstants.streamOptionAtom, op);
				}
				options.reposition = (AtomTerm) val;
			} else if (op.tag == PrologStream.aliasTag) {
				Term val = op.args[0].dereference();
				AtomTerm aval = switch (val) {
					case AtomTerm at -> at;
					default -> {
						PrologException.domainError(TermConstants.streamOptionAtom, op);
						yield null; // Never reached
					}
				};
				options.aliases.add(aval);
			} else if (op.tag == PrologStream.eofActionTag) {
				Term val = op.args[0].dereference();
				if (val != PrologStream.errorAtom && val != PrologStream.eofCodeAtom && val != PrologStream.resetAtom) {
					PrologException.domainError(TermConstants.streamOptionAtom, op);
				}
				options.reposition = (AtomTerm) val;
			} else {
				PrologException.domainError(TermConstants.streamOptionAtom, op);
			}
		}
		options.filename = source_sink;
		options.mode = mode;
		vstream.value = interpreter.getEnvironment().open(source_sink, mode, options);
		interpreter.addVariableUndo(vstream);
		return ExecuteOnlyCode.RC.SUCCESS_LAST;
	};

	/** peek_byte/2 - Peek at next byte without consuming it */
	public static final ExecuteOnlyCode PEEK_BYTE = (interpreter, backtrackMode, args) -> {
		PrologStream stream = interpreter.getEnvironment().resolveStream(args[0]);
		Term in_byte = args[1];
		switch (in_byte) {
			case VariableTerm vt -> {} // Valid type
			case IntegerTerm ch -> {
				int b = ch.value;
				if (b < -1 || 255 < b) {
					PrologException.typeError(TermConstants.inByteAtom, in_byte);
				}
			}
			default -> {
				PrologException.typeError(TermConstants.inByteAtom, in_byte);
			}
		}
		Term rc = IntegerTerm.get(stream.peekByte(args[0], interpreter));
		return interpreter.unify(in_byte, rc);
	};

	/** peek_char/2 - Peek at next character without consuming it */
	public static final ExecuteOnlyCode PEEK_CHAR = (interpreter, backtrackMode, args) -> {
		PrologStream stream = interpreter.getEnvironment().resolveStream(args[0]);
		Term inchar = args[1];
		switch (inchar) {
			case VariableTerm vt -> {} // Valid type
			case AtomTerm ch -> {
				if (!(ch == PrologStream.endOfFileAtom) & !(ch.value.length() == 1)) {
					PrologException.typeError(TermConstants.inCharacterAtom, inchar);
				}
			}
			default -> {
				PrologException.typeError(TermConstants.inCharacterAtom, inchar);
			}
		}
		int inch = stream.peekCode(args[0], interpreter);
		Term rc;
		if (inch == -1) {
			rc = PrologStream.endOfFileAtom;
		} else {
			rc = AtomTerm.get((char) inch);
		}
		return interpreter.unify(inchar, rc);
	};

	/** put_byte/2 - Write a byte to stream */
	public static final ExecuteOnlyCode PUT_BYTE = (interpreter, backtrackMode, args) -> {
		PrologStream stream = interpreter.getEnvironment().resolveStream(args[0]);
		Term in_byte = args[1];
		int b = switch (in_byte) {
			case VariableTerm vt -> {
				PrologException.instantiationError(in_byte);
				yield 0; // Never reached
			}
			case IntegerTerm ch -> {
				int value = ch.value;
				if (value < 0 || 255 < value) {
					PrologException.typeError(TermConstants.inByteAtom, in_byte);
				}
				yield value;
			}
			default -> {
				PrologException.typeError(TermConstants.inByteAtom, in_byte);
				yield 0; // Never reached
			}
		};
		stream.putByte(args[0], interpreter, b);
		return ExecuteOnlyCode.RC.SUCCESS_LAST;
	};

	/** put_char/2 - Write a character to stream */
	public static final ExecuteOnlyCode PUT_CHAR = (interpreter, backtrackMode, args) -> {
		PrologStream stream = interpreter.getEnvironment().resolveStream(args[0]);
		Term outchar = args[1];
		char ch = switch (outchar) {
			case VariableTerm vt -> {
				PrologException.instantiationError(outchar);
				yield (char) 0; // Never reached
			}
			case AtomTerm ach -> {
				if (ach.value.length() == 1) {
					yield ach.value.charAt(0);
				} else {
					PrologException.typeError(TermConstants.characterAtom, outchar);
					yield (char) 0; // Never reached
				}
			}
			default -> {
				PrologException.typeError(TermConstants.characterAtom, outchar);
				yield (char) 0; // Never reached
			}
		};
		stream.putCode(args[0], interpreter, ch);
		return ExecuteOnlyCode.RC.SUCCESS_LAST;
	};

	/** read_term/3 - Read a term from stream with options */
	public static final ExecuteOnlyCode READ_TERM = (interpreter, backtrackMode, args) -> {
		Environment environment = interpreter.getEnvironment();
		PrologStream stream = environment.resolveStream(args[0]);
		Term optionsList = args[2];
		ReadOptions options = new ReadOptions(environment.getOperatorSet());

		List<Term> singletons = new ArrayList<>();
		List<Term> variableLists = new ArrayList<>();
		List<Term> vnlists = new ArrayList<>();

		// parse and unify options
		Term cur = optionsList;
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
					PrologException.typeError(TermConstants.listAtom, optionsList);
					yield null; // Never reached
				}
			};
			if (ct.tag != TermConstants.listTag) {
				PrologException.typeError(TermConstants.listAtom, optionsList);
			}
			Term head = ct.args[0].dereference();
			cur = ct.args[1].dereference();
			switch (head) {
				case VariableTerm vt -> {
					PrologException.instantiationError(head);
				}
				default -> {}
			}
			CompoundTerm op = switch (head) {
				case CompoundTerm c -> c;
				default -> {
					PrologException.domainError(TermConstants.readOptionAtom, head);
					yield null; // Never reached
				}
			};
			if (op.tag == TermConstants.variablesTag) {
				variableLists.add(op.args[0]);
			} else if (op.tag == TermConstants.singletonsTag) {
				singletons.add(op.args[0]);
			} else if (op.tag == TermConstants.variableNamesTag) {
				vnlists.add(op.args[0]);
			} else {
				PrologException.domainError(TermConstants.readOptionAtom, head);
			}
		}

		Term readTerm = stream.readTerm(args[0], interpreter, options);
		int undoPos = interpreter.getUndoPosition();

		try {
			ExecuteOnlyCode.RC rc = interpreter.simpleUnify(args[1], readTerm);
			if (rc == ExecuteOnlyCode.RC.FAIL) {
				interpreter.undo(undoPos);
				return ExecuteOnlyCode.RC.FAIL;
			}
			Iterator<Term> i = singletons.iterator();
			if (i.hasNext()) {
				Term singletonsList = mapToList(options.singletons);
				while (i.hasNext()) {
					Term t = i.next();
					t = t.dereference();
					rc = interpreter.simpleUnify(t, singletonsList);
					if (rc == ExecuteOnlyCode.RC.FAIL) {
						interpreter.undo(undoPos);
						return ExecuteOnlyCode.RC.FAIL;
					}
				}
			}
			i = vnlists.iterator();
			if (i.hasNext()) {
				Term vnlist = mapToList(options.variableNames);
				while (i.hasNext()) {
					Term t = i.next();
					t = t.dereference();
					rc = interpreter.simpleUnify(t, vnlist);
					if (rc == ExecuteOnlyCode.RC.FAIL) {
						interpreter.undo(undoPos);
						return ExecuteOnlyCode.RC.FAIL;
					}
				}
			}
			i = variableLists.iterator();
			if (i.hasNext()) {
				Term vnlist = CompoundTerm.getList(options.variables);
				while (i.hasNext()) {
					Term t = i.next();
					t = t.dereference();
					rc = interpreter.simpleUnify(t, vnlist);
					if (rc == ExecuteOnlyCode.RC.FAIL) {
						interpreter.undo(undoPos);
						return ExecuteOnlyCode.RC.FAIL;
					}
				}
			}
			return ExecuteOnlyCode.RC.SUCCESS_LAST;
		} catch (PrologException ex) {
			interpreter.undo(undoPos);
			throw ex;
		}
	};

	/** set_input/1 - Set current input stream */
	public static final ExecuteOnlyCode SET_INPUT = (interpreter, backtrackMode, args) -> {
		PrologStream stream = interpreter.getEnvironment().resolveStream(args[0]);
		if (stream.getMode() != TermConstants.inputAtom) {
			PrologException.permissionError(TermConstants.inputAtom, TermConstants.streamAtom, args[0]);
		}
		interpreter.getEnvironment().setCurrentInput(stream);
		return ExecuteOnlyCode.RC.SUCCESS_LAST;
	};

	/** set_output/1 - Set current output stream */
	public static final ExecuteOnlyCode SET_OUTPUT = (interpreter, backtrackMode, args) -> {
		PrologStream stream = interpreter.getEnvironment().resolveStream(args[0]);
		if (stream.getMode() != TermConstants.outputAtom) {
			PrologException.permissionError(TermConstants.outputAtom, TermConstants.streamAtom, args[0]);
		}
		interpreter.getEnvironment().setCurrentOutput(stream);
		return ExecuteOnlyCode.RC.SUCCESS_LAST;
	};

	/** set_stream_position/2 - Set stream position */
	public static final ExecuteOnlyCode SET_STREAM_POSITION = (interpreter, backtrackMode, args) -> {
		PrologStream stream = interpreter.getEnvironment().resolveStream(args[0]);
		stream.setPosition(args[0], interpreter, args[1]);
		return ExecuteOnlyCode.RC.SUCCESS_LAST;
	};

	/** write_term/3 - Write a term to stream with options */
	public static final ExecuteOnlyCode WRITE_TERM = (interpreter, backtrackMode, args) -> {
		PrologStream stream = interpreter.getEnvironment().resolveStream(args[0]);
		Term optionsList = args[2];
		WriteOptions options = new WriteOptions(interpreter.getEnvironment().getOperatorSet());

		// parse options
		Term cur = optionsList;
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
					PrologException.typeError(TermConstants.listAtom, optionsList);
					yield null; // Never reached
				}
			};
			if (ct.tag != TermConstants.listTag) {
				PrologException.typeError(TermConstants.listAtom, optionsList);
			}
			Term head = ct.args[0].dereference();
			cur = ct.args[1].dereference();
			switch (head) {
				case VariableTerm vt -> {
					PrologException.instantiationError(head);
				}
				default -> {}
			}
			CompoundTerm op = switch (head) {
				case CompoundTerm c -> c;
				default -> {
					PrologException.domainError(TermConstants.readOptionAtom, head);
					yield null; // Never reached
				}
			};
			if (op.tag == TermConstants.quotedTag) {
				Term val = op.args[0].dereference();
				if (val != TermConstants.trueAtom && val != TermConstants.falseAtom) {
					PrologException.domainError(TermConstants.readOptionAtom, head);
				}
				options.quoted = val == TermConstants.trueAtom;
			} else if (op.tag == TermConstants.ignoreOpsTag) {
				Term val = op.args[0].dereference();
				if (val != TermConstants.trueAtom && val != TermConstants.falseAtom) {
					PrologException.domainError(TermConstants.readOptionAtom, head);
				}
				options.ignoreOps = val == TermConstants.trueAtom;
			} else if (op.tag == TermConstants.numbervarsTag) {
				Term val = op.args[0].dereference();
				if (val != TermConstants.trueAtom && val != TermConstants.falseAtom) {
					PrologException.domainError(TermConstants.readOptionAtom, head);
				}
				options.numbervars = val == TermConstants.trueAtom;
			} else {
				PrologException.domainError(TermConstants.writeOptionAtom, head);
			}
		}
		stream.writeTerm(args[0], interpreter, options, args[1]);
		return ExecuteOnlyCode.RC.SUCCESS_LAST;
	};

	// ============================================================================
	// Complex Predicate Implementations (with BacktrackInfo)
	// ============================================================================

	/** current_char_conversion/2 - Query character conversion (backtracking) */
	public static final ExecuteOnlyCode CURRENT_CHAR_CONVERSION = new ExecuteOnlyCode() {
		@Override
		public RC execute(final Interpreter interpreter, final boolean backtrackMode, final Term[] args) throws PrologException {
			if (backtrackMode) {
				CharConvBacktrackInfo bi = (CharConvBacktrackInfo) interpreter.popBacktrackInfo();
				interpreter.undo(bi.startUndoPosition);
				return nextSolution(interpreter, bi);
			} else {
				switch (args[0]) {
					case AtomTerm at -> {
						if (at.value.length() != 1) {
							PrologException.representationError(TermConstants.characterAtom);
						}
					}
					case VariableTerm vt -> {} // Valid
					default -> {
						PrologException.representationError(TermConstants.characterAtom);
					}
				}
				switch (args[1]) {
					case AtomTerm at -> {
						if (at.value.length() != 1) {
							PrologException.representationError(TermConstants.characterAtom);
						}
					}
					case VariableTerm vt -> {} // Valid
					default -> {
						PrologException.representationError(TermConstants.characterAtom);
					}
				}

				return switch (args[0]) {
					case VariableTerm v0 when args[1] instanceof VariableTerm -> {
						CharConvBacktrackInfo bi = new CharConvBacktrackInfo();
						bi.startUndoPosition = interpreter.getUndoPosition();
						bi.arg0 = args[0];
						bi.arg1 = args[1];
						yield nextSolution(interpreter, bi);
					}
					case VariableTerm v0 -> {
						CharConvBacktrackInfo bi = new CharConvBacktrackInfo();
						bi.startUndoPosition = interpreter.getUndoPosition();
						bi.arg0 = args[0];
						bi.charIt = interpreter.getEnvironment().getConversionTable().convertsTo(((AtomTerm) args[1]).value.charAt(0))
								.iterator();
						yield nextSolution(interpreter, bi);
					}
					case AtomTerm a0 when args[1] instanceof VariableTerm -> {
						Term res = AtomTerm.get(Character.toString(interpreter.getEnvironment().getConversionTable().convert(
								a0.value.charAt(0))));
						yield interpreter.unify(args[1], res);
					}
					default -> {
						// not possible
						PrologException.systemError();
						yield RC.FAIL;
					}
				};
			}
		}

		private RC nextSolution(final Interpreter interpreter, final CharConvBacktrackInfo bi) throws PrologException {
			if (bi.charIt != null) {
				while (bi.charIt.hasNext()) {
					Term res = AtomTerm.get(Character.toString(bi.charIt.next()));
					RC rc = interpreter.unify(bi.arg0, res);
					if (rc == RC.FAIL) {
						interpreter.undo(bi.startUndoPosition);
						continue;
					}
					interpreter.pushBacktrackInfo(bi);
					return RC.SUCCESS;
				}
			} else {
				while (bi.counter < Character.MAX_CODE_POINT) {
					if (!Character.isDefined(bi.counter)) {
						bi.counter++;
						continue;
					}
					Term res = AtomTerm.get(Character.toString(bi.counter));
					RC rc = interpreter.unify(bi.arg0, res);
					if (rc == RC.FAIL) {
						bi.counter++;
						interpreter.undo(bi.startUndoPosition);
						continue;
					}
					Term res2 = AtomTerm.get(Character.toString(interpreter.getEnvironment().getConversionTable().convert(bi.counter)));
					rc = interpreter.unify(bi.arg1, res2);
					if (rc == RC.FAIL) {
						bi.counter++;
						interpreter.undo(bi.startUndoPosition);
						continue;
					}
					bi.counter++;
					interpreter.pushBacktrackInfo(bi);
					return RC.SUCCESS;
				}
			}
			return RC.FAIL;
		}
	};

	/** current_op/3 - Query current operators (backtracking) */
	public static final ExecuteOnlyCode CURRENT_OP = new ExecuteOnlyCode() {
		@Override
		public RC execute(final Interpreter interpreter, final boolean backtrackMode, final Term[] args) throws PrologException {
			if (backtrackMode) {
				CurrentOpBacktrackInfo bi = (CurrentOpBacktrackInfo) interpreter.popBacktrackInfo();
				interpreter.undo(bi.startUndoPosition);
				return nextSolution(interpreter, bi);
			} else {
				Term op = args[2];
				Term specifier = args[1];
				Term priority = args[0];
				// validate args
				switch (op) {
					case AtomTerm at -> {}
					case VariableTerm vt -> {}
					default -> {
						PrologException.typeError(TermConstants.atomAtom, op);
					}
				}
				switch (specifier) {
					case VariableTerm vt -> {}
					case AtomTerm at when specifier == TermConstants.xfxAtom
							|| specifier == TermConstants.xfyAtom || specifier == TermConstants.yfxAtom
							|| specifier == TermConstants.fxAtom || specifier == TermConstants.fyAtom
							|| specifier == TermConstants.xfAtom || specifier == TermConstants.yfAtom -> {}
					default -> {
						PrologException.domainError(TermConstants.operatorSpecifierAtom, specifier);
					}
				}
				switch (priority) {
					case VariableTerm vt -> {} // Valid
					case IntegerTerm it -> {
						if (it.value <= 0 || 1200 < it.value) {
							PrologException.domainError(TermConstants.operatorPriorityAtom, priority);
						}
					}
					default -> {
						PrologException.domainError(TermConstants.operatorPriorityAtom, priority);
					}
				}

				// prepare and exec
				List<AtomTerm> ops = new ArrayList<>();
				List<AtomTerm> specifiers = new ArrayList<>();
				List<IntegerTerm> priorities = new ArrayList<>();

				Iterator<Operator> i = interpreter.getEnvironment().getOperatorSet().getOperators().iterator();
				while (i.hasNext()) {
					Operator o = i.next();
					ops.add(o.tag.functor);
					priorities.add(IntegerTerm.get(o.priority));
					AtomTerm a = o.specifier.getAtom();
					specifiers.add(a);
				}
				CurrentOpBacktrackInfo bi = new CurrentOpBacktrackInfo();
				bi.startUndoPosition = interpreter.getUndoPosition();
				bi.ops = ops.iterator();
				bi.specifiers = specifiers.iterator();
				bi.priorities = priorities.iterator();
				bi.op = op;
				bi.specifier = specifier;
				bi.priority = priority;
				return nextSolution(interpreter, bi);
			}
		}

		private RC nextSolution(final Interpreter interpreter, final CurrentOpBacktrackInfo bi) throws PrologException {
			try {
				while (bi.ops.hasNext()) {
					Term op = bi.ops.next();
					Term specifier = bi.specifiers.next();
					Term priority = bi.priorities.next();
					if (interpreter.simpleUnify(op, bi.op) == RC.SUCCESS_LAST
							&& interpreter.simpleUnify(specifier, bi.specifier) == RC.SUCCESS_LAST
							&& interpreter.simpleUnify(priority, bi.priority) == RC.SUCCESS_LAST) {
						interpreter.pushBacktrackInfo(bi);
						return RC.SUCCESS;
					}
					interpreter.undo(bi.startUndoPosition);
				}
				return RC.FAIL;
			} catch (PrologException ex) {
				interpreter.undo(bi.startUndoPosition);
				throw ex;
			}
		}
	};

	/** stream_property/2 - Query stream properties (backtracking) */
	public static final ExecuteOnlyCode STREAM_PROPERTY = new ExecuteOnlyCode() {
		@Override
		public RC execute(final Interpreter interpreter, final boolean backtrackMode, final Term[] args) throws PrologException {
			if (backtrackMode) {
				StreamPropertyBacktrackInfo bi = (StreamPropertyBacktrackInfo) interpreter.popBacktrackInfo();
				interpreter.undo(bi.startUndoPosition);
				return nextSolution(interpreter, bi);
			} else {
				Term stream = args[0];
				switch (stream) {
					case VariableTerm vt -> {} // Valid
					case JavaObjectTerm jt -> {
						switch (jt.value) {
							case PrologStream ps -> {
								ps.checkExists();
							}
							default -> {
								PrologException.domainError(TermConstants.streamAtom, stream);
							}
						}
					}
					default -> {
						PrologException.domainError(TermConstants.streamAtom, stream);
					}
				}
				Term property = args[1];
				switch (property) {
					case VariableTerm vt -> {} // Valid
					case AtomTerm at when property == TermConstants.inputAtom || property == TermConstants.outputAtom -> {} // Valid
					case CompoundTerm ct -> {
						if (ct.tag == PrologStream.filenameTag || ct.tag == PrologStream.aliasTag) {
							switch (ct.args[0]) {
								case AtomTerm a -> {}
								case VariableTerm v -> {}
								default -> {
									PrologException.domainError(TermConstants.streamPropertyAtom, property);
								}
							}
						} else if (ct.tag == PrologStream.endOfStreamTag) {
							switch (ct.args[0]) {
								case VariableTerm v -> {}
								case AtomTerm a when ct.args[0] == PrologStream.atAtom || ct.args[0] == PrologStream.pastAtom
										|| ct.args[0] == PrologStream.notAtom -> {}
								default -> {
									PrologException.domainError(TermConstants.streamPropertyAtom, property);
								}
							}
						} else if (ct.tag == PrologStream.eofActionTag) {
							switch (ct.args[0]) {
								case VariableTerm v -> {}
								case AtomTerm a when ct.args[0] == PrologStream.errorAtom || ct.args[0] == PrologStream.eofCodeAtom
										|| ct.args[0] == PrologStream.resetAtom -> {}
								default -> {
									PrologException.domainError(TermConstants.streamPropertyAtom, property);
								}
							}
						} else if (ct.tag == PrologStream.repositionTag || ct.tag == PrologStream.positionTag) {
							switch (ct.args[0]) {
								case VariableTerm v -> {}
								case AtomTerm a when ct.args[0] == TermConstants.trueAtom || ct.args[0] == TermConstants.falseAtom -> {}
								default -> {
									PrologException.domainError(TermConstants.streamPropertyAtom, property);
								}
							}
						} else if (ct.tag == PrologStream.typeTag) {
							switch (ct.args[0]) {
								case VariableTerm v -> {}
								case AtomTerm a when ct.args[0] == PrologStream.textAtom || ct.args[0] == PrologStream.binaryAtom -> {}
								default -> {
									PrologException.domainError(TermConstants.streamPropertyAtom, property);
								}
							}
						} else {
							PrologException.domainError(TermConstants.streamPropertyAtom, property);
						}
					}
					default -> {
						PrologException.domainError(TermConstants.streamPropertyAtom, property);
					}
				}
				StreamPropertyBacktrackInfo bi = new StreamPropertyBacktrackInfo();
				bi.startUndoPosition = interpreter.getUndoPosition();
				bi.stream2option = interpreter.getEnvironment().getStreamProperties();
				bi.streams = bi.stream2option.keySet().iterator();
				bi.stream = args[0];
				bi.property = args[1];
				return nextSolution(interpreter, bi);
			}
		}

		private RC nextSolution(final Interpreter interpreter, final StreamPropertyBacktrackInfo bi) throws PrologException {
			int undoPos = interpreter.getUndoPosition();
			while (true) {
				if (bi.options == null || !bi.options.hasNext()) {
					if (bi.streams.hasNext()) {
						PrologStream stream = bi.streams.next();
						bi.currentStream = stream.getStreamTerm();
						bi.options = bi.stream2option.get(stream).iterator();
						continue;
					} else {
						return RC.FAIL;
					}
				}
				Term currentProp = bi.options.next();
				if (interpreter.simpleUnify(bi.stream, bi.currentStream) == RC.SUCCESS_LAST
						&& interpreter.simpleUnify(bi.property, currentProp) == RC.SUCCESS_LAST) {
					interpreter.pushBacktrackInfo(bi);
					return RC.SUCCESS;
				}
				interpreter.undo(undoPos);
			}
		}
	};

	/** format/2 - format(+Format, +Arguments) */
	public static final ExecuteOnlyCode FORMAT = (interpreter, backtrackMode, args) -> {
		Environment env = interpreter.getEnvironment();
		PrologStream stream = env.getCurrentOutput();
		return formatImpl(interpreter, stream, args[0], args[1]);
	};

	/** format/3 - format(+Stream, +Format, +Arguments) */
	public static final ExecuteOnlyCode FORMAT_3 = (interpreter, backtrackMode, args) -> {
		Environment env = interpreter.getEnvironment();
		PrologStream stream = env.resolveStream(args[0]);
		return formatImpl(interpreter, stream, args[1], args[2]);
	};

	/**
	 * Common implementation for format/2 and format/3.
	 * Supports format directives: ~w (write), ~d (decimal), ~n (newline), ~a (atom)
	 */
	private static ExecuteOnlyCode.RC formatImpl(final Interpreter interpreter, final PrologStream stream,
			final Term formatTerm, final Term argsTerm) throws PrologException {

		// Get format string
		AtomTerm formatAtom = switch (formatTerm) {
			case VariableTerm vt -> {
				PrologException.instantiationError(formatTerm);
				yield null; // Never reached
			}
			case AtomTerm at -> at;
			default -> {
				PrologException.typeError(TermConstants.atomAtom, formatTerm);
				yield null; // Never reached
			}
		};

		// Convert arguments to list
		List<Term> argList = new ArrayList<>();
		Term current = argsTerm;
		while (current != TermConstants.emptyListAtom) {
			switch (current) {
				case VariableTerm vt -> {
					PrologException.instantiationError(current);
				}
				case CompoundTerm ct -> {
					if (ct.tag != TermConstants.listTag) {
						PrologException.typeError(TermConstants.listAtom, argsTerm);
					}
					argList.add(ct.args[0].dereference());
					current = ct.args[1].dereference();
				}
				default -> {
					PrologException.typeError(TermConstants.listAtom, argsTerm);
				}
			}
		}

		// Process format string
		String format = formatAtom.value;
		int argIndex = 0;
		StringBuilder output = new StringBuilder();

		for (int i = 0; i < format.length(); i++) {
			char c = format.charAt(i);
			if (c == '~' && i + 1 < format.length()) {
				char directive = format.charAt(i + 1);
				i++; // Skip directive character

				switch (directive) {
					case 'w', 'a' -> {
						// Write term or atom
						if (argIndex >= argList.size()) {
							PrologException.domainError(TermConstants.formatAtom, formatTerm);
						}
						output.append(argList.get(argIndex++).toString());
					}
					case 'd', 'D' -> {
						// Decimal integer
						if (argIndex >= argList.size()) {
							PrologException.domainError(TermConstants.formatAtom, formatTerm);
						}
						Term arg = argList.get(argIndex++);
						switch (arg) {
							case IntegerTerm it -> output.append(it.value);
							default -> PrologException.typeError(TermConstants.integerAtom, arg);
						}
					}
					case 'n' -> {
						// Newline
						output.append('\n');
					}
					case '~' -> {
						// Escaped tilde
						output.append('~');
					}
					default -> {
						// Unknown directive - just output it
						output.append('~').append(directive);
					}
				}
			} else {
				output.append(c);
			}
		}

		// Write output to stream
		try {
			stream.putCodeSequence(stream.getStreamTerm(), interpreter, output.toString());
		} catch (PrologException ex) {
			throw ex;
		}

		return ExecuteOnlyCode.RC.SUCCESS_LAST;
	}

	// ============================================================================
	// BacktrackInfo Classes
	// ============================================================================

	private static class CharConvBacktrackInfo extends BacktrackInfo {
		int startUndoPosition;
		Term arg0;
		Term arg1;
		Iterator<Character> charIt;
		char counter;

		CharConvBacktrackInfo() {
			super(-1, -1);
		}
	}

	private static class CurrentOpBacktrackInfo extends BacktrackInfo {
		int startUndoPosition;
		Iterator<AtomTerm> ops;
		Iterator<AtomTerm> specifiers;
		Iterator<IntegerTerm> priorities;
		Term op;
		Term specifier;
		Term priority;

		CurrentOpBacktrackInfo() {
			super(-1, -1);
		}
	}

	private static class StreamPropertyBacktrackInfo extends BacktrackInfo {
		int startUndoPosition;
		Map<PrologStream, List<Term>> stream2option;
		Iterator<PrologStream> streams;
		Term currentStream;
		Iterator<Term> options;
		Term stream;
		Term property;

		StreamPropertyBacktrackInfo() {
			super(-1, -1);
		}
	}
}
