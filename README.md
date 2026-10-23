Tool Belt (Fabric)
==================

A belt to carry your tools in. Swap the tool in your hand with one stored in your belt using a quick radial menu.

This is a Fabric port of [gigaherz's Tool Belt](https://github.com/gigaherz/ToolBelt) for **Minecraft 26.3**.

> **Status: early development.** The mod builds and loads, but hasn't been fully tested in-game yet. Back up your worlds before trying it.

## Installing (players)

You need:

- Minecraft **26.3**
- [Fabric Loader](https://fabricmc.net/use/installer/) **0.20.1** or newer
- [Fabric API](https://modrinth.com/mod/fabric-api) **0.165.0+26.3** or newer
- Java **25** (the official Minecraft launcher includes it)

Steps:

1. Run the [Fabric installer](https://fabricmc.net/use/installer/) and pick Minecraft 26.3. This adds a "fabric-loader-26.3" profile to your launcher.
2. Download the Tool Belt jar (`ToolBelt-Fabric-26.3-<version>.jar`) from this repository's [Releases page](https://github.com/Mason-Stahl/ToolBelt/releases). If there's no release yet, build it yourself (see [Building from source](#building-from-source)).
3. Download the Fabric API jar for 26.3.
4. Put both jars in your `mods` folder:
   - Windows: `%appdata%\.minecraft\mods`
   - macOS: `~/Library/Application Support/minecraft/mods`
   - Linux: `~/.minecraft/mods`

   Create the folder if it doesn't exist.
5. Start Minecraft with the Fabric profile.

On a multiplayer server, both the server and every player need the mod and Fabric API installed.

## How to use

- **Craft a belt:** string, leather and an iron ingot.

  ```
  S L S      S = String
  L   L      L = Leather
  L I L      I = Iron Ingot
  ```

- **Craft a pouch:** string, leather and a gold ingot.

  ```
  S G S      G = Gold Ingot
  L   L
  S L S
  ```

- **Upgrade a belt:** put a belt and a pouch anywhere in a crafting grid. Each upgrade adds one slot (2 slots to start, up to 9). The belt keeps everything stored in it.
- **Dye a belt:** craft it with any dye, like leather armor.
- **Fill the belt:** hold it and right-click to open its inventory. By default it only accepts items that don't stack (tools, weapons and so on).
- **Belt slot:** your inventory screen has a dedicated belt slot, just above the offhand slot (in the creative inventory tab it's on the other side of the player model). A belt there doesn't use up an inventory space. Shift-click a belt to move it there. It drops on death like the rest of your inventory (unless `keepInventory` is on), and goes straight back into the belt slot when you pick it up again, even if your inventory is full. Grave mods and data packs that save your inventory, such as Vanilla Tweaks Graves, store it in your grave and give it back to the belt slot.
- **Swap tools:** keep the belt in the belt slot (or anywhere in your inventory). If you have more than one belt, the one in the belt slot is used. Hold **R** to open the radial menu, move the mouse over a slot, and let go of R to swap it with the item in your hand. Letting go in the middle, with nothing highlighted, cancels.

Keys can be changed under Options → Controls → Key Binds → Tool Belt. There are also optional "cycle left/right" keys, which are unbound by default.

## Configuration

Config files are created in your `config` folder the first time you launch the game.

`toolbelt.json` sets which items the belt accepts. On a multiplayer server, the server's copy is the one that applies.

| Option | Default | What it does |
|---|---|---|
| `whitelist` | `[]` | Item IDs always allowed, e.g. `"minecraft:torch"` |
| `blacklist` | `[]` | Item IDs never allowed (the whitelist wins over this) |
| `allowAllNonStackableItems` | `true` | If `false`, only whitelisted items are allowed |

`toolbelt-client.json` controls the radial menu on your own client.

| Option | Default | What it does |
|---|---|---|
| `releaseToSwap` | `true` | Releasing the key swaps to the hovered slot. Set to `false` to require a click instead |
| `clipMouseToCircle` | `false` | Keeps the mouse inside the menu ring |
| `allowClickOutsideBounds` | `false` | Clicking outside the ring still picks the slot in that direction |
| `displayEmptySlots` | `false` | Always shows empty slots, so you can choose where to insert |
| `minecraftHasNoCircles` | `false` | Draws the menu as squares instead of a ring |

## Building from source

You only need this if you want to build the jar yourself. Players using a released jar can skip it.

You need [Java 25 (JDK)](https://adoptium.net/) and Git.

```sh
git clone -b fabric https://github.com/Mason-Stahl/ToolBelt.git
cd ToolBelt
./gradlew build          # on Windows: gradlew.bat build
```

The jar is written to `build/libs/ToolBelt-Fabric-26.3-<version>.jar`. Use that one, not the `-sources` jar.

To test in a development client or server:

```sh
./gradlew runClient
./gradlew runServer
./gradlew runClientGameTest   # automated tests; screenshots in build/run/clientGameTest/screenshots
```

## Credits and license

Original mod by [gigaherz](https://github.com/gigaherz/ToolBelt). Fabric port by Mason-Stahl. Licensed under BSD-3-Clause; see [LICENSE.txt](LICENSE.txt).
