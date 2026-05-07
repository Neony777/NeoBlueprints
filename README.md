# NeoBlueprints

A NeoForge 1.21.1 mod that locks crafting recipes behind **blueprint items**. Players must
find and consume a blueprint to permanently unlock specific recipes for their character.
Unlocks are per-player and persist across deaths and server restarts.

Inspired by [Amnezia](https://modrinth.com/mod/amnezia) (Fabric-only) — this is a native
NeoForge reimplementation.

---

## Requirements

| | |
|---|---|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.95+ |
| Java | 21 |

## Building

```bash
./gradlew build
```

First build downloads the NeoForge MDK and may take a few minutes. The jar lands in
`build/libs/neoblueprints-<version>.jar`.

## Running in dev

```bash
./gradlew runClient   # single-player test
./gradlew runServer   # dedicated server (accept EULA in run/eula.txt first)
```

---

## Configuration

On first server launch, `config/neoblueprints.json` is created automatically:

```json
{
  "_comment": [
    "Each entry defines one blueprint item.",
    "  id      - unique blueprint id ('neoblueprints:<name>')",
    "  name    - display name shown on the item",
    "  rarity  - common | uncommon | rare | epic | legendary",
    "  recipes - list of recipe ids unlocked when this blueprint is consumed"
  ],
  "blueprints": [
    {
      "id": "neoblueprints:iron_tools",
      "name": "Iron Tools Blueprint",
      "rarity": "uncommon",
      "recipes": [
        "minecraft:iron_pickaxe",
        "minecraft:iron_sword",
        "minecraft:iron_axe",
        "minecraft:iron_shovel",
        "minecraft:iron_hoe"
      ]
    }
  ]
}
```

Any recipe ID listed in any blueprint's `recipes` array is automatically locked for all
players until they consume the corresponding blueprint. Reload at runtime with
`/blueprint reload`.

> **Tip:** Most vanilla recipe IDs match the item ID (e.g. `minecraft:iron_pickaxe`).
> For mod recipes, check the mod's data pack or use a recipe viewer mod.

---

## Rarity tiers

| Rarity | Name colour |
|---|---|
| `common` | White |
| `uncommon` | Yellow |
| `rare` | Aqua |
| `epic` | Light purple |
| `legendary` | Gold |

Blueprint item names are coloured by rarity. All configured blueprints appear in the
**NeoBlueprints** creative tab, sorted by rarity then name.

---

## Commands

All commands require permission level 2 (op).

| Command | Description |
|---|---|
| `/blueprint give <player> <id>` | Give a blueprint item stamped with the given blueprint ID |
| `/blueprint unlock <player> <recipe>` | Directly unlock a recipe for a player (no item needed) |
| `/blueprint reset <player>` | Clear all of a player's unlocks |
| `/blueprint reload` | Reload `neoblueprints.json` from disk and sync to all online players |

---

## How it works

- **One item, many blueprints.** The `neoblueprints:blueprint` item stores its blueprint
  ID via the 1.21 data-component system (`neoblueprints:blueprint_id`). No separate item
  registration per blueprint — just stamp the component.

- **Per-player unlock storage.** Unlocks are stored on the player via a NeoForge
  `AttachmentType` (`PlayerUnlockData`) with `copyOnDeath = true`, so they survive death.

- **Server-side enforcement.** The vanilla `ResultSlot` in every player's 2×2 inventory
  crafting grid and any open 3×3 crafting table is swapped server-side for a
  `LockedResultSlot`. Its `mayPickup()` checks the active recipe against the locked set
  and the player's unlocks. Blocked crafts refuse pickup without consuming ingredients.

- **Client-side overlay.** When the player places ingredients matching a locked recipe,
  the result slot shows the would-be item faded out with a red ✕ overlay. Hovering it
  shows "Locked Recipe — Requires: \<Blueprint Name\>" with the blueprint name coloured
  by rarity.

- **Server → client sync.** Blueprint config and per-player unlocks are synced via
  custom NeoForge network payloads on login, respawn, and dimension change, keeping the
  client overlay accurate.

- **Recipe condition.** A `neoblueprints:blueprint_unlocked` `ICondition` is registered
  for datapack authors who want to annotate which blueprint a recipe needs alongside the
  recipe JSON itself.

---

## Project layout

```
src/main/java/com/example/neoblueprints/
├── NeoBlueprintsMod.java            # @Mod entry point
├── client/
│   ├── ClientEvents.java            # overlay + tooltip hooks
│   ├── ClientUnlockCache.java       # client-side unlock mirror
│   └── LockedRecipeOverlay.java     # faded item + red X rendering
├── command/BlueprintCommand.java
├── condition/BlueprintUnlockedCondition.java
├── config/
│   ├── BlueprintConfig.java         # JSON config load / reload / sync
│   └── BlueprintDefinition.java     # record + stream codec
├── data/
│   ├── PlayerUnlockData.java
│   └── UnlockHelper.java
├── event/
│   ├── CraftingLockHandler.java     # slot swap + item tooltip event
│   └── ServerSyncHandler.java       # login / respawn / dim-change sync
├── inventory/LockedResultSlot.java
├── item/
│   ├── BlueprintItem.java
│   └── BlueprintRarity.java
├── network/
│   ├── NetworkHandler.java
│   ├── SyncBlueprintsPayload.java
│   └── SyncUnlocksPayload.java
└── registry/
    ├── ModAttachments.java
    ├── ModConditions.java
    ├── ModCreativeTabs.java
    ├── ModDataComponents.java
    └── ModItems.java
```

---

## License

[MIT](LICENSE)
