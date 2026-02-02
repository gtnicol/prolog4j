package gnu.prolog;

import gnu.prolog.term.*;
import gnu.prolog.vm.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Prolog term manipulation operations.
 */
class TermManipulationTest {

	private Environment env;

	@BeforeEach
	void setup() {
		env = new Environment();
	}

	@Test
	void testAtomTermCreation() {
		AtomTerm atom = AtomTerm.get("foo");
		assertEquals("foo", atom.value);
		assertEquals(TermType.ATOM, atom.getType());
	}

	@Test
	void testAtomTermSingleton() {
		AtomTerm atom1 = AtomTerm.get("foo");
		AtomTerm atom2 = AtomTerm.get("foo");
		assertSame(atom1, atom2, "AtomTerms should be singletons");
	}

	@Test
	void testIntegerTermCreation() {
		IntegerTerm num = IntegerTerm.get(42);
		assertEquals(42, num.value);
		assertEquals(TermType.INTEGER, num.getType());
	}

	@Test
	void testFloatTermCreation() {
		FloatTerm num = new FloatTerm(3.14);
		assertEquals(3.14, num.value, 0.0001);
		assertEquals(TermType.FLOAT, num.getType());
	}

	@Test
	void testVariableTermCreation() {
		VariableTerm var = new VariableTerm("X");
		assertEquals("X", var.getName());
		assertEquals(TermType.VARIABLE, var.getType());
	}

	@Test
	void testCompoundTermCreation() {
		CompoundTerm ct = new CompoundTerm(
			CompoundTermTag.get("foo", 2),
			AtomTerm.get("a"),
			IntegerTerm.get(42)
		);
		assertEquals("foo", ct.tag.functor.value);
		assertEquals(2, ct.tag.arity);
		assertEquals(TermType.COMPOUND, ct.getType());
		assertEquals(2, ct.args.length);
	}

	@Test
	void testCompoundTermTagSingleton() {
		CompoundTermTag tag1 = CompoundTermTag.get("foo", 2);
		CompoundTermTag tag2 = CompoundTermTag.get("foo", 2);
		assertSame(tag1, tag2, "CompoundTermTags should be singletons");
	}

	@Test
	void testCompoundTermTagPredicateIndicator() {
		CompoundTermTag tag = CompoundTermTag.get("append", 3);
		CompoundTerm indicator = tag.getPredicateIndicator();

		assertEquals("/", indicator.tag.functor.value);
		assertEquals(2, indicator.tag.arity);
		assertEquals("append", ((AtomTerm) indicator.args[0]).value);
		assertEquals(3, ((IntegerTerm) indicator.args[1]).value);
	}

	@Test
	void testIsPredicateIndicator() {
		CompoundTerm validIndicator = new CompoundTerm(
			CompoundTermTag.get("/", 2),
			AtomTerm.get("foo"),
			IntegerTerm.get(2)
		);
		assertTrue(CompoundTermTag.isPredicateIndicator(validIndicator));

		AtomTerm notIndicator = AtomTerm.get("foo");
		assertFalse(CompoundTermTag.isPredicateIndicator(notIndicator));
	}

	@Test
	void testTermClone() {
		// Test cloning a compound term with variables
		VariableTerm varX = new VariableTerm("X");
		VariableTerm varY = new VariableTerm("Y");

		CompoundTerm original = new CompoundTerm(
			CompoundTermTag.get("foo", 2),
			varX,
			varY
		);

		TermCloneContext cloneContext = new TermCloneContext();
		Term cloned = original.clone(cloneContext);

		assertInstanceOf(CompoundTerm.class, cloned);
		CompoundTerm clonedCt = (CompoundTerm) cloned;

		// Tag should be the same (singleton)
		assertSame(original.tag, clonedCt.tag);

		// Variables should be cloned
		assertInstanceOf(VariableTerm.class, clonedCt.args[0]);
		assertInstanceOf(VariableTerm.class, clonedCt.args[1]);

		VariableTerm clonedX = (VariableTerm) clonedCt.args[0];
		VariableTerm clonedY = (VariableTerm) clonedCt.args[1];

		assertNotSame(varX, clonedX);
		assertNotSame(varY, clonedY);
		assertEquals(varX.getName(), clonedX.getName());
		assertEquals(varY.getName(), clonedY.getName());
	}

	@Test
	void testTermDereference() {
		// Test variable dereferencing through value chain
		VariableTerm varX = new VariableTerm("X");
		VariableTerm varY = new VariableTerm("Y");
		AtomTerm atom = AtomTerm.get("foo");

		// X is unbound
		Term deref = varX.dereference();
		assertSame(varX, deref);

		// Bind X to Y
		varX.value = varY;
		deref = varX.dereference();
		assertSame(varY, deref);

		// Bind Y to 'foo'
		varY.value = atom;
		deref = varX.dereference();
		assertSame(atom, deref);
	}

