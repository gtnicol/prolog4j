/* GNU Prolog for Java
 * Copyright (C) 2010       Daniel Thomas
 *
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

/**
 * For Predicates which do not need to be installed or uninstalled.
 * Provides default empty implementations of install/uninstall methods.
 *
 * Can be used as a functional interface for simple predicates.
 *
 * @author Daniel Thomas
 */
@FunctionalInterface
public interface ExecuteOnlyCode extends PrologCode
{
	/**
	 * Execute the predicate code.
	 *
	 * @param interpreter interpreter in which context code is executed
	 * @param backtrackMode true if predicate is called on backtracking
	 * @param args arguments of code
	 * @return RC.SUCCESS, RC.SUCCESS_LAST, or RC.FAIL
	 * @throws PrologException if an error occurs
	 */
	@Override
	RC execute(Interpreter interpreter, boolean backtrackMode, gnu.prolog.term.Term args[])
			throws PrologException;

	/**
	 * Default empty install method - predicates don't need installation.
	 *
	 * @param env the environment
	 */
	@Override
	default void install(Environment env) {}

	/**
	 * Default empty uninstall method - predicates don't need uninstallation.
	 *
	 * @param env the environment
	 */
	@Override
	default void uninstall(Environment env) {}
}
