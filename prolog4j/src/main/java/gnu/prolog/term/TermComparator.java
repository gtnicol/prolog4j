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

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicLong;

/** comparator for two term */
public class TermComparator implements Comparator<Term>
{
	private static final Map<Term, Long> GLOBAL_ORDER = new WeakHashMap<>();
	private static final AtomicLong nextOrderId = new AtomicLong(0);

	private static long getOrderId(final Term term)
	{
		synchronized (GLOBAL_ORDER)
		{
			return GLOBAL_ORDER.computeIfAbsent(term, key -> nextOrderId.getAndIncrement());
		}
	}

	private static int typeOrder(final TermType type)
	{
		return switch (type)
		{
			case VARIABLE, JAVA_OBJECT -> 1;
			case INTEGER, FLOAT, DECIMAL -> 2;
			case ATOM -> 3;
			case COMPOUND -> 4;
			case UNKNOWN -> 0;
		};
	}

	private static int compareNumeric(final Term t1, final Term t2)
	{
		BigDecimal bd1 = numericValue(t1);
		BigDecimal bd2 = numericValue(t2);
		int cmp = bd1.compareTo(bd2);
		if (cmp != 0)
		{
			return cmp;
		}
		return Integer.compare(numericTypeOrder(t1), numericTypeOrder(t2));
	}

	private static BigDecimal numericValue(final Term term)
	{
		return switch (term)
		{
			case FloatTerm ft -> ft.exactValue();
			case DecimalTerm dt -> dt.value;
			case IntegerTerm it -> BigDecimal.valueOf(it.value);
			default -> throw new IllegalArgumentException("Expected numeric term");
		};
	}

	private static int numericTypeOrder(final Term term)
	{
		if (term instanceof FloatTerm)
		{
			return 0;
		}
		if (term instanceof IntegerTerm)
		{
			return 1;
		}
		if (term instanceof DecimalTerm)
		{
			return 2;
		}
		return 3;
	}

	/**
	 * Compares its two arguments for order. Returns a negative integer, zero, or
	 * a positive integer as the first argument is less than, equal to, or greater
	 * than the second.
	 * <p>
	 * The implementor must ensure that sgn(compare(x, y)) == -sgn(compare(y, x))
	 * for all x and y. (This implies that compare(x, y) must throw an exception
	 * if and only if compare(y, x) throws an exception.)
	 * <p>
	 * The implementor must also ensure that the relation is transitive:
	 * ((compare(x, y)&gt;0) &amp;&amp; (compare(y, z)&gt;0)) implies compare(x,
	 * z)>0.
	 * <p>
	 * The implementer must also ensure that x.equals(y) implies that compare(x,
	 * y) == 0. Note that the converse is not necessarily true.
	 * <p>
	 * Finally, the implementer must ensure that compare(x, y) == 0 implies that
	 * sgn(compare(x, z)) == sgn(compare(y, z)), for all z.
	 * 
	 * @return a negative integer, zero, or a positive integer as the first
	 *         argument is less than, equal to, or greater than the second.
	 * @exception ClassCastException
	 *              the arguments' types prevent them from being compared by this
	 *              Comparator.
	 * @since JDK1.2
	 */
	public int compare(final Term o1, final Term o2)
	{
		Term t1 = o1.dereference();
		Term t2 = o2.dereference();
		if (t1 == t2)
		{
			return 0;
		}
		TermType ty1 = t1.getType();
		TermType ty2 = t2.getType();
		if (ty1 != ty2)
		{
			if (ty1.isNumeric() && ty2.isNumeric())
			{
				return compareNumeric(t1, t2);
			}
			int order1 = typeOrder(ty1);
			int order2 = typeOrder(ty2);
			if (order1 != order2)
			{
				return Integer.compare(order1, order2);
			}
			if ((ty1 == TermType.VARIABLE || ty1 == TermType.JAVA_OBJECT)
					&& (ty2 == TermType.VARIABLE || ty2 == TermType.JAVA_OBJECT))
			{
				return Long.compare(getOrderId(t1), getOrderId(t2));
			}
		}
		return switch (ty1)
		{
			case VARIABLE, JAVA_OBJECT -> {
				yield Long.compare(getOrderId(t1), getOrderId(t2));
			}
			case FLOAT -> {
				if (t1 instanceof FloatTerm ft1 && t2 instanceof FloatTerm ft2)
				{
					yield Double.compare(ft1.value, ft2.value);
				}
				yield 0;
			}
			case INTEGER -> {
				if (t1 instanceof IntegerTerm it1 && t2 instanceof IntegerTerm it2)
				{
					yield Integer.compare(it1.value, it2.value);
				}
				yield 0;
			}
			case DECIMAL -> {
				if (t1 instanceof DecimalTerm dt1 && t2 instanceof DecimalTerm dt2)
				{
					yield dt1.value.compareTo(dt2.value);
				}
				yield 0;
			}
			case ATOM -> {
				if (t1 instanceof AtomTerm at1 && t2 instanceof AtomTerm at2)
				{
					yield at1.value.compareTo(at2.value);
				}
				yield 0;
			}
			case COMPOUND -> {
				if (t1 instanceof CompoundTerm ct1 && t2 instanceof CompoundTerm ct2)
				{
					CompoundTermTag tag1 = ct1.tag;
					CompoundTermTag tag2 = ct2.tag;
					int ar1 = tag1.arity;
					int ar2 = tag2.arity;
					if (ar1 != ar2)
					{
						yield ar1 - ar2;
					}
					AtomTerm fu1 = tag1.functor;
					AtomTerm fu2 = tag2.functor;
					if (fu1 != fu2)
					{
						yield fu1.value.compareTo(fu2.value);
					}
					Term args1[] = ct1.args;
					Term args2[] = ct2.args;
					for (int i = 0; i < ar1; i++)
					{
						int rc = compare(args1[i], args2[i]);
						if (rc != 0)
						{
							yield rc;
						}
					}
				}
				yield 0;
			}
			case UNKNOWN -> 0;
		};
	}
}
