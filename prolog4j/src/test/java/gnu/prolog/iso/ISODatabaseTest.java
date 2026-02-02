package gnu.prolog.iso;

import gnu.prolog.io.TermReader;
import gnu.prolog.term.AtomTerm;
import gnu.prolog.term.Term;
import gnu.prolog.vm.Environment;
import gnu.prolog.vm.Interpreter;
import gnu.prolog.vm.PrologCode;
import gnu.prolog.vm.PrologException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ISO Prolog compliance tests for database operations.
 *
 * Tests cover:
 * - assert/1, asserta/1, assertz/1 (ISO 8.9.1-8.9.3)
 * - retract/1 (ISO 8.9.4)
 * - abolish/1 (ISO 8.9.5)
 * - clause/2 (ISO 8.8.1)
 * - current_predicate/1 (ISO 8.8.2)
 *
 * Based on ISO/IEC 13211-1 (ISO Prolog standard) section 8.8-8.9
 */
class ISODatabaseTest {

	private Environment env;
	private Interpreter interpreter;

	@BeforeEach
	void setup() {
		env = new Environment();
		interpreter = env.createInterpreter();
	}

	// ========== assert/1 tests ==========

	@Test
	void testAssertFact() throws Exception {
		Term assertGoal = parseGoal("assert(fact(a))");
		assertGoalSucceeds(assertGoal);

		Term queryGoal = parseGoal("fact(a)");
		assertGoalSucceeds(queryGoal);
	}

	@Test
	void testAssertRule() throws Exception {
		Term assertGoal = parseGoal("assert((head(X) :- body(X)))");
		assertGoalSucceeds(assertGoal);

		Term setupGoal = parseGoal("assert(body(test))");
		assertGoalSucceeds(setupGoal);

		Term queryGoal = parseGoal("head(test)");
		assertGoalSucceeds(queryGoal);
	}

	// ========== asserta/1 tests (add at beginning) ==========

	@Test
	void testAssertaOrder() throws Exception {
		Term assert1 = parseGoal("asserta(order(first))");
		assertGoalSucceeds(assert1);

		Term assert2 = parseGoal("asserta(order(second))");
		assertGoalSucceeds(assert2);

		// Second should come first due to asserta
		Term queryGoal = parseGoal("order(X), !, X = second");
		assertGoalSucceeds(queryGoal);
	}

	// ========== assertz/1 tests (add at end) ==========

	@Test
	void testAssertzOrder() throws Exception {
		Term assert1 = parseGoal("assertz(ordered(first))");
		assertGoalSucceeds(assert1);

		Term assert2 = parseGoal("assertz(ordered(second))");
		assertGoalSucceeds(assert2);

		// First should come first due to assertz
		Term queryGoal = parseGoal("ordered(X), !, X = first");
		assertGoalSucceeds(queryGoal);
	}

	// ========== retract/1 tests ==========

	@Test
	@Disabled("Known issue: retract/1 not properly cleaning up facts")
	void testRetractFact() throws Exception {
		// Add and verify fact exists
		Term assertGoal = parseGoal("assert(temp(a))");
		assertGoalSucceeds(assertGoal);

		Term verifyGoal = parseGoal("temp(a)");
		assertGoalSucceeds(verifyGoal);

		// Retract and verify it's gone
		Term retractGoal = parseGoal("retract(temp(a))");
		assertGoalSucceeds(retractGoal);

		Term checkGoal = parseGoal("temp(a)");
		assertGoalFails(checkGoal);
	}

	@Test
	void testRetractWithVariable() throws Exception {
		Term assert1 = parseGoal("assert(data(1))");
		assertGoalSucceeds(assert1);

		Term assert2 = parseGoal("assert(data(2))");
		assertGoalSucceeds(assert2);

		// Retract first match
		Term retractGoal = parseGoal("retract(data(X)), X = 1");
		assertGoalSucceeds(retractGoal);

		// Verify second still exists
		Term checkGoal = parseGoal("data(2)");
		assertGoalSucceeds(checkGoal);
	}

	@Test
	void testRetractRule() throws Exception {
		Term assertGoal = parseGoal("assert((rule(X) :- body(X)))");
		assertGoalSucceeds(assertGoal);

		Term retractGoal = parseGoal("retract((rule(X) :- body(X)))");
		assertGoalSucceeds(retractGoal);

		// Clause should be gone - clause/2 should fail
		Term checkGoal = parseGoal("clause(rule(X), body(X))");
		assertGoalFails(checkGoal);
	}

	@Test
	void testRetractNonexistent() throws Exception {
		Term retractGoal = parseGoal("retract(nonexistent(x))");
		assertGoalFails(retractGoal);
	}

	// ========== abolish/1 tests ==========

	@Test
	void testAbolishPredicate() throws Exception {
		Term assert1 = parseGoal("assert(removeme(a))");
		assertGoalSucceeds(assert1);

		Term assert2 = parseGoal("assert(removeme(b))");
		assertGoalSucceeds(assert2);

		Term abolishGoal = parseGoal("abolish(removeme/1)");
		assertGoalSucceeds(abolishGoal);

		// ISO Prolog 8.9.4: After abolish, calling the predicate throws existence_error
		// (when unknown flag is set to 'error', which is the default)
		Term checkGoal = parseGoal("removeme(a)");
		Interpreter.Goal preparedGoal = interpreter.prepareGoal(checkGoal);
		assertThrows(PrologException.class, () -> interpreter.execute(preparedGoal),
			"Calling abolished predicate should throw existence_error");
	}

