/* GNU Prolog for Java
 * Copyright (C) 1997-1999  Constantine Plotnikov
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
 
% See the following site for additional information
% http://pauillac.inria.fr/~deransar/prolog/bips.html
 
% build in predicates
% Numbers before section descriptions correspond to parts of ISO Prolog 
% standard
% 7.8 control constructs rules for control constructs are not the same as for
% predicates, as result following declarations are not needed, but they are 
% left for some future reason.
/*
:-control(true/0,  'gnu.prolog.vm.buildin.control.Control_true').
:-control(fail/0,  'gnu.prolog.vm.buildin.control.Control_fail').
:-control(!/0,     'gnu.prolog.vm.buildin.control.Control_cut').
:-control((',')/2, 'gnu.prolog.vm.buildin.control.Control_and').
:-control((';')/2, 'gnu.prolog.vm.buildin.control.Control_or').
:-control(('->')/2,'gnu.prolog.vm.buildin.control.Control_if_then').
:-control(catch/3, 'gnu.prolog.vm.buildin.control.Control_catch').
:-control(throw/1, 'gnu.prolog.vm.buildin.control.Control_throw').
*/
% the only used is call/1
%:-control(call/1, 'gnu.prolog.vm.interpreter.Call').
:-build_in(call/1, 'gnu.prolog.vm.interpreter.Call').

% Control constructs that need to be registered as predicates for current_predicate/1
% true/0 always succeeds
:-build_in(true/0, 'gnu.prolog.vm.interpreter.True').

% Higher-order predicates: call/2 and call/3
% call(+Goal, +Arg1) - Add Arg1 to Goal and call it
call(Atom, Arg1) :- atom(Atom), !, Goal =.. [Atom, Arg1], call(Goal).
call(Compound, Arg1) :- Compound =.. [Functor|Args], append(Args, [Arg1], NewArgs), Goal =.. [Functor|NewArgs], call(Goal).

% call(+Goal, +Arg1, +Arg2) - Add Arg1 and Arg2 to Goal and call it
call(Atom, Arg1, Arg2) :- atom(Atom), !, Goal =.. [Atom, Arg1, Arg2], call(Goal).
call(Compound, Arg1, Arg2) :- Compound =.. [Functor|Args], append(Args, [Arg1, Arg2], NewArgs), Goal =.. [Functor|NewArgs], call(Goal). 

% 8.2 term unification
% I'm really considering making this control constructs
:-build_in((=)/2,  'gnu.prolog.vm.builtins.unification.Predicates#UNIFY').
:-build_in((unify_with_occurs_check)/2, 'gnu.prolog.vm.builtins.unification.Predicates#UNIFY_WITH_OCCURS_CHECK').
:-build_in((\=)/2, 'gnu.prolog.vm.builtins.unification.Predicates#NOT_UNIFIABLE'). 

% 8.3 type testing
:-build_in(var/1,     'gnu.prolog.vm.builtins.typetesting.Predicates#VAR').
:-build_in(atom/1,    'gnu.prolog.vm.builtins.typetesting.Predicates#ATOM').
:-build_in(integer/1, 'gnu.prolog.vm.builtins.typetesting.Predicates#INTEGER').
%:-build_in(real/1,    'gnu.prolog.vm.builtins.typetesting.Predicates#REAL').
:-build_in(float/1,   'gnu.prolog.vm.builtins.typetesting.Predicates#FLOAT').
:-build_in(decimal/1, 'gnu.prolog.vm.builtins.typetesting.Predicates#DECIMAL').
:-build_in(atomic/1,  'gnu.prolog.vm.builtins.typetesting.Predicates#ATOMIC').
:-build_in(compound/1,'gnu.prolog.vm.builtins.typetesting.Predicates#COMPOUND').
:-build_in(nonvar/1,  'gnu.prolog.vm.builtins.typetesting.Predicates#NONVAR').
:-build_in(number/1,  'gnu.prolog.vm.builtins.typetesting.Predicates#NUMBER').
:-build_in(callable/1,'gnu.prolog.vm.builtins.typetesting.Predicates#CALLABLE').
:-build_in(ground/1,  'gnu.prolog.vm.builtins.typetesting.Predicates#GROUND').
:-build_in(java_object/1, 'gnu.prolog.vm.builtins.typetesting.Predicates#JAVA_OBJECT'). 
                                     
