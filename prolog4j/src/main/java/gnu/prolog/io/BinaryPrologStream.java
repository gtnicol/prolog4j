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
import gnu.prolog.term.JavaObjectTerm;
import gnu.prolog.term.Term;
import gnu.prolog.term.VariableTerm;
import gnu.prolog.vm.Interpreter;
import gnu.prolog.vm.PrologException;
import gnu.prolog.vm.TermConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * A PrologStream for binary data
 * 
 */
public class BinaryPrologStream extends PrologStream
{
	private static final Logger logger = LoggerFactory.getLogger(BinaryPrologStream.class);

	protected RandomAccessFile file;

	public BinaryPrologStream(AtomTerm fileAtom, AtomTerm mode, OpenOptions options) throws PrologException
	{
		super(options);
		String of = mode == readAtom ? "r" : "rw";
		try
		{
			file = new RandomAccessFile(fileAtom.value, of);
			if (mode == appendAtom)
			{
				file.seek(file.length());
			}
			else if (mode == writeAtom)
			{
				file.setLength(0); // truncate
			}
		}
		catch (Exception ex)
		{
			PrologException.systemError(ex);
		}
	}

	@Override
	public int getByte(Term streamTerm, Interpreter interpreter) throws PrologException
	{
		checkExists();
		if (mode == inputAtom)
		{
			getEndOfStreamState();
			if (endOfStream == pastAtom)
			{
				PrologException.permissionError(inputAtom, TermConstants.pastEndOfStreamAtom, streamTerm);
			}
			else if (endOfStream == atAtom)
			{
				endOfStream = pastAtom;
				return -1;
			}
			else
			{
				try
				{
					return file.read();
				}
				catch (IOException ex)
				{
					PrologException.systemError(ex);
				}
			}
		}
		else
		{
			PrologException.permissionError(inputAtom, streamAtom, streamTerm);
		}
		return -1; // fake return
	}

	@Override
	public int peekByte(Term streamTerm, Interpreter interpreter) throws PrologException
	{
		checkExists();
		if (mode == inputAtom)
		{
			getEndOfStreamState();
			if (endOfStream == pastAtom)
			{
				PrologException.permissionError(inputAtom, TermConstants.pastEndOfStreamAtom, streamTerm);
			}
			else if (endOfStream == atAtom)
			{
				endOfStream = pastAtom;
				return -1;
			}
			else
			{
				try
				{
					long pos = file.getFilePointer();
					int rc = file.read();
					file.seek(pos);
					return rc;
				}
				catch (IOException ex)
				{
					PrologException.systemError(ex);
				}
			}
		}
		else
		{
			PrologException.permissionError(inputAtom, streamAtom, streamTerm);
		}
		return -1; // fake
	}

	@Override
	public void putByte(Term streamTerm, Interpreter interpreter, int _byte) throws PrologException
	{
		checkExists();
		if (mode == outputAtom)
		{
			try
			{
				file.write(_byte);
			}
			catch (IOException ex)
			{
				PrologException.systemError(ex);
			}
		}
		else
		{
			PrologException.permissionError(outputAtom, streamAtom, streamTerm);
		}
	}

	@Override
	public Term getPosition(Term streamTerm, Interpreter interpreter) throws PrologException
	{
		try
		{
			return new JavaObjectTerm(Long.valueOf(file.getFilePointer()));
		}
		catch (IOException ex)
		{
			PrologException.systemError(ex);
		}
		return null; // fake
	}

	@Override
	public void setPosition(Term streamTerm, Interpreter interpreter, Term position) throws PrologException
	{
		try
		{
			if (reposition == TermConstants.falseAtom)
			{
				PrologException.permissionError(repositionAtom, streamAtom, getStreamTerm());
			}
			if (position instanceof VariableTerm)
			{
				PrologException.instantiationError(position);
			}
			else if (!(position instanceof JavaObjectTerm))
			{
				PrologException.domainError(TermConstants.streamPositionAtom, position);
			}
			JavaObjectTerm jt = (JavaObjectTerm) position;
			if (!(jt.value instanceof Long))
			{
				PrologException.domainError(TermConstants.streamPositionAtom, position);
			}
			Long longValue = (Long) jt.value;
			long pos = longValue.longValue();
			if (pos > file.length())
			{
				PrologException.domainError(TermConstants.streamPositionAtom, position);
			}
			file.seek(pos);
		}
		catch (IOException ex)
		{
			PrologException.systemError(ex);
		}
	}

	@Override
	public int getCode(Term streamTerm, Interpreter interpreter) throws PrologException
	{
		PrologException.permissionError(inputAtom, TermConstants.binaryStreamAtom, streamTerm);
		return -1;
	}

	@Override
	public int peekCode(Term streamTerm, Interpreter interpreter) throws PrologException
	{
		PrologException.permissionError(inputAtom, TermConstants.binaryStreamAtom, streamTerm);
		return -1;
	}

	@Override
	public void putCode(Term streamTerm, Interpreter interpreter, int code) throws PrologException
	{
		PrologException.permissionError(outputAtom, TermConstants.binaryStreamAtom, streamTerm);
	}

	@Override
	public void putCodeSequence(Term streamTerm, Interpreter interpreter, String seq) throws PrologException
	{
		PrologException.permissionError(outputAtom, TermConstants.binaryStreamAtom, streamTerm);
	}

	@Override
	public gnu.prolog.term.Term readTerm(Term streamTerm, gnu.prolog.vm.Interpreter i, gnu.prolog.io.ReadOptions o)
			throws PrologException
	{
		PrologException.permissionError(inputAtom, TermConstants.binaryStreamAtom, streamTerm);
		return null;
	}

	@Override
	public void writeTerm(Term streamTerm, gnu.prolog.vm.Interpreter i, gnu.prolog.io.WriteOptions o,
			gnu.prolog.term.Term t) throws PrologException
	{
		PrologException.permissionError(outputAtom, TermConstants.binaryStreamAtom, streamTerm);
	}

	@Override
	public void flushOutput(Term streamTerm) throws PrologException
	{
		if (mode == inputAtom)
		{
			checkExists();
			PrologException.permissionError(outputAtom, streamAtom, streamTerm);
		}
		else
		{
			// do nothing RAF cannot be flushed
		}
	}

	@Override
	public void close(final boolean force) throws PrologException
	{
		try
		{
			file.close();
		}
		catch (IOException ex)
		{
			if (!force)
			{
				PrologException.systemError(ex);
			}
			logger.warn("Error closing binary stream (forced)", ex);
		}
		super.close(force);
	}

	@Override
	public Term getEndOfStreamState() throws PrologException
	{
		try
		{
			long length = file.length();
			long pos = file.getFilePointer();
			if (pos < length)
			{
				endOfStream = notAtom;
			}
			else if (endOfStream == pastAtom)
			{
				if (eofAction == resetAtom || eofAction == eofCodeAtom)
				{
					endOfStream = atAtom;
				}
			}
			else
			{
				// At end of stream (pos >= length) but haven't read past it yet
				endOfStream = atAtom;
			}
		}
		catch (IOException ex)
		{
			PrologException.systemError(ex);
		}
		return super.getEndOfStreamState();
	}

}
