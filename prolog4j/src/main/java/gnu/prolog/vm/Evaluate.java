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

import gnu.prolog.database.Pair;
import gnu.prolog.term.AtomTerm;
import gnu.prolog.term.CompoundTerm;
import gnu.prolog.term.CompoundTermTag;
import gnu.prolog.term.DecimalTerm;
import gnu.prolog.term.FloatTerm;
import gnu.prolog.term.IntegerTerm;
import gnu.prolog.term.Term;
import gnu.prolog.term.VariableTerm;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Random;

/**
 * Evaluates mathematical expressions as instructed by the
 * {@link gnu.prolog.vm.builtins.arithmetics.Predicates is} predicate and
 * {@link gnu.prolog.vm.builtins.arithmetics.Predicates equality testing terms}.
 *
 */
public class Evaluate
{
	private Evaluate()
	{}

	public final static CompoundTermTag add2 = CompoundTermTag.get("+", 2);
	public final static CompoundTermTag sub2 = CompoundTermTag.get("-", 2);
	public final static CompoundTermTag mul2 = CompoundTermTag.get("*", 2);
	public final static CompoundTermTag intdiv2 = CompoundTermTag.get("//", 2);
	public final static CompoundTermTag div2 = CompoundTermTag.get("/", 2);
	public final static CompoundTermTag rem2 = CompoundTermTag.get("rem", 2);
	public final static CompoundTermTag mod2 = CompoundTermTag.get("mod", 2);
	public final static CompoundTermTag neg1 = CompoundTermTag.get("-", 1);
	public final static CompoundTermTag abs1 = CompoundTermTag.get("abs", 1);
	public final static CompoundTermTag sqrt1 = CompoundTermTag.get("sqrt", 1);
	public final static CompoundTermTag sign1 = CompoundTermTag.get("sign", 1);
	public final static CompoundTermTag intpart1 = CompoundTermTag.get("float_integer_part", 1);
	public final static CompoundTermTag fractpart1 = CompoundTermTag.get("float_fractional_part", 1);
	public final static CompoundTermTag float1 = CompoundTermTag.get("float", 1);
	public final static CompoundTermTag floor1 = CompoundTermTag.get("floor", 1);
	public final static CompoundTermTag truncate1 = CompoundTermTag.get("truncate", 1);
	public final static CompoundTermTag round1 = CompoundTermTag.get("round", 1);
	public final static CompoundTermTag ceiling1 = CompoundTermTag.get("ceiling", 1);
	public final static CompoundTermTag power2 = CompoundTermTag.get("**", 2);
	public final static CompoundTermTag caret2 = CompoundTermTag.get("^", 2);
	public final static CompoundTermTag min2 = CompoundTermTag.get("min", 2);
	public final static CompoundTermTag max2 = CompoundTermTag.get("max", 2);
	public final static CompoundTermTag xor2 = CompoundTermTag.get("xor", 2);
	public final static CompoundTermTag sin1 = CompoundTermTag.get("sin", 1);
	public final static CompoundTermTag cos1 = CompoundTermTag.get("cos", 1);
	public final static CompoundTermTag atan1 = CompoundTermTag.get("atan", 1);
	public final static CompoundTermTag exp1 = CompoundTermTag.get("exp", 1);
	public final static CompoundTermTag log1 = CompoundTermTag.get("log", 1);
	public final static CompoundTermTag brshift2 = CompoundTermTag.get(">>", 2);
	public final static CompoundTermTag blshift2 = CompoundTermTag.get("<<", 2);
	public final static CompoundTermTag band2 = CompoundTermTag.get("/\\", 2);
	public final static CompoundTermTag bor2 = CompoundTermTag.get("\\/", 2);
	public final static CompoundTermTag bnot1 = CompoundTermTag.get("\\", 1);
	/**
	 * Implementation of the random/1 predicate <a
	 * href="http://www.swi-prolog.org/man/arith.html#random/1">defined in
	 * SWI-Prolog</a>
	 *
	 * random(+IntExpr)
	 *
	 * Evaluates to a random integer i for which 0 &lt;= i &lt; IntExpr.
	 *
	 */
	public final static CompoundTermTag random1 = CompoundTermTag.get("random", 1);
	private final static Random random = new Random();

	public final static AtomTerm floatAtom = AtomTerm.get("float");

