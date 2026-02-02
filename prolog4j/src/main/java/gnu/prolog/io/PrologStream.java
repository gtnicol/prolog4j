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
package gnu.prolog.io;

import gnu.prolog.term.AtomTerm;
import gnu.prolog.term.CompoundTerm;
import gnu.prolog.term.CompoundTermTag;
import gnu.prolog.term.JavaObjectTerm;
import gnu.prolog.term.Term;
import gnu.prolog.vm.Environment;
import gnu.prolog.vm.Interpreter;
import gnu.prolog.vm.PrologException;
import gnu.prolog.vm.TermConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Abstract class representing Prolog Streams
 * 
 */
public abstract class PrologStream
{
	private static final Logger logger = LoggerFactory.getLogger(PrologStream.class);

	/**
	 * Options for opening a PrologStream.
	 * Use the {@link Builder} for a fluent API to construct instances.
	 */
	public static class OpenOptions
	{
		public AtomTerm mode;
		public AtomTerm type = PrologStream.textAtom;
		public AtomTerm eofAction = PrologStream.eofCodeAtom;
		public AtomTerm reposition = TermConstants.falseAtom;
		public Set<AtomTerm> aliases = new HashSet<>();
		public AtomTerm filename;
		public Environment environment;

		public OpenOptions(final AtomTerm filename, final AtomTerm mode, final Environment environment)
		{
			this.mode = mode;
			this.filename = filename;
			this.environment = environment;
			if (environment == null)
			{
				throw new IllegalArgumentException("Environment cannot be null");
			}
		}

		/**
		 * Creates a new Builder for constructing OpenOptions.
		 *
		 * @param filename the filename for the stream
		 * @param mode the mode (read, write, append)
		 * @param environment the Prolog environment
		 * @return a new Builder instance
		 */
		public static Builder builder(final AtomTerm filename, final AtomTerm mode, final Environment environment)
		{
			return new Builder(filename, mode, environment);
		}

		/**
		 * Fluent builder for OpenOptions.
		 */
		public static class Builder
		{
			private final OpenOptions options;

			public Builder(final AtomTerm filename, final AtomTerm mode, final Environment environment)
			{
				this.options = new OpenOptions(filename, mode, environment);
			}

			public Builder type(final AtomTerm type)
			{
				options.type = type;
				return this;
			}

			public Builder eofAction(final AtomTerm action)
			{
				options.eofAction = action;
				return this;
			}

			public Builder reposition(final AtomTerm reposition)
			{
				options.reposition = reposition;
				return this;
			}

			public Builder alias(final AtomTerm alias)
			{
				options.aliases.add(alias);
				return this;
			}

			public Builder aliases(final Set<AtomTerm> aliases)
			{
				options.aliases.addAll(aliases);
				return this;
			}

			public OpenOptions build()
			{
				return options;
			}
		}
	}

	/**
	 * Enumeration of stream modes for opening Prolog streams.
	 */
	public enum StreamMode
	{
		READ(AtomTerm.get("read")),
		WRITE(AtomTerm.get("write")),
		APPEND(AtomTerm.get("append"));

		private final AtomTerm atom;

		StreamMode(final AtomTerm atom)
		{
			this.atom = atom;
		}

		public AtomTerm atom()
		{
			return atom;
		}

		/**
		 * Returns the StreamMode for a given AtomTerm, or null if not found.
		 */
		public static StreamMode from(final AtomTerm atom)
		{
			for (final var mode : values())
			{
				if (mode.atom.equals(atom))
				{
					return mode;
				}
			}
			return null;
		}
	}

	/**
	 * Enumeration of stream types.
	 */
	public enum StreamType
	{
		TEXT(AtomTerm.get("text")),
		BINARY(AtomTerm.get("binary"));

		private final AtomTerm atom;

		StreamType(final AtomTerm atom)
		{
			this.atom = atom;
		}

		public AtomTerm atom()
		{
			return atom;
		}

		public static StreamType from(final AtomTerm atom)
		{
			for (final var type : values())
			{
				if (type.atom.equals(atom))
				{
					return type;
				}
			}
			return null;
		}
	}

	/**
	 * Enumeration of end-of-stream states.
	 */
	public enum EndOfStreamState
	{
		NOT(AtomTerm.get("not")),
		AT(AtomTerm.get("at")),
		PAST(AtomTerm.get("past"));

		private final AtomTerm atom;

		EndOfStreamState(final AtomTerm atom)
		{
			this.atom = atom;
		}

		public AtomTerm atom()
		{
			return atom;
		}

		public static EndOfStreamState from(final AtomTerm atom)
		{
			for (final var state : values())
			{
				if (state.atom.equals(atom))
				{
					return state;
				}
			}
			return null;
		}
	}

	/**
	 * Enumeration of EOF actions.
	 */
	public enum EofAction
	{
		ERROR(AtomTerm.get("error")),
		EOF_CODE(AtomTerm.get("eof_code")),
		RESET(AtomTerm.get("reset"));

		private final AtomTerm atom;

		EofAction(final AtomTerm atom)
		{
			this.atom = atom;
		}

		public AtomTerm atom()
		{
			return atom;
		}

		public static EofAction from(final AtomTerm atom)
		{
			for (final var action : values())
			{
				if (action.atom.equals(atom))
				{
					return action;
				}
			}
			return null;
		}
	}

