# GitHub Repository Setup

This source package has already been arranged to work as a normal Git repository.

## Suggested repository name

```text
IncogEcon
```

## Suggested description

> Cross-platform-friendly Minecraft economy plugin for Paper/Purpur with a Bazaar market, Buy/Sell Orders, auctions, player shops, secure trading, Sell Wands, stash, and XP banking.

## Suggested GitHub topics

```text
minecraft
minecraft-plugin
paper
purpur
economy
vault
bazaar
auction-house
geyser
bedrock
java
maven
minecraft-server
```

## First push

From the extracted `IncogEcon` folder:

```bash
git init
git add .
git commit -m "Initial IncogEcon 1.8.19 release"
git branch -M main
git remote add origin <your-repository-url>
git push -u origin main
```

## Branch protection suggestion

For a public/team repository, consider requiring the `Build` GitHub Action to pass before merging to `main`.

## Releases

Recommended tag format:

```text
v1.8.19
```

Create a GitHub Release from the tag and attach:

```text
IncogEcon-1.8.19.jar
```

Do not commit `target/` or compiled JARs into normal source commits; `.gitignore` excludes them.

## Before making the repository public

- Choose and add a real `LICENSE` file (see `docs/LICENSING.md`).
- Replace any placeholder repository/contact URLs you may add later.
- Confirm the server/version support statement you want to advertise.
- Build with JDK 25 and run a clean test server start.
- Test both Java and Bedrock player paths on your Geyser setup.
- Create the first release/tag.