	/**
	 * Check if a tag represents a valid evaluable function.
	 * ISO Prolog requires checking this before evaluating arguments to report correct errors.
	 *
	 * @param tag the compound term tag to check
	 * @return true if the tag is a known evaluable function
	 */
	private static boolean isEvaluableTag(final CompoundTermTag tag) {
		return tag == add2 || tag == sub2 || tag == mul2 || tag == intdiv2 || tag == div2 ||
			   tag == rem2 || tag == mod2 || tag == neg1 || tag == abs1 || tag == sqrt1 ||
			   tag == sign1 || tag == intpart1 || tag == fractpart1 || tag == float1 ||
			   tag == floor1 || tag == truncate1 || tag == round1 || tag == ceiling1 ||
			   tag == power2 || tag == caret2 || tag == min2 || tag == max2 || tag == xor2 ||
			   tag == sin1 || tag == cos1 || tag == atan1 || tag == exp1 || tag == log1 ||
			   tag == brshift2 || tag == blshift2 || tag == band2 || tag == bor2 || tag == bnot1 ||
			   tag == random1;
	}

	private static void zeroDivisor() throws PrologException
	{
		PrologException.evaluationError(TermConstants.zeroDivisorAtom);
	}

	private static void intOverflow() throws PrologException
	{
		PrologException.evaluationError(TermConstants.intOverflowAtom);
	}

	private static void floatOverflow() throws PrologException
	{
		PrologException.evaluationError(TermConstants.floatOverflowAtom);
	}

	private static void undefined() throws PrologException
	{
		PrologException.evaluationError(TermConstants.undefinedAtom);
	}

	private static Pair<Double, Double> toDouble(final Term arg0, final Term arg1)
	{
		double d0 = switch (arg0) {
			case IntegerTerm i0 -> i0.value;
			case FloatTerm f0 -> f0.value;
			case DecimalTerm dt0 -> dt0.value.doubleValue();
			default -> throw new IllegalArgumentException("Expected numeric term");
		};

		double d1 = switch (arg1) {
			case IntegerTerm i1 -> i1.value;
			case FloatTerm f1 -> f1.value;
			case DecimalTerm dt1 -> dt1.value.doubleValue();
			default -> throw new IllegalArgumentException("Expected numeric term");
		};

		return new Pair<>(d0, d1);
	}

	/**
	 * Convert numeric terms to BigDecimal for precise arithmetic
	 */
	private static BigDecimal toBigDecimal(final Term term) {
		return switch (term) {
			case IntegerTerm it -> BigDecimal.valueOf(it.value);
			case FloatTerm ft -> ft.exactValue();
			case DecimalTerm dt -> dt.value;
			default -> throw new IllegalArgumentException("Expected numeric term");
		};
	}

	private static BigDecimal exactBigDecimal(final Term term) {
		return switch (term) {
			case IntegerTerm it -> BigDecimal.valueOf(it.value);
			case FloatTerm ft -> ft.exactValue();
			case DecimalTerm dt -> dt.value;
			default -> throw new IllegalArgumentException("Expected numeric term");
		};
	}

	private static boolean isIntegerValue(final BigDecimal value) {
		return value.stripTrailingZeros().scale() <= 0;
	}

	private static FloatTerm floatFromExact(final BigDecimal exact, final int displayScale) throws PrologException {
		double dv = exact.doubleValue();
		if (Double.isInfinite(dv)) {
			floatOverflow();
		}
		return new FloatTerm(exact, displayScale);
	}

	/**
	 * Perform binary arithmetic operation on two terms, returning DecimalTerm if either operand is DecimalTerm
	 */
	private static Term binaryOp(final Term arg0, final Term arg1,
								  final java.util.function.BiFunction<BigDecimal, BigDecimal, BigDecimal> decimalOp,
								  final java.util.function.LongBinaryOperator intOp) throws PrologException {
		// If either operand is DecimalTerm, use BigDecimal arithmetic
		if (arg0 instanceof DecimalTerm || arg1 instanceof DecimalTerm) {
			BigDecimal bd0 = toBigDecimal(arg0);
			BigDecimal bd1 = toBigDecimal(arg1);
			BigDecimal result = decimalOp.apply(bd0, bd1);
			return new DecimalTerm(result);
		}

		// If either operand is FloatTerm, use double arithmetic
		if (arg0 instanceof FloatTerm || arg1 instanceof FloatTerm) {
			BigDecimal bd0 = exactBigDecimal(arg0);
			BigDecimal bd1 = exactBigDecimal(arg1);
			BigDecimal result = decimalOp.apply(bd0, bd1);
			int displayScale = isIntegerValue(result) ? 1 : 4;
			return floatFromExact(result, displayScale);
		}

		// Both are integers
		IntegerTerm i0 = (IntegerTerm) arg0;
		IntegerTerm i1 = (IntegerTerm) arg1;
		long res = intOp.applyAsLong(i0.value, i1.value);
		if (res > Integer.MAX_VALUE || res < Integer.MIN_VALUE) {
			intOverflow();
		}
		return IntegerTerm.get((int) res);
	}