	// tags used to supply stream options
	public static final CompoundTermTag filenameTag = CompoundTermTag.get("file_name", 1);
	public static final CompoundTermTag modeTag = CompoundTermTag.get("mode", 1);
	public static final CompoundTermTag aliasTag = CompoundTermTag.get("alias", 1);
	public static final CompoundTermTag positionTag = CompoundTermTag.get("position", 1);
	public static final CompoundTermTag endOfStreamTag = CompoundTermTag.get("end_of_stream", 1);
	public static final CompoundTermTag eofActionTag = CompoundTermTag.get("eof_action", 1);
	public static final CompoundTermTag repositionTag = CompoundTermTag.get("reposition", 1);
	public static final CompoundTermTag typeTag = CompoundTermTag.get("type", 1);
	// atoms used for stream options
	public static final AtomTerm atAtom = AtomTerm.get("at");
	public static final AtomTerm pastAtom = AtomTerm.get("past");
	public static final AtomTerm notAtom = AtomTerm.get("not");
	public static final AtomTerm inputAtom = AtomTerm.get("input");
	public static final AtomTerm outputAtom = AtomTerm.get("output");
	public static final AtomTerm errorAtom = AtomTerm.get("error");
	public static final AtomTerm eofCodeAtom = AtomTerm.get("eof_code");
	public static final AtomTerm resetAtom = AtomTerm.get("reset");
	public static final AtomTerm repositionAtom = AtomTerm.get("reposition");
	public static final AtomTerm streamAtom = AtomTerm.get("stream");
	public static final AtomTerm readAtom = AtomTerm.get("read");
	public static final AtomTerm userInputAtom = AtomTerm.get("user_input");
	public static final AtomTerm userOutputAtom = AtomTerm.get("user_output");
	public static final AtomTerm textAtom = AtomTerm.get("text");
	public static final AtomTerm binaryAtom = AtomTerm.get("binary");
	public static final AtomTerm appendAtom = AtomTerm.get("append");
	public static final AtomTerm streamOrAliasAtom = AtomTerm.get("stream_or_alias");
	public static final AtomTerm openAtom = AtomTerm.get("open");
	public static final AtomTerm sourceSinkAtom = AtomTerm.get("source_sink");
	public static final AtomTerm writeAtom = AtomTerm.get("write");
	public static final AtomTerm endOfFileAtom = AtomTerm.get("end_of_file");

	protected AtomTerm filename;
	protected AtomTerm mode;
	protected AtomTerm reposition;
	protected AtomTerm eofAction;
	protected AtomTerm endOfStream = notAtom;
	protected AtomTerm type;
	protected Set<AtomTerm> aliases;
	protected Term streamTerm = new JavaObjectTerm(this);
	protected boolean closed = false;
	protected Environment environment;

	protected PrologStream(OpenOptions options)
	{
		filename = options.filename;
		mode = options.mode == readAtom ? inputAtom : outputAtom;
		reposition = options.reposition;
		eofAction = options.eofAction;
		type = options.type;
		aliases = new HashSet<AtomTerm>(options.aliases);
		environment = options.environment;
	}

	public void checkExists() throws PrologException
	{
		if (closed)
		{
			logger.debug("Stream closed: {}", filename.value);
			PrologException.existenceError(streamAtom, streamTerm);
		}
	}

	public Term getStreamTerm()
	{
		return streamTerm;
	}

	public AtomTerm getMode()
	{
		return mode;
	}

	public void getProperties(List<Term> list) throws PrologException
	{
		list.add(new CompoundTerm(filenameTag, filename));
		// list.add(new CompoundTerm(modeTag, mode));
		list.add(mode);
		for (AtomTerm atomTerm : aliases)
		{
			list.add(new CompoundTerm(aliasTag, atomTerm));
		}
		if (reposition == TermConstants.trueAtom)
		{
			list.add(new CompoundTerm(positionTag, getPosition(streamTerm, null)));
		}
		list.add(new CompoundTerm(endOfStreamTag, getEndOfStreamState()));
		list.add(new CompoundTerm(eofActionTag, eofAction));
		list.add(new CompoundTerm(repositionTag, reposition));
		list.add(new CompoundTerm(typeTag, type));
	}

	public abstract int getByte(Term streamTerm, Interpreter interpreter) throws PrologException;

	public abstract int peekByte(Term streamTerm, Interpreter interpreter) throws PrologException;

	public abstract void putByte(Term streamTerm, Interpreter interpreter, int _byte) throws PrologException;

	public Term getEndOfStreamState() throws PrologException
	{
		checkExists();
		return endOfStream;
	}

	public abstract Term getPosition(Term streamTerm, Interpreter interpreter) throws PrologException;

	public abstract void setPosition(Term streamTerm, Interpreter interpreter, Term pos) throws PrologException;

	public abstract int getCode(Term streamTerm, Interpreter interpreter) throws PrologException;

	public abstract int peekCode(Term streamTerm, Interpreter interpreter) throws PrologException;

	public abstract void putCode(Term streamTerm, Interpreter interpreter, int code) throws PrologException;

	public abstract void putCodeSequence(Term streamTerm, Interpreter interpreter, String seq) throws PrologException;

	public abstract Term readTerm(Term streamTerm, Interpreter interpreter, ReadOptions options) throws PrologException;

	public abstract void writeTerm(Term streamTerm, Interpreter interpreter, WriteOptions options, Term term)
			throws PrologException;

	public abstract void flushOutput(Term streamTerm) throws PrologException;

	public void close(boolean force) throws PrologException
	{
		if (environment.close(this))// if we managed to close
		{
			closed = true;
		}
	}

	public boolean isClosed()
	{
		return closed;
	}

	public int getCurrentLine()
	{
		return -1;
	}

	public int getCurrentColumn()
	{
		return -1;
	}

	public Set<AtomTerm> getAliases()
	{
		return aliases;
	}

	protected void debug(final Exception ex)
	{
		if (logger.isDebugEnabled())
		{
			logger.debug("{}:{}:{}: {}", filename.value, getCurrentLine(), getCurrentColumn(), ex.toString());
		}
	}
}
