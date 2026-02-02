/* GNU Prolog for Java
 * Copyright (C) 1997-1999  Constantine Plotnikov
 * Copyright (C) 2009       Michiel Hendriks
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
package gnu.prolog.vm.builtins.datetime;

import gnu.prolog.io.PrologStream;
import gnu.prolog.term.AtomTerm;
import gnu.prolog.term.CompoundTerm;
import gnu.prolog.term.FloatTerm;
import gnu.prolog.term.IntegerTerm;
import gnu.prolog.term.JavaObjectTerm;
import gnu.prolog.term.Term;
import gnu.prolog.term.VariableTerm;
import gnu.prolog.vm.BacktrackInfo;
import gnu.prolog.vm.ExecuteOnlyCode;
import gnu.prolog.vm.Interpreter;
import gnu.prolog.vm.PrologException;
import gnu.prolog.vm.TermConstants;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

/**
 * Factory class for datetime predicates.
 * Provides implementations for date_time_stamp, date_time_value, format_time, get_time, parse_time, and stamp_date_time.
 */
public final class Predicates {

	private Predicates() {
		throw new AssertionError("Utility class should not be instantiated");
	}

	/** get_time/1 - Get current time as timestamp */
	public static final ExecuteOnlyCode GET_TIME = (interpreter, backtrackMode, args) -> {
		switch (args[0]) {
			case VariableTerm vt -> {} // Valid type
			default -> {
				PrologException.typeError(TermConstants.variableAtom, args[0]);
			}
		}
		Term res = new FloatTerm((new Date()).getTime() / 1000.0);
		return interpreter.unify(args[0], res);
	};

	/** date_time_stamp/2 - Convert DateTime to TimeStamp */
	public static final ExecuteOnlyCode DATE_TIME_STAMP = (interpreter, backtrackMode, args) -> {
		// date_time_stamp(+DateTime, -TimeStamp)
		Date date = AbstractDateTimePredicate.getDate(args[0]);
		switch (args[1]) {
			case VariableTerm vt -> {} // Valid type
			default -> {
				PrologException.typeError(TermConstants.variableAtom, args[1]);
			}
		}
		return interpreter.unify(args[1], new FloatTerm(date.getTime() / 1000.0));
	};

	/** parse_time/2,3 - Parse time string to timestamp */
	public static final ExecuteOnlyCode PARSE_TIME = (interpreter, backtrackMode, args) -> {
		// % parse_time(+Text, -Stamp)
		// % parse_time(+Text, -Stamp, +Format)
		String text = switch (args[0]) {
			case AtomTerm at -> at.value;
			default -> {
				PrologException.typeError(TermConstants.atomAtom, args[0]);
				yield null; // Never reached
			}
		};

		String format = "EEE, dd MMM yyyy HH:mm:ss zzz"; // RFC 1123
		if (args.length == 3) {
			format = switch (args[2]) {
				case AtomTerm at -> at.value;
				default -> {
					PrologException.typeError(TermConstants.atomAtom, args[2]);
					yield null; // Never reached
				}
			};
		}
		SimpleDateFormat fmt = new SimpleDateFormat(format);
		Date date;
		try {
			date = fmt.parse(text);
		} catch (ParseException e) {
			return ExecuteOnlyCode.RC.FAIL;
		}
		return interpreter.unify(args[1], new FloatTerm(date.getTime() / 1000.0));
	};