	public static Term evaluate(Term term) throws PrologException
	{
		term = term.dereference();// ensure we are looking at most instantiated
		// value
		switch (term) {
			case FloatTerm ft -> {
				return term;
			}
			case IntegerTerm it -> {
				return term;
			}
			case DecimalTerm dt -> {
				return term;
			}
			case VariableTerm vt -> {
				PrologException.instantiationError(term);
			}
			default -> {}
		}

		CompoundTerm ct = switch (term) {
			case CompoundTerm c -> c;
			case AtomTerm at -> {
				// Atoms are treated as 0-arity predicates in evaluable context
				PrologException.typeError(TermConstants.evaluableAtom,
					CompoundTermTag.get(at.value, 0).getPredicateIndicator());
				yield null; // Never reached
			}
			default -> {
				PrologException.typeError(TermConstants.evaluableAtom, term);
				yield null; // Never reached
			}
		};
		CompoundTermTag tag = ct.tag;
		int i, arity = tag.arity;
		Term sargs[] = ct.args;

		// ISO Prolog: Check if tag is a valid evaluable function before evaluating arguments
		// This ensures type_error reports the correct functor, not a nested argument
		if (!isEvaluableTag(tag)) {
			PrologException.typeError(TermConstants.evaluableAtom, tag.getPredicateIndicator());
		}

		Term args[] = new Term[arity];
		for (i = 0; i < arity; i++)
		{
			args[i] = evaluate(sargs[i].dereference());
		}
			if (tag == add2) // ***************************************
			{
				return binaryOp(args[0], args[1],
					(bd0, bd1) -> bd0.add(bd1, DecimalTerm.DEFAULT_CONTEXT),
					(i0, i1) -> i0 + i1);
			}
			else if (tag == sub2) // ***************************************
			{
				return binaryOp(args[0], args[1],
					(bd0, bd1) -> bd0.subtract(bd1, DecimalTerm.DEFAULT_CONTEXT),
					(i0, i1) -> i0 - i1);
			}
			else if (tag == mul2) // ***************************************
			{
				return binaryOp(args[0], args[1],
					(bd0, bd1) -> bd0.multiply(bd1, DecimalTerm.DEFAULT_CONTEXT),
					(i0, i1) -> i0 * i1);
			}
			else if (tag == intdiv2) // ***************************************
			{
				Term arg0 = args[0];
				Term arg1 = args[1];
				IntegerTerm i0 = switch (arg0) {
					case IntegerTerm it -> it;
					default -> {
						PrologException.typeError(TermConstants.integerAtom, arg0);
						yield null; // Never reached
					}
				};
				IntegerTerm i1 = switch (arg1) {
					case IntegerTerm it -> it;
					default -> {
						PrologException.typeError(TermConstants.integerAtom, arg1);
						yield null; // Never reached
					}
				};
				if (i1.value == 0)
				{
					zeroDivisor();
				}
				int res = i0.value / i1.value;
				return IntegerTerm.get(res);
			}
			else if (tag == div2) // ***************************************
			{
				Term arg0 = args[0];
				Term arg1 = args[1];

				// If either operand is DecimalTerm, use BigDecimal for precision and return DecimalTerm
				if (arg0 instanceof DecimalTerm || arg1 instanceof DecimalTerm) {
					BigDecimal bd0 = toBigDecimal(arg0);
					BigDecimal bd1 = toBigDecimal(arg1);

					if (bd1.compareTo(BigDecimal.ZERO) == 0) {
						zeroDivisor();
					}
					BigDecimal result = bd0.divide(bd1, DecimalTerm.DEFAULT_CONTEXT);
					return new DecimalTerm(result);
				}

				// Otherwise, use BigDecimal for exact decimal formatting and return FloatTerm
				BigDecimal bd0 = exactBigDecimal(arg0);
				BigDecimal bd1 = exactBigDecimal(arg1);
				if (bd1.compareTo(BigDecimal.ZERO) == 0) {
					zeroDivisor();
				}

				BigDecimal exactResult;
				int displayScale;
				try {
					exactResult = bd0.divide(bd1);
					if (isIntegerValue(exactResult)) {
						displayScale = 1;
					} else {
						displayScale = Math.max(4, exactResult.scale() + 3);
					}
				} catch (ArithmeticException ex) {
					exactResult = bd0.divide(bd1, 17, java.math.RoundingMode.DOWN);
					displayScale = 14;
				}

				return floatFromExact(exactResult, displayScale);
			}
			else if (tag == rem2) // ***************************************
			{
				Term arg0 = args[0];
				Term arg1 = args[1];
				IntegerTerm i0 = switch (arg0) {
					case IntegerTerm it -> it;
					default -> {
						PrologException.typeError(TermConstants.integerAtom, arg0);
						yield null; // Never reached
					}
				};
				IntegerTerm i1 = switch (arg1) {
					case IntegerTerm it -> it;
					default -> {
						PrologException.typeError(TermConstants.integerAtom, arg1);
						yield null; // Never reached
					}
				};
				if (i1.value == 0)
				{
					zeroDivisor();
				}
				int res = i0.value % i1.value;
				return IntegerTerm.get(res);
			}
			else if (tag == mod2) // ***************************************
			{
				Term arg0 = args[0];
				Term arg1 = args[1];
				IntegerTerm i0 = switch (arg0) {
					case IntegerTerm it -> it;
					default -> {
						PrologException.typeError(TermConstants.integerAtom, arg0);
						yield null; // Never reached
					}
				};
				IntegerTerm i1 = switch (arg1) {
					case IntegerTerm it -> it;
					default -> {
						PrologException.typeError(TermConstants.integerAtom, arg1);
						yield null; // Never reached
					}
				};
				if (i1.value == 0)
				{
					zeroDivisor();
				}
				int res = i0.value - (int) Math.floor((double) i0.value / i1.value) * i1.value;
				return IntegerTerm.get(res);
			}
			else if (tag == neg1) // ***************************************
			{
				Term arg0 = args[0];
				return switch (arg0) {
					case IntegerTerm i0 -> {
						if (i0.value == Integer.MIN_VALUE)
						{
							intOverflow();
						}
						yield IntegerTerm.get(-i0.value);
					}
					case FloatTerm f0 -> {
						BigDecimal exact = f0.exactValue().negate();
						int displayScale = f0.hasDisplayScale() ? f0.getDisplayScale()
							: (isIntegerValue(exact) ? 1 : 4);
						yield floatFromExact(exact, displayScale);
					}
					case DecimalTerm dt -> new DecimalTerm(dt.value.negate());
					default -> throw new IllegalStateException("Unexpected numeric term type");
				};
			}
			else if (tag == abs1) // ***************************************
			{
				Term arg0 = args[0];
				return switch (arg0) {
					case IntegerTerm i0 -> {
						if (i0.value == Integer.MIN_VALUE)
						{
							intOverflow();
						}
						yield IntegerTerm.get(Math.abs(i0.value));
					}
					case FloatTerm f0 -> {
						BigDecimal exact = f0.exactValue().abs();
						int displayScale = f0.hasDisplayScale() ? f0.getDisplayScale()
							: (isIntegerValue(exact) ? 1 : 4);
						yield floatFromExact(exact, displayScale);
					}
					case DecimalTerm dt -> new DecimalTerm(dt.value.abs());
					default -> throw new IllegalStateException("Unexpected numeric term type");
				};
			}
			else if (tag == sqrt1) // ***************************************
			{
				Term arg0 = args[0];

				// Use BigDecimal sqrt for DecimalTerm
				if (arg0 instanceof DecimalTerm) {
					BigDecimal bd = ((DecimalTerm) arg0).value;
					BigDecimal result = bd.sqrt(DecimalTerm.DEFAULT_CONTEXT);
					return new DecimalTerm(result);
				}

				double d0 = switch (arg0) {
					case IntegerTerm i0 -> i0.value;
					case FloatTerm f0 -> f0.value;
					default -> throw new IllegalArgumentException("Expected numeric term");
				};
				double res = Math.sqrt(d0);
				if (res == Double.POSITIVE_INFINITY || res == Double.NEGATIVE_INFINITY)
				{
					floatOverflow();
				}
				return new FloatTerm(res);
			}
			else if (tag == sign1) // ***************************************
			{
				Term arg0 = args[0];
				return switch (arg0) {
					case IntegerTerm i0 -> IntegerTerm.get(i0.value == 0 ? 0 : (i0.value > 0 ? 1 : -1));
					case FloatTerm f0 -> {
						BigDecimal exact = f0.value == 0.0 ? BigDecimal.ZERO
							: (f0.value > 0 ? BigDecimal.ONE : BigDecimal.ONE.negate());
						yield floatFromExact(exact, 1);
					}
					case DecimalTerm dt -> {
						int cmp = dt.value.compareTo(BigDecimal.ZERO);
						yield new DecimalTerm(cmp == 0 ? BigDecimal.ZERO : (cmp > 0 ? BigDecimal.ONE : BigDecimal.ONE.negate()));
					}
					default -> throw new IllegalStateException("Unexpected numeric term type");
				};
			}
			else if (tag == intpart1) // ***************************************
			{
				Term arg0 = args[0];
				return switch (arg0) {
					case IntegerTerm it -> {
						PrologException.typeError(floatAtom, arg0);
						yield null; // Never reached
					}
					case FloatTerm f0 -> {
						int sign = f0.value >= 0 ? 1 : -1;
						BigDecimal exact = f0.exactValue();
						BigDecimal intPart = exact.abs().setScale(0, java.math.RoundingMode.DOWN)
							.multiply(BigDecimal.valueOf(sign));
						yield floatFromExact(intPart, f0.hasDisplayScale() ? f0.getDisplayScale() : 1);
					}
					case DecimalTerm dt -> {
						BigDecimal intPart = dt.value.setScale(0, java.math.RoundingMode.DOWN);
						yield new DecimalTerm(intPart);
					}
					default -> throw new IllegalStateException("Unexpected term type");
				};
			}
			else if (tag == fractpart1) // ***************************************
			{
				Term arg0 = args[0];
				return switch (arg0) {
					case IntegerTerm it -> {
						PrologException.typeError(floatAtom, arg0);
						yield null; // Never reached
					}
					case FloatTerm f0 -> {
						int sign = f0.value >= 0 ? 1 : -1;
						BigDecimal exact = f0.exactValue();
						BigDecimal intPart = exact.abs().setScale(0, java.math.RoundingMode.DOWN)
							.multiply(BigDecimal.valueOf(sign));
						BigDecimal fractPart = exact.subtract(intPart);
						yield floatFromExact(fractPart, f0.hasDisplayScale() ? f0.getDisplayScale() : 4);
					}
					case DecimalTerm dt -> {
						BigDecimal intPart = dt.value.setScale(0, java.math.RoundingMode.DOWN);
						BigDecimal fractPart = dt.value.subtract(intPart);
						yield new DecimalTerm(fractPart);
					}
					default -> throw new IllegalStateException("Unexpected term type");
				};
			}
			else if (tag == float1) // ***************************************
			{
				Term arg0 = args[0];
				return switch (arg0) {
					case IntegerTerm i0 -> floatFromExact(BigDecimal.valueOf(i0.value), 1);
					case FloatTerm f0 -> {
						if (f0.hasDisplayScale())
						{
							yield floatFromExact(f0.exactValue(), 17);
						}
						yield arg0;
					}
					case DecimalTerm dt -> floatFromExact(dt.value, 1);
					default -> throw new IllegalStateException("Unexpected numeric term type");
				};
			}
			else if (tag == floor1) // ***************************************
			{
				Term arg0 = args[0];
				return switch (arg0) {
					case IntegerTerm it -> {
						PrologException.typeError(floatAtom, arg0);
						yield null; // Never reached
					}
					case FloatTerm f0 -> {
						double res = Math.floor(f0.value);
						if (res < Integer.MIN_VALUE || res > Integer.MAX_VALUE)
						{
							intOverflow();
						}
						yield IntegerTerm.get((int) Math.round(res));
					}
					case DecimalTerm dt -> {
						BigDecimal floor = dt.value.setScale(0, java.math.RoundingMode.FLOOR);
						try {
							yield IntegerTerm.get(floor.intValueExact());
						} catch (ArithmeticException e) {
							intOverflow();
							yield null; // Never reached
						}
					}
					default -> throw new IllegalStateException("Unexpected term type");
				};
			}
			else if (tag == truncate1) // ***************************************
			{
				Term arg0 = args[0];
				return switch (arg0) {
					case IntegerTerm it -> {
						PrologException.typeError(floatAtom, arg0);
						yield null; // Never reached
					}
					case FloatTerm f0 -> {
						int sign = f0.value >= 0 ? 1 : -1;
						double res = sign * Math.floor(Math.abs(f0.value));
						if (res < Integer.MIN_VALUE || res > Integer.MAX_VALUE)
						{
							intOverflow();
						}
						yield IntegerTerm.get((int) Math.round(res));
					}
					case DecimalTerm dt -> {
						BigDecimal truncated = dt.value.setScale(0, java.math.RoundingMode.DOWN);
						try {
							yield IntegerTerm.get(truncated.intValueExact());
						} catch (ArithmeticException e) {
							intOverflow();
							yield null; // Never reached
						}
					}
					default -> throw new IllegalStateException("Unexpected term type");
				};
			}
			else if (tag == round1) // ***************************************
			{
				Term arg0 = args[0];
				return switch (arg0) {
					case IntegerTerm it -> {
						PrologException.typeError(floatAtom, arg0);
						yield null; // Never reached
					}
					case FloatTerm f0 -> {
						double res = Math.floor(f0.value + 0.5);
						if (res < Integer.MIN_VALUE || res > Integer.MAX_VALUE)
						{
							intOverflow();
						}
						yield IntegerTerm.get((int) Math.round(res));
					}
					case DecimalTerm dt -> {
						BigDecimal rounded = dt.value.setScale(0, java.math.RoundingMode.HALF_UP);
						try {
							yield IntegerTerm.get(rounded.intValueExact());
						} catch (ArithmeticException e) {
							intOverflow();
							yield null; // Never reached
						}
					}
					default -> throw new IllegalStateException("Unexpected term type");
				};
			}
			else if (tag == ceiling1) // ***************************************
			{
				Term arg0 = args[0];
				return switch (arg0) {
					case IntegerTerm it -> {
						PrologException.typeError(floatAtom, arg0);
						yield null; // Never reached
					}
					case FloatTerm f0 -> {
						double res = -Math.floor(-f0.value);
						if (res < Integer.MIN_VALUE || res > Integer.MAX_VALUE)
						{
							intOverflow();
						}
						yield IntegerTerm.get((int) Math.round(res));
					}
					case DecimalTerm dt -> {
						BigDecimal ceiling = dt.value.setScale(0, java.math.RoundingMode.CEILING);
						try {
							yield IntegerTerm.get(ceiling.intValueExact());
						} catch (ArithmeticException e) {
							intOverflow();
							yield null; // Never reached
						}
					}
					default -> throw new IllegalStateException("Unexpected term type");
				};
			}
			else if (tag == power2) // ***************************************
			{
				Pair<Double, Double> doubles = toDouble(args[0], args[1]);
				double d0 = doubles.left();
				double d1 = doubles.right();

				if (d0 == 0 && d1 < 0)
				{
					undefined();
				}
				double res = Math.pow(d0, d1);
				if (res == Double.POSITIVE_INFINITY || res == Double.NEGATIVE_INFINITY)
				{
					floatOverflow();
				}
				return new FloatTerm(res);
			}
			else if (tag == caret2) // *************************************** (^ power operator)
			{
				Term arg0 = args[0];
				Term arg1 = args[1];

				// If both are integers and exponent is non-negative, return integer
				if (arg0 instanceof IntegerTerm && arg1 instanceof IntegerTerm)
				{
					IntegerTerm i0 = (IntegerTerm) arg0;
					IntegerTerm i1 = (IntegerTerm) arg1;

					if (i0.value == 0 && i1.value < 0)
					{
						undefined();
					}

					if (i1.value >= 0 && i1.value <= 30) // Prevent overflow for reasonable exponents
					{
						long base = i0.value;
						int exp = i1.value;
						long result = 1;
						for (int j = 0; j < exp; j++)
						{
							result *= base;
							if (result > Integer.MAX_VALUE || result < Integer.MIN_VALUE)
							{
								// Overflow, fall back to float
								break;
							}
						}
						if (result <= Integer.MAX_VALUE && result >= Integer.MIN_VALUE)
						{
							return IntegerTerm.get((int) result);
						}
					}
				}

				// Otherwise use floating point
				Pair<Double, Double> doubles = toDouble(arg0, arg1);
				double d0 = doubles.left();
				double d1 = doubles.right();

				if (d0 == 0 && d1 < 0)
				{
					undefined();
				}
				double res = Math.pow(d0, d1);
				if (res == Double.POSITIVE_INFINITY || res == Double.NEGATIVE_INFINITY)
				{
					floatOverflow();
				}
				return new FloatTerm(res);
			}
			else if (tag == min2) // ***************************************
			{
				Term arg0 = args[0];
				Term arg1 = args[1];

				// If either operand is DecimalTerm, use BigDecimal comparison
				if (arg0 instanceof DecimalTerm || arg1 instanceof DecimalTerm) {
					BigDecimal bd0 = toBigDecimal(arg0);
					BigDecimal bd1 = toBigDecimal(arg1);
					return new DecimalTerm(bd0.compareTo(bd1) <= 0 ? bd0 : bd1);
				}

				return switch (arg0) {
					case IntegerTerm i0 when arg1 instanceof IntegerTerm -> {
						IntegerTerm i1 = (IntegerTerm) arg1;
						yield IntegerTerm.get(Math.min(i0.value, i1.value));
					}
					case FloatTerm f0 when arg1 instanceof IntegerTerm -> {
						IntegerTerm i1 = (IntegerTerm) arg1;
						yield new FloatTerm(Math.min(f0.value, i1.value));
					}
					case IntegerTerm i0 when arg1 instanceof FloatTerm -> {
						FloatTerm f1 = (FloatTerm) arg1;
						yield new FloatTerm(Math.min(i0.value, f1.value));
					}
					case FloatTerm f0 when arg1 instanceof FloatTerm -> {
						FloatTerm f1 = (FloatTerm) arg1;
						yield new FloatTerm(Math.min(f0.value, f1.value));
					}
					default -> throw new IllegalStateException("Unexpected numeric term types");
				};
			}
			else if (tag == max2) // ***************************************
			{
				Term arg0 = args[0];
				Term arg1 = args[1];

				// If either operand is DecimalTerm, use BigDecimal comparison
				if (arg0 instanceof DecimalTerm || arg1 instanceof DecimalTerm) {
					BigDecimal bd0 = toBigDecimal(arg0);
					BigDecimal bd1 = toBigDecimal(arg1);
					return new DecimalTerm(bd0.compareTo(bd1) >= 0 ? bd0 : bd1);
				}

				return switch (arg0) {
					case IntegerTerm i0 when arg1 instanceof IntegerTerm -> {
						IntegerTerm i1 = (IntegerTerm) arg1;
						yield IntegerTerm.get(Math.max(i0.value, i1.value));
					}
					case FloatTerm f0 when arg1 instanceof IntegerTerm -> {
						IntegerTerm i1 = (IntegerTerm) arg1;
						yield new FloatTerm(Math.max(f0.value, i1.value));
					}
					case IntegerTerm i0 when arg1 instanceof FloatTerm -> {
						FloatTerm f1 = (FloatTerm) arg1;
						yield new FloatTerm(Math.max(i0.value, f1.value));
					}
					case FloatTerm f0 when arg1 instanceof FloatTerm -> {
						FloatTerm f1 = (FloatTerm) arg1;
						yield new FloatTerm(Math.max(f0.value, f1.value));
					}
					default -> throw new IllegalStateException("Unexpected numeric term types");
				};
			}
			else if (tag == xor2) // ***************************************
			{
				Term arg0 = args[0];
				Term arg1 = args[1];
				typeTestInt(arg0);
				typeTestInt(arg1);
				IntegerTerm i0 = (IntegerTerm) arg0;
				IntegerTerm i1 = (IntegerTerm) arg1;
				int res = i0.value ^ i1.value;
				return IntegerTerm.get(res);
			}
			else if (tag == sin1) // ***************************************
			{
				Term arg0 = args[0];
				if (arg0 instanceof DecimalTerm dt)
				{
					double res = Math.sin(dt.value.doubleValue());
					return new DecimalTerm(BigDecimal.valueOf(res));
				}
				double d0 = switch (arg0) {
					case IntegerTerm i0 -> i0.value;
					case FloatTerm f0 -> f0.value;
					default -> throw new IllegalArgumentException("Expected numeric term");
				};
				double res = Math.sin(d0);
				return new FloatTerm(res);
			}
			else if (tag == cos1) // ***************************************
			{
				Term arg0 = args[0];
				if (arg0 instanceof DecimalTerm dt)
				{
					double res = Math.cos(dt.value.doubleValue());
					return new DecimalTerm(BigDecimal.valueOf(res));
				}
				double d0 = switch (arg0) {
					case IntegerTerm i0 -> i0.value;
					case FloatTerm f0 -> f0.value;
					default -> throw new IllegalArgumentException("Expected numeric term");
				};
				double res = Math.cos(d0);
				return new FloatTerm(res);
			}
			else if (tag == atan1) // ***************************************
			{
				Term arg0 = args[0];
				if (arg0 instanceof DecimalTerm dt)
				{
					double res = Math.atan(dt.value.doubleValue());
					return new DecimalTerm(BigDecimal.valueOf(res));
				}
				double d0 = switch (arg0) {
					case IntegerTerm i0 -> i0.value;
					case FloatTerm f0 -> f0.value;
					default -> throw new IllegalArgumentException("Expected numeric term");
				};
				double res = Math.atan(d0);
				return new FloatTerm(res);
			}
			else if (tag == exp1) // ***************************************
			{
				Term arg0 = args[0];
				if (arg0 instanceof DecimalTerm dt)
				{
					double res = Math.exp(dt.value.doubleValue());
					if (res == Double.POSITIVE_INFINITY || res == Double.NEGATIVE_INFINITY)
					{
						floatOverflow();
					}
					return new DecimalTerm(BigDecimal.valueOf(res));
				}
				double d0 = switch (arg0) {
					case IntegerTerm i0 -> i0.value;
					case FloatTerm f0 -> f0.value;
					default -> throw new IllegalArgumentException("Expected numeric term");
				};
				double res = Math.exp(d0);
				if (res == Double.POSITIVE_INFINITY || res == Double.NEGATIVE_INFINITY)
				{
					floatOverflow();
				}
				return new FloatTerm(res);
			}
			else if (tag == log1) // ***************************************
			{
				Term arg0 = args[0];
				if (arg0 instanceof DecimalTerm dt)
				{
					double d0 = dt.value.doubleValue();
					if (d0 <= 0)
					{
						undefined();
					}
					double res = Math.log(d0);
					if (res == Double.POSITIVE_INFINITY || res == Double.NEGATIVE_INFINITY)
					{
						floatOverflow();
					}
					return new DecimalTerm(BigDecimal.valueOf(res));
				}
				double d0 = switch (arg0) {
					case IntegerTerm i0 -> i0.value;
					case FloatTerm f0 -> f0.value;
					default -> throw new IllegalArgumentException("Expected numeric term");
				};
				if (d0 <= 0)
				{
					undefined();
				}
				double res = Math.log(d0);
				if (res == Double.POSITIVE_INFINITY || res == Double.NEGATIVE_INFINITY)
				{
					floatOverflow();
				}
				return new FloatTerm(res);
			}
			else if (tag == brshift2) // ***************************************
			{
				Term arg0 = args[0];
				Term arg1 = args[1];
				typeTestInt(arg0);
				typeTestInt(arg1);
				IntegerTerm i0 = (IntegerTerm) arg0;
				IntegerTerm i1 = (IntegerTerm) arg1;
				int res = i0.value >> i1.value;
				return IntegerTerm.get(res);
			}
			else if (tag == blshift2) // ***************************************
			{
				Term arg0 = args[0];
				Term arg1 = args[1];
				typeTestInt(arg0);
				typeTestInt(arg1);
				IntegerTerm i0 = (IntegerTerm) arg0;
				IntegerTerm i1 = (IntegerTerm) arg1;
				int res = i0.value << i1.value;
				return IntegerTerm.get(res);
			}
			else if (tag == band2) // ***************************************
			{
				Term arg0 = args[0];
				Term arg1 = args[1];
				typeTestInt(arg0);
				typeTestInt(arg1);
				IntegerTerm i0 = (IntegerTerm) arg0;
				IntegerTerm i1 = (IntegerTerm) arg1;
				int res = i0.value & i1.value;
				return IntegerTerm.get(res);
			}
			else if (tag == bor2) // ***************************************
			{
				Term arg0 = args[0];
				Term arg1 = args[1];
				typeTestInt(arg0);
				typeTestInt(arg1);
				IntegerTerm i0 = (IntegerTerm) arg0;
				IntegerTerm i1 = (IntegerTerm) arg1;
				int res = i0.value | i1.value;
				return IntegerTerm.get(res);
			}
			else if (tag == bnot1) // ***************************************
			{
				Term arg0 = args[0];
				typeTestInt(arg0);
				IntegerTerm i0 = (IntegerTerm) arg0;
				int res = ~i0.value;
				return IntegerTerm.get(res);
			}
			else if (tag == random1) // ***************************************
			{
				Term arg0 = args[0];
				typeTestInt(arg0);
				IntegerTerm limit = (IntegerTerm) arg0;
				if (limit.value < 0)
				{
					PrologException.domainError(TermConstants.notLessThanZeroAtom, arg0);
				}
				double rand;
				synchronized (random)
				{// avoid concurrency issues
					rand = random.nextDouble();// rand is uniformly distributed from 0 to
					// 1
				}
				int res = (int) (rand * limit.value);// scale it and cast it
				return IntegerTerm.get(res);
			}
			else
			// ***************************************
			{
				PrologException.typeError(TermConstants.evaluableAtom, tag.getPredicateIndicator());
			}
		return null; // fake return
	}

	/**
	 * Test the term for an integer term
	 *
	 * @param term
	 * @throws PrologException
	 */
	protected static void typeTestInt(final Term term) throws PrologException
	{
		switch (term) {
			case IntegerTerm it -> {
				return;
			}
			case VariableTerm vt -> {
				PrologException.instantiationError(term);
			}
			default -> {
				PrologException.typeError(TermConstants.integerAtom, term);
			}
		}
	}
}