% 8.4 term comparison
:-build_in((==)/2,  'gnu.prolog.vm.builtins.termcomparsion.Predicates#TERM_IDENTICAL').
:-build_in((\==)/2, 'gnu.prolog.vm.builtins.termcomparsion.Predicates#TERM_NOT_IDENTICAL').
:-build_in((@<)/2,  'gnu.prolog.vm.builtins.termcomparsion.Predicates#TERM_LESS_THAN').
:-build_in((@=<)/2, 'gnu.prolog.vm.builtins.termcomparsion.Predicates#TERM_LESS_THAN_OR_EQUAL').
:-build_in((@>)/2,  'gnu.prolog.vm.builtins.termcomparsion.Predicates#TERM_GREATER_THAN').
:-build_in((@>=)/2, 'gnu.prolog.vm.builtins.termcomparsion.Predicates#TERM_GREATER_THAN_OR_EQUAL'). 

% 8.5 term creation and decomposition
:-build_in(functor/3,  'gnu.prolog.vm.builtins.termcreation.Predicates#FUNCTOR').
:-build_in(arg/3,      'gnu.prolog.vm.builtins.termcreation.Predicates#ARG').
:-build_in((=..)/2,    'gnu.prolog.vm.builtins.termcreation.Predicates#UNIV').
:-build_in(copy_term/2,'gnu.prolog.vm.builtins.termcreation.Predicates#COPY_TERM').
:-build_in(term_variables/2,'gnu.prolog.vm.builtins.termcreation.Predicates#TERM_VARIABLES'). 

% 8.6 arithmetics evaluation
:-build_in((is)/2,'gnu.prolog.vm.builtins.arithmetics.Predicates#IS').

% 8.7 arithmetic comparison
:-build_in((=:=)/2,'gnu.prolog.vm.builtins.arithmetics.Predicates#EQUAL').
:-build_in((=\=)/2,'gnu.prolog.vm.builtins.arithmetics.Predicates#NOT_EQUAL').
:-build_in((<)/2,  'gnu.prolog.vm.builtins.arithmetics.Predicates#LESS_THAN').
:-build_in((=<)/2, 'gnu.prolog.vm.builtins.arithmetics.Predicates#LESS_THAN_OR_EQUAL').
:-build_in((>)/2,  'gnu.prolog.vm.builtins.arithmetics.Predicates#GREATER_THAN').
:-build_in((>=)/2, 'gnu.prolog.vm.builtins.arithmetics.Predicates#GREATER_THAN_OR_EQUAL'). 

% 8.8 clause retrieval and information
:-build_in(clause/2,  'gnu.prolog.vm.builtins.database.Predicates#CLAUSE').
:-build_in(current_predicate/1, 'gnu.prolog.vm.builtins.database.Predicates#CURRENT_PREDICATE').

% 8.9 clause creation and destruction
:-build_in(asserta/1,  'gnu.prolog.vm.builtins.database.Predicates#ASSERTA').
:-build_in(assertz/1,  'gnu.prolog.vm.builtins.database.Predicates#ASSERTZ').
:-build_in(retract/1,  'gnu.prolog.vm.builtins.database.Predicates#RETRACT').
:-build_in(abolish/1,  'gnu.prolog.vm.builtins.database.Predicates#ABOLISH').

% assert/1 is typically an alias for assertz/1
assert(Clause) :- assertz(Clause). 

% 8.10 All solutions
:-build_in(findall/3, 'gnu.prolog.vm.builtins.allsolutions.Predicates#FINDALL').
:-build_in(bagof/3,   'gnu.prolog.vm.builtins.allsolutions.Predicates#BAGOF').
:-build_in(setof/3,   'gnu.prolog.vm.builtins.allsolutions.Predicates#SETOF'). 

