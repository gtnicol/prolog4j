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

import gnu.prolog.database.Pair;
import gnu.prolog.vm.PrologException;
import gnu.prolog.vm.TermConstants;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Compound terms are the basic method for combining terms. In
 * <code>foo(a,b) foo/2</code> is the compound term while <code>a</code> and
 * <code>b</code> are {@link AtomTerm AtomTerms}
 *
 * @author Constantine Plotnilkov
 * @version 0.0.1
 */
public final class CompoundTerm extends Term
{
	private static final long serialVersionUID = -8207470525318790957L;

	public static boolean isListPair(final Term term)
	{
		return term instanceof CompoundTerm ct && ct.tag == TermConstants.listTag;
	}

	/**
	 * get list pair
	 * 
	 * @param head
	 *          head term
	 * @param tail
	 *          tail term
	 * @return '.'(head, tail) term
	 */
	public static CompoundTerm getList(final Term head, final Term tail)
	{
		return new CompoundTerm(TermConstants.listTag, head, tail);
	}

	/**
	 * get prolog list by java array
	 * 
	 * @param list
	 * @return a Term representation of the list
	 */
	public static Term getList(final Term[] list)
	{
		Term tlist = TermConstants.emptyListAtom;
		for (int i = list.length - 1; i >= 0; i--)
		{
			tlist = getList(list[i], tlist);
		}
		return tlist;
	}

	/**
	 * get prolog list by java list
	 *
	 * @param list
	 * @return a Term representation of the list
	 */
	public static Term getList(final List<Term> list)
	{
		return IntStream.iterate(list.size() - 1, i -> i >= 0, i -> i - 1)
			.mapToObj(list::get)
			.<Term>reduce(TermConstants.emptyListAtom,
					(acc, term) -> getList(term, acc),
					(a, b) -> a);
	}

	/**
	 * Adds all the non-compound Terms in term to the collection col by
	 * recursively running over the arguments of all the compound terms.
	 * 
	 * @param term
	 *          the Term to extract all the Term elements from to produce the
	 *          collection
	 * @param col
	 *          the collection to add the terms to
	 * @return whether we successfully formed a collection. If false then the
	 *         state of col is undefined.
	 */
	public static boolean toCollection(Term term, final Collection<Term> col)
	{
		while ((term = term.dereference()) instanceof CompoundTerm ct)
		{
			if (ct.tag != TermConstants.listTag)
			{
				return false;
			}
			else
			{
				col.add(ct.args[0]);
				term = ct.args[1];
			}
		}
		return term instanceof AtomTerm at && at == TermConstants.listAtom;
	}

	/**
	 * get conjunction term
	 * 
	 * @param head
	 *          head term
	 * @param tail
	 *          tail term
	 * @return ','(head, tail) term
	 */
	public static CompoundTerm getConjunction(Term head, Term tail)
	{
		return new CompoundTerm(TermConstants.conjunctionTag, head, tail);
	}

	/**
	 * get disjunction term
	 * 
	 * @param head
	 *          head term
	 * @param tail
	 *          tail term
	 * @return ';'(head, tail) term
	 */
	public static CompoundTerm getDisjunction(Term head, Term tail)
	{
		return new CompoundTerm(TermConstants.disjunctionTag, head, tail);
	}

	/**
	 * Check that the term is a listPair where the head is instantiated and throw
	 * the relevant PrologException if not.
	 * 
	 * @param term
	 * @return a (head,body) Pair
	 * @throws PrologException
	 */
	public static Pair<Term, Term> getInstantiatedHeadBody(Term term) throws PrologException
	{
		if (term instanceof VariableTerm)
		{
			PrologException.instantiationError(term);
		}
		if (!CompoundTerm.isListPair(term))
		{
			PrologException.typeError(TermConstants.listAtom, term);
		}
		CompoundTerm ct = (CompoundTerm) term;
		Term head = ct.args[0].dereference();
		term = ct.args[1].dereference();
		if (head instanceof VariableTerm)
		{
			PrologException.instantiationError(head);
		}
		return new Pair<Term, Term>(head, term);
	}

	/**
	 * get term with specified term tag and arguments.
	 * 
	 * @param tg
	 *          tag of new term
	 * @param arg1
	 *          1st argument of term
	 */
	public CompoundTerm(final CompoundTermTag tg, final Term arg1)
	{
		this(tg, new Term[] { arg1 });
	}

	/**
	 * get term with specified term tag and arguments.
	 * 
	 * @param tg
	 *          tag of new term
	 * @param arg1
	 *          1st argument of term
	 * @param arg2
	 *          2nd argument of term
	 */
	public CompoundTerm(final CompoundTermTag tg, final Term arg1, final Term arg2)
	{
		this(tg, new Term[] { arg1, arg2 });
	}

