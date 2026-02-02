Prolog4J (GNU Prolog for Java) 0.1.0
------------------------------------

This is a conforming implementation of ISO Prolog standard part 1
in Java 21. Any outstanding bugs are listed in NEWS.txt and the
ANALYSIS.md file in this directory.

This package is a library designed to be embedded into Java
applications that need Prolog to solve tasks. The interpreter is
intended for applications where Prolog performs combinatory search
while Java handles the rest. The library allows easy communication
between Java and Prolog.

The library is released under LGPL terms.

## Documentation

- Javadoc API documentation
- Texinfo manual (manual.texinfo)
- Predicate reference (predicates.txt)
- Codebase analysis (ANALYSIS.md)

## Build

This project uses Maven. Java 21 is required.

```bash
# Compile
mvn clean compile

# Run tests
mvn test

# Package JAR
mvn clean package

# Generate Javadoc
mvn javadoc:javadoc
```

## Architecture

The codebase is organized as a Maven multi-module project:

- **prolog4j**: Core library (gnu.prolog.*)
- **prolog4j-cli**: Command-line interface

Key packages:
- gnu.prolog.vm: Virtual machine and interpreter
- gnu.prolog.term: Term representation (immutable)
- gnu.prolog.database: Module and predicate management
- gnu.prolog.io: I/O and parsing (JavaCC-generated)

## Known Limitations

- Bug #30568: findall type_error tests fail
- Bug #30335: arg/3 unification of variables inside compound terms
- Bug #30630: Potential deadlock with multithreaded access
- Bug #30780: Unicode input may cause parser infinite loop

See ANALYSIS.md for detailed bug analysis and recommendations.
