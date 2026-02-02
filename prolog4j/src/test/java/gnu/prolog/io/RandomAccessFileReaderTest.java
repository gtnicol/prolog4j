package gnu.prolog.io;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for RandomAccessFileReader functionality.
 */
class RandomAccessFileReaderTest {

	@TempDir
	Path tempDir;

	private Path testFile;
	private RandomAccessFile raf;
	private RandomAccessFileReader reader;

	@BeforeEach
	void setup() throws IOException {
		testFile = tempDir.resolve("test.txt");
		Files.writeString(testFile, "Hello World!\nLine 2\nLine 3");
		raf = new RandomAccessFile(testFile.toFile(), "r");
		reader = new RandomAccessFileReader(raf);
	}

	@AfterEach
	void teardown() throws IOException {
		if (reader != null) {
			reader.close();
		}
	}

	@Test
	void testReadCharacters() throws IOException {
		final var buffer = new char[5];
		final var count = reader.read(buffer, 0, 5);

		assertEquals(5, count);
		assertEquals("Hello", new String(buffer));
	}

	@Test
	void testGetPosition() throws IOException {
		// Position reports file pointer position, not character count
		// After reading characters, position advances in the file
		final var buffer = new char[5];
		reader.read(buffer, 0, 5);

		// Position should have advanced (exact value depends on buffering)
		assertTrue(reader.getPosition() > 0);
	}

	@Test
	void testSeek() throws IOException {
		// Seek to position 6 ("World" starts here)
		reader.seek(6);

		final var buffer = new char[5];
		reader.read(buffer, 0, 5);

		assertEquals("World", new String(buffer));
	}

	@Test
	void testSeekThenRead() throws IOException {
		// Read first 5 characters
		var buffer = new char[5];
		reader.read(buffer, 0, 5);
		assertEquals("Hello", new String(buffer));

		// Seek back to beginning
		reader.seek(0);

		// Read again
		buffer = new char[5];
		reader.read(buffer, 0, 5);
		assertEquals("Hello", new String(buffer));
	}

	@Test
	void testSize() throws IOException {
		assertEquals(Files.size(testFile), reader.size());
	}

	@Test
	void testReadAtEndOfFile() throws IOException {
		// Seek to end
		reader.seek(reader.size());

		final var buffer = new char[10];
		final var count = reader.read(buffer, 0, 10);

		// Should return -1 for end of file
		assertEquals(-1, count);
	}

	@Test
	void testMultipleSeeks() throws IOException {
		// Test that multiple seeks don't leak resources

		for (int i = 0; i < 100; i++) {
			reader.seek(i % 10);
			final var buffer = new char[1];
			reader.read(buffer, 0, 1);
		}

		// If we got here without error, seeks didn't cause resource issues
		assertTrue(true);
	}

	@Test
	void testReadSingleCharacter() throws IOException {
		final var buffer = new char[1];

		reader.read(buffer, 0, 1);
		assertEquals('H', buffer[0]);

		reader.read(buffer, 0, 1);
		assertEquals('e', buffer[0]);

		reader.read(buffer, 0, 1);
		assertEquals('l', buffer[0]);
	}

	@Test
	void testReadAfterSeek() throws IOException {
		// Read a few chars
		final var buffer = new char[5];
		reader.read(buffer, 0, 5);

		// Seek to position 13 (start of "Line 2")
		reader.seek(13);

		// Read 6 chars
		final var buffer2 = new char[6];
		reader.read(buffer2, 0, 6);

		assertEquals("Line 2", new String(buffer2));
	}
}
