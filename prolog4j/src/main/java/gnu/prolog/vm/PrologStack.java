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

import java.util.Arrays;

/**
 * A generic stack implementation optimized for Prolog VM operations.
 * Provides efficient push/pop operations with automatic growth and
 * GC-friendly null-clearing on removal.
 *
 * @param <T> the type of elements in this stack
 */
public final class PrologStack<T> implements Cloneable
{
	private static final int PAGE = 4096;

	private Object[] data;
	private int size;

	public PrologStack()
	{
		this.data = new Object[PAGE];
		this.size = 0;
	}

	/**
	 * Push an element onto the stack.
	 *
	 * @param element the element to push
	 */
	public void push(final T element)
	{
		if (size == data.length)
		{
			data = Arrays.copyOf(data, data.length + PAGE);
		}
		data[size++] = element;
	}

	/**
	 * Pop and return the top element from the stack.
	 *
	 * @return the top element
	 * @throws IllegalStateException if the stack is empty
	 */
	@SuppressWarnings("unchecked")
	public T pop()
	{
		if (size == 0)
		{
			throw new IllegalStateException("Stack underflow");
		}
		final var result = (T) data[--size];
		data[size] = null; // GC-friendly
		return result;
	}

	/**
	 * Return the top element without removing it.
	 *
	 * @return the top element
	 */
	@SuppressWarnings("unchecked")
	public T peek()
	{
		return (T) data[size - 1];
	}

	/**
	 * Get the element at the specified index.
	 *
	 * @param index the index (0 = bottom of stack)
	 * @return the element at that index
	 */
	@SuppressWarnings("unchecked")
	public T get(final int index)
	{
		return (T) data[index];
	}

	/**
	 * Set the element at the specified index.
	 *
	 * @param index   the index (0 = bottom of stack)
	 * @param element the element to set
	 */
	public void set(final int index, final T element)
	{
		data[index] = element;
	}

	/**
	 * Return the current size of the stack.
	 *
	 * @return the number of elements
	 */
	public int size()
	{
		return size;
	}

	/**
	 * Check if the stack is empty.
	 *
	 * @return true if the stack contains no elements
	 */
	public boolean isEmpty()
	{
		return size == 0;
	}

	/**
	 * Truncate the stack to the specified position.
	 * All elements from position to current size are null-cleared for GC.
	 *
	 * @param position the new size of the stack
	 */
	public void truncate(final int position)
	{
		for (int i = position; i < size; i++)
		{
			data[i] = null;
		}
		size = position;
	}

	/**
	 * Find an element searching backwards from the top.
	 *
	 * @param element the element to find (by reference equality)
	 * @return the index of the element, or -1 if not found
	 */
	public int find(final T element)
	{
		for (int i = size - 1; i >= 0; i--)
		{
			if (element == data[i])
			{
				return i;
			}
		}
		return -1;
	}

	/**
	 * Clear all elements from the stack.
	 */
	public void clear()
	{
		truncate(0);
	}

	/**
	 * Restore this stack's state from another stack.
	 * Used for ReturnPoint state restoration.
	 *
	 * @param source the stack to copy state from
	 */
	public void restore(final PrologStack<T> source)
	{
		this.data = source.data.clone();
		this.size = source.size;
	}

	/**
	 * Create a deep copy of this stack.
	 *
	 * @return a new PrologStack with copied data
	 */
	@Override
	public PrologStack<T> clone()
	{
		final var copy = new PrologStack<T>();
		copy.data = this.data.clone();
		copy.size = this.size;
		return copy;
	}
}
