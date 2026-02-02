package gnu.prolog.io;

import gnu.prolog.term.AtomTerm;
import gnu.prolog.term.IntegerTerm;
import gnu.prolog.term.JavaObjectTerm;
import gnu.prolog.term.Term;
import gnu.prolog.vm.Environment;
import gnu.prolog.vm.PrologException;
import gnu.prolog.vm.TermConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for BinaryPrologStream functionality.
 */
class BinaryPrologStreamTest {

	@TempDir
	Path tempDir;

	private Environment env;
	private Path testFile;

	@BeforeEach
	void setup() throws IOException {
		env = new Environment();
		testFile = tempDir.resolve("test.bin");
		Files.createFile(testFile);
	}

	@AfterEach
	void teardown() throws IOException {
		if (env != null) {
			env.closeStreams();
		}
	}

	@Test
	void testWriteAndReadByte() throws PrologException, IOException {
		final var filename = AtomTerm.get(testFile.toString());

		// Write mode
		final var writeOptions = new PrologStream.OpenOptions(filename, PrologStream.writeAtom, env);
		writeOptions.type = PrologStream.binaryAtom;
		final var writeStream = new BinaryPrologStream(filename, PrologStream.writeAtom, writeOptions);

		final var streamTerm = writeStream.getStreamTerm();
		writeStream.putByte(streamTerm, null, 65);
		writeStream.putByte(streamTerm, null, 66);
		writeStream.putByte(streamTerm, null, 67);
		writeStream.close(true);

		// Read mode
		final var readOptions = new PrologStream.OpenOptions(filename, PrologStream.readAtom, env);
		readOptions.type = PrologStream.binaryAtom;
		final var readStream = new BinaryPrologStream(filename, PrologStream.readAtom, readOptions);

		final var readStreamTerm = readStream.getStreamTerm();
		assertEquals(65, readStream.getByte(readStreamTerm, null));
		assertEquals(66, readStream.getByte(readStreamTerm, null));
		assertEquals(67, readStream.getByte(readStreamTerm, null));
		assertEquals(-1, readStream.getByte(readStreamTerm, null)); // EOF
		readStream.close(true);
	}

	@Test
	void testPeekByte() throws PrologException, IOException {
		// Write test data
		Files.write(testFile, new byte[]{10, 20, 30});

		final var filename = AtomTerm.get(testFile.toString());
		final var options = new PrologStream.OpenOptions(filename, PrologStream.readAtom, env);
		options.type = PrologStream.binaryAtom;
		final var stream = new BinaryPrologStream(filename, PrologStream.readAtom, options);

		final var streamTerm = stream.getStreamTerm();

		// Peek should return first byte without consuming it
		assertEquals(10, stream.peekByte(streamTerm, null));
		assertEquals(10, stream.peekByte(streamTerm, null)); // Same byte
		assertEquals(10, stream.getByte(streamTerm, null));   // Now consume it
		assertEquals(20, stream.getByte(streamTerm, null));

		stream.close(true);
	}

	@Test
	void testGetPosition() throws PrologException, IOException {
		Files.write(testFile, new byte[]{1, 2, 3, 4, 5});

		final var filename = AtomTerm.get(testFile.toString());
		final var options = new PrologStream.OpenOptions(filename, PrologStream.readAtom, env);
		options.type = PrologStream.binaryAtom;
		options.reposition = TermConstants.trueAtom;
		final var stream = new BinaryPrologStream(filename, PrologStream.readAtom, options);

		final var streamTerm = stream.getStreamTerm();

		// Initial position
		final var pos0 = stream.getPosition(streamTerm, null);
		assertInstanceOf(JavaObjectTerm.class, pos0);
		assertEquals(0L, ((JavaObjectTerm) pos0).value);

		// After reading 3 bytes
		stream.getByte(streamTerm, null);
		stream.getByte(streamTerm, null);
		stream.getByte(streamTerm, null);

		final var pos3 = stream.getPosition(streamTerm, null);
		assertEquals(3L, ((JavaObjectTerm) pos3).value);

		stream.close(true);
	}

