# Building IncogEcon with Maven

## Requirements

- Maven 3.x
- JDK 25 or newer

The project compiles with:

```xml
<maven.compiler.release>25</maven.compiler.release>
```

## Arch Linux

```bash
sudo pacman -S maven jdk-openjdk
```

Check versions:

```bash
java -version
javac -version
mvn -version
```

If multiple JDKs are installed:

```bash
archlinux-java status
```

Select a JDK 25+ installation if necessary.

## Build

From the project root:

```bash
mvn clean package
```

Output:

```text
target/IncogEcon-1.9.0.jar
```

Only the JAR belongs in the server's `plugins/` directory. Vault is required at runtime. DiscordSRV is optional.

## CI

The repository includes `.github/workflows/build.yml`, which performs the Maven build with Java 25 on pushes and pull requests.