	/**
	 * get term with specified term tag and arguments.
	 * 
	 * @param tg
	 *          tag of new term
	 * @param arg1
	 *          1st argument of term
	 * @param arg2
	 *          2nd argument of term
	 * @param arg3
	 *          3rd argument of term
	 */
	public CompoundTerm(final CompoundTermTag tg, final Term arg1, final Term arg2, final Term arg3)
	{
		this(tg, new Term[] { arg1, arg2, arg3 });
	}

	/**
	 * get term with specified functor and arity
	 * 
	 * @param functor
	 *          a functor of new term
	 * @param arity
	 *          arity of new term
	 */
	public CompoundTerm(final String functor, final int arity)
	{
		this(AtomTerm.get(functor), arity);
	}

	/**
	 * get term with specified functor and arity
	 * 
	 * @param functor
	 *          a functor of new term
	 * @param arity
	 *          arity of new term
	 */
	public CompoundTerm(final AtomTerm functor, final int arity)
	{
		this(CompoundTermTag.get(functor, arity));
	}

	/**
	 * get term with specified term functor and arguments.
	 * 
	 * @param functor
	 *          a functor of new term
	 * @param args
	 *          arguments of term, this array is directly assigned to term and any
	 *          changes that are done to array change term.
	 */
	public CompoundTerm(final AtomTerm functor, final Term args[])
	{
		this(CompoundTermTag.get(functor, args.length), args);
	}

	/**
	 * get term with specified term functor and arguments.
	 * 
	 * @param functor
	 *          a functor of new term
	 * @param args
	 *          arguments of term, this array is directly assigned to term and any
	 *          changes that are done to array change term.
	 */
	public CompoundTerm(final String functor, final Term args[])
	{
		this(CompoundTermTag.get(functor, args.length), args);
	}

	/** term tag */
	public final CompoundTermTag tag;
	/** term arguments */
	public final Term[] args;

	/**
	 * Get an argument of this compound term.
	 *
	 * @param index the argument index (0-based)
	 * @return the argument at the specified index
	 */
	public Term getArg(final int index)
	{
		return args[index];
	}

	/**
	 * Get the number of arguments (arity) of this compound term.
	 *
	 * @return the number of arguments
	 */
	public int getArity()
	{
		return args.length;
	}

	/**
	 * a contructor
	 * 
	 * @param tag
	 *          tag of term
	 */
	public CompoundTerm(final CompoundTermTag tag)
	{
		this.tag = tag;
		args = new Term[tag.arity];
	}

	/**
	 * a constructor
	 * 
	 * @param tag
	 *          tag of term
	 * @param args
	 *          arguments of term
	 */
	public CompoundTerm(final CompoundTermTag tag, final Term args[])
	{
		this.tag = tag;
		this.args = args.clone();
	}

	/**
	 * Work item for iterative clone traversal.
	 */
	private record CloneWork(CompoundTerm source, CompoundTerm target, int index) {}

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
		CompoundTerm result = (CompoundTerm) context.getTerm(this);
		if (result != null)
		{
			return result;
		}

		result = new CompoundTerm(tag);
		context.putTerm(this, result);

		final var queue = new ArrayDeque<CloneWork>();
		queue.push(new CloneWork(this, result, 0));

		while (!queue.isEmpty())
		{
			final var work = queue.pop();
			for (int i = work.index; i < work.source.args.length; i++)
			{
				final var arg = work.source.args[i];
				if (arg == null)
				{
					continue;
				}

				final var dereferenced = arg.dereference();
				if (dereferenced instanceof VariableTerm vt)
				{
					work.target.args[i] = vt.clone(context);
				}
				else if (dereferenced instanceof CompoundTerm ct)
				{
					CompoundTerm cloned = (CompoundTerm) context.getTerm(ct);
					if (cloned == null)
					{
						cloned = new CompoundTerm(ct.tag);
						context.putTerm(ct, cloned);
						queue.push(new CloneWork(work.source, work.target, i + 1));
						queue.push(new CloneWork(ct, cloned, 0));
						work.target.args[i] = cloned;
						break;
					}
					work.target.args[i] = cloned;
				}
				else
				{
					work.target.args[i] = dereferenced;
				}
			}
		}
		return result;
	}

	/**
	 * get type of term
	 * 
	 * @return type of term
	 */
	@Override
	public TermType getType()
	{
		return TermType.COMPOUND;
	}
}
