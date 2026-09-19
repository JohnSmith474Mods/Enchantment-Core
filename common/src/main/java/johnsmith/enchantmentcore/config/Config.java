package johnsmith.enchantmentcore.config;

import com.mojang.serialization.Codec;

import java.util.List;

import johnsmith.configoverhauled.api.Category;
import johnsmith.configoverhauled.api.ConfigManager;
import johnsmith.configoverhauled.api.Group;
import johnsmith.configoverhauled.api.Property;
import johnsmith.configoverhauled.api.registry.ConfigRegistry;
import johnsmith.enchantmentcore.Constants;
import johnsmith.enchantmentcore.api.config.PackInclusionType;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

public class Config {
    public static final ConfigManager MANAGER = ConfigRegistry.getOrCreateManager(Constants.MOD_ID);

    public static final Category ORPHANS = MANAGER.define("orphans");
    public static final Category CLIENT = MANAGER.define("client");
    public static final Category GLOBAL = MANAGER.define("general");
    public static final Category ENCHANTMENT = MANAGER.define("enchantment");
    public static final Category DEBUG = MANAGER.define("debug");

    public static final Group TOOLTIP = CLIENT.define("tooltip");
    public static final Group ACCESSIBILITY = CLIENT.define("accessibility");

    public static final Group COMBAT_RULES = GLOBAL.define("combat_rules");
    public static final Group DATA = GLOBAL.define("data");
    public static final Group DATA_TRANSFORMER = GLOBAL.define("data_transformer");
    public static final Group EXPLOSION = GLOBAL.define("explosion");
    public static final Group SPELL_FIELD = GLOBAL.define("spell_field");

    public static final Group RENDERER = DEBUG.define("renderer");

    public static final Property<Integer> ALERT_COLOR = ACCESSIBILITY.define("alert_color")
            .clientSide()
            .asRGBColor(0xFF5555)
            .withComment("The color of the orphaned values alert.")
            .register();

    public static final Property<Integer> LINK_COLOR = ACCESSIBILITY.define("log_directory_link_color")
            .clientSide()
            .asRGBColor(0x55FFFF)
            .withComment("The color of the log directory link in the orphaned values alert.")
            .register();

    public static final Property<Boolean> TOOLTIP_FORMATTING = TOOLTIP.define("formatting")
            .clientSide()
            .asBoolean(false)
            .withComment("Whether the tooltip changes should be enabled.")
            .register();

    public static final Property<Float> BOUNDED_PROTECTION_NUMERATOR = COMBAT_RULES.define("protection_numerator")
            .globalSide()
            .asFloat(20.F, 1.F, 32_768.F)
            .withComment("The maximum effective points you can get from enchantments.")
            .register();

    public static final Property<Float> BOUNDED_PROTECTION_DENOMINATOR = COMBAT_RULES.define("protection_denominator")
            .globalSide()
            .asFloat(25.F, 1.F, 32_768.F)
            .withComment("The value used to calculate damage reduction. Will be greater than the Numerator and there is nothing you can do about it.")
            .register();

    public static final Property<List<Block>> SPELL_FIELD_BLOCK_BLACKLIST = SPELL_FIELD.define("block_blacklist")
            .globalSide()
            .asBlocks(List.of(
                    Blocks.END_PORTAL,
                    Blocks.END_PORTAL_FRAME,
                    Blocks.BEDROCK,
                    Blocks.COMMAND_BLOCK,
                    Blocks.CHAIN_COMMAND_BLOCK,
                    Blocks.REPEATING_COMMAND_BLOCK,
                    Blocks.STRUCTURE_BLOCK,
                    Blocks.STRUCTURE_VOID,
                    Blocks.BARRIER,
                    Blocks.LIGHT
            )).withComment("Blocks that block-based spell field effects are prohibited from modifying.")
            .register();

    public static final Property<PackInclusionType> CONFIGURABLE_DEFAULT_ENCHANTMENTS = DATA.define("configurable_enchantment_resource_pack_activation_type")
            .globalSide()
            .asEnum(PackInclusionType.OPTIONAL, Codec.STRING.xmap(PackInclusionType::valueOf, Enum::name))
            .withComment("Defines the activation behavior of the built-in Configurable Default Enchantments pack. Valid values: OPTIONAL, ACTIVE, REQUIRED.")
            .register();

