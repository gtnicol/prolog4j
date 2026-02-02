package gnu.prolog.term;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TermComparatorTest {

	@Test
	void testVariableOrderingStableAcrossComparators() {
		VariableTerm x = new VariableTerm("X");
		VariableTerm y = new VariableTerm("Y");

		TermComparator c1 = new TermComparator();
		int xy = c1.compare(x, y);
		assertNotEquals(0, xy, "Distinct variables should not compare as equal");

		TermComparator c2 = new TermComparator();
		int xy2 = c2.compare(x, y);
		assertEquals(Integer.signum(xy), Integer.signum(xy2));

		int yx = c2.compare(y, x);
		assertEquals(-Integer.signum(xy2), Integer.signum(yx));
	}

	@Test
	void testNumericOrderingUsesValueAcrossTypes() {
		TermComparator comparator = new TermComparator();

		assertTrue(comparator.compare(new FloatTerm(2.0), IntegerTerm.get(1)) > 0);
		assertTrue(comparator.compare(IntegerTerm.get(1), new DecimalTerm("2.0")) < 0);
	}

	@Test
	void testIntegerComparisonAvoidsOverflow() {
		TermComparator comparator = new TermComparator();
		assertTrue(comparator.compare(IntegerTerm.get(Integer.MAX_VALUE), IntegerTerm.get(-1)) > 0);
	}
}
