/* GNU Prolog for Java
 * Copyright (C) 1997-1999  Constantine Plotnikov
 * Copyright (C) 2009       Michiel Hendriks
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
package gnu.prolog.vm.builtins.dialogs;

import gnu.prolog.term.AtomTerm;
import gnu.prolog.term.CompoundTerm;
import gnu.prolog.term.CompoundTermTag;
import gnu.prolog.term.Term;
import gnu.prolog.term.VariableTerm;
import gnu.prolog.vm.ExecuteOnlyCode;
import gnu.prolog.vm.Interpreter;
import gnu.prolog.vm.PrologException;
import gnu.prolog.vm.TermConstants;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;

/**
 * Factory class for dialog predicates.
 * Provides implementations for GUI dialog operations including message boxes,
 * confirmation dialogs, input prompts, and file choosers.
 */
public final class Predicates {

	private Predicates() {
		throw new AssertionError("Utility class should not be instantiated");
	}

	// ============================================================================
	// Constants
	// ============================================================================

	static final AtomTerm DIALOG_OPTION_ATOM = AtomTerm.get("dialog_option");

	// Buttons
	private static final AtomTerm OK_ATOM = AtomTerm.get("ok");
	private static final AtomTerm CANCEL_ATOM = AtomTerm.get("cancel");
	private static final AtomTerm YES_ATOM = AtomTerm.get("yes");
	private static final AtomTerm NO_ATOM = AtomTerm.get("no");
	private static final AtomTerm IGNORE_ATOM = AtomTerm.get("ignore");
	private static final AtomTerm ABORT_ATOM = AtomTerm.get("abort");
	private static final AtomTerm RETRY_ATOM = AtomTerm.get("retry");

	// Other options
	private static final CompoundTermTag TITLE_TAG = CompoundTermTag.get("title", 1);
	private static final CompoundTermTag MESSAGE_TAG = CompoundTermTag.get("message", 1);
	private static final CompoundTermTag SELECTION_TAG = CompoundTermTag.get("selection", 1);
	static final CompoundTermTag FILEMASK_TAG = CompoundTermTag.get("filemask", 1);
	private static final CompoundTermTag TYPE_TAG = CompoundTermTag.get("type", 1);

	// Message dialog types
	private static final AtomTerm ERROR_ATOM = AtomTerm.get("error");
	private static final AtomTerm WARNING_ATOM = AtomTerm.get("warning");
	private static final AtomTerm INFO_ATOM = AtomTerm.get("info");
	private static final AtomTerm QUESTION_ATOM = AtomTerm.get("question");

	// ============================================================================
	// Helper Methods
	// ============================================================================

	private static DialogOptions processOptions(final Term optionsList) throws PrologException {
		DialogOptions options = new DialogOptions();
		Term cur = optionsList;
		while (cur != TermConstants.emptyListAtom) {
			switch (cur) {
				case VariableTerm vt -> {
					PrologException.instantiationError(cur);
				}
				default -> {}
			}
			CompoundTerm ct = switch (cur) {
				case CompoundTerm c -> c;
				default -> {
					PrologException.typeError(TermConstants.listAtom, optionsList);
					yield null; // Never reached
				}
			};
			if (ct.tag != TermConstants.listTag) {
				PrologException.typeError(TermConstants.listAtom, optionsList);
			}
			Term head = ct.args[0].dereference();
			cur = ct.args[1].dereference();
			switch (head) {
				case VariableTerm vt -> {
					PrologException.instantiationError(head);
				}
				default -> {}
			}

			switch (head) {
				case AtomTerm at -> {
					if (OK_ATOM.equals(head)) {
						options.buttons |= DialogOptions.BUTTON_OK;
						continue;
					}
					if (CANCEL_ATOM.equals(head)) {
						options.buttons |= DialogOptions.BUTTON_CANCEL;
						continue;
					}
					if (YES_ATOM.equals(head)) {
						options.buttons |= DialogOptions.BUTTON_YES;
						continue;
					}
					if (NO_ATOM.equals(head)) {
						options.buttons |= DialogOptions.BUTTON_NO;
						continue;
					}
					if (IGNORE_ATOM.equals(head)) {
						options.buttons |= DialogOptions.BUTTON_IGNORE;
						continue;
					}
					if (ABORT_ATOM.equals(head)) {
						options.buttons |= DialogOptions.BUTTON_ABORT;
						continue;
					}
					if (RETRY_ATOM.equals(head)) {
						options.buttons |= DialogOptions.BUTTON_RETRY;
						continue;
					}
				}
				default -> {}
			}

			CompoundTerm op = switch (head) {
				case CompoundTerm c -> c;
				default -> {
					PrologException.domainError(DIALOG_OPTION_ATOM, head);
					yield null; // Never reached
				}
			};
			if (op.tag == TITLE_TAG) {
				Term val = op.args[0].dereference();
				AtomTerm atVal = switch (val) {
					case AtomTerm at -> at;
					default -> {
						PrologException.domainError(DIALOG_OPTION_ATOM, op);
						yield null; // Never reached
					}
				};
				options.title = atVal.value;
			} else if (op.tag == MESSAGE_TAG) {
				Term val = op.args[0].dereference();
				AtomTerm atVal = switch (val) {
					case AtomTerm at -> at;
					default -> {
						PrologException.domainError(DIALOG_OPTION_ATOM, op);
						yield null; // Never reached
					}
				};
				options.message = atVal.value;
			} else if (op.tag == SELECTION_TAG) {
				Term val = op.args[0].dereference();
				AtomTerm atVal = switch (val) {
					case AtomTerm at -> at;
					default -> {
						PrologException.domainError(DIALOG_OPTION_ATOM, op);
						yield null; // Never reached
					}
				};
				options.selection = atVal.value;
			} else if (op.tag == FILEMASK_TAG) {
				Term val = op.args[0].dereference();
				FileFilter filter = switch (val) {
					case AtomTerm at -> new TermFileFilter(val);
					case CompoundTerm ctf when ctf.tag.arity == 1 -> new TermFileFilter(val);
					default -> {
						PrologException.domainError(DIALOG_OPTION_ATOM, op);
						yield null; // Never reached
					}
				};
				if (options.fileFilters == null) {
					options.fileFilters = new ArrayList<>();
				}
				options.fileFilters.add(filter);
			} else if (op.tag == TYPE_TAG) {
				Term val = op.args[0].dereference();
				if (ERROR_ATOM.equals(val)) {
					options.messageType = JOptionPane.ERROR_MESSAGE;
				} else if (WARNING_ATOM.equals(val)) {
					options.messageType = JOptionPane.WARNING_MESSAGE;
				} else if (INFO_ATOM.equals(val)) {
					options.messageType = JOptionPane.INFORMATION_MESSAGE;
				} else if (QUESTION_ATOM.equals(val)) {
					options.messageType = JOptionPane.QUESTION_MESSAGE;
				} else {
					PrologException.domainError(DIALOG_OPTION_ATOM, op);
				}
			} else {
				PrologException.domainError(DIALOG_OPTION_ATOM, op);
			}
		}
		return options;
	}