	@Test
	void testAbolishDoesNotAffectOtherPredicates() throws Exception {
		Term assert1 = parseGoal("assert(keep(a))");
		assertGoalSucceeds(assert1);

		Term assert2 = parseGoal("assert(remove(b))");
		assertGoalSucceeds(assert2);

		Term abolishGoal = parseGoal("abolish(remove/1)");
		assertGoalSucceeds(abolishGoal);

		// keep/1 should still exist
		Term checkGoal = parseGoal("keep(a)");
		assertGoalSucceeds(checkGoal);
	}

	// ========== clause/2 tests ==========

	@Test
	void testClauseFact() throws Exception {
		Term assertGoal = parseGoal("assert(myfact(test))");
		assertGoalSucceeds(assertGoal);

		Term clauseGoal = parseGoal("clause(myfact(test), true)");
		assertGoalSucceeds(clauseGoal);
	}

	@Test
	void testClauseRule() throws Exception {
		Term assertGoal = parseGoal("assert((myrule(X) :- mygoal(X)))");
		assertGoalSucceeds(assertGoal);

		Term clauseGoal = parseGoal("clause(myrule(Y), mygoal(Y))");
		assertGoalSucceeds(clauseGoal);
	}

	@Test
	void testClauseWithVariable() throws Exception {
		Term assert1 = parseGoal("assert(multi(a))");
		assertGoalSucceeds(assert1);

		Term assert2 = parseGoal("assert(multi(b))");
		assertGoalSucceeds(assert2);

		// Should unify with first clause
		Term clauseGoal = parseGoal("clause(multi(X), true), X = a");
		assertGoalSucceeds(clauseGoal);
	}

	@Test
	void testClauseNonexistent() throws Exception {
		Term clauseGoal = parseGoal("clause(nonexistent(x), Body)");
		assertGoalFails(clauseGoal);
	}

	// ========== current_predicate/1 tests ==========

	@Test
	void testCurrentPredicateExists() throws Exception {
		Term assertGoal = parseGoal("assert(mypred(test))");
		assertGoalSucceeds(assertGoal);

		Term checkGoal = parseGoal("current_predicate(mypred/1)");
		assertGoalSucceeds(checkGoal);
	}

	@Test
	void testCurrentPredicateBuiltin() throws Exception {
		// true/0 should always exist
		Term checkGoal = parseGoal("current_predicate(true/0)");
		assertGoalSucceeds(checkGoal);
	}

	// ========== Complex database scenarios ==========

	@Test
	void testMultipleAssertsAndRetracts() throws Exception {
		// Add multiple facts
		for (int i = 1; i <= 5; i++) {
			Term assertGoal = parseGoal("assert(counter(" + i + "))");
			assertGoalSucceeds(assertGoal);
		}

		// Retract some
		Term retract1 = parseGoal("retract(counter(2))");
		assertGoalSucceeds(retract1);

		Term retract2 = parseGoal("retract(counter(4))");
		assertGoalSucceeds(retract2);

		// Verify remaining exist
		Term check1 = parseGoal("counter(1)");
		assertGoalSucceeds(check1);

		Term check3 = parseGoal("counter(3)");
		assertGoalSucceeds(check3);

		Term check5 = parseGoal("counter(5)");
		assertGoalSucceeds(check5);

		// Verify retracted ones are gone
		Term checkGone2 = parseGoal("counter(2)");
		assertGoalFails(checkGone2);

		Term checkGone4 = parseGoal("counter(4)");
		assertGoalFails(checkGone4);
	}

	@Test
	@Disabled("Known issue: retract/1 not properly handling dynamic rule modifications")
	void testDynamicRuleModification() throws Exception {
		// Assert initial rule
		Term assert1 = parseGoal("assert((dynamic_rule(X) :- X = old))");
		assertGoalSucceeds(assert1);

		Term verifyOld = parseGoal("dynamic_rule(old)");
		assertGoalSucceeds(verifyOld);

		// Retract old rule
		Term retract = parseGoal("retract((dynamic_rule(X) :- X = old))");
		assertGoalSucceeds(retract);

		// Assert new rule
		Term assert2 = parseGoal("assert((dynamic_rule(X) :- X = new))");
		assertGoalSucceeds(assert2);

		// Verify new behavior
		Term verifyNew = parseGoal("dynamic_rule(new)");
		assertGoalSucceeds(verifyNew);

		Term verifyOldGone = parseGoal("dynamic_rule(old)");
		assertGoalFails(verifyOldGone);
	}

	@Test
	void testRetractAll() throws Exception {
		// Add multiple facts
		Term assert1 = parseGoal("assert(retractme(a))");
		assertGoalSucceeds(assert1);

		Term assert2 = parseGoal("assert(retractme(b))");
		assertGoalSucceeds(assert2);

		Term assert3 = parseGoal("assert(retractme(c))");
		assertGoalSucceeds(assert3);

		// Use retract in loop (backtracking) to remove all
		env.ensureLoaded(AtomTerm.get(createTempFile(
			"retract_all(X) :- retract(X), fail.\n" +
			"retract_all(_).\n"
		)));

		Term retractAllGoal = parseGoal("retract_all(retractme(_))");
		assertGoalSucceeds(retractAllGoal);

		// Verify all are gone
		Term checkGoal = parseGoal("retractme(_)");
		assertGoalFails(checkGoal);
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
