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
 * Core Virtual Machine (VM) for GNU Prolog for Java.
 *
 * <h2>Architecture Overview</h2>
 *
 * <p>This package contains the core execution engine for Prolog programs.
 * The architecture follows a separation between the environment (shared state)
 * and interpreters (per-thread execution contexts).</p>
 *
 * <h3>Key Classes</h3>
 *
 * <ul>
 *   <li>{@link gnu.prolog.vm.Environment} - The Prolog processor/engine that manages
 *       modules, predicates, streams, and flags. One Environment can be shared across
 *       multiple threads, but operations should be properly synchronized.</li>
 *
 *   <li>{@link gnu.prolog.vm.Interpreter} - Single-threaded execution context for
 *       running Prolog goals. Each thread must have its own Interpreter instance
 *       created via {@link gnu.prolog.vm.Environment#createInterpreter()}.</li>
 *
 *   <li>{@link gnu.prolog.vm.PrologCode} - Interface for executable predicate code.
 *       Implementations include built-in predicates and interpreted user-defined code.</li>
 *
 *   <li>{@link gnu.prolog.vm.PrologException} - Prolog-level exceptions following
 *       ISO Prolog exception handling semantics.</li>
 * </ul>
 *
 * <h3>Threading Model</h3>
 *
 * <p><strong>IMPORTANT:</strong> The {@link gnu.prolog.vm.Interpreter} class is
 * <em>NOT thread-safe</em>. Each thread that needs to execute Prolog goals must
 * create its own Interpreter instance:</p>
 *
 * <pre>{@code
 * Environment env = new Environment();
 * env.ensureLoaded(AtomTerm.get("myprogram.pl"));
 *
 * // For each thread:
 * Interpreter interpreter = env.createInterpreter();
 * env.runInitialization(interpreter);
 *
 * // Now use interpreter.runOnce() or interpreter.execute()
 * }</pre>
 *
 * <h3>Basic Usage Pattern</h3>
 *
 * <pre>{@code
 * // 1. Create environment
 * Environment env = new Environment();
 *
 * // 2. Load Prolog code
 * env.ensureLoaded(AtomTerm.get("program.pl"));
 *
 * // 3. Create interpreter
 * Interpreter interpreter = env.createInterpreter();
 * env.runInitialization(interpreter);
 *
 * // 4. Execute goals
 * Term goal = new CompoundTerm("my_predicate", new Term[]{...});
 * RC result = interpreter.runOnce(goal);
 *
 * // 5. Handle result
 * switch (result) {
 *     case SUCCESS, SUCCESS_LAST -> System.out.println("Goal succeeded");
 *     case FAIL -> System.out.println("Goal failed");
 *     case HALT -> System.out.println("Interpreter halted");
 * }
 * }</pre>
 *
 * @see gnu.prolog.vm.Environment
 * @see gnu.prolog.vm.Interpreter
 * @see gnu.prolog.vm.PrologCode
 */
package gnu.prolog.vm;
