/* GNU Prolog for Java
 * Copyright (C) 2013		Rishabh Garg
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

package gnu.prolog.io;

/**
 * This class is intended for printing unicode-terms.
 * Uses Java's built-in Character methods for Unicode category detection.
 *
 * @author Rishabh Garg
 * @version 0.4
 */
public class UnicodeWriter
{
	/**
	 * Check if character is valid start of an atom term.
	 * All unicode lowercase letters are valid.
	 * @param c the character to check
	 * @return true if the unicode character is a valid start of an atom.
	 */
	protected static boolean isAtomStartCharUnicode(final char c)
	{
		return Character.isLowerCase(c);
	}

	/**
	 * Check if character is valid continuation of an atom term.
	 * Includes letters (Lu, Ll, Lt, Lm, Lo), digits (Nd), marks (Mn, Mc),
	 * letter numbers (Nl), and connector punctuation (Pc).
	 * @param c the character to check
	 * @return true if the unicode character is a valid continuation of an atom.
	 */
	public static boolean isAtomCharUnicode(final char c)
	{
		if (Character.isLetterOrDigit(c))
		{
			return true;
		}
		final int type = Character.getType(c);
		return type == Character.NON_SPACING_MARK
			|| type == Character.COMBINING_SPACING_MARK
			|| type == Character.LETTER_NUMBER
			|| type == Character.CONNECTOR_PUNCTUATION;
	}
}
