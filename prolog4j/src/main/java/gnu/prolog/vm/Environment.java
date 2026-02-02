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

import gnu.prolog.Version;
import gnu.prolog.database.Module;
import gnu.prolog.database.Pair;
import gnu.prolog.database.Predicate;
import gnu.prolog.database.PredicateListener;
import gnu.prolog.database.PredicateUpdatedEvent;
import gnu.prolog.database.PrologTextLoaderError;
import gnu.prolog.database.PrologTextLoaderState;
import gnu.prolog.io.BinaryPrologStream;
import gnu.prolog.io.CharConversionTable;
import gnu.prolog.io.OperatorSet;
import gnu.prolog.io.PrologStream;
import gnu.prolog.io.TextInputPrologStream;
import gnu.prolog.io.TextOutputPrologStream;
import gnu.prolog.term.AtomTerm;
import gnu.prolog.term.CompoundTerm;
import gnu.prolog.term.CompoundTermTag;
import gnu.prolog.term.IntegerTerm;
import gnu.prolog.term.JavaObjectTerm;
import gnu.prolog.term.Term;
import gnu.prolog.term.VariableTerm;
import gnu.prolog.vm.PrologCode.RC;
import gnu.prolog.vm.interpreter.InterpretedCodeCompiler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.io.Writer;
import java.lang.ref.Cleaner;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * this class represent prolog processor.
 *
 * Implements AutoCloseable to support try-with-resources for proper cleanup.
 * The close() method removes this environment as a listener from its module.
 */
public class Environment implements PredicateListener, AutoCloseable
{
	private static final Logger logger = LoggerFactory.getLogger(Environment.class);

	/** Shared cleaner for all Environment instances */
	private static final Cleaner CLEANER = Cleaner.create();

	/**
	 * Cleaner state for this environment.
	 * IMPORTANT: Must NOT hold a strong reference to the Environment being cleaned,
	 * otherwise the Cleaner will never trigger. Uses a weak reference instead.
	 */
	private static class CleanupState implements Runnable
	{
		private final Module module;
		private final java.lang.ref.WeakReference<PredicateListener> listener;

		CleanupState(final Module module, final PredicateListener env)
		{
			this.module = module;
			this.listener = new java.lang.ref.WeakReference<>(env);
		}

		@Override
		public void run()
		{
			// Called when environment is phantom-reachable
			PredicateListener env = listener.get();
			if (module != null && env != null)
			{
				module.removePredicateListener(env);
			}
		}
	}

	/** Cleanable registration for this environment */
	private final Cleaner.Cleanable cleanable;

	protected OperatorSet opSet = new OperatorSet();
	/** current state of loaded database */
	protected PrologTextLoaderState prologTextLoaderState;
	/** predicate which used instead of real code when predicate is not defined */
	protected PrologCode undefinedPredicate;
	/** PredicateTag to code mapping - uses ConcurrentHashMap for lock-free reads */
	protected final Map<CompoundTermTag, PrologCode> tag2code = new ConcurrentHashMap<>();

	/** ReadWriteLock for flag operations - allows concurrent reads */
	private final ReadWriteLock flagLock = new ReentrantReadWriteLock();
	/** ReadWriteLock for stream operations - allows concurrent reads */
	private final ReadWriteLock streamLock = new ReentrantReadWriteLock();
	/** ReadWriteLock for code loading operations */
	private final ReadWriteLock codeLock = new ReentrantReadWriteLock();

	// TODO move into TermConstants, possibly consider using enums.
	// flag atoms
	public final static AtomTerm boundedAtom = AtomTerm.get("bounded");
	public final static AtomTerm integerRoundingFunctionAtom = AtomTerm.get("integer_rounding_function");
	public final static AtomTerm downAtom = AtomTerm.get("down");
	public final static AtomTerm towardZeroAtom = AtomTerm.get("toward_zero");
	public final static AtomTerm charConversionAtom = AtomTerm.get("char_conversion");
	public final static AtomTerm onAtom = AtomTerm.get("on");
	public final static AtomTerm offAtom = AtomTerm.get("off");
	public final static AtomTerm debugAtom = AtomTerm.get("debug");
	public final static AtomTerm unknownAtom = AtomTerm.get("unknown");
	public final static AtomTerm errorAtom = AtomTerm.get("error");
	public final static AtomTerm warningAtom = AtomTerm.get("warning");
	public final static AtomTerm doubleQuotesAtom = AtomTerm.get("double_quotes");

