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
package gnu.prolog.vm.interpreter;

import gnu.prolog.term.AtomTerm;
import gnu.prolog.term.CompoundTerm;
import gnu.prolog.term.Term;
import gnu.prolog.term.VariableTerm;
import gnu.prolog.vm.BacktrackInfo;
import gnu.prolog.vm.BacktrackInfoWithCleanup;
import gnu.prolog.vm.Environment;
import gnu.prolog.vm.ExecuteOnlyCode;
import gnu.prolog.vm.Interpreter;
import gnu.prolog.vm.PrologCode;
import gnu.prolog.vm.PrologException;
import gnu.prolog.vm.TermConstants;

import java.lang.ref.Cleaner;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Predicate call/1 - meta-call predicate for executing arbitrary goals.
 * This is a core control construct that compiles and executes Prolog terms as goals.
 */
public final class Call implements ExecuteOnlyCode
{
	/** Shared cleaner for all CallTermBacktrackInfo instances */
	private static final Cleaner CLEANER = Cleaner.create();

	/** head functor, it is completly unimportant what it is */
	public static final AtomTerm headFunctor = AtomTerm.get("$$$call$$$");
	/** term arry constant */
	public static final Term termArrayType[] = new Term[0];

	/** call term backtrack info */
	public static class CallTermBacktrackInfo extends BacktrackInfo
	{
		/** Cleaner state for uninstalling code */
		private static class CleanupState implements Runnable
		{
			private final PrologCode code;
			private final Environment environment;

			CleanupState(final PrologCode code, final Environment environment)
			{
				this.code = code;
				this.environment = environment;
			}

			@Override
			public void run()
			{
				if (code != null && environment != null)
				{
					code.uninstall(environment);
				}
			}
		}

		/** Cleanable registration for this backtrack info */
		private Cleaner.Cleanable cleanable;
		public CallTermBacktrackInfo(Interpreter in, PrologCode code, Term args[], Term callTerm)
		{
			super(in.getUndoPosition(), -1);
			this.code = code;
			this.args = args.clone();
			this.callTerm = callTerm;
			this.environment = null; // Set later via setEnvironment()
		}

		/** prolog code being tried */
		PrologCode code;
		/** argument of prolog code */
		Term args[];
		/** Term passed as parameter */
		Term callTerm;
		/** environment */
		Environment environment;

		/**
		 * Sets the environment and registers cleanup via Cleaner.
		 * Should be called once after construction.
		 *
		 * @param env the environment to set
		 */
		public void setEnvironment(final Environment env)
		{
			this.environment = env;
			// Register cleanup action using Cleaner
			if (code != null && env != null)
			{
				this.cleanable = CLEANER.register(this, new CleanupState(code, env));
			}
		}

		/**
		 * Explicitly cleanup resources by uninstalling code from environment.
		 * This method is idempotent and can be called multiple times safely.
		 */
		public void cleanup()
		{
			if (code != null && environment != null)
			{
				code.uninstall(environment);
				if (cleanable != null)
				{
					cleanable.clean();
				}
			}
		}

	}

	@Override
	public RC execute(Interpreter interpreter, boolean backtrackMode, gnu.prolog.term.Term args[]) throws PrologException
	{
		return staticExecute(interpreter, backtrackMode, args[0]);
	}

