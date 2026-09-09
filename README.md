# Enchantment Core

Enchantment Core is a data-driven enchantment library for Minecraft mod development. It provides a structured API for defining custom spell fields, dynamic level-based values, and bidirectional JSON transformations for Fabric, Forge, and NeoForge environments.

The framework replaces hard-coded enchantment behaviors with a declarative, JSON-driven architecture. Components are governed by [DataFixerUpper](https://github.com/Mojang/DataFixerUpper) (DFU) compliant transformers that intercept and convert static primitive values into dynamic, configurable properties at runtime.

## Quickstart Guide 1.21 - 1.21.1

### Installation

Add the Modrinth Maven repository and the library dependency to `build.gradle`. Replace `[VERSION]` with the target release version.

```groovy
repositories {
    maven {
        name = "Modrinth"
        url = "[https://api.modrinth.com/maven](https://api.modrinth.com/maven)"
    }
}

dependencies {
    modImplementation "maven.modrinth:enchantmentcore:[VERSION]"
}
```

# JSON Data Definition

Enchantments and custom spell fields are defined via JSON within standard Minecraft datapacks. The library exposes custom effect components registered under the `enchantment_core` namespace, such as `enchantment_core:spell_field` or `enchantment_core:explode`.

The `spell_field` component dictates spatial operations. It allows the definition of volumes to establish physical boundaries and topological multipliers. It executes targeted `entity_effects`, `block_effects`, `volume_effects`, or `visual_effects` within those boundaries.

Level-based values accept dynamic configuration wrappers. Use types such as `enchantment_core:configurable_linear` or `enchantment_core:configurable_constant` to bind standard scaling parameters to the Config Overhauled API automatically.

Refer to the official project wiki and API reference for exhaustive schema documentation, custom effect component parameters, and advanced topological configurations.

```json
{
  "description": {
    "translate": "enchantment.example.custom_spell"
  },
  "supported_items": "#minecraft:enchantable/weapon",
  "weight": {
    "config": {
      "mod_id": "example_mod",
      "category": "enchantment",
      "group": "custom_spell",
      "property": "weight"
    },
    "fallback": 10
  },
  "max_level": {
    "config": {
      "mod_id": "example_mod",
      "category": "enchantment",
      "group": "custom_spell",
      "property": "max_level"
    },
    "fallback": 3
  },
  "effects": {
    "minecraft:post_attack": [
      {
        "enchantment_level": {
          "type": "enchantment_core:configurable_linear",
          "base_config": {
            "mod_id": "example_mod",
            "category": "enchantment",
            "group": "custom_spell",
            "property": "power_base"
          },
          "base_default": 1.0,
          "per_level_config": {
            "mod_id": "example_mod",
            "category": "enchantment",
            "group": "custom_spell",
            "property": "power_per_level"
          },
          "per_level_default": 1.0
        },
        "effect": {
          "type": "enchantment_core:spell_field",
          "volumes": [ ... ],
          "entity_effects": [ ... ]
        }
      }
    ]
  }
}
```

# Commands
The library provides server-side administrative commands requiring permission level 2.

## Data-Migration
* Import:
    * Executed via `/enchantment_core import <source_namespace> <target_namespace>`.
    * Scans the active enchantment registry exclusively for entries matching `<source_namespace>`. Intercepts static primitive values from the matched JSON structures and wraps them in dynamically bound Config-Overhauled implementations assigned to the `<target_namespace>`. Exports the modified JSON files to `enchantment_core/import/<source_namespace>`.
* Export:
    * Executed via `/enchantment_core export <source_namespace>`.
    * Scans the active enchantment registry exclusively for entries matching `<source_namespace>`. Processes dynamic `LevelBasedValue` configurations within those specific enchantments and executes structural un-wrapping procedures. Exports the resulting standard vanilla JSON files to `enchantment_core/export/<source_namespace>`.

## Debug
* Spell Field:
    * Executed via `/enchantment_core spellfield <targets> <level> <payload>`.
    * Executes a raw JSON spell field definition directly on the specified target entities.

# Credits
This project utilizes [Config Overhauled](https://modrinth.com/mod/config-overhauled) for runtime configuration variable linkage.

This project was created using [jaredlll08](https://modrinth.com/user/jaredlll08)'s [MultiLoader-Template](https://github.com/jaredlll08/MultiLoader-Template).