    public static final Property<List<String>> USER_TYPE_EXCLUSIONS = DATA_TRANSFORMER.define("type_exclusions")
            .globalSide()
            .asList(List.of(), Codec.STRING)
            .withComment("List of effect type identifiers to exclude from dynamic data transformation.")
            .register();

    public static final Property<List<String>> USER_STRUCTURAL_EXCLUSIONS = DATA_TRANSFORMER.define("structural_exclusions")
            .globalSide()
            .asList(List.of(), Codec.STRING)
            .withComment("List of JSON node keys to exclude from dynamic data transformation.")
            .register();

    public static final Property<Boolean> ENHANCE_BLOCK_EXPLOSIONS = EXPLOSION.define("enhance_block_explosion")
            .globalSide()
            .asBoolean(false)
            .withComment("Whether explosions occurring inside of blocks, should behave as if they were not inside a block.")
            .register();

    public static final Property<Boolean> ENABLE_DEBUG_RENDERER = RENDERER.define("enable_debug_renderer")
            .clientSide()
            .asBoolean(false)
            .withComment("Whether the spell field debug renderer should be enabled when entity hitboxes are shown.")
            .register();

    public static final Property<Integer> BOUNDING_BOX_COLOR = RENDERER.define("bounding_box_color")
            .clientSide()
            .asARGBColor(0xFFFFFF00)
            .withComment("The color of the volume bounding boxes (ARGB).")
            .register();

    public static final Property<Integer> ORIGIN_X_COLOR = RENDERER.define("origin_x_color")
            .clientSide()
            .asRGBColor(0xFF0000)
            .withComment("The color of the X-axis (Right/Left) on the origin crosshair.")
            .register();

    public static final Property<Integer> ORIGIN_Y_COLOR = RENDERER.define("origin_y_color")
            .clientSide()
            .asRGBColor(0x00FF00)
            .withComment("The color of the Y-axis (Up/Down) on the origin crosshair.")
            .register();

    public static final Property<Integer> ORIGIN_Z_COLOR = RENDERER.define("origin_z_color")
            .clientSide()
            .asRGBColor(0x0000FF)
            .withComment("The color of the Z-axis (Forward/Backward) on the origin crosshair.")
            .register();

    public static final Property<Integer> AXIS_LINE_COLOR = RENDERER.define("axis_line_color")
            .clientSide()
            .asRGBColor(0xFFFF00)
            .withComment("The color of the topology axis direction line.")
            .register();

    public static final Property<Integer> TOPOLOGY_PLANE_COLOR = RENDERER.define("topology_plane_color")
            .clientSide()
            .asARGBColor(0x4000FFFF)
            .withComment("The color and transparency of the topology plane surfaces (ARGB).")
            .register();

    public static final Property<Integer> TOPOLOGY_SPHERE_COLOR = RENDERER.define("topology_sphere_color")
            .clientSide()
            .asARGBColor(0x2000FFFF)
            .withComment("The color and transparency of the omnidirectional topology spherical wireframes (ARGB).")
            .register();

    public static final Property<Integer> FIELD_POINT_HIGH_COLOR = RENDERER.define("field_point_high_color")
            .clientSide()
            .asARGBColor(0xC8FF0000)
            .withComment("The color of the volumetric grid points where the topology multiplier is strong (1.0).")
            .register();

    public static final Property<Integer> FIELD_POINT_LOW_COLOR = RENDERER.define("field_point_low_color")
            .clientSide()
            .asARGBColor(0x3200FF00)
            .withComment("The color of the volumetric grid points where the topology multiplier is weak (0.0).")
            .register();

    public static final Property<Integer> VECTOR_STEM_COLOR = RENDERER.define("vector_stem_color")
            .clientSide()
            .asARGBColor(0x96FFFFFF)
            .withComment("The color of the directional vector stems.")
            .register();

    public static final Property<Integer> VECTOR_TIP_COLOR = RENDERER.define("vector_tip_color")
            .clientSide()
            .asARGBColor(0xFFFFFF00)
            .withComment("The color of the directional vector arrowheads.")
            .register();

    public static final Property<Integer> RENDER_RETENTION_TICKS = RENDERER.define("render_retention_ticks")
            .clientSide()
            .asInteger(60, 1, 1200)
            .withComment("The duration in ticks that transient spell field debug visualizers persist on screen.")
            .register();
}