% 8.11 stream selection and control
:-build_in(current_input/1,  'gnu.prolog.vm.builtins.io.Predicates#CURRENT_INPUT').
:-build_in(current_output/1, 'gnu.prolog.vm.builtins.io.Predicates#CURRENT_OUTPUT').
:-build_in(set_input/1,      'gnu.prolog.vm.builtins.io.Predicates#SET_INPUT').
:-build_in(set_output/1,     'gnu.prolog.vm.builtins.io.Predicates#SET_OUTPUT').
:-build_in(open/4,           'gnu.prolog.vm.builtins.io.Predicates#OPEN').
open(Source_sink, Mode, Stream):- open(Source_sink, Mode, Stream, []).
:-build_in(close/2,           'gnu.prolog.vm.builtins.io.Predicates#CLOSE').
close(S_or_a) :- close(S_or_a, []).
:-build_in(flush_output/1, 'gnu.prolog.vm.builtins.io.Predicates#FLUSH_OUTPUT').
flush_output:-current_output(Stream), flush_output(Stream).
:-build_in(stream_property/2, 'gnu.prolog.vm.builtins.io.Predicates#STREAM_PROPERTY').
:-build_in(at_end_of_stream/1, 'gnu.prolog.vm.builtins.io.Predicates#AT_END_OF_STREAM').
at_end_of_stream:- current_input(S), at_end_of_stream(S).
:-build_in(set_stream_position/2, 'gnu.prolog.vm.builtins.io.Predicates#SET_STREAM_POSITION'). 

% 8.12 character input/output
:-build_in(get_char/2, 'gnu.prolog.vm.builtins.io.Predicates#GET_CHAR').
get_char(Char):-
   current_input(S),get_char(S,Char).
get_code(Code):-
   current_input(S),
   get_char(S,Char),
   ( Char = end_of_file ->
     code = -1
   ; char_code(Char,Code)
   ).
get_code(S, Code):-
   get_char(S,Char),
   ( Char = end_of_file ->
     code = -1
   ; char_code(Char,Code)
   ).
:-build_in(peek_char/2, 'gnu.prolog.vm.builtins.io.Predicates#PEEK_CHAR').
peek_char(Char):-
   current_input(S),peek_char(S,Char).
peek_code(Code):-
   current_input(S),
   peek_char(S,Char),
   ( Char = end_of_file ->
     code = -1
   ; char_code(Char,Code)
   ).
peek_code(S, Code):-
   peek_char(S,Char),
   ( Char = end_of_file ->
     code = -1
   ; char_code(Char,Code)
   ).
:-build_in(put_char/2, 'gnu.prolog.vm.builtins.io.Predicates#PUT_CHAR').
put_char(Char):- current_output(S),put_char(S,Char).
put_code(Code):- current_output(S), char_code(Char,Code), put_char(S,Char).
put_code(S, Code):- char_code(Char,Code), put_char(S,Char).
nl(S):- put_char(S,'\n').
nl:- current_output(S),put_char(S,'\n').

% 8.13 byte input/output

:-build_in(get_byte/2, 'gnu.prolog.vm.builtins.io.Predicates#GET_BYTE').
get_byte(Char):- current_input(S),get_byte(S,Char).
:-build_in(peek_byte/2, 'gnu.prolog.vm.builtins.io.Predicates#PEEK_BYTE').
peek_byte(Char):- current_input(S),peek_byte(S,Char).
:-build_in(put_byte/2, 'gnu.prolog.vm.builtins.io.Predicates#PUT_BYTE').
put_byte(Char):- current_output(S),put_byte(S,Char). 


% Define xor operator (SWI-Prolog extension, commonly used)
:-op(400, yfx, xor).

% DCG operator
:-op(1200, xfx, -->).

% 8.14 Term input/output

:-build_in(read_term/3, 'gnu.prolog.vm.builtins.io.Predicates#READ_TERM').
read_term(Term,Options):-current_input(S),read_term(S,Term,Options).
read(S, Term):-read_term(S, Term,[]).
read(Term):-current_input(S),read_term(S,Term,[]).
:-build_in(write_term/3, 'gnu.prolog.vm.builtins.io.Predicates#WRITE_TERM').
write_term(Term,Options):-current_output(S),write_term(S,Term,Options).
write(Term):-current_output(S),write_term(S,Term,[numbervars(true)]).
write(S,Term):-write_term(S,Term,[numbervars(true)]).
writeq(Term):-current_output(S),write_term(S,Term,[quoted(true),numbervars(true)]).
writeq(S,Term):-write_term(S,Term,[quoted(true),numbervars(true)]).
write_canonical(Term):-current_output(S),write_term(S,Term,[quoted(true),ignore_ops(true)]).
write_canonical(S,Term):-write_term(S,Term,[quoted(true),ignore_ops(true)]).
:-build_in(format/2, 'gnu.prolog.vm.builtins.io.Predicates#FORMAT').
:-build_in(format/3, 'gnu.prolog.vm.builtins.io.Predicates#FORMAT_3').
:-build_in(op/3, 'gnu.prolog.vm.builtins.io.Predicates#OP').
:-build_in(current_op/3, 'gnu.prolog.vm.builtins.io.Predicates#CURRENT_OP').

