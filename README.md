# 🥷 TerminalInNinja

A modern SSH client for Windows, inspired by Termius.

## Features

- 🔐 SSH connection with password or private key authentication
- 📁 Host grouping/folders
- 🔍 Quick search
- 🎨 Dark theme (Termius-style)
- 💾 Local JSON config storage
- 🗂️ Multiple terminal tabs

## Requirements

- Java 21+
- Gradle 8.5+

## Build & Run

```bash
# Run directly
./gradlew run

# Build JAR
./gradlew jar

# Run JAR
java -jar build/libs/TerminalInNinja-1.0.0.jar
```

## Project Structure

```
src/main/java/com/ninja/terminal/
├── app/           # Application entry point
├── controller/    # JavaFX controllers
├── model/         # Data models (HostInfo, HostGroup, AppConfig)
├── service/       # Business logic (SSH, Config)
└── util/          # Utilities
```

## Configuration

Config is stored at `~/.terminalinninja/config.json`

## TODO

- [ ] JediTerm integration for proper terminal emulation
- [ ] SFTP file transfer
- [ ] Port forwarding
- [ ] Snippet management
- [ ] Import/Export hosts

## License

MIT
