# Arena Fighter

**Arena Fighter** is a Minecraft Beta 1.7.3 plugin built on [Project Poseidon](https://github.com/retromcorg/Project-Poseidon). 
It enables players to duel each other in a dedicated arenas, providing a seamless PvP experience.

---

## Features

- Challenge other players to 1v1 fights
- Spectator support
- Automatic teleportation of players to the arena and back after the duel
- Economy support for wagers and bets [Planned feature- WIP]

---

## Requirements

- [Project Poseidon](https://github.com/retromcorg/Project-Poseidon)

---

## Installation

1. Download the compiled `ArenaFighter.jar` plugin.
2. Place the JAR file into your server’s `plugins` folder.
3. Ensure [Poseidon](https://github.com/retromcorg/Project-Poseidon) is also installed.
4. Restart the server to load the plugin.

---

## Usage

- Use `/arena create <name>` to set and create an arena, the data for which will be sent to 
 `plugins\ArenaFighter\arenas.txt` from there it can be edited
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
```
Drag and drop the plugin without the `original` prefix into your plugins folder and load the game
