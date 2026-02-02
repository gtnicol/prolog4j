/* GNU Prolog for Java
 * Copyright (C) 1997-1999  Constantine Plotnikov
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
package gnu.prolog.term;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Floating point number term (uses double)
 *
 * @author Constantine Plotnikov
 * @version 0.0.1
 */
public final class FloatTerm extends NumericTerm
{
	private static final long serialVersionUID = -5988244457397590539L;
	private static final int DISPLAY_CANONICAL = -1;

	/**
	 * The "slack" used in float comparison
	 */
	// public static final double FLOAT_EPSILON = 0.0000001d;

	/**
	 * get floating point number term
	 * 
	 * @param str
	 *          string representation of float number
	 * @throws IllegalArgumentException
	 *           when str is not valid string
	 */
	public FloatTerm(final String str)
	{
		try
		{
			exact = new BigDecimal(str);
			displayScale = DISPLAY_CANONICAL;
			value = exact.doubleValue();
		}
		catch (NumberFormatException ex)
		{
			throw new IllegalArgumentException("argument should be floating point number", ex);
		}
	}

	/**
	 * a constructor
	 * 
	 * @param val
	 *          double value
	 */
	public FloatTerm(final double val)
	{
		exact = BigDecimal.valueOf(val);
		displayScale = DISPLAY_CANONICAL;
		value = val;
	}

	/**
	 * a constructor with exact decimal value and display scale
	 *
	 * @param exactValue
	 *          exact decimal representation
	 * @param displayScale
	 *          number of digits after decimal point, or negative for canonical output
	 */
	public FloatTerm(final BigDecimal exactValue, final int displayScale)
	{
		exact = exactValue;
		this.displayScale = displayScale;
		if (displayScale >= 0)
		{
			value = exactValue.setScale(displayScale, RoundingMode.DOWN).doubleValue();
		}
		else
		{
			value = exactValue.doubleValue();
		}
	}

	/** value of term */
	public final double value;
	private final BigDecimal exact;
	private final int displayScale;

	/**
	 * get type of term
	 * 
	 * @return type of term
	 */
	@Override
	public TermType getType()
	{
		return TermType.FLOAT;
	}

	public BigDecimal exactValue()
	{
		return exact;
	}

	public int getDisplayScale()
	{
		return displayScale;
	}

	public boolean hasDisplayScale()
	{
		return displayScale >= 0;
	}

	public String toDisplayString()
	{
		if (displayScale >= 0)
		{
			BigDecimal scaled = exact.setScale(displayScale, RoundingMode.DOWN);
			String s = scaled.toPlainString();
			return s.contains(".") ? s : s + ".0";
		}
		BigDecimal stripped = exact.stripTrailingZeros();
		String s = stripped.toPlainString();
		return s.contains(".") ? s : s + ".0";
	}

	@Override
	public boolean equals(final Object obj)
	{
		return obj instanceof FloatTerm ft && equals(ft);
	}

	public boolean equals(final FloatTerm oft)
	{
		return oft != null && value == oft.value;
		/* && Math.abs(value - oft.value) < FLOAT_EPSILON */
	}

	@Override
	public int hashCode()
	{
		return Double.hashCode(value);
	}
}