	public final static AtomTerm dialectAtom = AtomTerm.get("dialect");
	public final static AtomTerm versionAtom = AtomTerm.get("version");
	// integer terms
	public final static IntegerTerm maxIntegerTerm = IntegerTerm.get(Integer.MAX_VALUE);
	public final static IntegerTerm minIntegerTerm = IntegerTerm.get(Integer.MIN_VALUE);
	// The identifier string for this prolog engine
	public final static AtomTerm dialectTerm = AtomTerm.get("gnuprologjava");
	// the version
	public final static IntegerTerm versionTerm = IntegerTerm.get(Version.intEncoded());

	public final static AtomTerm prologFlagAtom = AtomTerm.get("prolog_flag");
	public final static AtomTerm flagValueAtom = AtomTerm.get("flag_value");
	public final static AtomTerm modifyAtom = AtomTerm.get("modify");
	public final static CompoundTermTag plusTag = CompoundTermTag.get("+", 2);
	/** atom to flag */
	protected Map<AtomTerm, Term> atom2flag = new HashMap<>();
	protected Set<AtomTerm> changableFlags = new HashSet<>();

	/** constructor of environment, it loads builtins to database at start. */
	public Environment()
	{
		this(null, null);
	}

	public Environment(InputStream stdin, OutputStream stdout)
	{
		createTextLoader();
		initEnvironment();
		initStreams(stdin, stdout);
		// Register cleanup action using Cleaner
		this.cleanable = CLEANER.register(this, new CleanupState(getModule(), this));
	}

	/**
	 * Initialize the environment
	 */
	protected void initEnvironment()
	{
		// load builtins
		CompoundTerm term = new CompoundTerm(AtomTerm.get("resource"), new Term[] { AtomTerm
				.get("/gnu/prolog/vm/builtins/builtins.pro") });
		ensureLoaded(term);
		// set flags for environment
		createNewPrologFlag(boundedAtom, TermConstants.trueAtom, false);
		createNewPrologFlag(TermConstants.maxIntegerAtom, maxIntegerTerm, false);
		createNewPrologFlag(TermConstants.minIntegerAtom, minIntegerTerm, false);
		createNewPrologFlag(TermConstants.maxCharacterCodeAtom, IntegerTerm.get(Character.MAX_VALUE), false);
		createNewPrologFlag(integerRoundingFunctionAtom, downAtom, false);
		createNewPrologFlag(charConversionAtom, offAtom, true);
		createNewPrologFlag(debugAtom, offAtom, true);
		// we can't have a Term with an arity higher than the available memory
		long maxMemory = Runtime.getRuntime().totalMemory() / 64L;
		IntegerTerm maxArity = (maxMemory < maxIntegerTerm.value) ? IntegerTerm.get((int) maxMemory) : maxIntegerTerm;
		createNewPrologFlag(TermConstants.maxArityAtom, maxArity, false);
		createNewPrologFlag(unknownAtom, errorAtom, true);
		createNewPrologFlag(doubleQuotesAtom, DoubleQuotesValue.getDefault().getAtom(), true);
		createNewPrologFlag(dialectAtom, dialectTerm, false);
		createNewPrologFlag(versionAtom, versionTerm, false);

		EnvInitializer.runInitializers(this);
	}

	protected void createTextLoader()
	{
		prologTextLoaderState = new PrologTextLoaderState(this);
	}

	/**
	 * Returns the PrologTextLoaderState for this Environment.
	 *
	 * @return the PrologTextLoader for this Environment
	 * @deprecated Use {@link #getPrologTextLoaderState()} instead. This method
	 *             will be removed in a future version.
	 */
	@Deprecated(since = "0.3.0", forRemoval = true)
	public PrologTextLoaderState getTextLoaderState()
	{
		return prologTextLoaderState;
	}

