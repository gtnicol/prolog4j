/* GNU Prolog for Java
 * Copyright (C) 2025
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

import java.math.BigDecimal;
import java.math.MathContext;

/**
 * Arbitrary precision decimal number term (uses BigDecimal).
 * This provides exact decimal arithmetic for applications requiring
 * higher precision than double-precision floating point.
 *
 * @version 0.3.0
 */
public final class DecimalTerm extends NumericTerm {
	private static final long serialVersionUID = 1L;

	/**
	 * Default math context for operations (DECIMAL128: 34 digits, HALF_EVEN rounding)
	 */
	public static final MathContext DEFAULT_CONTEXT = MathContext.DECIMAL128;

	/** Value of term */
	public final BigDecimal value;

	/**
	 * Construct decimal term from string
	 *
	 * @param str string representation of decimal number
	 * @throws IllegalArgumentException when str is not valid
	 */
	public DecimalTerm(final String str) {
		try {
			value = new BigDecimal(str);
		} catch (NumberFormatException ex) {
			throw new IllegalArgumentException("argument should be decimal number", ex);
		}
	}

	/**
	 * Construct decimal term from BigDecimal value
	 *
	 * @param val BigDecimal value
	 */
	public DecimalTerm(final BigDecimal val) {
		if (val == null) {
			throw new IllegalArgumentException("value cannot be null");
		}
		value = val;
	}

	/**
	 * Construct decimal term from double value
	 *
	 * @param val double value
	 */
	public DecimalTerm(final double val) {
		value = BigDecimal.valueOf(val);
	}

	/**
	 * Construct decimal term from long value
	 *
	 * @param val long value
	 */
	public DecimalTerm(final long val) {
		value = BigDecimal.valueOf(val);
	}

	/**
	 * Get type of term
	 *
	 * @return type of term
	 */
	@Override
	public TermType getType() {
		return TermType.DECIMAL;
	}

	@Override
	public boolean equals(final Object obj) {
		return obj instanceof DecimalTerm dt && equals(dt);
	}

	public boolean equals(final DecimalTerm other) {
		return other != null && value.compareTo(other.value) == 0;
	}

	@Override
	public int hashCode() {
		return value.stripTrailingZeros().hashCode();
	}

	/**
	 * Convert to double (may lose precision)
	 *
	 * @return double value
	 */
	public double doubleValue() {
		return value.doubleValue();
	}

	/**
	 * Convert to long (truncates fractional part)
	 *
	 * @return long value
	 */
	public long longValue() {
		return value.longValue();
	}

	@Override
	public String toString() {
		return value.toString();
	}
}
