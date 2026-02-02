package gnu.prolog.iso;

import gnu.prolog.io.TermReader;
import gnu.prolog.term.AtomTerm;
import gnu.prolog.term.Term;
import gnu.prolog.vm.Environment;
import gnu.prolog.vm.Interpreter;
import gnu.prolog.vm.PrologCode;
import gnu.prolog.vm.PrologException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ISO Prolog compliance tests for term manipulation predicates.
 *
 * Tests cover:
 * - functor/3 (ISO 8.5.1)
 * - arg/3 (ISO 8.5.2)
 * - =.. (univ) (ISO 8.5.3)
 * - copy_term/2 (ISO 8.5.4)
 * - term_variables/2
 *
 * Based on ISO/IEC 13211-1 (ISO Prolog standard) section 8.5
 */
class ISOTermManipulationTest {

	private Environment env;
	private Interpreter interpreter;

	@BeforeEach
	void setup() {
		env = new Environment();
		interpreter = env.createInterpreter();
	}

	// ========== functor/3 tests ==========

	@Test
	void testFunctorExtractFromCompound() throws Exception {
		Term goal = parseGoal("functor(foo(a, b, c), F, A), F = foo, A = 3");
		assertGoalSucceeds(goal);
	}

	@Test
	void testFunctorExtractFromAtom() throws Exception {
		Term goal = parseGoal("functor(atom, F, A), F = atom, A = 0");
		assertGoalSucceeds(goal);
	}

	@Test
	void testFunctorExtractFromInteger() throws Exception {
		Term goal = parseGoal("functor(42, F, A), F = 42, A = 0");
		assertGoalSucceeds(goal);
	}

	@Test
	void testFunctorConstructCompound() throws Exception {
		Term goal = parseGoal("functor(T, foo, 3), T = foo(_, _, _)");
		assertGoalSucceeds(goal);
	}

	@Test
	void testFunctorConstructAtom() throws Exception {
		Term goal = parseGoal("functor(T, atom, 0), T = atom");
		assertGoalSucceeds(goal);
	}

	@Test
	void testFunctorBidirectional() throws Exception {
		Term goal = parseGoal("functor(foo(a, b), F, 2), functor(T, F, 2), T = foo(_, _)");
		assertGoalSucceeds(goal);
	}

	// ========== arg/3 tests ==========

	@Test
	void testArgFirstArgument() throws Exception {
		Term goal = parseGoal("arg(1, foo(a, b, c), X), X = a");
		assertGoalSucceeds(goal);
	}

	@Test
	void testArgSecondArgument() throws Exception {
		Term goal = parseGoal("arg(2, foo(a, b, c), X), X = b");
		assertGoalSucceeds(goal);
	}

	@Test
	void testArgThirdArgument() throws Exception {
		Term goal = parseGoal("arg(3, foo(a, b, c), X), X = c");
		assertGoalSucceeds(goal);
	}

	@Test
	void testArgWithVariable() throws Exception {
		Term goal = parseGoal("arg(1, foo(X, b, c), Y), Y = X");
		assertGoalSucceeds(goal);
	}

	@Test
	void testArgOutOfBounds() throws Exception {
		Term goal = parseGoal("arg(4, foo(a, b, c), X)");
		assertGoalFails(goal);
	}

	@Test
	void testArgZeroIndex() throws Exception {
		Term goal = parseGoal("arg(0, foo(a, b, c), X)");
		assertGoalFails(goal);
	}

	@Test
	void testArgNegativeIndex() throws Exception {
		Term goal = parseGoal("arg(-1, foo(a, b, c), X)");
		// ISO Prolog 8.5.2: arg/3 with N < 0 throws domain_error(not_less_than_zero, N)
		Interpreter.Goal preparedGoal = interpreter.prepareGoal(goal);
		assertThrows(PrologException.class, () -> interpreter.execute(preparedGoal),
			"arg/3 should throw domain_error for negative index");
	}

	// ========== =.. (univ) tests ==========

