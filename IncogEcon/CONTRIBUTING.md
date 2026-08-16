# Contributing

Thanks for helping improve IncogEcon.

## Before opening a pull request

1. Use Java 25+.
2. Run `mvn clean package`.
3. Preserve existing persistent data formats unless the change includes a safe migration.
4. Do not make player-facing functionality depend on Java-only inventory gestures; normal player workflows must remain cross-platform-friendly.
5. Keep `incogshop.*` permission nodes backward compatible unless a migration plan is included.
6. Avoid unrelated rewrites when fixing a focused issue.

## Pull requests

Describe:

- What changed
- Why it changed
- How it was tested
- Whether config/data migration is required
- Whether Java and Bedrock player workflows are affected
