# HardmodePlus

A Fabric mod for Minecraft Java 26.3 that lets you set the attributes of every mob, from zombies to cows, and adds an extra multiplier and new options to Hard difficulty. It's part of a bigger project of mine, a set of mods that let every player adjust progression and experience to their own taste.

A fresh install changes nothing. Every value starts at vanilla, and only what you change is touched. New values apply as soon as you close the settings, also to mobs that are already in the world, without a restart. If you don't want to set everything up yourself, five ready-made presets set everything up with one click.

## Features

### Mob settings

Every hostile mob, the neutral ones (bee, dolphin, goat, iron golem, llama, trader llama, nautilus, panda, polar bear, pufferfish, wolf), the Ender Dragon and the Wither, and all peaceful animals. Mobs from other mods show up too, sorted by their spawn category.

| Tab | Values |
| --- | --- |
| Combat | Health, attack damage, attack knockback, armor, armor toughness |
| Movement | Movement speed, flying speed, jump strength, step height, water movement |
| Senses and body | Follow range, size, knockback resistance, explosion knockback resistance, gravity |
| Other | Burning time, fall damage multiplier, safe fall distance, zombie reinforcements |

A mob only shows the values it actually has: a creeper has no attack damage, so that field is empty.

The mod doesn't overwrite a mob's own values. It adds its own attribute modifiers, so random spawn bonuses like zombie leaders stay as they are, and setting a value back to vanilla removes the change again. When the maximum health changes, a mob keeps its share of health, so a mob at full health stays at full health.

### Multiplier

Only while the world is set to Hard:

| Area | What it does |
| --- | --- |
| General | Added to every area below, the quick setting for everything |
| Mob health | More maximum health |
| Damage to players | All damage a mob deals to players: melee, arrows, explosions, magic |
| Mob speed | Walking and flying speed |
| Follow range | How far mobs notice and follow you |

The total of an area is the general value plus its own value, for example +50 % general and +25 % health gives +75 % health. The multiplier applies to hostile and neutral mobs. Whether the bosses get it too is a switch in the options. Peaceful animals only use their own values.

### Features

New game rules in their own tab, sorted into health, food, thirst, condition, player and mob variants. Every feature has its own switch and is off by default.

**Health**

| Feature | What it does |
| --- | --- |
| Heart loss | After each death you have fewer hearts for a while. Several deaths add up, up to a limit you set |
| View edge | With every lost heart the screen edge gets bigger and darker, optionally with Blindness shortly before death |
| Sleep healing | Sleeping through the night heals you, but makes you hungrier |
| Healing | How much food natural healing costs, from which food level it starts and whether fast healing stays |
| Bleeding | Monster hits can cause light or heavy bleeding. Bandage, herbal bandage and tourniquet stop it |
| Broken bone | A high fall can break a bone: slower, no sprinting, and you limp. You can't jump, but you still step up one block. A splint makes you only half as slow, a cast heals the bone |

**Food and thirst**

| Feature | What it does |
| --- | --- |
| Hunger rules | How fast the food bar drops, also while standing and sprinting. An empty bar darkens the screen and hurts. Replaces the vanilla starvation damage |
| Thirst | Its own thirst bar above the food bar. Drink water bottles, milk, the canteen or a bowl of water. Sprinting, mining, jumping and fighting make you thirsty, heat even more. A low bar slows down stamina recovery, an empty one hurts |

**Condition**

| Feature | What it does |
| --- | --- |
| Stamina | A stamina bar: sprinting, mining, placing blocks, attacking and jumping use it up. The more levels you have, the more stamina you get, and a better tool makes mining cheaper. Optionally jumping, mining, building and attacking only work with enough stamina left |
| Exhaustion | Part of stamina: when the bar is empty you are slower, can't sprint and mine slower. |
| Tiredness | Several nights without sleep: you mine and recover slower until you sleep through once |
| Dazed | A hit from behind slows you down for a moment |
| Blast shock | An explosion hit dazes and slows you, optionally with nausea |
| Cold | In cold biomes and at night you slowly freeze. Leather armor, fire and lava help |

**Player and world**

| Feature | What it does |
| --- | --- |
| Safe surface | Once the Ender Dragon is dead, no monsters spawn under the open sky in the Overworld |
| Siege night | Every few days monsters see very far during the night and come for you |
| Number of monsters | More or fewer naturally spawning monsters, separately for surface, caves, Nether and End |
| Monsters grow stronger | Monsters start like vanilla and get stronger every world day until your values count in full on day X |
| Daily surcharge | On top of your values, a few percent more every world day up to a maximum (Hard only) |
| Experience | More or less experience from all sources, from -90 % to +120 % |
| Torches burn out | Torches you place go out after a set time and leave a stick. Village and mine torches stay |

**Mob variants**

Zombies, skeletons, spiders, cave spiders and creepers can spawn as a fast, tough, strong or random variant with their own skin and size. Share, strength and size of every variant can be set, optionally only underground. Zombies, skeletons and creepers can get glowing eyes. Variants give more experience and loot. All other monsters can get a small random spread of their values.

**Loot only from players** (in the options): monsters, golems or animals drop loot and experience only when a player kills them, so mob farms stop working.

### New items

| Item | Recipe | Use |
| --- | --- | --- |
| Bandage | Paper + string, 3 string, 2 wool or 2 leaf litter + string | Stops light bleeding |
| Herbal bandage | Bandage + small flower | Heals hearts and stops light bleeding |
| Tourniquet | Leather + string + stick | Stops any bleeding |
| Splint | 2 sticks + string | Holds a broken bone still: half as slow, but it does not heal and breaks after a few days |
| Cast | 3 bone meal + paper | Heals a broken bone over time |

