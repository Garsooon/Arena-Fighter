# Arena Fighter

[![GitHub Release](https://img.shields.io/github/v/release/Garsooon/Arena-Fighter?label=release)](https://github.com/Garsooon/Arena-Fighter/releases/latest)
[![Downloads](https://img.shields.io/github/downloads/Garsooon/Arena-Fighter/total.svg?style=flat)](https://github.com/Garsooon/Arena-Fighter/releases)
[![GitHub Views](https://komarev.com/ghpvc/?username=Garsooon&label=Views&color=blue&style=flat)](https://github.com/Garsooon/Arena-Fighter)
[![GitHub Stars](https://img.shields.io/github/stars/Garsooon/Arena-Fighter?style=social)](https://github.com/Garsooon/Arena-Fighter/stargazers)

**Arena Fighter** is a Minecraft Beta 1.7.3 plugin built on [Project Poseidon](https://github.com/retromcorg/Project-Poseidon).  
It enables players to duel each other in dedicated arenas, providing a seamless PvP experience.

---

## Features

- Challenge other players to 1v1 fights
- Spectator support
- Automatic teleportation of players to the arena and back after the duel
- Configurable punishments for combat logging and command usage
- Economy support for wagers and bets
- Player Stats and leaderboard

---

## Requirements

- [Project Poseidon](https://github.com/retromcorg/Project-Poseidon)

---

## Installation

1. Download the compiled `ArenaFighter.jar` plugin.
2. Place the JAR file into your server’s `plugins` folder.
3. Ensure [Poseidon](https://github.com/retromcorg/Project-Poseidon) is also installed.
4. Restart the server to load the plugin.
5. Add the permission nodes `arenafighter.fight` or `arenafighter.admin` to your permission groups if applicable.

---

## Permission Nodes

`arenafighter.fight` - Gives access to use and fight in Arenas
`arenafighter.admin` - Gives access to admin commands such as Arena creation.

---

## Usage

- Use `/arena create <name>` to set and create an arena, then you may run</br>
`/arena setspawn <spawn1|spawn2|spectator>` to change the spawns of the arena.
- Use `/fight challenge <player>` to challenge another player to a duel.
- Upon accepting the challenge, both players are teleported to the arena.
- When one player dies, both are teleported back to their original locations.

---

## Development

This plugin is designed specifically for Beta 1.7.3 Minecraft and uses [Poseidon's](https://github.com/retromcorg/Project-Poseidon) event and plugin API for compatibility. It has not been tested on UberBukkit or any other forks of bukkit.

Contributing code is also welcome. When contributing please try to keep code readable by others by either keeping functions simple or commenting your code. You can find the current goals of the project at [Issue#1](https://github.com/Garsooon/Arena-Fighter/issues/1)

### Building

Make sure you have Maven installed and configured then run:

```bash
mvn clean package
