/* GNU Prolog for Java
 * Copyright (C) 2011 Matt Lilley
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

import gnu.prolog.term.Term;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** back track information with cleanup information included */
public class BacktrackInfoWithCleanup extends BacktrackInfo
{
	private static final Logger logger = LoggerFactory.getLogger(BacktrackInfoWithCleanup.class);
	/**
	 * a constructor
	 * 
	 * @param cleanup
	 *          {@link #codePosition}
	 * */
	public BacktrackInfoWithCleanup(Term cleanup)
	{
		super(-1, -1);
		this.cleanup = cleanup;
	}

	private Term cleanup;

	public void cleanup(final Interpreter interpreter)
	{
		if (cleanup != null)
		{
			try
			{
				gnu.prolog.vm.interpreter.Call.staticExecute(interpreter, false, cleanup);
			}
			catch (PrologException e)
			{
				logger.warn("Exception during cleanup execution", e);
			}
		}
	}
}
