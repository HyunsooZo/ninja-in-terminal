# 🥷 NinjaInTerminal

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
java -jar build/libs/NinjaInTerminal-1.0.0.jar
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

Config is stored at `~/.ninja-in-terminal/config.json`

## Completed Features

- ✅ JediTerm integration for proper terminal emulation
- ✅ SFTP file transfer
- ✅ Snippet management with packages
- ✅ Command Palette (CTRL+J)
- ✅ Settings UI (fonts, themes, SSH config)
- ✅ Startup command execution

## TODO

- [ ] Port forwarding (local, remote, dynamic)
- [ ] Workspaces (split view, focus mode)
- [ ] Keychain (SSH key management)
- [ ] Import/Export hosts

## License

MIT