% TODO char conversions are not yet supported
% Note: even though these are accepted the conversion is not performed during reading
:-build_in(char_conversion/2, 'gnu.prolog.vm.builtins.io.Predicates#CHAR_CONVERSION').
:-build_in(current_char_conversion/2, 'gnu.prolog.vm.builtins.io.Predicates#CURRENT_CHAR_CONVERSION').

% 8.15 logic and control

'\\+'(Goal) :- call(Goal),!,fail.
'\\+'(Goal).

not(Goal) :- '\\+'(Goal).
fail_if(Goal) :- '\\+'(Goal).

once(Goal) :- call(Goal),!.

repeat.
repeat:-repeat.

% 8.16 Atomic term processing
:-build_in(atom_length/2,'gnu.prolog.vm.builtins.atomicterms.Predicates#ATOM_LENGTH').
:-build_in(atom_concat/3,'gnu.prolog.vm.builtins.atomicterms.Predicates#ATOM_CONCAT').
:-build_in(sub_atom/5,'gnu.prolog.vm.builtins.atomicterms.Predicates#SUB_ATOM').
:-build_in(atom_chars/2,'gnu.prolog.vm.builtins.atomicterms.Predicates#ATOM_CHARS').
:-build_in(atom_codes/2,'gnu.prolog.vm.builtins.atomicterms.Predicates#ATOM_CODES').
:-build_in(char_code/2,'gnu.prolog.vm.builtins.atomicterms.Predicates#CHAR_CODE').
:-build_in(number_chars/2,'gnu.prolog.vm.builtins.atomicterms.Predicates#NUMBER_CHARS').
:-build_in(number_codes/2,'gnu.prolog.vm.builtins.atomicterms.Predicates#NUMBER_CODES').

% 8.17 Implementation defined hooks
:-build_in(set_prolog_flag/2,'gnu.prolog.vm.builtins.imphooks.Predicates#SET_PROLOG_FLAG').
:-build_in(current_prolog_flag/2,'gnu.prolog.vm.builtins.imphooks.Predicates#CURRENT_PROLOG_FLAG').
:-build_in(halt/1,'gnu.prolog.vm.builtins.imphooks.Predicates#HALT').
halt:-halt(0).

% DCG (Definite Clause Grammars) support
:-build_in(phrase/2, 'gnu.prolog.vm.builtins.dcg.Predicates#PHRASE_2').
:-build_in(phrase/3, 'gnu.prolog.vm.builtins.dcg.Predicates#PHRASE_3').

% Load non-ISO extensions
:-ensure_loaded(resource('/gnu/prolog/vm/builtins/ext.debug.pro')).
:-ensure_loaded(resource('/gnu/prolog/vm/builtins/ext.list.pro')).
:-ensure_loaded(resource('/gnu/prolog/vm/builtins/ext.datetime.pro')).
:-ensure_loaded(resource('/gnu/prolog/vm/builtins/ext.meta.pro')).
:-ensure_loaded(resource('/gnu/prolog/vm/builtins/ext.misc.pro')).
:-ensure_loaded(resource('/gnu/prolog/vm/builtins/ext.java.pro')).
:-ensure_loaded(resource('/gnu/prolog/vm/builtins/ext.uuid.pro')).
:-ensure_loaded(resource('/gnu/prolog/vm/builtins/ext.dialogs.pro')).
:-ensure_loaded(resource('/gnu/prolog/vm/builtins/ext.database.pro')).

% Directives used outside of their normal directive context.
:-build_in(ensure_loaded/1, 'gnu.prolog.vm.builtins.io.Predicates#ENSURE_LOADED').