	private static JFileChooser createFileDialog(final Term[] args) throws PrologException {
		DialogOptions options;
		if (args.length >= 2) {
			options = processOptions(args[1]);
		} else {
			options = new DialogOptions();
		}

		final var chooser = new JFileChooser();
		final var dir = System.getProperty("user.dir");
		if (dir != null) {
			chooser.setCurrentDirectory(new File(dir));
		}
		chooser.setMultiSelectionEnabled(false);
		if (options.title != null) {
			chooser.setDialogTitle(options.title);
		}
		if (options.fileFilters != null) {
			FileFilter selfilter = null;
			chooser.setAcceptAllFileFilterUsed(false);
			for (FileFilter filter : options.fileFilters) {
				chooser.addChoosableFileFilter(filter);
				if (selfilter == null) {
					selfilter = filter;
				}
			}
			chooser.addChoosableFileFilter(chooser.getAcceptAllFileFilter());
			if (selfilter != null) {
				chooser.setFileFilter(selfilter);
			}
		}
		if (options.selection != null) {
			chooser.setSelectedFile(new File(options.selection));
		}
		return chooser;
	}

	// ============================================================================
	// Predicate Implementations
	// ============================================================================

	/** message/1 - Display a message dialog */
	public static final ExecuteOnlyCode MESSAGE = (interpreter, backtrackMode, args) -> {
		DialogOptions options = processOptions(args[0]);
		if (options.title == null) {
			options.title = UIManager.getString("OptionPane.messageDialogTitle", null);
		}
		JOptionPane.showMessageDialog(null, options.message, options.title, options.messageType);
		return ExecuteOnlyCode.RC.SUCCESS_LAST;
	};

	// ============================================================================
	// Complex Predicate Implementations (with BacktrackInfo)
	// ============================================================================