	/**
	 * true if the environment is currently initialized
	 * 
	 * @return if the environment is currently initialized
	 */
	public boolean isInitialized()
	{
		return getModule().getInitialization().size() == 0;
	}

	/**
	 * Run the initialization.
	 * 
	 * This executes any goals loaded into the initailization list by the
	 * :-initialization(Goal). directive or by the use of the NONISO abbreviation
	 * :- Goal.
	 * 
	 * This should be run after {@link #ensureLoaded(Term)} with the
	 * {@link Interpreter} obtained from {@link #createInterpreter()}.
	 * 
	 * @param interpreter
	 */
	public void runInitialization(Interpreter interpreter)
	{
		Module module = getModule();
		module.addPredicateListener(this);
		List<Pair<PrologTextLoaderError, Term>> initialization;
		synchronized (module)
		{// get the initialization list and then clear it so that it is no longer
			// referenced from module so that it will not be modified while we are
			// processing it
			initialization = module.getInitialization();
			module.clearInitialization();
		}
		for (Pair<PrologTextLoaderError, Term> loaderTerm : initialization)
		{
			Term term = loaderTerm.right();
			try
			{
				Interpreter.Goal goal = interpreter.prepareGoal(term);
				RC rc = interpreter.execute(goal);
				if (rc == PrologCode.RC.SUCCESS)
				{
					interpreter.stop(goal);
				}
				else if (rc != PrologCode.RC.SUCCESS_LAST)
				{
					prologTextLoaderState.logError(loaderTerm.left(), "Goal Failed: " + term);
				}
			}
			catch (PrologException ex)
			{
				prologTextLoaderState.logError(loaderTerm.left(), ex.getMessage());
			}
		}
	}

	/**
	 * Closes this environment and cleans up resources.
	 * Removes this environment as a listener from its module.
	 * This method is idempotent and can be called multiple times safely.
	 */
	@Override
	public void close()
	{
		getModule().removePredicateListener(this);
		// Close all open streams to prevent resource leaks
		closeStreams();
		// Clear caches to help with garbage collection
		tag2code.clear();
		flagLock.writeLock().lock();
		try
		{
			atom2flag.clear();
		}
		finally
		{
			flagLock.writeLock().unlock();
		}
		synchronized (tag2listeners)
		{
			tag2listeners.clear();
		}
		streamLock.writeLock().lock();
		try
		{
			alias2stream.clear();
		}
		finally
		{
			streamLock.writeLock().unlock();
		}
		// Clean up the cleaner registration
		cleanable.clean();
	}

	/**
	 * get copy of current state of flags for this environment
	 *
	 * @return copy of current state of flags for this environment
	 */
	public Map<AtomTerm, Term> getPrologFlags()
	{
		flagLock.readLock().lock();
		try
		{
			return new HashMap<>(atom2flag);
		}
		finally
		{
			flagLock.readLock().unlock();
		}
	}

	/**
	 * get flag for this environment
	 *
	 * @param flag
	 *          the flag to get the value of
	 * @return the value of the flag
	 */
	public Term getPrologFlag(final AtomTerm flag)
	{
		flagLock.readLock().lock();
		try
		{
			return atom2flag.get(flag);
		}
		finally
		{
			flagLock.readLock().unlock();
		}
	}

	/**
	 * create a new flag for this environment
	 *
	 * @param flag
	 *          the flag to add
	 * @param value
	 *          the value of the flag
	 * @param changeable
	 *          whether the flag's value can be changed
	 */
	protected void createNewPrologFlag(final AtomTerm flag, final Term value, final boolean changeable)
	{
		flagLock.writeLock().lock();
		try
		{
			atom2flag.put(flag, value);
			if (changeable)
			{
				changableFlags.add(flag);
			}
		}
		finally
		{
			flagLock.writeLock().unlock();
		}
	}

