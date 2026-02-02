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
 * Input/Output subsystem for GNU Prolog for Java.
 *
 * <h2>Overview</h2>
 *
 * <p>This package provides the I/O infrastructure for Prolog streams,
 * term reading/writing, and operator handling.</p>
 *
 * <h3>Stream Architecture</h3>
 *
 * <p>The stream system follows ISO Prolog semantics:</p>
 *
 * <ul>
 *   <li>{@link gnu.prolog.io.PrologStream} - Abstract base class for all Prolog streams.
 *       Provides common functionality for stream properties, positioning, and closing.</li>
 *
 *   <li>{@link gnu.prolog.io.TextInputPrologStream} - Text input stream for reading
 *       characters and terms.</li>
 *
 *   <li>{@link gnu.prolog.io.TextOutputPrologStream} - Text output stream for writing
 *       characters and terms.</li>
 *
 *   <li>{@link gnu.prolog.io.BinaryPrologStream} - Binary stream for byte-level I/O.</li>
 * </ul>
 *
 * <h3>Term Reading and Writing</h3>
 *
 * <ul>
 *   <li>{@link gnu.prolog.io.TermReader} - Parses Prolog terms from text input.
 *       Handles operators, character conversion, and syntax.</li>
 *
 *   <li>{@link gnu.prolog.io.TermWriter} - Formats Prolog terms for text output.
 *       Supports various write options (quoted, ignore_ops, etc.).</li>
 *
 *   <li>{@link gnu.prolog.io.ReadOptions} - Options for term reading (variables_names, etc.)</li>
 *
 *   <li>{@link gnu.prolog.io.WriteOptions} - Options for term writing (quoted, portrayed, etc.)</li>
 * </ul>
 *
 * <h3>Operators</h3>
 *
 * <ul>
 *   <li>{@link gnu.prolog.io.OperatorSet} - Manages the set of defined operators
 *       with their precedence and associativity.</li>
 *
 *   <li>{@link gnu.prolog.io.Operator} - Represents a single operator definition.</li>
 * </ul>
 *
 * <h3>Stream Options</h3>
 *
 * <p>Streams are opened with {@link gnu.prolog.io.PrologStream.OpenOptions}:</p>
 *
 * <ul>
 *   <li><strong>mode</strong> - read, write, or append</li>
 *   <li><strong>type</strong> - text or binary</li>
 *   <li><strong>eof_action</strong> - error, eof_code, or reset</li>
 *   <li><strong>reposition</strong> - whether stream supports repositioning</li>
 *   <li><strong>alias</strong> - symbolic names for the stream</li>
 * </ul>
 *
 * <h3>Character Conversion</h3>
 *
 * <p>{@link gnu.prolog.io.CharConversionTable} handles character conversion
 * as specified by the char_conversion/2 predicate.</p>
 *
 * @see gnu.prolog.io.PrologStream
 * @see gnu.prolog.io.TermReader
 * @see gnu.prolog.io.TermWriter
 * @see gnu.prolog.io.OperatorSet
 */
package gnu.prolog.io;