The medical items have their own "Medicine" tab in the creative inventory.

| Item | Recipe | Use |
| --- | --- | --- |
| Canteen | 1 iron ingot + 3 leather | Several sips, fill it at water |
| Bowl of water | Right-click on water with a bowl | One small drink, the bowl is left over |

### HUD

Thirst and stamina bars next to the vanilla bars, the highest level you ever reached above the level number, the hidden saturation of food and thirst as a gold outline, and the effect icons centered at the top and a bit bigger. The saturation outline and the centered effects can be switched off for your own game.

### Adjustment

In the options you choose what is used: both combined (default), only the values per mob, or only the multiplier. The part that is switched off stays saved and comes back when you switch it on again.

| Adjustment | Zombie with 40 health and +50 % on Hard |
| --- | --- |
| Both combined | 60 |
| Values per mob only | 40 |
| Multiplier only | 30 |

### Presets and profiles

Five presets set up everything at once. All of them turn on the same survival rules (heart loss, view edge, sleep healing, hunger rules, thirst, exhaustion, tiredness, dazed, blast shock, cold, bleeding, broken bones and burning torches). They only differ in the monsters.

| Preset | Monsters |
| --- | --- |
| Relaxed | Vanilla |
| Tougher | Health and damage +25 %, speed +7.5 %, 10 % more monsters, a few variants, 10 % less experience |
| Veteran | Health and damage +50 %, speed +15 %, faster zombies and spiders, skeletons see 24 blocks. 25 % more monsters, more variants, 25 % less experience, monster loot only from players |
| Nightmare | Health and damage +75 %, speed +22.5 %, zombies and skeletons with 2 armor, faster spiders and creepers. 40 % more monsters, siege nights, 40 % less experience, monster and golem loot only from players |
| Realism | Health and damage +100 %, speed +30 %, zombies and skeletons with 4 armor, fast spiders and creepers. 50 % more monsters, 30 % variants, siege nights, half the experience, monster and golem loot only from players |

Below them are three profiles of your own to save and load complete setups. Loading a preset or a profile asks first, because anything you haven't saved is replaced.

**Export and import:** "Export" saves the current setup as a file in `config/hardmodeplus/profiles/`. Share it with friends or put it into a modpack. "Import..." lists the files in that folder, so a file you got from someone else just needs to be dropped in there. From there you can also delete files and open the folder in your file explorer.

Everything that would lose data asks first: loading, overwriting or clearing a profile, deleting a file and "Reset Vanilla".

## Settings

With [Mod Menu](https://modrinth.com/mod/modmenu) you get a settings screen. You can also edit `config/hardmodeplus.json` yourself, it only contains what you changed.

- Main tabs for mob settings, features and the multiplier. The mob settings have their own tabs for combat, movement, senses and body, and other values, with an icon for every mob. The features are sorted into health, food, thirst, condition and player.
- White = vanilla, yellow = changed but not saved yet, green = changed and saved, red = invalid input.
- Hover a field for the vanilla value and the allowed range, hover a column title for an explanation. Tooltips always stay fully on the screen.
- Scroll with the mouse wheel or drag the scrollbar.
- The arrow at the end of a row resets it. "Reset Vanilla" resets everything after asking, your profiles are kept.
- The options have three pages. General: "Mod active" (back to vanilla without losing your settings), the adjustment and the multiplier for bosses. Display & Controls: sprint fix, saturation display and centered effects, only for your own game. Loot: loot only from players.

If the config file ever gets broken, the mod starts with vanilla values and keeps the old file as `hardmodeplus.json.broken`, so nothing is lost.

On the very first start a small note in the corner says that the mod is free and where the official download is. Mod Menu also shows buttons to the official pages.

## Requirements

- Minecraft Java Edition 26.3
- Fabric Loader 0.19.2 or newer
- Fabric API
- Java 25
- [Mod Menu](https://modrinth.com/mod/modmenu) is optional and adds the settings screen

## Commands

For operators, also on servers without Mod Menu:

- `/hardmodeplus` shows whether the mod is on and how many values are changed
- `/hardmodeplus preset <name>` loads a preset (relaxed, tougher, veteran, nightmare, realism)
- `/hardmodeplus reload` reads the config file again
- `/hardmodeplus on` / `off`

## Multiplayer

The server decides. Its `config/hardmodeplus.json` sets the mob values, the multiplier and the features for everyone, a player's own file has no effect there, the server sends its values to every player. Because the mod adds its own items and effects, every player needs it installed to join. The display options (saturation, centered effects, sprint fix) are stored per player in `config/hardmodeplus-client.json`.

## Other mods

Mobs from other mods can be set like vanilla mobs. HardmodePlus works well together with GearBalance and EnchantPlus, but needs neither of them.

## Translations

Currently English and German. You can add another language with your own resource pack (`assets/hardmodeplus/lang/<code>.json`, based on `en_us.json`).

If you would like your translation to be part of the mod, open an issue or a pull request on GitHub, and I will add it.

## Modpacks

You may use HardmodePlus in modpacks. Please link to the official download (CurseForge or GitHub) instead of uploading the jar file somewhere else.

## Building

For personal use:

```
./gradlew build
```

The jar is placed in `build/libs`. Please do not publish modified versions.

## License

Copyright (c) 2026 Schluid. All rights reserved. See [LICENSE](LICENSE)
