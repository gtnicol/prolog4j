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
/** variable term.
 * @author Constantine Plotnikov
 * @version 0.0.1
 */
package gnu.prolog.term;

/**
 * Represents Prolog variables such as X or A.
 *
 * @see Term#dereference()
 */
public non-sealed class VariableTerm extends Term
{
	private static final long serialVersionUID = -8440602532721728373L;

	/** create new unnamed variable term */
	public VariableTerm()
	{}

	/**
	 * Create a new named variable term
	 * 
	 * @param name
	 *          the name of the term.
	 * @see #name
	 */
	public VariableTerm(final String name)
	{
		this.name = name;
	}

	/**
	 * a constructor
	 * 
	 * @param name
	 *          name of term
	 */
	/** value of variable term */
	public Term value = null;

	/**
	 * Name of the variable when it was declared
	 *
	 * Used for display purposes
	 */
	private String name = null;

	/**
	 * Get the name of this variable.
	 *
	 * @return the variable name, or null if unnamed
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * clone the object using clone context
	 *
	 * @param context
	 *          clone context
	 * @return cloned term
	 */
	@Override
	public Term clone(final TermCloneContext context)
	{
		VariableTerm variable = this;
		while (true)
		{
			if (variable.value == null)
			{
				VariableTerm term = (VariableTerm) context.getTerm(variable);
				if (term == null)
				{
					term = new VariableTerm(variable.getName());
					context.putTerm(variable, term);
				}
				return term;
			}
			else if (variable.value instanceof VariableTerm vt)
			{
				variable = vt;
			}
			else
			{
				return variable.value.clone(context);
			}
		}
	}

	/**
	 * dereference term.
	 * 
	 * @return dereferenced term
	 */
	@Override
	public Term dereference()
	{
		VariableTerm variable = this;
		do
		{
			Term val = variable.value;
			if (val == null)
			{
				return variable;
			}
			else if (val instanceof VariableTerm vt)
			{
				variable = vt;
			}
			else
			{
				return val.dereference();
			}
		} while (true);
		/*
		 * if(value == null) { return this; } return value.dereference();
		 */
	}

	/**
	 * Get the bound value of this variable.
	 *
	 * @return the bound value or null if unbound
	 */
	public Term bound()
	{
		return value;
	}

	/**
	 * Check if this variable is unbound.
	 *
	 * @return true if unbound
	 */
	public boolean unbound()
	{
		return value == null;
	}

	/**
	 * Bind this variable to a term.
	 * Note: Caller must call interpreter.addVariableUndo() first for proper undo tracking.
	 *
	 * @param term the term to bind to
	 */
	public void bind(final Term term)
	{
		this.value = term;
	}

	/**
	 * get type of term
	 *
	 * @return type of term
	 */
	@Override
	public TermType getType()
	{
		return TermType.VARIABLE;
	}
}
