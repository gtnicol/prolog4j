/* GNU Prolog for Java
 * Copyright (C) 1997-1999  Constantine Plotnikov
 * Copyright (C) 2010       Daniel Thomas
 * Copyright (C) 2025       Gavin Nicol
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
package gnu.prolog.io.parser.gen;

import gnu.prolog.io.parser.NameToken;

/**
 * Describes the input token stream.
 * Custom Token class that creates NameToken for NAME_TOKEN tokens.
 */
public class Token implements java.io.Serializable {

  private static final long serialVersionUID = 1L;

  /**
   * An integer that describes the kind of this token.
   */
  public int kind;

  /** The line number of the first character of this Token. */
  public int beginLine;
  /** The column number of the first character of this Token. */
  public int beginColumn;
  /** The line number of the last character of this Token. */
  public int endLine;
  /** The column number of the last character of this Token. */
  public int endColumn;

  /**
   * The string image of the token.
   */
  public String image;

  /**
   * A reference to the next regular (non-special) token from the input
   * stream.
   */
  public Token next;

  /**
   * This field is used to access special tokens that occur prior to this
   * token.
   */
  public Token specialToken;

  /**
   * An optional attribute value of the Token.
   */
  public Object getValue() {
    return null;
  }

  /**
   * No-argument constructor
   */
  public Token() {}

  /**
   * Constructs a new token for the specified Kind.
   */
  public Token(final int kind) {
    this(kind, null);
  }

  /**
   * Constructs a new token for the specified Image and Kind.
   */
  public Token(final int kind, final String image) {
    this.kind = kind;
    this.image = image;
  }

  /**
   * Returns the image.
   */
  @Override
  public String toString() {
    return image;
  }

  /**
   * Returns a new Token object. Creates NameToken for NAME_TOKEN kinds.
   */
  public static Token newToken(final int kind, final String image) {
    switch (kind) {
      case TermParserConstants.NAME_TOKEN:
        final var token = new NameToken();
        token.kind = kind;
        token.image = image;
        return token;
      default:
        return new Token(kind, image);
    }
  }

  public static Token newToken(final int kind) {
    return newToken(kind, null);
  }
}
