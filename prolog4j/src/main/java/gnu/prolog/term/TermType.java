/* GNU Prolog for Java
 * Copyright (C) 1997-1999  Constantine Plotnikov
 * Copyright (C) 2025       Modernization
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
package gnu.prolog.term;

/**
 * Enum representing the type of a Prolog term.
 * Replaces the integer constants previously defined in Term class.
 *
 * @since 0.3.0
 */
public enum TermType {
	/** Unknown term type */
	UNKNOWN,
	/** Variable term */
	VARIABLE,
	/** Java object term */
	JAVA_OBJECT,
	/** Floating point number term */
	FLOAT,
	/** Integer number term */
	INTEGER,
	/** Arbitrary precision decimal number term */
	DECIMAL,
	/** Atom term */
	ATOM,
	/** Compound term */
	COMPOUND;

	/**
	 * Check if this term type represents a numeric type (INTEGER, FLOAT, or DECIMAL).
	 * @return true if this is INTEGER, FLOAT, or DECIMAL
	 */
	public boolean isNumeric() {
		return this == INTEGER || this == FLOAT || this == DECIMAL;
	}

	/**
	 * Check if this term type represents an atomic type (ATOM, INTEGER, FLOAT, or DECIMAL).
	 * @return true if this is an atomic type
	 */
	public boolean isAtomic() {
		return this == ATOM || this == INTEGER || this == FLOAT || this == DECIMAL;
	}

	/**
	 * Check if this term type is comparable for standard term ordering.
	 * @return true if this type can be ordered
	 */
	public boolean isComparable() {
		return this != UNKNOWN;
	}
}