	public void setPrologFlag(final AtomTerm flag, final Term value) throws PrologException
	{
		flagLock.writeLock().lock();
		try
		{
			final var current = atom2flag.get(flag);
			if (current == null)
			{
				PrologException.domainError(prologFlagAtom, flag);
			}
			if (flag == boundedAtom)
			{
				if (value != TermConstants.trueAtom && value != TermConstants.falseAtom)
				{
					PrologException.domainError(flagValueAtom, new CompoundTerm(plusTag, flag, value));
				}
			}
			else if (flag == TermConstants.maxIntegerAtom)
			{
				if (!(value instanceof IntegerTerm))
				{
					PrologException.domainError(prologFlagAtom, new CompoundTerm(plusTag, flag, value));
				}
			}
			else if (flag == TermConstants.minIntegerAtom)
			{
				if (!(value instanceof IntegerTerm))
				{
					PrologException.domainError(prologFlagAtom, new CompoundTerm(plusTag, flag, value));
				}
			}
			else if (flag == integerRoundingFunctionAtom)
			{
				if (value != downAtom && value != towardZeroAtom)
				{
					PrologException.domainError(flagValueAtom, new CompoundTerm(plusTag, flag, value));
				}
			}
			else if (flag == charConversionAtom)
			{
				if (value != onAtom && value != offAtom)
				{
					PrologException.domainError(flagValueAtom, new CompoundTerm(plusTag, flag, value));
				}
			}
			else if (flag == debugAtom)
			{
				if (value != onAtom && value != offAtom)
				{
					PrologException.domainError(flagValueAtom, new CompoundTerm(plusTag, flag, value));
				}
			}
			else if (flag == TermConstants.maxArityAtom)
			{
				if (!(value instanceof IntegerTerm))
				{
					PrologException.domainError(prologFlagAtom, new CompoundTerm(plusTag, flag, value));
				}
			}
			else if (flag == unknownAtom)
			{
				if (value != errorAtom && value != TermConstants.failAtom && value != warningAtom)
				{
					PrologException.domainError(flagValueAtom, new CompoundTerm(plusTag, flag, value));
				}
			}
			else if (flag == doubleQuotesAtom)
			{
				if (!(value instanceof AtomTerm at) || ((DoubleQuotesValue.fromAtom(at)) == null))
				{
					PrologException.domainError(flagValueAtom, new CompoundTerm(plusTag, flag, value));
				}
			}
			if (!changableFlags.contains(flag))
			{
				PrologException.permissionError(modifyAtom, TermConstants.flagAtom, flag);
			}
			atom2flag.put(flag, value);
		}
		finally
		{
			flagLock.writeLock().unlock();
		}
	}

	public List<PrologTextLoaderError> getLoadingErrors()
	{
		return prologTextLoaderState.getErrors();
	}

	public PrologTextLoaderState getPrologTextLoaderState()
	{
		return prologTextLoaderState;
	}

	/**
	 * Ensure that prolog text designated by term is loaded
	 *
	 * You must use {@link #runInitialization(Interpreter)} after using this and
	 * before expecting answers.
	 *
	 * @param term
	 *
	 * @see gnu.prolog.vm.builtins.io.Predicates
	 * */
	public void ensureLoaded(final Term term)
	{
		codeLock.writeLock().lock();
		try
		{
			prologTextLoaderState.ensureLoaded(term);
		}
		finally
		{
			codeLock.writeLock().unlock();
		}
	}

	/**
	 * create interpreter for this environment
	 * 
	 * Use this to create different {@link Interpreter Interpreters} for different
	 * threads.
	 * 
	 * @return an interpreter for this environment.
	 */
	public Interpreter createInterpreter()
	{
		return new Interpreter(this);
	}

	public Module getModule()
	{
		return prologTextLoaderState.getModule();
	}