	/** stamp_date_time/3 - Convert TimeStamp to DateTime */
	public static final ExecuteOnlyCode STAMP_DATE_TIME = (interpreter, backtrackMode, args) -> {
		// stamp_date_time(+TimeStamp, -DateTime, +TimeZone)
		double ts = switch (args[0]) {
			case FloatTerm ft -> ft.value;
			default -> {
				PrologException.typeError(TermConstants.floatAtom, args[0]);
				yield 0.0; // Never reached
			}
		};
		switch (args[1]) {
			case VariableTerm vt -> {} // Valid type
			default -> {
				PrologException.typeError(TermConstants.variableAtom, args[1]);
			}
		}

		TimeZone tz = switch (args[2]) {
			case IntegerTerm it -> new SimpleTimeZone(it.value * 1000, "-");
			case AtomTerm at -> TimeZone.getTimeZone(at.value);
			default -> {
				PrologException.typeError(TermConstants.atomAtom, args[2]);
				yield null; // Never reached
			}
		};

		Calendar cal = Calendar.getInstance(tz);
		cal.setTimeInMillis(Math.round(ts * 1000));
		Term[] dateTime = new Term[9];
		dateTime[0] = IntegerTerm.get(cal.get(Calendar.YEAR));
		dateTime[1] = IntegerTerm.get(cal.get(Calendar.MONTH) + 1);
		dateTime[2] = IntegerTerm.get(cal.get(Calendar.DAY_OF_MONTH));
		dateTime[3] = IntegerTerm.get(cal.get(Calendar.HOUR_OF_DAY));
		dateTime[4] = IntegerTerm.get(cal.get(Calendar.MINUTE));
		dateTime[5] = new FloatTerm(cal.get(Calendar.SECOND) + (cal.get(Calendar.MILLISECOND) / 1000.0));
		dateTime[6] = IntegerTerm.get(cal.get(Calendar.ZONE_OFFSET) / 1000);
		if (tz != null) {
			dateTime[7] = AtomTerm.get(tz.getID());
			if (tz.useDaylightTime()) {
				if (tz.inDaylightTime(cal.getTime())) {
					dateTime[8] = TermConstants.trueAtom;
				} else {
					dateTime[8] = TermConstants.falseAtom;
				}
			} else {
				dateTime[8] = AtomTerm.get("-");
			}
		} else {
			dateTime[7] = AtomTerm.get("-");
			dateTime[8] = AtomTerm.get("-");
		}
		Term res = new CompoundTerm(AbstractDateTimePredicate.date9Tag, dateTime);
		return interpreter.unify(args[1], res);
	};

	/** format_time/3,4 - Format time to string */
	public static final ExecuteOnlyCode FORMAT_TIME = (interpreter, backtrackMode, args) -> {
		// format_time(+Out, +Format, +StampOrDateTime)
		// format_time(+Out, +Format, +StampOrDateTime, +Locale)

		// figure out the output
		OutputFormat outFormat = OutputFormat.OF_STREAM;
		PrologStream outstream = null;
		Term outterm = null;
		Term outtermTail = null;
		switch (args[0]) {
			case JavaObjectTerm jt -> {
				switch (jt.value) {
					case PrologStream ps -> {} // Valid
					default -> {
						PrologException.domainError(TermConstants.streamAtom, args[0]);
					}
				}
			}
			case AtomTerm at -> {
				outstream = interpreter.getEnvironment().resolveStream(args[0]);
				if (outstream == null) {
					PrologException.domainError(TermConstants.streamAtom, args[0]);
				}
			}
			case CompoundTerm ct -> {
				if (ct.tag.functor == TermConstants.atomAtom) {
					if (ct.tag.arity != 1) {
						PrologException.typeError(TermConstants.outputAtom, ct);
					}
					outterm = ct.args[0];
					outFormat = OutputFormat.OF_ATOM;
				} else if (ct.tag.functor == TermConstants.codesAtom) {
					if (ct.tag.arity > 2) {
						PrologException.typeError(TermConstants.outputAtom, ct);
					}
					outterm = ct.args[0];
					if (ct.args.length > 1) {
						outtermTail = ct.args[1];
					}
					outFormat = OutputFormat.OF_CODES;
				} else if (ct.tag.functor == TermConstants.charsAtom) {
					if (ct.tag.arity > 2) {
						PrologException.typeError(TermConstants.outputAtom, ct);
					}
					outterm = ct.args[0];
					if (ct.args.length > 1) {
						outtermTail = ct.args[1];
					}
					outFormat = OutputFormat.OF_CHARS;
				} else {
					PrologException.typeError(AtomTerm.get("atom_codes_chars"), args[0]);
				}
			}
			default -> {
				PrologException.typeError(TermConstants.outputAtom, args[0]);
			}
		}
		// either `outstream' or `outterm*' is set

		switch (args[1]) {
			case AtomTerm at -> {} // Valid
			default -> {
				PrologException.typeError(TermConstants.atomAtom, args[1]);
			}
		}
		String format = ((AtomTerm) args[1]).value;

		Date date = switch (args[2]) {
			case FloatTerm ft -> new Date(Math.round(ft.value * 1000));
			default -> AbstractDateTimePredicate.getDate(args[2]);
		};

		Locale locale = Locale.getDefault();
		if (args.length > 3) {
			switch (args[3]) {
				case AtomTerm at -> {
					String loc = at.value;
					// Replace underscore with hyphen for language tag format
					String languageTag = loc.replace('_', '-');
					locale = Locale.forLanguageTag(languageTag);
				}
				default -> {
					PrologException.typeError(TermConstants.atomAtom, args[3]);
				}
			}
		}

		SimpleDateFormat sdf = new SimpleDateFormat(format, locale);
		String result = sdf.format(date);
		if (outstream != null) {
			outstream.writeTerm(args[0], interpreter, null, AtomTerm.get(result));
			return ExecuteOnlyCode.RC.SUCCESS_LAST;
		} else {
			Term res = TermConstants.emptyListAtom;
			if (outtermTail != null) {
				res = outtermTail;
			}
			switch (outFormat) {
				case OF_ATOM:
					res = AtomTerm.get(result);
					break;
				case OF_CHARS:
					for (int i = result.length() - 1; i >= 0; i--) {
						res = CompoundTerm.getList(AtomTerm.get(result.charAt(i)), res);
					}
					break;
				case OF_CODES:
					for (int i = result.length() - 1; i >= 0; i--) {
						res = CompoundTerm.getList(IntegerTerm.get(result.charAt(i)), res);
					}
					break;
				default:
					PrologException.systemError();
			}

			return interpreter.unify(outterm, res);
		}
	};

