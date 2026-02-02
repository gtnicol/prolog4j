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
package gnu.prolog.term;

import gnu.prolog.io.TermWriter;

/**
 * base class for all terms.
 *
 * @author Constantine Plotniokov
 * @version 0.0.1
 */
public sealed abstract class Term implements java.io.Serializable, Cloneable
	permits AtomicTerm, CompoundTerm, VariableTerm
{
	private static final long serialVersionUID = -5388107925239494079L;

	/**
	 * clone the term.
	 * 
	 * @return cloned term
	 */
	@Override
	public Object clone()
	{
		TermCloneContext context = new TermCloneContext();
		return clone(context);
	}

	/**
	 * clone the object using clone context
	 * 
	 * @param context
	 *          clone context
	 * @return cloned term
	 */
	public abstract Term clone(final TermCloneContext context);

	/**
	 * dereference term.
	 * 
	 * Necessary because of {@link VariableTerm}. It means that the term which is
	 * eventually pointed to by however long a chain of intermediate terms is the
	 * one which you get.
	 * 
	 * @return dereferenced term
	 */
	public Term dereference()
	{
		return this;
	}

	/**
	 * get type of term as enum
	 *
	 * @return type of term
	 * @since 0.3.0
	 */
	public TermType getType()
	{
		return TermType.UNKNOWN;
	}

	@Override
	public String toString()
	{
		return TermWriter.toString(this);
	}
}
