# Installation and Upgrade Guide

## Requirements

- Paper/Purpur 26.2
- Java 25+
- Vault
- A Vault-compatible economy provider when using the default `economy.mode: VAULT`

Optional:

- DiscordSRV 1.30.5 for Discord price commands/history
- Geyser/Floodgate for Bedrock connectivity

## Fresh install

1. Stop the server.
2. Install Vault and a compatible economy provider.
3. Copy `IncogEcon-1.9.0.jar` into `plugins/`.
4. Start the server.
5. Review `plugins/IncogEcon/config.yml`.
6. Configure optional Discord integration if desired.
7. Grant staff permissions through LuckPerms or your permission manager.

## Updating IncogEcon

1. Stop the server.
2. Back up `plugins/IncogEcon/`.
3. Replace the old IncogEcon JAR with the new one.
4. Ensure only one IncogEcon/Incog-Shop JAR is present.
5. Start the server and review the log.

Persistent files are preserved between versions.

## Migrating from Incog-Shop

IncogEcon retains the old Java package/permission namespace for compatibility, but its Bukkit plugin name changed from `Incog-Shop` to `IncogEcon`.

On first startup, IncogEcon checks for:

```text
plugins/Incog-Shop/
```

If found, missing files are copied into:

```text
plugins/IncogEcon/
```

The old `plugins/Incog-Shop/` folder is left untouched as a backup. A migration marker is written so the process is not repeated unnecessarily.

Remove the old Incog-Shop JAR before starting IncogEcon so both plugins are never loaded together.

## Cross-platform servers

IncogEcon does not require Geyser/Floodgate APIs. Configure those separately. Normal player-facing IncogEcon features are designed around ordinary buttons/chat prompts so they do not require Java-only inventory gestures.

See `CROSS-PLATFORM.md`.

## Vault note

`plugin.yml` currently declares Vault as a hard dependency, so Vault must be installed for the plugin to load. In `VAULT` economy mode, Vault must also expose a working economy provider or IncogEcon disables itself to protect player balances.
