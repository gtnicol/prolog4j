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

import gnu.prolog.io.PrologStream;
import gnu.prolog.term.CompoundTerm;
import gnu.prolog.term.DecimalTerm;
import gnu.prolog.term.FloatTerm;
import gnu.prolog.term.IntegerTerm;
import gnu.prolog.term.JavaObjectTerm;
import gnu.prolog.term.Term;
import gnu.prolog.term.VariableTerm;
import gnu.prolog.vm.PrologCode.RC;
import gnu.prolog.vm.interpreter.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Single-threaded execution context for Prolog goals.
 *
 * <p><strong>Thread Safety:</strong> This class is NOT thread-safe. Each thread must
 * create its own Interpreter via {@link Environment#createInterpreter()}.
 * Sharing an Interpreter across threads results in undefined behavior.</p>
 *
 * <p>Usage pattern:</p>
 * <pre>{@code
 * // For each thread:
 * Interpreter interpreter = environment.createInterpreter();
 * environment.runInitialization(interpreter);
 *
 * // Execute goals
 * Goal goal = interpreter.prepareGoal(term);
 * RC rc = interpreter.execute(goal);
 * interpreter.stop(goal);  // if not SUCCESS_LAST or FAIL
 * }</pre>
 *
 * @see Environment#createInterpreter()
 */
public final class Interpreter implements HasEnvironment
{
	private static final Logger logger = LoggerFactory.getLogger(Interpreter.class);

	/**
	 * Environment for this interpreter
	 */
	private Environment environment;

	/**
	 * Keeps track of prolog call/return traces
	 */
	private Tracer tracer;

	/**
	 * Contains an {@link PrologHalt} instance when the interpreter was halted in
	 * an {@link #execute(Goal)}.
	 */
	private PrologHalt haltExitCode;


	/**
	 * this constructor should not be used by client programs
	 * 
	 * @param environment
	 */
	protected Interpreter(Environment environment)
	{
		this.environment = environment;
		PrologStream outstream = null;
		try
		{
			outstream = environment.getUserOutput();
		}
		catch (PrologException e)
		{
			logger.error("Could not get an output stream", e);
		}
		tracer = new Tracer(outstream);
	}

	/** get environment */
	public Environment getEnvironment()
	{
		return environment;
	}

	public Tracer getTracer()
	{
		return tracer;
	}

	private final PrologStack<BacktrackInfo> backtrack = new PrologStack<>();

	/**
	 * push backtrack information
	 *
	 * @param bi
	 */
	public void pushBacktrackInfo(final BacktrackInfo bi)
	{
		backtrack.push(bi);
	}

	/**
	 * pop backtrack information
	 *
	 * @return the popped top backtrack information
	 */
	public BacktrackInfo popBacktrackInfo()
	{
		if (backtrack.isEmpty())
		{
			throw new IllegalStateException("Backtrack stack underflow");
		}
		var result = backtrack.pop();
		while (result instanceof BacktrackInfoWithCleanup cleanup)
		{
			cleanup.cleanup(this);
			if (backtrack.isEmpty())
			{
				throw new IllegalStateException("Only cleanup frames on backtrack stack");
			}
			result = backtrack.pop();
		}
		return result;
	}

	public void popBacktrackInfoUntil(final BacktrackInfo cutPoint)
	{
		final var pos = backtrack.find(cutPoint);
		if (pos < 0)
		{
			throw new IllegalArgumentException("cutPoint not found");
		}
		for (int i = pos + 1; i < backtrack.size(); i++)
		{
			if (backtrack.get(i) instanceof BacktrackInfoWithCleanup cleanup)
			{
				cleanup.cleanup(this);
			}
		}
		backtrack.truncate(pos + 1);
	}

	/**
	 * peek top backtrack information
	 *
	 * @return the top backtrack information
	 */
	public BacktrackInfo peekBacktrackInfo()
	{
		return backtrack.peek();
	}

	// Undo Stack methods
	private final PrologStack<VariableTerm> vars = new PrologStack<>();
	private final PrologStack<UndoData> undo = new PrologStack<>();
	private boolean undoPositionAsked = true;

	/**
	 * get current undo position
	 *
	 * @return the current undo position
	 */
	public int getUndoPosition()
	{
		undoPositionAsked = true;
		return undo.size();
	}

	/**
	 * undo changes until this position
	 *
	 * @param position
	 */
	public void undo(final int position)
	{
		for (int i = undo.size() - 1; i >= position; i--)
		{
			undo.get(i).undo();
		}
		undo.truncate(position);
		undoPositionAsked = true;
	}

	/**
	 * add variable undo
	 *
	 * @param variable
	 */
	public void addVariableUndo(final VariableTerm variable)
	{
		if (undoPositionAsked || undo.isEmpty() || !(undo.get(undo.size() - 1) instanceof VariableUndoData))
		{
			addSpecialUndo(new VariableUndoData());
		}
		vars.push(variable);
	}

	/**
	 * add special undo
	 *
	 * @param datum
	 */
	public void addSpecialUndo(final UndoData datum)
	{
		undo.push(datum);
	}

	private class VariableUndoData implements UndoData
	{
		private final int start;

		protected VariableUndoData()
		{
			start = vars.size();
		}

		public void undo()
		{
			for (int i = vars.size() - 1; i >= start; i--)
			{
				final var term = vars.get(i);
				if (term != null)
				{
					term.value = null;
				}
			}
			vars.truncate(start);
		}
	}

	/**
	 * unify two terms, no undo done
	 * 
	 * @param t1
	 * @param t2
	 * @return {@link RC#SUCCESS_LAST} or {@link RC#FAIL}
	 * @throws PrologException
	 */
	public RC simpleUnify(Term t1, Term t2) throws PrologException
	{
		RC rc = PrologCode.RC.SUCCESS_LAST;
		if (t1 == t2)
		{
			// Same reference, do nothing. Added only for clarity.
		}
		else if (t1 instanceof VariableTerm vt1)
		{
			addVariableUndo(vt1);
			vt1.value = t2;
		}
		else if (t2 instanceof VariableTerm vt2)
		{
			addVariableUndo(vt2);
			vt2.value = t1;
		}
		else if (t1.getClass() != t2.getClass())
		{
			rc = PrologCode.RC.FAIL;
		}
		else if (t1 instanceof CompoundTerm ct1/* && t2 instanceof CompoundTerm */)
		{
			CompoundTerm ct2 = (CompoundTerm) t2;
			if (ct1.tag != ct2.tag)
			{
				rc = PrologCode.RC.FAIL;
			}
			else
			{
				Term args1[] = ct1.args;
				Term args2[] = ct2.args;
				// System.err.println("unify "+ct2.tag+" al1 = "+args1.length+" al2 = "+args2.length);
				for (int i = args2.length - 1; rc != PrologCode.RC.FAIL && i >= 0; i--)
				{
					rc = simpleUnify(args1[i].dereference(), args2[i].dereference());
				}
			}
		}
		else if (t1 instanceof FloatTerm ft1/* && t2 instanceof FloatTerm */)
		{
			FloatTerm ft2 = (FloatTerm) t2;
			if (!ft1.equals(ft2))
			{
				rc = PrologCode.RC.FAIL;
			}
		}
		else if (t1 instanceof DecimalTerm dt1 /* && t2 instanceof DecimalTerm */)
		{
			DecimalTerm dt2 = (DecimalTerm) t2;
			if (!dt1.equals(dt2))
			{
				rc = PrologCode.RC.FAIL;
			}
		}
		else if (t1 instanceof IntegerTerm it1 /* && t2 instanceof IntegerTerm */)
		{
			IntegerTerm it2 = (IntegerTerm) t2;
			if (it1.value != it2.value)
			{
				rc = PrologCode.RC.FAIL;
			}
		}
		else if (t1 instanceof JavaObjectTerm jot1 /* && t2 instanceof JavaObjectTerm */)
		{
			JavaObjectTerm jot2 = (JavaObjectTerm) t2;
			if (jot1.value != jot2.value)
			{
				rc = PrologCode.RC.FAIL;
			}
		}
		else
		{
			rc = PrologCode.RC.FAIL;
		}
		return rc;
	}

	/**
	 * unify two terms and undo the unification if it fails.
	 * 
	 * @param t1
	 * @param t2
	 * @return {@link RC#SUCCESS_LAST} or {@link RC#FAIL}
	 * @throws PrologException
	 */
	public RC unify(Term t1, Term t2) throws PrologException
	{
		int undoPos = getUndoPosition();
		RC rc = simpleUnify(t1, t2);
		if (rc == PrologCode.RC.FAIL)
		{
			undo(undoPos);
		}
		return rc;
	}

	/** user level calls */
	public static final class Goal
	{
		private Term goal;
		protected boolean firstTime = true;
		private boolean stopped = false;

		protected Goal(Term goal)
		{
			this.goal = goal;
		}

		protected Term getGoal()
		{
			return goal;
		}

		protected boolean isStopped()
		{
			return stopped;
		}

		/**
		 * Should only be called by {@link #stop(Goal)}
		 */
		protected void stop()
		{
			stopped = true;
		}

		@Override
		public String toString()
		{
			return goal.toString() + " f:" + firstTime + " s:" + stopped;
		}
	}

	/**
	 * The most recently {@link #prepareGoal(Term)}ed Goal.
	 */
	private Goal currentGoal;

	/**
	 * Used to store the current state so that we can support
	 * {@link gnu.prolog.vm.builtins.io.Predicate_ensure_loaded}
	 * 
	 * @see #prepareGoal(Term)
	 * @see #stop(Goal)
	 * 
	 *      WARNING: may result in obscure bugs, if so sorry.
	 * 
	 * @author Daniel Thomas
	 */
	static class ReturnPoint
	{
		public PrologStack<BacktrackInfo> backtrack;
		public PrologStack<VariableTerm> vars;
		public PrologStack<UndoData> undo;
		public boolean undoPositionAsked;
		public Goal goal;
	}

	/**
	 * Map of Goals to ReturnPoints so that we can save the state in a return
	 * point if in {@link #prepareGoal(Term)} we discover that we need to execute
	 * another goal first before finishing executing the {@link #currentGoal}.
	 */
	private Map<Goal, ReturnPoint> returnPoints = new HashMap<>();

	/**
	 * prepare goal for execution
	 *
	 * If this is called before the Goal which was previously prepared but has not
	 * yet been stopped is stopped then we save that state so we can jump back to
	 * it when this goal has been stopped.
	 *
	 * @param term
	 * @return the prepared Goal
	 */
	public Goal prepareGoal(final Term term)
	{
		ReturnPoint rp = null;
		if (currentGoal != null)
		{
			rp = new ReturnPoint();
			rp.backtrack = backtrack.clone();
			rp.vars = vars.clone();
			rp.undo = undo.clone();
			rp.undoPositionAsked = undoPositionAsked;
			rp.goal = currentGoal;
		}
		currentGoal = new Goal(term);
		if (rp != null)
		{
			returnPoints.put(currentGoal, rp);
		}
		return currentGoal;
	}

	/**
	 * Execute the {@link Goal} and return the status code indicating how
	 * successful this was.
	 * 
	 * @param goal
	 *          the goal created using {@link #prepareGoal(Term)} which is to be
	 *          run.
	 * @return {@link RC#SUCCESS}, {@link RC#SUCCESS_LAST} , {@link RC#FAIL} or
	 *         {@link RC#HALT}
	 * @throws PrologException
	 */
	public RC execute(Goal goal) throws PrologException
	{
		haltExitCode = null;
		try
		{
			try
			{
				if (currentGoal == null)
				{
					throw new IllegalStateException("The goal is not prepared");
				}
				if (currentGoal != goal)
				{
					throw new IllegalArgumentException("The goal is not currently active");
				}
				if (goal.isStopped())
				{
					throw new Stopped(goal);
				}
				try
				{
					RC rc = gnu.prolog.vm.interpreter.Call.staticExecute(this, !goal.firstTime, goal.getGoal());
					switch (rc)
					{
						case SUCCESS_LAST:
						case FAIL:
							stop(goal);
							break;
						case SUCCESS:
							goal.firstTime = false;
							break;
						case HALT:
							break;// on HALT all we can do is return HALT which is what
						// happens if we do nothing for it.
					}
					return rc;
				}
				finally
				{
					environment.getUserOutput().flushOutput(null);
				}
			}
			catch (RuntimeException rex)
			{
				PrologException.systemError(rex);
				throw rex; // fake
			}
			catch (StackOverflowError se)
			{
				// too much recursion
				// System.err.println("Stack overflow while executing: " + goal);
				PrologException.systemError(se);
				throw se; // fake
			}
			catch (OutOfMemoryError me)
			{
				// too much memory usage
				// System.err.println("Out of memory error while executing: " + goal);
				PrologException.systemError(me);
				throw me; // fake
			}
		}
		catch (PrologHalt ph)
		{
			stop(goal);
			haltExitCode = ph;
			return PrologCode.RC.HALT;
		}
		catch (PrologException ex)
		{
			stop(goal);
			throw ex;
		}
	}

	/**
	 * Once the goal has been finished with and if the goal has not been stopped
	 * (as it will have been if RC.SUCCESS_LAST or RC.FAIL has been returned) then
	 * {@link #stop(Goal)} should be run.
	 *
	 *
	 * @param goal
	 *          the goal to stop.
	 */
	public void stop(final Goal goal)
	{
		if (currentGoal != goal)
		{
			throw new IllegalArgumentException(String.format("The goal (%s) is not currently active: (%s) is active", goal,
					currentGoal));
		}
		if (goal.isStopped())
		{
			throw new Stopped(goal);
		}
		goal.stop();

		for (int i = 0; i < backtrack.size(); i++)
		{
			if (backtrack.get(i) instanceof BacktrackInfoWithCleanup cleanup)
			{
				cleanup.cleanup(this);
			}
		}
		backtrack.clear();

		currentGoal = null;

		// Restore state from return point if one exists
		final var rp = returnPoints.remove(goal);
		if (rp != null)
		{
			backtrack.restore(rp.backtrack);
			vars.restore(rp.vars);
			undo.restore(rp.undo);
			undoPositionAsked = rp.undoPositionAsked;
			currentGoal = rp.goal;
		}
	}

	/**
	 * Run the provided goalTerm once returning the value returned by
	 * {@link #execute(Goal)} and then stop the goal. This is thus an atomic
	 * operation on the Interpreter.
	 * 
	 * Runs {@link #prepareGoal(Term)} then {@link #execute(Goal)} then if
	 * necessary {@link #stop(Goal)}. Returns the return code from
	 * {@link #execute(Goal)}.
	 * 
	 * @param goalTerm
	 *          the term to be executed
	 * @return {@link RC#SUCCESS}, {@link RC#SUCCESS_LAST} or {@link RC#FAIL}
	 * @throws PrologException
	 */
	public RC runOnce(Term goalTerm) throws PrologException
	{
		Goal goal = prepareGoal(goalTerm);
		try
		{
			return execute(goal);
		}
		finally
		{
			if (currentGoal == goal && !goal.isStopped())
			{
				stop(goal);
			}
		}
	}

	/**
	 * Only call this method if you have had {@link RC#HALT} returned by the most
	 * recent call to {@link #execute(Goal)}. Otherwise and
	 * {@link IllegalStateException} will be thrown.
	 * 
	 * @return The exit code when the prolog interpreter was halted
	 */
	public int getExitCode()
	{
		if (haltExitCode == null)
		{
			throw new IllegalStateException("Prolog Interpreter was not halted");
		}
		return haltExitCode.getExitCode();
	}

	/**
	 * Someone tried to do something with a {@link Goal} which had already been
	 * stopped.
	 * 
	 * @author Daniel Thomas
	 */
	private static class Stopped extends IllegalStateException
	{
		public Stopped(Goal goal)
		{
			super(String.format("The goal (%s) is already stopped", goal));
		}

		private static final long serialVersionUID = 1L;
	}
}
