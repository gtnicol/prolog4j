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
 * ISO Prolog compliance tests for list operations.
 *
 * Tests cover:
 * - append/3, member/2, length/2
 * - reverse/2, nth/3, last/2
 * - is_list/1, proper_list/1
 * - findall/3, bagof/3, setof/3
 * - sort/2, msort/2, keysort/2
 *
 * Based on ISO/IEC 13211-1 (ISO Prolog standard) and common extensions
 */
class ISOListTest {

	private Environment env;
	private Interpreter interpreter;

	@BeforeEach
	void setup() {
		env = new Environment();
		interpreter = env.createInterpreter();
	}

	// ========== append/3 tests ==========

	@Test
	void testAppendTwoLists() throws Exception {
		Term goal = parseGoal("append([1, 2], [3, 4], X), X = [1, 2, 3, 4]");
		assertGoalSucceeds(goal);
	}

	@Test
	void testAppendEmptyFirst() throws Exception {
		Term goal = parseGoal("append([], [1, 2], X), X = [1, 2]");
		assertGoalSucceeds(goal);
	}

	@Test
	void testAppendEmptySecond() throws Exception {
		Term goal = parseGoal("append([1, 2], [], X), X = [1, 2]");
		assertGoalSucceeds(goal);
	}

	@Test
	void testAppendBothEmpty() throws Exception {
		Term goal = parseGoal("append([], [], X), X = []");
		assertGoalSucceeds(goal);
	}

	@Test
	void testAppendSplit() throws Exception {
		Term goal = parseGoal("append(X, Y, [1, 2, 3]), X = [1], Y = [2, 3]");
		assertGoalSucceeds(goal);
	}

	@Test
	void testAppendCheck() throws Exception {
		Term goal = parseGoal("append([1, 2], [3, 4], [1, 2, 3, 4])");
		assertGoalSucceeds(goal);
	}

	// ========== member/2 tests ==========

	@Test
	void testMemberExists() throws Exception {
		Term goal = parseGoal("member(2, [1, 2, 3])");
		assertGoalSucceeds(goal);
	}

	@Test
	void testMemberDoesNotExist() throws Exception {
		Term goal = parseGoal("member(4, [1, 2, 3])");
		assertGoalFails(goal);
	}

	@Test
	void testMemberFirst() throws Exception {
		Term goal = parseGoal("member(1, [1, 2, 3])");
		assertGoalSucceeds(goal);
	}

	@Test
	void testMemberLast() throws Exception {
		Term goal = parseGoal("member(3, [1, 2, 3])");
		assertGoalSucceeds(goal);
	}

	@Test
	void testMemberWithVariable() throws Exception {
		Term goal = parseGoal("member(X, [1, 2, 3]), X = 1");
		assertGoalSucceeds(goal);
	}

	@Test
	void testMemberEmptyList() throws Exception {
		Term goal = parseGoal("member(X, [])");
		assertGoalFails(goal);
	}

	// ========== length/2 tests ==========

	@Test
	void testLengthOfList() throws Exception {
		Term goal = parseGoal("length([1, 2, 3], X), X = 3");
		assertGoalSucceeds(goal);
	}

	@Test
	void testLengthOfEmptyList() throws Exception {
		Term goal = parseGoal("length([], X), X = 0");
		assertGoalSucceeds(goal);
	}

	@Test
	void testLengthGenerateList() throws Exception {
		Term goal = parseGoal("length(X, 3), X = [_, _, _]");
		assertGoalSucceeds(goal);
	}

	@Test
	void testLengthCheck() throws Exception {
		Term goal = parseGoal("length([a, b, c, d], 4)");
		assertGoalSucceeds(goal);
	}

	@Test
	void testLengthCheckFails() throws Exception {
		Term goal = parseGoal("length([a, b, c], 5)");
		assertGoalFails(goal);
	}

	// ========== reverse/2 tests ==========

	@Test
	void testReverse() throws Exception {
		Term goal = parseGoal("reverse([1, 2, 3], X), X = [3, 2, 1]");
		assertGoalSucceeds(goal);
	}

	@Test
	void testReverseEmpty() throws Exception {
		Term goal = parseGoal("reverse([], X), X = []");
		assertGoalSucceeds(goal);
	}

	@Test
	void testReverseSingle() throws Exception {
		Term goal = parseGoal("reverse([a], X), X = [a]");
		assertGoalSucceeds(goal);
	}

