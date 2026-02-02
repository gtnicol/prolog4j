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

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Atom term. The objects of this class represent prolog atoms. This
 * encapsulates Strings and chars.
 *
 * @author Constantin Plotnikov
 * @version 0.0.1
 */
public final class AtomTerm extends AtomicTerm
{
	private static final long serialVersionUID = -7013961090908432585L;

	/** a map from string to atom */
	private static final ConcurrentHashMap<String, AtomTerm> atoms = new ConcurrentHashMap<>();

	/**
	 * get atom term
	 *
	 * @param s
	 *          string representation of atom.
	 * @return the AtomTerm for the String
	 */
	public static AtomTerm get(final String s)
	{
		final var existing = atoms.get(s);
		if (existing != null)
		{
			return existing;
		}
		final var fresh = new AtomTerm(s);
		final var previous = atoms.putIfAbsent(s, fresh);
		return previous != null ? previous : fresh;
	}

	/**
	 * get atom term
	 *
	 * @param ch
	 *          character representation of atom.
	 * @return the atom term for the character
	 */
	public static AtomTerm get(final char ch)
	{
		return get(String.valueOf(ch));
	}

	/**
	 * Return an object to replace the object extracted from the stream. The
	 * object will be used in the graph in place of the original.
	 * This method is part of the serialization resolution mechanism.
	 *
	 * @return resolved object
	 * @see java.io.Serializable
	 */
	public Object readResolve()
	{
		return get(value);
	}

	/** value of atom */
	final public String value;

	/**
	 * a constructor.
	 * 
	 * @param value
	 *          value of atom
	 */
	protected AtomTerm(final String value) // constructor is private to package
	{
		this.value = value;
	}

	/**
	 * get type of term
	 *
	 * @return type of term
	 */
	@Override
	public TermType getType()
	{
		return TermType.ATOM;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode()
	{
		return Objects.hashCode(value);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object obj)
	{
		return obj instanceof AtomTerm other && Objects.equals(value, other.value);
	}
}
