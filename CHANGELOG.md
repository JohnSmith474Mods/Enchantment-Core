### Added
- **Dynamic Description System**: Introduced a data-driven way to inject evaluated mathematical curves directly into enchantment translation strings (supporting `<val:key>` and `<array:key>` syntax).
- **Auto-Wiring Codec**: The `EnchantmentCodecMixin` now automatically scans for `PointerValue` markers inside deeply nested effects and wires them to the root description component.
- `PointerValue` LevelBasedValue (registered as `enchantment_core:pointer`) to mark target equations for extraction.
- `DESCRIPTION_ARGS` Data Component (registered as `enchantment_core:description_args`) to store the lookup map of extracted curves.
- `DynamicDescriptionParser` utility to seamlessly parse translation strings and inject evaluated arguments.
- `POWDER_SNOW_WALKABLE` EnchantmentEffectComponent

### Fixed
- **Forge Mixin Loading in Dependency JARs**: Explicitly declared `MixinConfigs` in `jar.manifest` within `forge/build.gradle` so Mixin configurations (`enchantment_core.mixins.json` and `enchantment_core.forge.mixins.json`) are discovered and loaded when consumed as a library dependency without `reobfJar`.
- **Premature Registry Codec Querying**: Wrapped dispatch getters in `EnchantmentCoreRegistries` (`getDistanceScalingFunctionCodec()`, `getSpellFieldShapeCodec()`, and `getBaseSpellFieldEffectCodec()`) with `Codec.lazyInitialized(...)` to prevent premature supplier queries and `NullPointerException` crashes during Forge's early lifecycle events before custom registries are filled.