	@Test
	void testReverseBidirectional() throws Exception {
		Term goal = parseGoal("reverse([1, 2, 3], X), reverse(X, Y), Y = [1, 2, 3]");
		assertGoalSucceeds(goal);
	}

	// ========== is_list/1 tests ==========

	@Test
	void testIsListTrue() throws Exception {
		Term goal = parseGoal("is_list([1, 2, 3])");
		assertGoalSucceeds(goal);
	}

	@Test
	void testIsListEmpty() throws Exception {
		Term goal = parseGoal("is_list([])");
		assertGoalSucceeds(goal);
	}

	@Test
	void testIsListAtom() throws Exception {
		Term goal = parseGoal("is_list(atom)");
		assertGoalFails(goal);
	}

	@Test
	void testIsListImproper() throws Exception {
		// [1, 2|tail] is not a proper list
		Term goal = parseGoal("is_list([1, 2|atom])");
		assertGoalFails(goal);
	}

	// ========== findall/3 tests ==========

	@Test
	void testFindallBasic() throws Exception {
		env.ensureLoaded(AtomTerm.get(createTempFile(
			"num(1).\n" +
			"num(2).\n" +
			"num(3).\n"
		)));

		Term goal = parseGoal("findall(X, num(X), L), L = [1, 2, 3]");
		assertGoalSucceeds(goal);
	}

	@Test
	void testFindallNoSolutions() throws Exception {
		Term goal = parseGoal("findall(X, fail, L), L = []");
		assertGoalSucceeds(goal);
	}

	@Test
	void testFindallWithCondition() throws Exception {
		env.ensureLoaded(AtomTerm.get(createTempFile(
			"value(1).\n" +
			"value(2).\n" +
			"value(3).\n" +
			"value(4).\n"
		)));

		Term goal = parseGoal("findall(X, (value(X), X > 2), L), L = [3, 4]");
		assertGoalSucceeds(goal);
	}

	@Test
	void testFindallTemplate() throws Exception {
		env.ensureLoaded(AtomTerm.get(createTempFile(
			"person(john, 25).\n" +
			"person(mary, 30).\n" +
			"person(bob, 35).\n"
		)));

		Term goal = parseGoal("findall(Name, person(Name, _), L), L = [john, mary, bob]");
		assertGoalSucceeds(goal);
	}

	@Test
	void testFindallComplex() throws Exception {
		env.ensureLoaded(AtomTerm.get(createTempFile(
			"person(john, 25).\n" +
			"person(mary, 30).\n"
		)));

		Term goal = parseGoal("findall(pair(N, A), person(N, A), L), " +
							   "L = [pair(john, 25), pair(mary, 30)]");
		assertGoalSucceeds(goal);
	}

	// ========== bagof/3 tests ==========

	@Test
	void testBagofBasic() throws Exception {
		env.ensureLoaded(AtomTerm.get(createTempFile(
			"data(a, 1).\n" +
			"data(a, 2).\n" +
			"data(b, 3).\n"
		)));

		Term goal = parseGoal("bagof(X, data(a, X), L), L = [1, 2]");
		assertGoalSucceeds(goal);
	}

	@Test
	void testBagofNoSolutions() throws Exception {
		Term goal = parseGoal("bagof(X, fail, L)");
		assertGoalFails(goal);
	}

	@Test
	void testBagofExistential() throws Exception {
		env.ensureLoaded(AtomTerm.get(createTempFile(
			"data(a, 1).\n" +
			"data(a, 2).\n" +
			"data(b, 3).\n"
		)));

		Term goal = parseGoal("bagof(X, K^data(K, X), L), length(L, 3)");
		assertGoalSucceeds(goal);
	}

	// ========== setof/3 tests ==========

	@Test
	void testSetofBasic() throws Exception {
		env.ensureLoaded(AtomTerm.get(createTempFile(
			"item(3).\n" +
			"item(1).\n" +
			"item(2).\n" +
			"item(1).\n"
		)));

		// setof should sort and remove duplicates
		Term goal = parseGoal("setof(X, item(X), L), L = [1, 2, 3]");
		assertGoalSucceeds(goal);
	}

	@Test
	void testSetofNoSolutions() throws Exception {
		Term goal = parseGoal("setof(X, fail, L)");
		assertGoalFails(goal);
	}

	// ========== sort/2 tests ==========

	@Test
	void testSort() throws Exception {
		Term goal = parseGoal("sort([3, 1, 2], X), X = [1, 2, 3]");
		assertGoalSucceeds(goal);
	}

