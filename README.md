# Airline Overbooking Manager

An academic Maven-based Java project scaffold for an airline overbooking management system. The initial version establishes the project structure only; business logic has not been implemented.

## Technology

- Java 21
- Apache Maven 3.9.16
- Python 3.13 and Docker are reserved for future project workflows; they are not configured or required by this Java-only scaffold.

## Build

With Java 21 and Maven 3.9.16 installed, run:

```bash
mvn clean install
```

## Run the desktop UI

Start the JavaFX application from the project root with:

```bash
mvn javafx:run
```

The SQLite database is created at `data/airline-overbooking.db` by default. Set
the `airline.database` system property to use a different database file. Python
must be available as `python` to run no-show estimates; the executable can be
overridden with the `airline.python` system property.

## Source layout

Java packages are under `src/main/java/com/airline/overbooking`:

- `model` - domain data types
- `service` - application services
- `repository` - data access abstractions
- `algorithm` - overbooking-related algorithms
- `controller` - application entry and request coordination
- `util` - shared utilities

Resources belong in `src/main/resources`; tests belong in `src/test/java`.