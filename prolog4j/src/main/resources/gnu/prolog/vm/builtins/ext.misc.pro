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
 
%
% Miscellaneous extensions
%

% List all perdicates with the given name
% listing(+Pred)
:-build_in(listing/1,'gnu.prolog.vm.builtins.misc.Predicates#LISTING').
:-build_in(listing/0,'gnu.prolog.vm.builtins.misc.Predicates#LISTING').

% Determine or test the Order between two terms in the standard order of terms. 
% Order is one of <, > or =, with the obvious meaning.
% compare(?Order, +Term1, +Term2)
:-build_in(compare/3,'gnu.prolog.vm.builtins.termcomparsion.Predicates#COMPARE').  

% Retrieve the current stacktrace of evaluating predicates (excluding
% the current predicate). Note: it will only contain the compound tags
% of the executed predicates.
% stacktrace(?List)
:-build_in(stacktrace/1,'gnu.prolog.vm.builtins.misc.Predicates#STACKTRACE').

% consult(+File)
% Load a Prolog source file (alias for ensure_loaded/1)
consult(File) :- ensure_loaded(File).

% repeat(+Repeats)
% Like repeat/0 but limits the number of repeats to Repeats
% @throws instantiation_error if Repeats is a variable
repeat(Repeats) :- Repeats =< 0, !, fail.
repeat(Repeats).
repeat(Repeats) :- NewRepeats is Repeats -1, repeat(NewRepeats).