	/**
	 * load code for prolog
	 *
	 * @param tag
	 *          the tag of the {@link PrologCode} to load
	 * @return the loaded PrologCode
	 * @throws PrologException
	 */
	public PrologCode loadPrologCode(final CompoundTermTag tag) throws PrologException
	{
		codeLock.readLock().lock();
		try
		{
			// simple variant, later I will need to add compilation.
			final var p = getModule().getDefinedPredicate(tag);
			if (p == null) // case of undefined predicate
			{
				return getUndefinedPredicateCode(tag);
			}
			switch (p.getType())
			{
				case CONTROL:
					// really only call should be loaded in this way
				case BUILD_IN:
				case EXTERNAL:
				{
					try
					{
						final var javaClassName = p.getJavaClassName();
						PrologCode code;

						// Check if using new pattern: ClassName#FIELD_NAME
						if (javaClassName.contains("#"))
						{
							final var parts = javaClassName.split("#", 2);
							final var cls = Class.forName(parts[0]);
							final var field = cls.getField(parts[1]);
							code = (PrologCode) field.get(null); // null for static field
						}
						else
						{
							// Old pattern: ClassName (instantiate class)
							final var cls = Class.forName(javaClassName);
							code = (PrologCode) cls.getDeclaredConstructor().newInstance();
						}

						code.install(this);
						return code;
					}
					catch (/* ClassNotFound */Exception ex)
					// Maybe it will be needed to separate different cases later
					{
						logger.error("Failed to load predicate code for {}", tag, ex);
						return getUndefinedPredicateCode(tag);
					}
				}
				case USER_DEFINED:
				{
					final var code = InterpretedCodeCompiler.compile(p.getClauses());
					code.install(this);
					return code;
				}
				default:
					return getUndefinedPredicateCode(tag);
			}
		}
		finally
		{
			codeLock.readLock().unlock();
		}
	}

	/**
	 * get undefined predicate code
	 * 
	 * @param tag
	 * @return undefined predicate code for the tag
	 */
	public PrologCode getUndefinedPredicateCode(CompoundTermTag tag)
	{
		return new UndefinedPredicateCode(tag);
	}

	/**
	 * get prolog code
	 *
	 * @param tag
	 * @return the {@link PrologCode} for the tag
	 * @throws PrologException
	 */
	public PrologCode getPrologCode(final CompoundTermTag tag) throws PrologException
	{
		// ConcurrentHashMap allows lock-free reads
		var code = tag2code.get(tag);
		if (code == null)
		{
			// Use computeIfAbsent for atomic load-and-cache
			code = tag2code.computeIfAbsent(tag, t -> {
				try
				{
					return loadPrologCode(t);
				}
				catch (PrologException e)
				{
					throw new RuntimeException(e);
				}
			});
		}
		return code;
	}

	protected final Map<CompoundTermTag, List<PrologCodeListenerRef>> tag2listeners = new HashMap<CompoundTermTag, List<PrologCodeListenerRef>>();
	protected final ReferenceQueue<? super PrologCodeListener> prologCodeListenerReferenceQueue = new ReferenceQueue<PrologCodeListener>();

	/**
	 * A {@link WeakReference} to a {@link PrologCodeListener}
	 * 
	 */
	private static class PrologCodeListenerRef extends WeakReference<PrologCodeListener>
	{
		PrologCodeListenerRef(ReferenceQueue<? super PrologCodeListener> queue, PrologCodeListener listener,
				CompoundTermTag tag)
		{
			super(listener, queue);
			this.tag = tag;
		}

		CompoundTermTag tag;
	}

	protected void pollPrologCodeListeners()
	{
		PrologCodeListenerRef ref;
		synchronized (tag2listeners)
		{
			while (null != (ref = (PrologCodeListenerRef) prologCodeListenerReferenceQueue.poll()))
			{
				List<PrologCodeListenerRef> list = tag2listeners.get(ref.tag);
				list.remove(ref);
			}
		}
	}

	// this functionality will be needed later, but I need to think more ;-)
	/**
	 * add prolog code listener
	 * 
	 * @param tag
	 * @param listener
	 */
	public void addPrologCodeListener(CompoundTermTag tag, PrologCodeListener listener)
	{
		synchronized (tag2listeners)
		{
			pollPrologCodeListeners();
			List<PrologCodeListenerRef> list = tag2listeners.get(tag);
			if (list == null)
			{
				list = new ArrayList<>();
				tag2listeners.put(tag, list);
			}
			list.add(new PrologCodeListenerRef(prologCodeListenerReferenceQueue, listener, tag));
		}
	}

