### Fixed

* **Forge MixinExtras Bundling**:

    * Fixed a startup `ClassNotFoundException` (`com.llamalad7.mixinextras.injector.wrapoperation.Operation`) on Forge where MixinExtras is not bundled natively.

    * Enabled Forge Jar-in-Jar (`jarJar`) to bundle `mixinextras-forge` `0.5.0` (`[0.5.0,)`) inside the published Forge artifact and updated `mixinextras-common` to `0.5.0` to match the declared `minVersion`.

* **Fabric Minecraft Version Compatibility**:

    * Fixed the `minecraft` dependency in `fabric.mod.json` being restricted to only the base Minecraft version instead of the full supported version range for the release.