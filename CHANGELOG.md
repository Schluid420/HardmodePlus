# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2026-11-09

First release.

### Added

- Settings for 19 attributes of every hostile, neutral and peaceful mob and the two bosses: health, attack damage, attack knockback, armor, toughness, movement and flying speed, jump strength, step height, water movement, follow range, size, knockback and explosion knockback resistance, gravity, burning time, fall damage, safe fall distance and zombie reinforcements.
- Multiplier on Hard difficulty: a general percentage plus mob health, damage to players, mob speed and follow range on their own.
- Adjustment switch: values per mob, multiplier, or both combined.
- Features tab with its own switch for every feature, all off by default, sorted into health, food, thirst, condition, player and mob variants.
- Health: heart loss after death, darker view edge at low health, sleep healing, adjustable natural healing, bleeding and broken bones. A broken bone makes you limp: no jumping, but one block is climbed by itself with a smooth camera.
- Food and thirst: own hunger rules, and a thirst bar with water bottles, milk, canteen and bowl of water.
- Condition: stamina (cost per action, more stamina with more levels, cheaper mining with better tools) with exhaustion when it runs out. Optionally actions only happen with enough stamina left for them. Tiredness after nights without sleep, dazed when hit from behind, blast shock and cold.
- Player and world: safe surface after the Ender Dragon, siege nights, number of monsters per area, monsters that grow stronger with the world days, a daily surcharge, experience gain from -90 % to +120 %, and torches that burn out.
- Mob variants: fast, tough, strong and random zombies, skeletons, spiders, cave spiders and creepers with their own skins, sizes and glowing eyes, optionally only underground, with more loot. A small random spread for all other monsters.
- Loot only from players, separately for monsters, golems and animals.
- New items: bandage, herbal bandage, tourniquet, splint (breaks after a few days), cast, canteen and bowl of water. The medical items have their own creative tab.
- HUD: thirst and stamina bars, highest level reached, saturation outline, effect icons centered at the top. Display options are stored per player.
- New values apply to all loaded mobs when the settings are closed and when the difficulty changes, without a restart.
- Settings screen via Mod Menu with main tabs, sub tabs, vanilla values and allowed ranges in the tooltips, a reset per row and a draggable scrollbar.
- Five presets (Relaxed, Tougher, Veteran, Nightmare, Realism) with the same survival rules and stronger monsters from step to step.
- `/hardmodeplus` command for operators: status, presets, reload, on and off.
- On servers the server's settings count for every player.
- Three profiles of your own to save and load complete setups.
- Export and import of setups as files in `config/hardmodeplus/profiles/`.
- A short note on the very first start that the mod is free, and links to the official pages in Mod Menu.
- Broken config files are kept as `hardmodeplus.json.broken`.
- English and German translations.
