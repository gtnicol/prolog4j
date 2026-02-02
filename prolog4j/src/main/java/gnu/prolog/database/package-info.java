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
 * Database and module management for GNU Prolog for Java.
 *
 * <h2>Overview</h2>
 *
 * <p>This package handles the storage and management of Prolog predicates,
 * modules, and the loading of Prolog source files.</p>
 *
 * <h3>Key Classes</h3>
 *
 * <ul>
 *   <li>{@link gnu.prolog.database.Module} - Represents a Prolog module containing
 *       predicates. Maintains a mapping from predicate tags to predicate definitions.</li>
 *
 *   <li>{@link gnu.prolog.database.Predicate} - Represents a predicate definition
 *       with its type (builtin, user-defined, external), clauses, and properties
 *       (dynamic, discontiguous, etc.).</li>
 *
 *   <li>{@link gnu.prolog.database.PrologTextLoaderState} - Manages the state of
 *       loading Prolog source files, including tracking loaded files, handling
 *       operators, and collecting errors.</li>
 *
 *   <li>{@link gnu.prolog.database.PrologTextLoader} - Parses and loads Prolog
 *       source files into the database.</li>
 * </ul>
 *
 * <h3>Predicate Types</h3>
 *
 * <p>Predicates can have different types as defined in {@link gnu.prolog.database.Predicate.TYPE}:</p>
 *
 * <ul>
 *   <li>{@code UNDEFINED} - Type not yet set</li>
 *   <li>{@code CONTROL} - Control constructs (call, !, etc.)</li>
 *   <li>{@code BUILD_IN} - Built-in ISO predicates</li>
 *   <li>{@code USER_DEFINED} - User-defined predicates from Prolog source</li>
 *   <li>{@code EXTERNAL} - Predicates implemented in Java</li>
 * </ul>
 *
 * <h3>Predicate Lifecycle</h3>
 *
 * <ol>
 *   <li>Predicates are created via {@link gnu.prolog.database.Module#createDefinedPredicate}</li>
 *   <li>Type is set once via {@link gnu.prolog.database.Predicate#setType}</li>
 *   <li>For user-defined predicates, clauses are added via
 *       {@link gnu.prolog.database.Predicate#addClauseLast} or
 *       {@link gnu.prolog.database.Predicate#addClauseFirst}</li>
 *   <li>Dynamic predicates can have clauses added/removed at runtime</li>
 * </ol>
 *
 * <h3>Loading Prolog Files</h3>
 *
 * <p>Use {@link gnu.prolog.vm.Environment#ensureLoaded(gnu.prolog.term.Term)} to load
 * Prolog source files. The loading process:</p>
 *
 * <ol>
 *   <li>Parses the source file</li>
 *   <li>Creates/updates predicates in the module</li>
 *   <li>Queues initialization goals</li>
 *   <li>After loading, call {@link gnu.prolog.vm.Environment#runInitialization}
 *       to execute queued initialization goals</li>
 * </ol>
 *
 * @see gnu.prolog.database.Module
 * @see gnu.prolog.database.Predicate
 * @see gnu.prolog.database.PrologTextLoaderState
 */
package gnu.prolog.database;