	/**
	 * remove prolog code listener
	 * 
	 * @param tag
	 * @param listener
	 */
	public void removePrologCodeListener(CompoundTermTag tag, PrologCodeListener listener)
	{
		synchronized (tag2listeners)
		{
			pollPrologCodeListeners();
			List<PrologCodeListenerRef> list = tag2listeners.get(tag);
			if (list != null)
			{
				ListIterator<PrologCodeListenerRef> i = list.listIterator();
				while (i.hasNext())
				{
					PrologCodeListenerRef ref = i.next();
					PrologCodeListener lst = ref.get();
					if (lst == null)
					{
						i.remove();
					}
					else if (lst == listener)
					{
						i.remove();
						return;
					}
				}
			}
		}
	}

	public void predicateUpdated(PredicateUpdatedEvent evt)
	{
		PrologCode code = tag2code.remove(evt.getTag());
		pollPrologCodeListeners();
		if (code == null) // if code was not loaded yet
		{
			return;
		}
		CompoundTermTag tag = evt.getTag();
		synchronized (tag2listeners)
		{
			List<PrologCodeListenerRef> list = tag2listeners.get(tag);
			if (list != null)
			{
				PrologCodeUpdatedEvent uevt = new PrologCodeUpdatedEvent(this, tag);
				ListIterator<PrologCodeListenerRef> i = list.listIterator();
				while (i.hasNext())
				{
					PrologCodeListenerRef ref = i.next();
					PrologCodeListener lst = ref.get();
					if (lst == null)
					{
						i.remove();
					}
					else
					{
						lst.prologCodeUpdated(uevt);
					}
				}
			}
		}
	}

	private static InputStream defaultInputStream;
	private static OutputStream defaultOutputStream;

	/**
	 * @return the defaultInputStream
	 */
	public static InputStream getDefaultInputStream()
	{
		return defaultInputStream == null ? System.in : defaultInputStream;
	}

	/**
	 * @param defaultInputStream
	 *          the defaultInputStream to set
	 */
	public static void setDefaultInputStream(InputStream defaultInputStream)
	{
		Environment.defaultInputStream = defaultInputStream;
	}

	/**
	 * @return the defaultOutputStream
	 */
	public static OutputStream getDefaultOutputStream()
	{
		return defaultOutputStream == null ? System.out : defaultOutputStream;
	}

	/**
	 * @param defaultOutputStream
	 *          the defaultOutputStream to set
	 */
	public static void setDefaultOutputStream(OutputStream defaultOutputStream)
	{
		Environment.defaultOutputStream = defaultOutputStream;
	}

	// IO support
	protected PrologStream userInput;
	protected PrologStream userOutput;
	protected PrologStream currentInput;
	protected PrologStream currentOutput;

	protected List<PrologStream> openStreams = new ArrayList<>();
	protected Map<AtomTerm, PrologStream> alias2stream = new HashMap<>();

	public OperatorSet getOperatorSet()
	{
		return opSet;
	}

	protected void initStreams(InputStream stdin, OutputStream stdout)
	{
		try
		{
			PrologStream.OpenOptions inops = new PrologStream.OpenOptions(PrologStream.userInputAtom, PrologStream.readAtom,
					this);
			PrologStream.OpenOptions outops = new PrologStream.OpenOptions(PrologStream.userOutputAtom,
					PrologStream.appendAtom, this);
			inops.aliases.add(PrologStream.userInputAtom);
			inops.eofAction = PrologStream.resetAtom;
			inops.reposition = TermConstants.falseAtom;
			inops.type = PrologStream.textAtom;
			outops.aliases.add(PrologStream.userOutputAtom);
			outops.eofAction = PrologStream.resetAtom;
			outops.reposition = TermConstants.falseAtom;
			outops.type = PrologStream.textAtom;
			userInput = new TextInputPrologStream(inops,
					new InputStreamReader(stdin == null ? getDefaultInputStream() : stdin, StandardCharsets.UTF_8));
			userOutput = new TextOutputPrologStream(outops,
					new OutputStreamWriter(stdout == null ? getDefaultOutputStream() : stdout, StandardCharsets.UTF_8));
			setCurrentInput(getUserInput());
			setCurrentOutput(getUserOutput());
			alias2stream.put(PrologStream.userOutputAtom, userOutput);
			alias2stream.put(PrologStream.userInputAtom, userInput);
			openStreams.add(userInput);
			openStreams.add(userOutput);
		}
		catch (Exception ex)
		{
			logger.error("Unable to initialize standard streams", ex);
			throw new IllegalStateException("unable to initialize standard streams");
		}
	}

