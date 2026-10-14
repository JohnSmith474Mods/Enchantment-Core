### Added
- **Dynamic Description System**: Introduced a data-driven way to inject evaluated mathematical curves directly into enchantment translation strings (supporting `<val:key>` and `<array:key>` syntax).
- **Auto-Wiring Codec**: The `EnchantmentCodecMixin` now automatically scans for `PointerValue` markers inside deeply nested effects and wires them to the root description component.
- `PointerValue` LevelBasedValue (registered as `enchantment_core:pointer`) to mark target equations for extraction.
- `DESCRIPTION_ARGS` Data Component (registered as `enchantment_core:description_args`) to store the lookup map of extracted curves.
- `DynamicDescriptionParser` utility to seamlessly parse translation strings and inject evaluated arguments.
- `POWDER_SNOW_WALKABLE` EnchantmentEffectComponent