	@Test
	void testUnivDecomposeCompound() throws Exception {
		Term goal = parseGoal("foo(a, b, c) =.. X, X = [foo, a, b, c]");
		assertGoalSucceeds(goal);
	}

	@Test
	void testUnivDecomposeAtom() throws Exception {
		Term goal = parseGoal("atom =.. X, X = [atom]");
		assertGoalSucceeds(goal);
	}

	@Test
	void testUnivDecomposeInteger() throws Exception {
		Term goal = parseGoal("42 =.. X, X = [42]");
		assertGoalSucceeds(goal);
	}

	@Test
	void testUnivConstructCompound() throws Exception {
		Term goal = parseGoal("X =.. [foo, a, b, c], X = foo(a, b, c)");
		assertGoalSucceeds(goal);
	}

	@Test
	void testUnivConstructAtom() throws Exception {
		Term goal = parseGoal("X =.. [atom], X = atom");
		assertGoalSucceeds(goal);
	}

	@Test
	void testUnivBidirectional() throws Exception {
		Term goal = parseGoal("foo(a, b) =.. L, X =.. L, X = foo(a, b)");
		assertGoalSucceeds(goal);
	}

	@Test
	void testUnivWithVariables() throws Exception {
		Term goal = parseGoal("foo(X, Y) =.. [F, A, B], F = foo");
		assertGoalSucceeds(goal);
	}

	@Test
	void testUnivChangeFunction() throws Exception {
		Term goal = parseGoal("foo(a, b) =.. [_|Args], X =.. [bar|Args], X = bar(a, b)");
		assertGoalSucceeds(goal);
	}

	// ========== copy_term/2 tests ==========

	@Test
	void testCopyTermGround() throws Exception {
		Term goal = parseGoal("copy_term(foo(a, b), X), X = foo(a, b)");
		assertGoalSucceeds(goal);
	}

	@Test
	void testCopyTermWithVariables() throws Exception {
		// Variables should be renamed but structure preserved
		Term goal = parseGoal("copy_term(foo(X, X), foo(Y, Z)), Y == Z");
		assertGoalSucceeds(goal);
	}

	@Test
	void testCopyTermIndependence() throws Exception {
		// Copied term should be independent
		env.ensureLoaded(AtomTerm.get(createTempFile(
			"test_copy :- \n" +
			"    copy_term(foo(X, X), foo(Y, Z)),\n" +
			"    X = a,\n" +
			"    var(Y), var(Z).\n"
		)));

		Term goal = parseGoal("test_copy");
		assertGoalSucceeds(goal);
	}

	@Test
	void testCopyTermAtom() throws Exception {
		Term goal = parseGoal("copy_term(atom, X), X = atom");
		assertGoalSucceeds(goal);
	}

	@Test
	void testCopyTermInteger() throws Exception {
		Term goal = parseGoal("copy_term(42, X), X = 42");
		assertGoalSucceeds(goal);
	}

	// ========== term_variables/2 tests (extension) ==========

	@Test
	void testTermVariablesGround() throws Exception {
		Term goal = parseGoal("term_variables(foo(a, b), X), X = []");
		assertGoalSucceeds(goal);
	}

	@Test
	void testTermVariablesSingleVar() throws Exception {
		Term goal = parseGoal("term_variables(foo(X, a), L), length(L, 1)");
		assertGoalSucceeds(goal);
	}

	@Test
	void testTermVariablesMultipleDistinct() throws Exception {
		Term goal = parseGoal("term_variables(foo(X, Y, Z), L), length(L, 3)");
		assertGoalSucceeds(goal);
	}

	@Test
	void testTermVariablesRepeated() throws Exception {
		// Same variable appearing multiple times should be listed once
		Term goal = parseGoal("term_variables(foo(X, X, Y), L), length(L, 2)");
		assertGoalSucceeds(goal);
	}

	@Test
	void testTermVariablesNested() throws Exception {
		Term goal = parseGoal("term_variables(foo(bar(X, Y), baz(Z)), L), length(L, 3)");
		assertGoalSucceeds(goal);
	}

	// ========== Complex term manipulation scenarios ==========