	public PrologStream getUserInput() throws PrologException
	{
		streamLock.readLock().lock();
		try
		{
			return userInput;
		}
		finally
		{
			streamLock.readLock().unlock();
		}
	}

	public PrologStream getUserOutput() throws PrologException
	{
		streamLock.readLock().lock();
		try
		{
			return userOutput;
		}
		finally
		{
			streamLock.readLock().unlock();
		}
	}

	public PrologStream getCurrentInput() throws PrologException
	{
		streamLock.readLock().lock();
		try
		{
			return currentInput;
		}
		finally
		{
			streamLock.readLock().unlock();
		}
	}

	public PrologStream getCurrentOutput() throws PrologException
	{
		streamLock.readLock().lock();
		try
		{
			return currentOutput;
		}
		finally
		{
			streamLock.readLock().unlock();
		}
	}

	public void setCurrentInput(final PrologStream stream) throws PrologException
	{
		streamLock.writeLock().lock();
		try
		{
			currentInput = stream;
		}
		finally
		{
			streamLock.writeLock().unlock();
		}
	}

	public void setCurrentOutput(final PrologStream stream) throws PrologException
	{
		streamLock.writeLock().lock();
		try
		{
			currentOutput = stream;
		}
		finally
		{
			streamLock.writeLock().unlock();
		}
	}

	public Map<PrologStream, List<Term>> getStreamProperties() throws PrologException
	{
		streamLock.readLock().lock();
		try
		{
			final var map = new HashMap<PrologStream, List<Term>>();
			for (final var stream : openStreams)
			{
				final var list = new ArrayList<Term>();
				stream.getProperties(list);
				map.put(stream, list);
			}
			return map;
		}
		finally
		{
			streamLock.readLock().unlock();
		}
	}

	public PrologStream resolveStream(final Term alias) throws PrologException
	{
		streamLock.readLock().lock();
		try
		{
			final var resolved = alias.dereference();
			if (resolved instanceof VariableTerm)
			{
				PrologException.instantiationError(resolved);
			}
			else if (resolved instanceof AtomTerm)
			{
				final var stream = alias2stream.get(resolved);
				if (stream == null)
				{
					PrologException.existenceError(PrologStream.streamAtom, resolved);
				}
				else
				{
					stream.checkExists();
				}
				return stream;
			}
			else if (resolved instanceof JavaObjectTerm jt)
			{
				if (!(jt.value instanceof PrologStream ps))
				{
					PrologException.domainError(PrologStream.streamOrAliasAtom, resolved);
					return null; // unreachable but needed for scope
				}
				if (ps.isClosed())
				{
					PrologException.existenceError(PrologStream.streamAtom, resolved);
				}
				return ps;
			}
			else
			{
				PrologException.domainError(PrologStream.streamOrAliasAtom, resolved);
			}
			return null;
		}
		finally
		{
			streamLock.readLock().unlock();
		}
	}