	/** confirm/2 - Show a confirmation dialog with buttons */
	public static final ExecuteOnlyCode CONFIRM = new ExecuteOnlyCode() {
		@Override
		public RC execute(final Interpreter interpreter, final boolean backtrackMode, final Term[] args) throws PrologException {
			DialogOptions options;
			if (args.length >= 2) {
				options = processOptions(args[1]);
			} else {
				options = new DialogOptions();
			}
			if (options.title == null) {
				options.title = UIManager.getString("OptionPane.titleText");
			}
			if (options.buttons == 0) {
				options.buttons = DialogOptions.BUTTON_OK | DialogOptions.BUTTON_CANCEL;
			}

			Object initialValue = null;
			List<TermOption> opts = new ArrayList<>();
			if ((options.buttons & DialogOptions.BUTTON_OK) != 0) {
				opts.add(new TermOption(UIManager.getString("OptionPane.okButtonText", null), OK_ATOM));
			}
			if ((options.buttons & DialogOptions.BUTTON_YES) != 0) {
				opts.add(new TermOption(UIManager.getString("OptionPane.yesButtonText", null), YES_ATOM));
			}
			if ((options.buttons & DialogOptions.BUTTON_NO) != 0) {
				opts.add(new TermOption(UIManager.getString("OptionPane.noButtonText", null), NO_ATOM));
			}
			if ((options.buttons & DialogOptions.BUTTON_ABORT) != 0) {
				opts.add(new TermOption("Abort", ABORT_ATOM));
			}
			if ((options.buttons & DialogOptions.BUTTON_IGNORE) != 0) {
				opts.add(new TermOption("Ignore", IGNORE_ATOM));
			}
			if ((options.buttons & DialogOptions.BUTTON_RETRY) != 0) {
				opts.add(new TermOption("Retry", RETRY_ATOM));
			}
			if ((options.buttons & DialogOptions.BUTTON_CANCEL) != 0) {
				opts.add(new TermOption(UIManager.getString("OptionPane.cancelButtonText", null), CANCEL_ATOM));
			}

			if (options.selection != null) {
				for (TermOption opt : opts) {
					switch (opt.result) {
						case AtomTerm at -> {
							if (at.value.equals(options.selection)) {
								initialValue = opt;
							}
						}
						default -> {}
					}
					if (initialValue != null) {
						break;
					}
				}
			}

			Object result = JOptionPane.showOptionDialog(null, options.message, options.title, JOptionPane.DEFAULT_OPTION,
					options.messageType, null, opts.toArray(), initialValue);
			return switch (result) {
				case Integer i -> {
					if (i >= 0 && i <= opts.size()) {
						yield interpreter.unify(args[0], opts.get(i).result);
					}
					yield RC.FAIL;
				}
				default -> RC.FAIL;
			};
		}
	};

	/** file_open/2 - Show a file open dialog */
	public static final ExecuteOnlyCode FILE_OPEN = new ExecuteOnlyCode() {
		@Override
		public RC execute(final Interpreter interpreter, final boolean backtrackMode, final Term[] args) throws PrologException {
			JFileChooser choose = createFileDialog(args);
			if (choose.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
				Term term = AtomTerm.get(choose.getSelectedFile().toString());
				return interpreter.unify(args[0], term);
			}
			return RC.FAIL;
		}
	};

	/** file_save/2 - Show a file save dialog */
	public static final ExecuteOnlyCode FILE_SAVE = new ExecuteOnlyCode() {
		@Override
		public RC execute(final Interpreter interpreter, final boolean backtrackMode, final Term[] args) throws PrologException {
			JFileChooser choose = createFileDialog(args);
			while (choose.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
				if (choose.getSelectedFile().exists()) {
					if (JOptionPane.showConfirmDialog(null, String.format(
							"Are you sure you want to overwrite the existing file:\n%s ?", choose.getSelectedFile().toString()),
							"File Exists", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
						// ask for a new file
						continue;
					}
				}
				Term term = AtomTerm.get(choose.getSelectedFile().toString());
				return interpreter.unify(args[0], term);
			}
			return RC.FAIL;
		}
	};

	/** prompt/2 - Show an input prompt dialog */
	public static final ExecuteOnlyCode PROMPT = new ExecuteOnlyCode() {
		@Override
		public RC execute(final Interpreter interpreter, final boolean backtrackMode, final Term[] args) throws PrologException {
			DialogOptions options;
			if (args.length >= 2) {
				options = processOptions(args[1]);
			} else {
				options = new DialogOptions();
			}
			if (options.title == null) {
				options.title = UIManager.getString("OptionPane.inputDialogTitle", null);
			}
			Object result = JOptionPane.showInputDialog(null, options.message, options.title, options.messageType, null, null,
					options.selection);
			return switch (result) {
				case String s -> interpreter.unify(args[0], AtomTerm.get(s));
				default -> RC.FAIL;
			};
		}
	};

	// ============================================================================
	// Helper Classes
	// ============================================================================

	private static class DialogOptions {
		static final int BUTTON_OK = 1;
		static final int BUTTON_CANCEL = 2;
		static final int BUTTON_YES = 4;
		static final int BUTTON_NO = 8;
		static final int BUTTON_IGNORE = 16;
		static final int BUTTON_ABORT = 32;
		static final int BUTTON_RETRY = 64;

		String title;
		String message;
		String selection;
		List<FileFilter> fileFilters;
		int messageType = -1;
		int buttons;
	}

	private static class TermOption {
		final String text;
		final Term result;

		TermOption(final String res) {
			this(res, AtomTerm.get(res));
		}

		TermOption(final String res, final Term resTerm) {
			text = res;
			result = resTerm;
		}

		@Override
		public String toString() {
			return text;
		}
	}
}
