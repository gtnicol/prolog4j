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
package gnu.prolog.vm.builtins.uuid;

import gnu.prolog.term.AtomTerm;
import gnu.prolog.term.IntegerTerm;
import gnu.prolog.term.Term;
import gnu.prolog.term.VariableTerm;
import gnu.prolog.vm.ExecuteOnlyCode;
import gnu.prolog.vm.Interpreter;
import gnu.prolog.vm.PrologException;
import gnu.prolog.vm.TermConstants;

import java.util.UUID;

/**
 * Factory class for UUID predicates.
 * Provides implementations for UUID generation, comparison, and property inspection operations.
 */
public final class Predicates {

	private Predicates() {
		throw new AssertionError("Utility class should not be instantiated");
	}

	// ============================================================================
	// Constants
	// ============================================================================

	private static final AtomTerm UUID_ATOM = AtomTerm.get("uuid");

	// ============================================================================
	// Helper Methods
	// ============================================================================

	/**
	 * Get the UUID from an atom term. Returns null in case of an invalid UUID.
	 *
	 * @param value the term to extract UUID from
	 * @return the UUID from an atom term. Returns null in case of an invalid UUID.
	 * @throws PrologException if the term is not an atom
	 */
	private static UUID getUUID(final Term value) throws PrologException {
		String data = switch (value) {
			case AtomTerm at -> at.value;
			default -> {
				PrologException.typeError(TermConstants.atomAtom, value);
				yield null; // Never reached
			}
		};
		try {
			return UUID.fromString(data);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	// ============================================================================
	// Predicate Implementations
	// ============================================================================

	/** uuid3/2 - Generate UUID version 3 (name-based using MD5) */
	public static final ExecuteOnlyCode UUID3 = new ExecuteOnlyCode() {
		@Override
		public RC execute(final Interpreter interpreter, final boolean backtrackMode, final Term[] args) throws PrologException {
			switch (args[0]) {
				case VariableTerm vt -> {} // Valid type
				default -> {
					PrologException.instantiationError(args[0]);
				}
			}
			AtomTerm name = switch (args[1]) {
				case AtomTerm at -> at;
				default -> {
					PrologException.typeError(TermConstants.atomAtom, args[1]);
					yield null; // Never reached
				}
			};
			Term uuidTerm = AtomTerm.get(UUID.nameUUIDFromBytes(name.value.getBytes()).toString());
			return interpreter.unify(args[0], uuidTerm);
		}
	};

	/** uuid4/1 - Generate UUID version 4 (random) */
	public static final ExecuteOnlyCode UUID4 = new ExecuteOnlyCode() {
		@Override
		public RC execute(final Interpreter interpreter, final boolean backtrackMode, final Term[] args) throws PrologException {
			switch (args[0]) {
				case VariableTerm vt -> {} // Valid type
				default -> {
					PrologException.instantiationError(args[0]);
				}
			}
			Term uuidTerm = AtomTerm.get(UUID.randomUUID().toString());
			return interpreter.unify(args[0], uuidTerm);
		}
	};

	/** uuid_compare/3 - Compare two UUIDs */
	public static final ExecuteOnlyCode UUID_COMPARE = new ExecuteOnlyCode() {
		@Override
		public RC execute(final Interpreter interpreter, final boolean backtrackMode, final Term[] args) throws PrologException {
			UUID uuid1 = getUUID(args[1]);
			UUID uuid2 = getUUID(args[2]);
			if (uuid1 == null || uuid2 == null) {
				return RC.FAIL;
			}
			int cmp = uuid1.compareTo(uuid2);
			if (cmp > 0) {
				return interpreter.unify(args[0], AtomTerm.get(">"));
			} else if (cmp < 0) {
				return interpreter.unify(args[0], AtomTerm.get("<"));
			}
			return interpreter.unify(args[0], AtomTerm.get("="));
		}
	};

	/** uuid_variant/2 - Get UUID variant */
	public static final ExecuteOnlyCode UUID_VARIANT = new ExecuteOnlyCode() {
		@Override
		public RC execute(final Interpreter interpreter, final boolean backtrackMode, final Term[] args) throws PrologException {
			UUID uuid = getUUID(args[0]);
			if (uuid == null) {
				PrologException.domainError(UUID_ATOM, args[0]);
			}
			return interpreter.unify(args[1], IntegerTerm.get(uuid.variant()));
		}
	};

	/** uuid_version/2 - Get UUID version */
	public static final ExecuteOnlyCode UUID_VERSION = new ExecuteOnlyCode() {
		@Override
		public RC execute(final Interpreter interpreter, final boolean backtrackMode, final Term[] args) throws PrologException {
			UUID uuid = getUUID(args[0]);
			if (uuid == null) {
				PrologException.domainError(UUID_ATOM, args[0]);
			}
			return interpreter.unify(args[1], IntegerTerm.get(uuid.version()));
		}
	};
}