	@Test
	void testBuildTermFromParts() throws Exception {
		env.ensureLoaded(AtomTerm.get(createTempFile(
			"build_term(Functor, Args, Term) :- \n" +
			"    length(Args, Arity),\n" +
			"    functor(Term, Functor, Arity),\n" +
			"    fill_args(1, Args, Term).\n" +
			"\n" +
			"fill_args(_, [], _).\n" +
			"fill_args(N, [H|T], Term) :- \n" +
			"    arg(N, Term, H),\n" +
			"    N1 is N + 1,\n" +
			"    fill_args(N1, T, Term).\n"
		)));

		Term goal = parseGoal("build_term(foo, [a, b, c], X), X = foo(a, b, c)");
		assertGoalSucceeds(goal);
	}

	@Test
	void testExtractTermParts() throws Exception {
		env.ensureLoaded(AtomTerm.get(createTempFile(
			"extract_parts(Term, Functor, Args) :- \n" +
			"    Term =.. [Functor|Args].\n"
		)));

		Term goal = parseGoal("extract_parts(foo(a, b, c), F, A), F = foo, A = [a, b, c]");
		assertGoalSucceeds(goal);
	}

	@Test
	void testModifyTermArgument() throws Exception {
		env.ensureLoaded(AtomTerm.get(createTempFile(
			"replace_arg(N, OldTerm, NewArg, NewTerm) :- \n" +
			"    OldTerm =.. [F|Args],\n" +
			"    replace_nth(N, Args, NewArg, NewArgs),\n" +
			"    NewTerm =.. [F|NewArgs].\n" +
			"\n" +
			"replace_nth(1, [_|T], New, [New|T]).\n" +
			"replace_nth(N, [H|T], New, [H|R]) :- \n" +
			"    N > 1,\n" +
			"    N1 is N - 1,\n" +
			"    replace_nth(N1, T, New, R).\n"
		)));

		Term goal = parseGoal("replace_arg(2, foo(a, b, c), x, X), X = foo(a, x, c)");
		assertGoalSucceeds(goal);
	}

	@Test
	void testDeepCopyPreservesStructure() throws Exception {
		Term goal = parseGoal("copy_term(foo(bar(X, Y), baz(X, Z)), Copy), " +
							   "Copy = foo(bar(A, B), baz(C, D)), A == C");
		assertGoalSucceeds(goal);
	}

	@Test
	void testTermConstruction() throws Exception {
		// Build a term dynamically and verify it
		env.ensureLoaded(AtomTerm.get(createTempFile(
			"make_pred(Name, Arg, Pred) :- \n" +
			"    functor(Pred, Name, 1),\n" +
			"    arg(1, Pred, Arg).\n"
		)));

		Term goal = parseGoal("make_pred(test, value, P), P = test(value)");
		assertGoalSucceeds(goal);
	}

	// Helper methods

	private Term parseGoal(final String goalText) throws Exception {
		TermReader reader = new TermReader(new StringReader(goalText + "."), env);
		return reader.readTerm(env.getOperatorSet());
	}

	private void assertGoalSucceeds(final Term goal) throws PrologException {
		Interpreter.Goal preparedGoal = interpreter.prepareGoal(goal);
		PrologCode.RC rc = interpreter.execute(preparedGoal);
		assertTrue(rc == PrologCode.RC.SUCCESS || rc == PrologCode.RC.SUCCESS_LAST,
			"Goal should succeed: " + goal);
	}

	private void assertGoalFails(final Term goal) throws PrologException {
		Interpreter.Goal preparedGoal = interpreter.prepareGoal(goal);
		PrologCode.RC rc = interpreter.execute(preparedGoal);
		assertEquals(PrologCode.RC.FAIL, rc, "Goal should fail: " + goal);
	}

	private String createTempFile(final String content) throws Exception {
		java.io.File tempFile = java.io.File.createTempFile("prolog_test", ".pl");
		tempFile.deleteOnExit();
		java.nio.file.Files.write(tempFile.toPath(), content.getBytes());
		return tempFile.getAbsolutePath();
	}
}