	/**
	 * this method is used for execution of code
	 * 
	 * @param interpreter
	 *          interpreter in which context code is executed
	 * @param backtrackMode
	 *          true if predicate is called on backtracking and false otherwise
	 * @param arg
	 *          argument of code
	 * @return either RC.SUCCESS, RC.SUCCESS_LAST, or RC.FAIL.
	 * @throws PrologException
	 */
	public static RC staticExecute(Interpreter interpreter, boolean backtrackMode, Term arg) throws PrologException
	{
		BacktrackInfo bi = backtrackMode ? interpreter.popBacktrackInfo() : null;
		while (bi instanceof BacktrackInfoWithCleanup bic)
		{
			bic.cleanup(interpreter);
			bi = backtrackMode ? interpreter.popBacktrackInfo() : null;
		}
		CallTermBacktrackInfo cbi = (CallTermBacktrackInfo) bi;
		PrologCode code; // code to call
		Term args[]; // arguments of code
		Term callTerm; // term being called
		if (cbi == null)
		{
			callTerm = arg;
			if (callTerm instanceof VariableTerm)
			{
				PrologException.instantiationError(callTerm);
			}
			// This was originally done using two Lists by keeping their sizes in sync
			// but I (Daniel) refactored this to a map. (This may have broken
			// something).
			Map<Term, VariableTerm> argumentsToArgumentVariables = new HashMap<>();
			Term body;
			try
			{
				body = getClause(callTerm, argumentsToArgumentVariables);
			}
			catch (IllegalArgumentException ex) // term not callable
			{
				PrologException.typeError(TermConstants.callableAtom, callTerm);
				return RC.FAIL; // fake return
			}
			Term headArgs[] = argumentsToArgumentVariables.values().toArray(termArrayType);
			Term head = new CompoundTerm(headFunctor, headArgs);
			Term clause = new CompoundTerm(TermConstants.clauseTag, head, body);
			args = argumentsToArgumentVariables.keySet().toArray(termArrayType);
			List<Term> clauses = new ArrayList<Term>(1);
			clauses.add(clause);
			code = InterpretedCodeCompiler.compile(clauses);
			code.install(interpreter.getEnvironment());
			// System.err.println("converted clause");
			// System.err.println(gnu.prolog.io.TermWriter.toString(clause));
			// System.err.println("converted code");
			// System.err.print(code);
		}
		else
		{
			cbi.undo(interpreter);
			args = cbi.args;
			code = cbi.code;
			callTerm = cbi.callTerm;
		}
		RC rc = code.execute(interpreter, backtrackMode, args);
		if (rc == RC.SUCCESS) // redo is possible
		{
			cbi = new CallTermBacktrackInfo(interpreter, code, args, callTerm);
			cbi.setEnvironment(interpreter.getEnvironment());
			interpreter.pushBacktrackInfo(cbi);
		}
		else
		{
			code.uninstall(interpreter.getEnvironment());
			if (cbi != null)
			{
				cbi.code = null;
			}
		}
		return rc;
	}

	/**
	 * convert callable term to clause
	 * 
	 * @param term
	 * @param argumentsToArgumentVariables
	 * @return
	 */
	public static Term getClause(Term term, Map<Term, VariableTerm> argumentsToArgumentVariables)
	{
		if (term instanceof AtomTerm)
		{
			return term;
		}
		else if (term instanceof VariableTerm)
		{
			if (!argumentsToArgumentVariables.containsKey(term))
			{
				VariableTerm var1 = new VariableTerm();
				argumentsToArgumentVariables.put(term, var1);
				return var1;
			}
			return argumentsToArgumentVariables.get(term);
		}
		else if (term instanceof CompoundTerm ct)
		{
			if (ct.tag == TermConstants.ifTag || ct.tag == TermConstants.conjunctionTag
					|| ct.tag == TermConstants.disjunctionTag)
			{
				return new CompoundTerm(ct.tag, getClause(ct.args[0].dereference(), argumentsToArgumentVariables), getClause(
						ct.args[1].dereference(), argumentsToArgumentVariables));
			}
			Term newArgs[] = new Term[ct.tag.arity];
			for (int i = 0; i < newArgs.length; i++)
			{
				Term arg = ct.args[i].dereference();
				if (!argumentsToArgumentVariables.containsKey(arg))
				{
					newArgs[i] = new VariableTerm();
					argumentsToArgumentVariables.put(arg, (VariableTerm) newArgs[i]);
				}
				else
				{
					newArgs[i] = argumentsToArgumentVariables.get(arg);
				}
			}
			return new CompoundTerm(ct.tag, newArgs);
		}
		else
		{
			throw new IllegalArgumentException("the term is not callable");
		}
	}
}