	/** Helper enum for format_time output modes */
	private enum OutputFormat {
		OF_STREAM, OF_ATOM, OF_CHARS, OF_CODES
	}

	// ============================================================================
	// Complex Predicate Implementations (with BacktrackInfo)
	// ============================================================================

	/** date_time_value/3 - Extract value from DateTime structure (backtracking) */
	public static final ExecuteOnlyCode DATE_TIME_VALUE = new ExecuteOnlyCode() {
		// Atom constants for date time keys
		private final AtomTerm yearAtom = AtomTerm.get("year");
		private final AtomTerm monthAtom = AtomTerm.get("month");
		private final AtomTerm dayAtom = AtomTerm.get("day");
		private final AtomTerm hourAtom = AtomTerm.get("hour");
		private final AtomTerm minuteAtom = AtomTerm.get("minute");
		private final AtomTerm secondAtom = AtomTerm.get("second");
		private final AtomTerm utcOffsetAtom = AtomTerm.get("utc_offset");
		private final AtomTerm timeZoneAtom = AtomTerm.get("time_zone");
		private final AtomTerm daylightSavingAtom = AtomTerm.get("daylight_saving");
		private final AtomTerm dateAtom = AbstractDateTimePredicate.dateAtom;
		private final AtomTerm timeAtom = AbstractDateTimePredicate.timeAtom;

		// Array used for iterating through all possible keys
		private final AtomTerm[] date9keys = new AtomTerm[] { yearAtom, monthAtom, dayAtom, hourAtom, minuteAtom,
				secondAtom, utcOffsetAtom, timeZoneAtom, daylightSavingAtom, dateAtom, timeAtom };

		@Override
		public RC execute(final Interpreter interpreter, final boolean backtrackMode, final Term[] args) throws PrologException {
			// date_time_value(?Key, +DateTime, ?Value)
			if (backtrackMode) {
				Date9BacktrackInfo bi = (Date9BacktrackInfo) interpreter.popBacktrackInfo();
				interpreter.undo(bi.startUndoPosition);
				return nextSolution(interpreter, bi);
			}

			CompoundTerm date9 = switch (args[1]) {
				case CompoundTerm ct -> ct;
				default -> {
					PrologException.typeError(dateAtom, args[1]);
					yield null; // Never reached
				}
			};
			if (date9.tag != AbstractDateTimePredicate.date9Tag) {
				PrologException.typeError(dateAtom, args[1]);
			}
			if (date9.args.length != 9) {
				PrologException.typeError(dateAtom, args[1]);
			}

			return switch (args[0]) {
				case VariableTerm vt -> {
					Date9BacktrackInfo bi = new Date9BacktrackInfo();
					bi.startUndoPosition = interpreter.getUndoPosition();
					bi.key = args[0];
					bi.value = args[2];
					bi.date9 = date9;
					bi.date9keys = date9keys;
					bi.dateAtom = dateAtom;
					bi.timeAtom = timeAtom;
					bi.yearAtom = yearAtom;
					bi.monthAtom = monthAtom;
					bi.dayAtom = dayAtom;
					bi.hourAtom = hourAtom;
					bi.minuteAtom = minuteAtom;
					bi.secondAtom = secondAtom;
					bi.utcOffsetAtom = utcOffsetAtom;
					bi.timeZoneAtom = timeZoneAtom;
					bi.daylightSavingAtom = daylightSavingAtom;
					yield nextSolution(interpreter, bi);
				}
				default -> {
					Term res = getDate9Value(args[0], date9, yearAtom, monthAtom, dayAtom, hourAtom, minuteAtom,
							secondAtom, utcOffsetAtom, timeZoneAtom, daylightSavingAtom, dateAtom, timeAtom);
					if (res == null) {
						yield RC.FAIL;
					}
					yield interpreter.unify(args[2], res);
				}
			};
		}

		private Term getDate9Value(final Term keyTerm, final CompoundTerm date9, final AtomTerm yearAtom,
				final AtomTerm monthAtom, final AtomTerm dayAtom, final AtomTerm hourAtom, final AtomTerm minuteAtom,
				final AtomTerm secondAtom, final AtomTerm utcOffsetAtom, final AtomTerm timeZoneAtom,
				final AtomTerm daylightSavingAtom, final AtomTerm dateAtom, final AtomTerm timeAtom) throws PrologException {
			AtomTerm key = switch (keyTerm) {
				case AtomTerm at -> at;
				default -> {
					PrologException.typeError(TermConstants.atomAtom, keyTerm);
					yield null; // Never reached
				}
			};

			if (key == yearAtom) {
				return date9.args[0];
			} else if (key == monthAtom) {
				return date9.args[1];
			} else if (key == dayAtom) {
				return date9.args[2];
			} else if (key == hourAtom) {
				return date9.args[3];
			} else if (key == minuteAtom) {
				return date9.args[4];
			} else if (key == secondAtom) {
				return date9.args[5];
			} else if (key == utcOffsetAtom) {
				return date9.args[6];
			} else if (key == timeZoneAtom) {
				if (date9.args[7] == AtomTerm.get("-")) {
					return null;
				}
				return date9.args[7];
			} else if (key == daylightSavingAtom) {
				if (date9.args[8] == AtomTerm.get("-")) {
					return null;
				}
				return date9.args[8];
			} else if (key == dateAtom) {
				return new CompoundTerm(AbstractDateTimePredicate.date3Tag, new Term[] { date9.args[0], date9.args[1], date9.args[2] });
			} else if (key == timeAtom) {
				return new CompoundTerm(AbstractDateTimePredicate.date3Tag, new Term[] { date9.args[3], date9.args[4], date9.args[5] });
			}
			return null;
		}

		private RC nextSolution(final Interpreter interpreter, final Date9BacktrackInfo bi) throws PrologException {
			while (bi.date9idx < bi.date9keys.length) {
				Term key = bi.date9keys[bi.date9idx];
				Term res = getDate9Value(key, bi.date9, bi.yearAtom, bi.monthAtom, bi.dayAtom, bi.hourAtom, bi.minuteAtom,
						bi.secondAtom, bi.utcOffsetAtom, bi.timeZoneAtom, bi.daylightSavingAtom, bi.dateAtom, bi.timeAtom);
				bi.date9idx++;
				if (res == null) {
					continue;
				}
				if (interpreter.unify(bi.value, res) == RC.FAIL) {
					interpreter.undo(bi.startUndoPosition);
					continue;
				}
				if (interpreter.unify(bi.key, key) == RC.FAIL) {
					interpreter.undo(bi.startUndoPosition);
					continue;
				}
				interpreter.pushBacktrackInfo(bi);
				return RC.SUCCESS;
			}
			return RC.FAIL;
		}
	};

	// ============================================================================
	// BacktrackInfo Classes
	// ============================================================================

	private static class Date9BacktrackInfo extends BacktrackInfo {
		Term key;
		CompoundTerm date9;
		Term value;
		int date9idx;
		int startUndoPosition;
		AtomTerm[] date9keys;
		AtomTerm dateAtom;
		AtomTerm timeAtom;
		AtomTerm yearAtom;
		AtomTerm monthAtom;
		AtomTerm dayAtom;
		AtomTerm hourAtom;
		AtomTerm minuteAtom;
		AtomTerm secondAtom;
		AtomTerm utcOffsetAtom;
		AtomTerm timeZoneAtom;
		AtomTerm daylightSavingAtom;

		Date9BacktrackInfo() {
			super(-1, -1);
		}
	}
}
