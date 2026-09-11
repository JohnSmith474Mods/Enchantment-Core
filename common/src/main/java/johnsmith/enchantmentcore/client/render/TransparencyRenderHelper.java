package johnsmith.enchantmentcore.client.render;

import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.List;

import johnsmith.enchantmentcore.enchantment.effect.TransparencyEffect;
import johnsmith.enchantmentcore.registry.EnchantmentEffectComponentRegistry;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ConditionalEffect;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

public class TransparencyRenderHelper {
    private static final ThreadLocal<Float> CURRENT_ALPHA = ThreadLocal.withInitial(() -> 1.0F);

    public static void setAlphaActive(float alpha) {
        CURRENT_ALPHA.set(alpha);
    }

    public static void clearAlphaActive() {
        CURRENT_ALPHA.set(1.0F);
    }

    public static boolean isAlphaActive() {
        return CURRENT_ALPHA.get() < 1.0F;
    }

    public static float getAlpha() {
        return CURRENT_ALPHA.get();
    }

    public static float calculateAlpha(ItemStack stack) {
        if (stack.isEmpty()) return 1.0F;

        ItemEnchantments enchantments = stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
        if (enchantments.isEmpty()) return 1.0F;

        float minAlpha = 1.0F;
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : enchantments.entrySet()) {
            List<ConditionalEffect<TransparencyEffect>> effects = entry.getKey().value().effects().get(EnchantmentEffectComponentRegistry.TRANSPARENCY.get());
            if (effects != null) {
                for (ConditionalEffect<TransparencyEffect> cond : effects) {
                    minAlpha = Math.min(minAlpha, cond.effect().alphaMultiplier().calculate(entry.getIntValue()));
                }
            }
        }

        return Math.max(0.0F, Math.min(1.0F, minAlpha));
    }

    public static MultiBufferSource wrapBuffer(MultiBufferSource original, float alpha) {
        return renderType -> new VertexConsumer() {
            private final VertexConsumer delegate = original.getBuffer(renderType);

            @Override
            public VertexConsumer addVertex(float x, float y, float z) {
                delegate.addVertex(x, y, z);
                return this;
            }

            @Override
            public VertexConsumer setColor(int r, int g, int b, int a) {
                delegate.setColor(r, g, b, (int) (a * alpha));
                return this;
            }

            @Override public VertexConsumer setUv(float u, float v) { delegate.setUv(u, v); return this; }
            @Override public VertexConsumer setUv1(int u, int v) { delegate.setUv1(u, v); return this; }
            @Override public VertexConsumer setUv2(int u, int v) { delegate.setUv2(u, v); return this; }
            @Override public VertexConsumer setNormal(float x, float y, float z) { delegate.setNormal(x, y, z); return this; }
        };
    }
}