	@Test
	void testSetPosition() throws PrologException, IOException {
		Files.write(testFile, new byte[]{10, 20, 30, 40, 50});

		final var filename = AtomTerm.get(testFile.toString());
		final var options = new PrologStream.OpenOptions(filename, PrologStream.readAtom, env);
		options.type = PrologStream.binaryAtom;
		options.reposition = TermConstants.trueAtom;
		final var stream = new BinaryPrologStream(filename, PrologStream.readAtom, options);

		final var streamTerm = stream.getStreamTerm();

		// Read first 3 bytes
		stream.getByte(streamTerm, null);
		stream.getByte(streamTerm, null);
		stream.getByte(streamTerm, null);

		// Seek back to position 1
		stream.setPosition(streamTerm, null, new JavaObjectTerm(1L));

		// Should now read byte at position 1
		assertEquals(20, stream.getByte(streamTerm, null));

		stream.close(true);
	}

	@Test
	void testEndOfStreamStates() throws PrologException, IOException {
		Files.write(testFile, new byte[]{42});

		final var filename = AtomTerm.get(testFile.toString());
		final var options = new PrologStream.OpenOptions(filename, PrologStream.readAtom, env);
		options.type = PrologStream.binaryAtom;
		final var stream = new BinaryPrologStream(filename, PrologStream.readAtom, options);

		final var streamTerm = stream.getStreamTerm();

		// Before reading, should be "not"
		assertEquals(PrologStream.notAtom, stream.getEndOfStreamState());

		// Read the only byte
		final var result = stream.getByte(streamTerm, null);
		assertEquals(42, result);

		// Now at EOF
		assertEquals(PrologStream.atAtom, stream.getEndOfStreamState());

		stream.close(true);
	}

	@Test
	void testTextOperationThrowsException() throws PrologException, IOException {
		final var filename = AtomTerm.get(testFile.toString());
		final var options = new PrologStream.OpenOptions(filename, PrologStream.readAtom, env);
		options.type = PrologStream.binaryAtom;
		final var stream = new BinaryPrologStream(filename, PrologStream.readAtom, options);

		final var streamTerm = stream.getStreamTerm();

		// Text operations should throw permission error on binary stream
		assertThrows(PrologException.class, () -> stream.getCode(streamTerm, null));
		assertThrows(PrologException.class, () -> stream.peekCode(streamTerm, null));
		assertThrows(PrologException.class, () -> stream.putCode(streamTerm, null, 65));
		assertThrows(PrologException.class, () -> stream.putCodeSequence(streamTerm, null, "test"));

		stream.close(true);
	}

	@Test
	void testAppendMode() throws PrologException, IOException {
		// Write initial data
		Files.write(testFile, new byte[]{1, 2, 3});

		final var filename = AtomTerm.get(testFile.toString());
		final var options = new PrologStream.OpenOptions(filename, PrologStream.appendAtom, env);
		options.type = PrologStream.binaryAtom;
		final var stream = new BinaryPrologStream(filename, PrologStream.appendAtom, options);

		final var streamTerm = stream.getStreamTerm();
		stream.putByte(streamTerm, null, 4);
		stream.putByte(streamTerm, null, 5);
		stream.close(true);

		// Verify file now has 5 bytes
		final var content = Files.readAllBytes(testFile);
		assertEquals(5, content.length);
		assertArrayEquals(new byte[]{1, 2, 3, 4, 5}, content);
	}

	@Test
	void testWriteModeTruncates() throws PrologException, IOException {
		// Write initial data
		Files.write(testFile, new byte[]{1, 2, 3, 4, 5});

		final var filename = AtomTerm.get(testFile.toString());
		final var options = new PrologStream.OpenOptions(filename, PrologStream.writeAtom, env);
		options.type = PrologStream.binaryAtom;
		final var stream = new BinaryPrologStream(filename, PrologStream.writeAtom, options);

		final var streamTerm = stream.getStreamTerm();
		stream.putByte(streamTerm, null, 99);
		stream.close(true);

		// File should now only have 1 byte
		final var content = Files.readAllBytes(testFile);
		assertEquals(1, content.length);
		assertEquals(99, content[0]);
	}
}
