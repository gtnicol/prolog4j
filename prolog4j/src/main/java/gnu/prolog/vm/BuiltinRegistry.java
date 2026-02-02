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
package gnu.prolog.vm;

import gnu.prolog.term.CompoundTermTag;

import java.util.Set;

/**
 * Interface for querying registered builtin predicates.
 * Provides runtime access to information about which predicates are builtins.
 */
public interface BuiltinRegistry
{
	/**
	 * Returns the set of all registered builtin predicate tags.
	 *
	 * @return an unmodifiable set of builtin predicate tags
	 */
	Set<CompoundTermTag> getBuiltins();

	/**
	 * Checks if a predicate tag corresponds to a builtin predicate.
	 *
	 * @param tag the predicate tag to check
	 * @return true if the predicate is a builtin, false otherwise
	 */
	boolean isBuiltin(CompoundTermTag tag);

	/**
	 * Returns the number of registered builtin predicates.
	 *
	 * @return the count of builtin predicates
	 */
	default int count()
	{
		return getBuiltins().size();
	}
}
