package gnu.prolog;

import gnu.prolog.io.TermReader;
import gnu.prolog.term.*;
import gnu.prolog.vm.Environment;
import gnu.prolog.vm.PrologException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Prolog term parsing functionality.
 */
class ParsingTest {

	private Environment env;

	@BeforeEach
	void setup() {
		env = new Environment();
	}

	@Test
	void testParseAtom() throws Exception {
		Term term = parseTerm("atom");
		assertInstanceOf(AtomTerm.class, term);
		assertEquals("atom", ((AtomTerm) term).value);
	}

	@Test
	void testParseInteger() throws Exception {
		Term term = parseTerm("42");
		assertInstanceOf(IntegerTerm.class, term);
		assertEquals(42, ((IntegerTerm) term).value);
	}

	@Test
	void testParseNegativeInteger() throws Exception {
		Term term = parseTerm("-17");
		assertInstanceOf(IntegerTerm.class, term);
		assertEquals(-17, ((IntegerTerm) term).value);
	}

	@Test
	void testParseFloat() throws Exception {
		Term term = parseTerm("3.14");
		assertInstanceOf(FloatTerm.class, term);
		assertEquals(3.14, ((FloatTerm) term).value, 0.0001);
	}

	@Test
	void testParseVariable() throws Exception {
		Term term = parseTerm("X");
		assertInstanceOf(VariableTerm.class, term);
		assertEquals("X", ((VariableTerm) term).getName());
	}

	@Test
	void testParseAnonymousVariable() throws Exception {
		Term term = parseTerm("_");
		assertInstanceOf(VariableTerm.class, term);
		assertTrue(((VariableTerm) term).getName().startsWith("_"));
	}

	@Test
	void testParseSimpleCompound() throws Exception {
		Term term = parseTerm("foo(bar)");
		assertInstanceOf(CompoundTerm.class, term);
		CompoundTerm ct = (CompoundTerm) term;
		assertEquals("foo", ct.tag.functor.value);
		assertEquals(1, ct.tag.arity);
		assertInstanceOf(AtomTerm.class, ct.args[0]);
		assertEquals("bar", ((AtomTerm) ct.args[0]).value);
	}

	@Test
	void testParseCompoundWithMultipleArgs() throws Exception {
		Term term = parseTerm("append(X, Y, Z)");
		assertInstanceOf(CompoundTerm.class, term);
		CompoundTerm ct = (CompoundTerm) term;
		assertEquals("append", ct.tag.functor.value);
		assertEquals(3, ct.tag.arity);
		assertInstanceOf(VariableTerm.class, ct.args[0]);
		assertInstanceOf(VariableTerm.class, ct.args[1]);
		assertInstanceOf(VariableTerm.class, ct.args[2]);
	}

	@Test
	void testParseList() throws Exception {
		Term term = parseTerm("[1, 2, 3]");
		assertInstanceOf(CompoundTerm.class, term);

		// Lists are represented as ./2 compound terms
		CompoundTerm list = (CompoundTerm) term;
		assertEquals(".", list.tag.functor.value);
		assertEquals(2, list.tag.arity);

		// First element should be 1
		assertInstanceOf(IntegerTerm.class, list.args[0]);
		assertEquals(1, ((IntegerTerm) list.args[0]).value);
	}

	@Test
	void testParseEmptyList() throws Exception {
		Term term = parseTerm("[]");
		assertInstanceOf(AtomTerm.class, term);
		assertEquals("[]", ((AtomTerm) term).value);
	}

	@Test
	void testParseListWithTail() throws Exception {
		Term term = parseTerm("[H|T]");
		assertInstanceOf(CompoundTerm.class, term);
		CompoundTerm list = (CompoundTerm) term;
		assertEquals(".", list.tag.functor.value);
		assertInstanceOf(VariableTerm.class, list.args[0]); // H
		assertInstanceOf(VariableTerm.class, list.args[1]); // T
	}

	@Test
	void testParseNestedCompound() throws Exception {
		Term term = parseTerm("f(g(a), h(b, c))");
		assertInstanceOf(CompoundTerm.class, term);
		CompoundTerm ct = (CompoundTerm) term;
		assertEquals("f", ct.tag.functor.value);
		assertEquals(2, ct.tag.arity);

		// First arg is g(a)
		assertInstanceOf(CompoundTerm.class, ct.args[0]);
		CompoundTerm g = (CompoundTerm) ct.args[0];
		assertEquals("g", g.tag.functor.value);

		// Second arg is h(b, c)
		assertInstanceOf(CompoundTerm.class, ct.args[1]);
		CompoundTerm h = (CompoundTerm) ct.args[1];
		assertEquals("h", h.tag.functor.value);
		assertEquals(2, h.tag.arity);
	}

	@Test
	void testParseQuotedAtom() throws Exception {
		Term term = parseTerm("'Hello World'");
		assertInstanceOf(AtomTerm.class, term);
		assertEquals("Hello World", ((AtomTerm) term).value);
	}

	@Test
	void testParseOperators() throws Exception {
		// Arithmetic expression
		Term term = parseTerm("1 + 2");
		assertInstanceOf(CompoundTerm.class, term);
		CompoundTerm ct = (CompoundTerm) term;
		assertEquals("+", ct.tag.functor.value);
		assertEquals(2, ct.tag.arity);
	}

	@Test
	void testTermTypeEnum() throws Exception {
		assertEquals(TermType.ATOM, parseTerm("atom").getType());
		assertEquals(TermType.INTEGER, parseTerm("42").getType());
		assertEquals(TermType.FLOAT, parseTerm("3.14").getType());
		assertEquals(TermType.VARIABLE, parseTerm("X").getType());
		assertEquals(TermType.COMPOUND, parseTerm("f(x)").getType());
	}

	// Helper method to parse a term from string
	private Term parseTerm(final String input) throws Exception {
		// Add period terminator if not present
		String termInput = input.endsWith(".") ? input : input + ".";
		StringReader reader = new StringReader(termInput);
		TermReader termReader = new TermReader(reader, env);
		return termReader.readTerm(env.getOperatorSet());
	}
}
