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

/**
 * Prolog term representations for GNU Prolog for Java.
 *
 * <h2>Term Hierarchy</h2>
 *
 * <p>This package contains the sealed class hierarchy for representing Prolog terms.
 * The base class {@link gnu.prolog.term.Term} is sealed and permits only the specific
 * term types defined in this package.</p>
 *
 * <h3>Term Types</h3>
 *
 * <ul>
 *   <li>{@link gnu.prolog.term.AtomTerm} - Represents Prolog atoms (constants).
 *       Use {@code AtomTerm.get("name")} to obtain instances (interned).</li>
 *
 *   <li>{@link gnu.prolog.term.IntegerTerm} - Represents integer numbers.
 *       Use {@code IntegerTerm.get(42)} to obtain instances.</li>
 *
 *   <li>{@link gnu.prolog.term.FloatTerm} - Represents floating-point numbers.</li>
 *
 *   <li>{@link gnu.prolog.term.DecimalTerm} - Represents arbitrary-precision decimals.</li>
 *
 *   <li>{@link gnu.prolog.term.VariableTerm} - Represents Prolog variables.
 *       Variables are mutable; their {@code value} field can be bound during unification.</li>
 *
 *   <li>{@link gnu.prolog.term.CompoundTerm} - Represents compound terms (structures).
 *       Has a {@link gnu.prolog.term.CompoundTermTag} (functor/arity) and arguments.</li>
 *
 *   <li>{@link gnu.prolog.term.JavaObjectTerm} - Wraps Java objects for use in Prolog.</li>
 * </ul>
 *
 * <h3>Immutability</h3>
 *
 * <p>All term types are immutable <em>except</em> {@link gnu.prolog.term.VariableTerm},
 * which has a mutable {@code value} field that gets bound during unification.
 * This design allows terms to be shared safely across different parts of the system,
 * while variables can be unified during goal execution.</p>
 *
 * <h3>Term Construction</h3>
 *
 * <pre>{@code
 * // Atoms (interned)
 * AtomTerm hello = AtomTerm.get("hello");
 *
 * // Integers (cached for small values)
 * IntegerTerm answer = IntegerTerm.get(42);
 *
 * // Variables
 * VariableTerm x = new VariableTerm("X");
 *
 * // Compound terms: foo(X, 42)
 * CompoundTerm compound = new CompoundTerm(
 *     CompoundTermTag.get("foo", 2),
 *     new Term[]{x, answer}
 * );
 *
 * // Lists: [1, 2, 3]
 * Term list = CompoundTerm.getList(
 *     IntegerTerm.get(1),
 *     IntegerTerm.get(2),
 *     IntegerTerm.get(3)
 * );
 * }</pre>
 *
 * <h3>Dereferencing</h3>
 *
 * <p>Variables may be bound to other terms. Always call {@link gnu.prolog.term.Term#dereference()}
 * to follow the variable chain to the actual term value:</p>
 *
 * <pre>{@code
 * Term actual = term.dereference();
 * if (actual instanceof AtomTerm atom) {
 *     // Process atom
 * }
 * }</pre>
 *
 * @see gnu.prolog.term.Term
 * @see gnu.prolog.term.CompoundTermTag
 */
package gnu.prolog.term;