	public Term open(final AtomTerm source, final AtomTerm mode, final PrologStream.OpenOptions options)
			throws PrologException
	{
		streamLock.writeLock().lock();
		try
		{
			for (final var alias : options.aliases)
			{
				if (alias2stream.get(alias) != null)
				{
					PrologException.permissionError(PrologStream.openAtom, PrologStream.sourceSinkAtom,
							new CompoundTerm(PrologStream.aliasTag, alias));
				}
			}
			PrologStream stream = null;
			if (options.type == PrologStream.binaryAtom)
			{
				stream = new BinaryPrologStream(source, mode, options);
			}
			else if (options.type == PrologStream.textAtom)
			{
				final var random = options.reposition == TermConstants.trueAtom;
				if (options.mode == PrologStream.readAtom)
				{
					if (!new File(source.value).exists())
					{
						PrologException.existenceError(PrologStream.sourceSinkAtom, source);
					}
					try
					{
						if (random)
						{
							final var raf = new RandomAccessFile(source.value, "r");
							try
							{
								stream = new TextInputPrologStream(options, raf);
							}
							catch (Exception e)
							{
								raf.close();
								throw e;
							}
						}
						else
						{
							final var reader = new FileReader(source.value, StandardCharsets.UTF_8);
							try
							{
								stream = new TextInputPrologStream(options, reader);
							}
							catch (Exception e)
							{
								reader.close();
								throw e;
							}
						}
					}
					catch (IOException ex)
					{
						PrologException.permissionError(PrologStream.openAtom, PrologStream.sourceSinkAtom, source);
					}
				}
				else
				{
					final var append = options.mode == PrologStream.appendAtom;
					try
					{
						if (random)
						{
							final var raf = new RandomAccessFile(source.value, "rw");
							try
							{
								if (append)
								{
									raf.seek(raf.length());
								}
								stream = new TextOutputPrologStream(options, raf);
							}
							catch (Exception e)
							{
								raf.close();
								throw e;
							}
						}
						else
						{
							final var writer = new FileWriter(source.value, append);
							try
							{
								stream = new TextOutputPrologStream(options, writer);
							}
							catch (Exception e)
							{
								writer.close();
								throw e;
							}
						}
					}
					catch (IOException ex)
					{
						PrologException.permissionError(PrologStream.openAtom, PrologStream.sourceSinkAtom, source);
					}
				}
			}
			else
			{
				PrologException.domainError(AtomTerm.get("invalid_stream_type"), options.type);
			}

			for (final var alias : options.aliases)
			{
				alias2stream.put(alias, stream);
			}
			openStreams.add(stream);
			return stream.getStreamTerm();
		}
		finally
		{
			streamLock.writeLock().unlock();
		}
	}

	/**
	 *
	 * @param stream
	 * @return true if we closed and false if we did not close.
	 * @throws PrologException
	 */
	public boolean close(final PrologStream stream) throws PrologException
	{
		streamLock.writeLock().lock();
		try
		{
			if (stream == userInput)
			{
				return false;
			}
			if (stream == userOutput)
			{
				userOutput.flushOutput(null);
				return false;
			}
			for (final var alias : stream.getAliases())
			{
				alias2stream.remove(alias);
			}
			openStreams.remove(stream);
			if (currentInput == stream)
			{
				currentInput = userInput;
			}
			if (currentOutput == stream)
			{
				currentOutput = userOutput;
			}
			return true;
		}
		finally
		{
			streamLock.writeLock().unlock();
		}
	}

	/**
	 * Closes all open streams
	 */
	public void closeStreams()
	{
		for (PrologStream stream : new ArrayList<PrologStream>(openStreams))
		{
			// Skip user input/output streams - they should remain open
			if (stream == userInput || stream == userOutput)
			{
				continue;
			}
			try
			{
				// Close the stream resources - this will also deregister via close(stream)
				stream.close(true); // force=true to ignore errors during shutdown
			}
			catch (PrologException e)
			{
				logger.error("Error closing stream during shutdown", e);
			}
		}
	}

	/**
	 * @return the convTable
	 */
	public CharConversionTable getConversionTable()
	{
		return prologTextLoaderState.getConversionTable();
	}

	/**
	 * Returns a BuiltinRegistry that provides information about registered builtin predicates.
	 *
	 * @return a BuiltinRegistry for querying builtin predicates
	 */
	public BuiltinRegistry getBuiltinRegistry()
	{
		return new BuiltinRegistry()
		{
			@Override
			public Set<CompoundTermTag> getBuiltins()
			{
				final var result = new HashSet<CompoundTermTag>();
				for (final var tag : getModule().getPredicateTags())
				{
					final var predicate = getModule().getDefinedPredicate(tag);
					if (predicate != null && predicate.getType().isBuiltIn())
					{
						result.add(tag);
					}
				}
				return Collections.unmodifiableSet(result);
			}

			@Override
			public boolean isBuiltin(final CompoundTermTag tag)
			{
				final var predicate = getModule().getDefinedPredicate(tag);
				return predicate != null && predicate.getType().isBuiltIn();
			}
		};
	}
}