	@Test
	void testSortWithDuplicates() throws Exception {
		// sort removes duplicates
		Term goal = parseGoal("sort([3, 1, 2, 1, 3], X), X = [1, 2, 3]");
		assertGoalSucceeds(goal);
	}

	@Test
	void testSortEmpty() throws Exception {
		Term goal = parseGoal("sort([], X), X = []");
		assertGoalSucceeds(goal);
	}

	@Test
	void testSortAtoms() throws Exception {
		Term goal = parseGoal("sort([c, a, b], X), X = [a, b, c]");
		assertGoalSucceeds(goal);
	}

	// ========== msort/2 tests ==========

	@Test
	void testMsort() throws Exception {
		Term goal = parseGoal("msort([3, 1, 2], X), X = [1, 2, 3]");
		assertGoalSucceeds(goal);
	}

	@Test
	void testMsortPreservesDuplicates() throws Exception {
		// msort keeps duplicates
		Term goal = parseGoal("msort([3, 1, 2, 1], X), X = [1, 1, 2, 3]");
		assertGoalSucceeds(goal);
	}

	// ========== nth/3 tests (extension) ==========

	@Test
	void testNthFirst() throws Exception {
		Term goal = parseGoal("nth(1, [a, b, c], X), X = a");
		assertGoalSucceeds(goal);
	}

	@Test
	void testNthMiddle() throws Exception {
		Term goal = parseGoal("nth(2, [a, b, c], X), X = b");
		assertGoalSucceeds(goal);
	}

	@Test
	void testNthLast() throws Exception {
		Term goal = parseGoal("nth(3, [a, b, c], X), X = c");
		assertGoalSucceeds(goal);
	}

	@Test
	void testNthOutOfBounds() throws Exception {
		Term goal = parseGoal("nth(4, [a, b, c], X)");
		assertGoalFails(goal);
	}

	// ========== last/2 tests (extension) ==========

	@Test
	void testLast() throws Exception {
		Term goal = parseGoal("last([1, 2, 3], X), X = 3");
		assertGoalSucceeds(goal);
	}

	@Test
	void testLastSingle() throws Exception {
		Term goal = parseGoal("last([a], X), X = a");
		assertGoalSucceeds(goal);
	}

	@Test
	void testLastEmpty() throws Exception {
		Term goal = parseGoal("last([], X)");
		assertGoalFails(goal);
	}

	// ========== Complex list operations ==========

	@Test
	void testNestedLists() throws Exception {
		Term goal = parseGoal("append([[1, 2], [3]], [[4, 5]], X), " +
							   "X = [[1, 2], [3], [4, 5]]");
		assertGoalSucceeds(goal);
	}

	@Test
	void testListFlattening() throws Exception {
		env.ensureLoaded(AtomTerm.get(createTempFile(
			"flatten([], []).\n" +
			"flatten([H|T], Flat) :- \n" +
			"    is_list(H), !,\n" +
			"    flatten(H, FH),\n" +
			"    flatten(T, FT),\n" +
			"    append(FH, FT, Flat).\n" +
			"flatten([H|T], [H|FT]) :- \n" +
			"    flatten(T, FT).\n"
		)));

		Term goal = parseGoal("flatten([[1, 2], [3, [4, 5]]], X), X = [1, 2, 3, 4, 5]");
		assertGoalSucceeds(goal);
	}

	@Test
	void testListMap() throws Exception {
		env.ensureLoaded(AtomTerm.get(createTempFile(
			"double(X, Y) :- Y is X * 2.\n" +
			"map(_, [], []).\n" +
			"map(Pred, [H|T], [RH|RT]) :- \n" +
			"    call(Pred, H, RH),\n" +
			"    map(Pred, T, RT).\n"
		)));

		Term goal = parseGoal("map(double, [1, 2, 3], X), X = [2, 4, 6]");
		assertGoalSucceeds(goal);
	}

	@Test
	void testListFilter() throws Exception {
		env.ensureLoaded(AtomTerm.get(createTempFile(
			"positive(X) :- X > 0.\n" +
			"filter(_, [], []).\n" +
			"filter(Pred, [H|T], [H|RT]) :- \n" +
			"    call(Pred, H), !,\n" +
			"    filter(Pred, T, RT).\n" +
			"filter(Pred, [_|T], RT) :- \n" +
			"    filter(Pred, T, RT).\n"
		)));

		Term goal = parseGoal("filter(positive, [-1, 2, -3, 4, 5], X), X = [2, 4, 5]");
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