	@Test
	void testListConstruction() {
		// Build list [1, 2, 3]
		CompoundTerm list = new CompoundTerm(
			CompoundTermTag.get(".", 2),
			IntegerTerm.get(1),
			new CompoundTerm(
				CompoundTermTag.get(".", 2),
				IntegerTerm.get(2),
				new CompoundTerm(
					CompoundTermTag.get(".", 2),
					IntegerTerm.get(3),
					AtomTerm.get("[]")
				)
			)
		);

		assertEquals(".", list.tag.functor.value);
		assertEquals(2, list.tag.arity);

		// First element is 1
		assertEquals(IntegerTerm.get(1), list.args[0]);

		// Tail is [2, 3]
		assertInstanceOf(CompoundTerm.class, list.args[1]);
	}

	@Test
	void testEmptyList() {
		AtomTerm emptyList = AtomTerm.get("[]");
		assertEquals("[]", emptyList.value);
		assertEquals(TermType.ATOM, emptyList.getType());
	}

	@Test
	void testListWithTail() {
		// Build list [1 | T]
		VariableTerm tail = new VariableTerm("T");
		CompoundTerm list = new CompoundTerm(
			CompoundTermTag.get(".", 2),
			IntegerTerm.get(1),
			tail
		);

		assertEquals(".", list.tag.functor.value);
		assertEquals(IntegerTerm.get(1), list.args[0]);
		assertSame(tail, list.args[1]);
	}

	@Test
	void testTermEquality() {
		// Atoms with same value are equal
		assertEquals(AtomTerm.get("foo"), AtomTerm.get("foo"));

		// Integers with same value are equal
		assertEquals(IntegerTerm.get(42), IntegerTerm.get(42));

		// Floats with same value are equal
		assertEquals(new FloatTerm(3.14), new FloatTerm(3.14));

		// Different atoms are not equal
		assertNotEquals(AtomTerm.get("foo"), AtomTerm.get("bar"));

		// Different integers are not equal
		assertNotEquals(IntegerTerm.get(42), IntegerTerm.get(17));
	}

	@Test
	void testTermHashCode() {
		// Same atoms should have same hash code
		assertEquals(
			AtomTerm.get("foo").hashCode(),
			AtomTerm.get("foo").hashCode()
		);

		// Same integers should have same hash code
		assertEquals(
			IntegerTerm.get(42).hashCode(),
			IntegerTerm.get(42).hashCode()
		);
	}

	@Test
	void testCompoundTermEquality() {
		CompoundTerm ct1 = new CompoundTerm(
			CompoundTermTag.get("foo", 2),
			AtomTerm.get("a"),
			IntegerTerm.get(42)
		);

		CompoundTerm ct2 = new CompoundTerm(
			CompoundTermTag.get("foo", 2),
			AtomTerm.get("a"),
			IntegerTerm.get(42)
		);

		// Compound terms are not reference equal
		assertNotSame(ct1, ct2);

		// Same tag and same arguments
		assertSame(ct1.tag, ct2.tag);
		assertEquals(ct1.args.length, ct2.args.length);
		assertEquals(ct1.args[0], ct2.args[0]);
		assertEquals(ct1.args[1], ct2.args[1]);
	}

	@Test
	void testNestedTermConstruction() {
		// Build: foo(bar(baz))
		CompoundTerm inner = new CompoundTerm(
			CompoundTermTag.get("bar", 1),
			AtomTerm.get("baz")
		);

		CompoundTerm outer = new CompoundTerm(
			CompoundTermTag.get("foo", 1),
			inner
		);

		assertEquals("foo", outer.tag.functor.value);
		assertEquals(1, outer.tag.arity);
		assertSame(inner, outer.args[0]);

		assertEquals("bar", inner.tag.functor.value);
		assertEquals(AtomTerm.get("baz"), inner.args[0]);
	}

	@Test
	void testCharAtomTerm() {
		AtomTerm charAtom = AtomTerm.get('a');
		assertEquals("a", charAtom.value);
		assertEquals(TermType.ATOM, charAtom.getType());

		// Same character should return same atom
		assertSame(AtomTerm.get('a'), AtomTerm.get('a'));
	}

	@Test
	void testStandardCompoundTermTags() {
		// Test predefined tags
		assertEquals(",", CompoundTermTag.comma.functor.value);
		assertEquals(2, CompoundTermTag.comma.arity);

		assertEquals("{}", CompoundTermTag.curly1.functor.value);
		assertEquals(1, CompoundTermTag.curly1.arity);

		assertEquals("-", CompoundTermTag.minus2.functor.value);
		assertEquals(2, CompoundTermTag.minus2.arity);

		assertEquals("/", CompoundTermTag.divide2.functor.value);
		assertEquals(2, CompoundTermTag.divide2.arity);
	}

	@Test
	void testNumericTermInterface() {
		IntegerTerm intTerm = IntegerTerm.get(42);
		assertInstanceOf(NumericTerm.class, intTerm);

		FloatTerm floatTerm = new FloatTerm(3.14);
		assertInstanceOf(NumericTerm.class, floatTerm);
	}

	@Test
	void testAtomicTermInterface() {
		AtomTerm atom = AtomTerm.get("foo");
		assertInstanceOf(AtomicTerm.class, atom);

		IntegerTerm num = IntegerTerm.get(42);
		assertInstanceOf(AtomicTerm.class, num);

		FloatTerm flt = new FloatTerm(3.14);
		assertInstanceOf(AtomicTerm.class, flt);
	}
}
