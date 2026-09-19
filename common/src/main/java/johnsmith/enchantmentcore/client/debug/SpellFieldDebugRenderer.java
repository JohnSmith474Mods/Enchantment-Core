package johnsmith.enchantmentcore.client.debug;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import johnsmith.enchantmentcore.config.Config;
import johnsmith.enchantmentcore.api.enchantment.spellfield.math.LocalVolume;
import johnsmith.enchantmentcore.api.enchantment.spellfield.math.Topology;
import johnsmith.enchantmentcore.api.enchantment.spellfield.math.DistanceMetric;
import johnsmith.enchantmentcore.api.enchantment.spellfield.math.SpatialVector;
import johnsmith.enchantmentcore.api.enchantment.spellfield.math.FieldAxis;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.Mth;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Primary debug renderer for Enchantment Core spell fields.
 * Implements the Minecraft 26.2 Gizmos rendering architecture.
 */
public class SpellFieldDebugRenderer implements DebugRenderer.SimpleDebugRenderer {

    /**
     * Entry point for the 26.2 debug rendering pipeline.
     *
     * @param cx               Camera X translation (Unused by Gizmos).
     * @param cy               Camera Y translation (Unused by Gizmos).
     * @param cz               Camera Z translation (Unused by Gizmos).
     * @param debugValueAccess System debug value access instance.
     * @param frustum          Active camera culling frustum.
     * @param partialTick      Engine partial tick for coordinate interpolation.
     */
    @Override
    public void emitGizmos(double cx, double cy, double cz, DebugValueAccess debugValueAccess, Frustum frustum, float partialTick) {
        if (!Config.ENABLE_DEBUG_RENDERER.get()) return;

        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.debugEntries.isCurrentlyEnabled(DebugScreenEntries.ENTITY_HITBOXES)) return;

        Map<Integer, SpellFieldDebugTracker.TrackedSpellField> activeFields = SpellFieldDebugTracker.getActiveShapes();
        if (activeFields.isEmpty()) return;

        Level level = minecraft.level;
        if (level == null) return;

        renderGizmos(level, partialTick, activeFields);
        renderOriginAxes(level, partialTick, activeFields);
        renderVectorFields(level, partialTick, activeFields);
        renderFieldPoints(level, partialTick, activeFields);
        renderSurfaces(level, partialTick, activeFields);
    }

    /**
     * Prevents configuration parsing errors from producing invisible geometry.
     * Overrides the alpha channel if it evaluates to 0.
     */
    private static int enforceAlpha(int color, int fallbackColor) {
        if ((color & 0xFF000000) == 0) {
            return fallbackColor;
        }
        return color;
    }

    private static void renderGizmos(Level level, float partialTick, Map<Integer, SpellFieldDebugTracker.TrackedSpellField> activeFields) {
        int bbCol = enforceAlpha(Config.BOUNDING_BOX_COLOR.get(), 0xFFFFFF00);
        GizmoStyle style = GizmoStyle.stroke(bbCol);

        for (Map.Entry<Integer, SpellFieldDebugTracker.TrackedSpellField> entry : activeFields.entrySet()) {
            SpellFieldDebugTracker.TrackedSpellField trackedField = entry.getValue();
            Entity anchorEntity = trackedField.anchorEntity;
            if (anchorEntity == null) continue;

            Entity clientEntity = level.getEntity(anchorEntity.getId());
            if (clientEntity != null) anchorEntity = clientEntity;

            Vec3 lerpPos = anchorEntity.getPosition(partialTick);

            for (LocalVolume volume : trackedField.volumes) {
                AABB worldBounds = volume.bounds().move(lerpPos);
                Gizmos.cuboid(worldBounds, style);
            }
        }
    }

    private static void renderOriginAxes(Level level, float partialTick, Map<Integer, SpellFieldDebugTracker.TrackedSpellField> activeFields) {
        int xCol = enforceAlpha(Config.ORIGIN_X_COLOR.get(), 0xFFFF0000);
        int yCol = enforceAlpha(Config.ORIGIN_Y_COLOR.get(), 0xFF00FF00);
        int zCol = enforceAlpha(Config.ORIGIN_Z_COLOR.get(), 0xFF0000FF);
        int aCol = enforceAlpha(Config.AXIS_LINE_COLOR.get(), 0xFFFF00FF);

        for (Map.Entry<Integer, SpellFieldDebugTracker.TrackedSpellField> entry : activeFields.entrySet()) {
            SpellFieldDebugTracker.TrackedSpellField trackedField = entry.getValue();
            Entity anchorEntity = trackedField.anchorEntity;
            if (anchorEntity == null) continue;

            Entity clientEntity = level.getEntity(anchorEntity.getId());
            if (clientEntity != null) anchorEntity = clientEntity;

            Vec3 lerpPos = anchorEntity.getPosition(partialTick);

            for (LocalVolume volume : trackedField.volumes) {
                if (volume.topology() == null) continue;

                Topology topology = volume.topology();
                float originRange = topology.originRange().calculate(trackedField.enchantmentLevel);
                if (originRange <= 0.0F) continue;

                Vec3 absVolumeCenter = lerpPos.add(volume.volumeCenter());
                Vec3 absOrigin = topology.getOrigin(trackedField.enchantmentLevel, anchorEntity, absVolumeCenter);

                float s = 0.5F;

                Gizmos.line(absOrigin.add(-s, 0, 0), absOrigin.add(s, 0, 0), xCol, 2.0F);
                Gizmos.line(absOrigin.add(0, -s, 0), absOrigin.add(0, s, 0), yCol, 2.0F);
                Gizmos.line(absOrigin.add(0, 0, -s), absOrigin.add(0, 0, s), zCol, 2.0F);

                if (topology.axis().isPresent()) {
                    Vec3 dir = topology.axis().get().resolve(trackedField.enchantmentLevel, anchorEntity);
                    if (dir.lengthSqr() > 0.0001D) {
                        dir = dir.normalize();
                        float axialRange = topology.axialRange().map(v -> v.calculate(trackedField.enchantmentLevel)).orElse(originRange);
                        Vec3 axisEnd = absOrigin.add(dir.scale(axialRange));

                        Gizmos.line(absOrigin, axisEnd, aCol, 3.0F);
                    }
                }
            }
        }
    }

    private static void renderVectorFields(Level level, float partialTick, Map<Integer, SpellFieldDebugTracker.TrackedSpellField> activeFields) {
        int stemCol = enforceAlpha(Config.VECTOR_STEM_COLOR.get(), 0x96FFFFFF);
        int tipCol = enforceAlpha(Config.VECTOR_TIP_COLOR.get(), 0xFFFFFF00);

        for (Map.Entry<Integer, SpellFieldDebugTracker.TrackedSpellField> entry : activeFields.entrySet()) {
            SpellFieldDebugTracker.TrackedSpellField trackedField = entry.getValue();
            if (trackedField.vectorFields.isEmpty()) continue;

            Entity anchorEntity = trackedField.anchorEntity;
            if (anchorEntity == null) continue;

            Entity clientEntity = level.getEntity(anchorEntity.getId());
            if (clientEntity != null) anchorEntity = clientEntity;

            Vec3 lerpPos = anchorEntity.getPosition(partialTick);
            Vec3 absEpicenter = lerpPos.add(0, anchorEntity.getBbHeight() / 2.0, 0);

            for (LocalVolume volume : trackedField.volumes) {
                double maxDim = Math.max(volume.bounds().maxX - volume.bounds().minX, Math.max(volume.bounds().maxY - volume.bounds().minY, volume.bounds().maxZ - volume.bounds().minZ));
                double step = Math.max(1.0, maxDim / 6.0);
                Vec3 absVolumeCenter = lerpPos.add(volume.volumeCenter());

                for (double x = volume.bounds().minX; x <= volume.bounds().maxX; x += step) {
                    for (double y = volume.bounds().minY; y <= volume.bounds().maxY; y += step) {
                        for (double z = volume.bounds().minZ; z <= volume.bounds().maxZ; z += step) {
                            Vec3 targetPos = lerpPos.add(x, y, z);

                            if (volume.topology() != null) {
                                float scalar = volume.topology().evaluateMultiplier(trackedField.enchantmentLevel, anchorEntity, absVolumeCenter, targetPos);
                                if (scalar <= 0.05F) continue;
                            }

                            for (FieldAxis axis : trackedField.vectorFields) {
                                Vec3 direction = axis.getImpulseVector(anchorEntity, absEpicenter, targetPos);
                                if (direction.lengthSqr() < 0.0001D) continue;
                                direction = direction.normalize();

                                float length = 0.4F;
                                Vec3 start = targetPos;
                                Vec3 end = start.add(direction.scale(length));
                                Vec3 tipEnd = end.add(direction.scale(length * 0.2F));

                                Gizmos.line(start, end, stemCol, 2.0F);
                                Gizmos.line(end, tipEnd, tipCol, 4.0F);
                            }
                        }
                    }
                }
            }
        }
    }

    private static void renderFieldPoints(Level level, float partialTick, Map<Integer, SpellFieldDebugTracker.TrackedSpellField> activeFields) {
        int highCol = enforceAlpha(Config.FIELD_POINT_HIGH_COLOR.get(), 0xC8FF0000);
        int hr = (highCol >> 16) & 0xFF, hg = (highCol >> 8) & 0xFF, hb = highCol & 0xFF, ha = (highCol >> 24) & 0xFF;

        int lowCol = enforceAlpha(Config.FIELD_POINT_LOW_COLOR.get(), 0xC800FF00);
        int lr = (lowCol >> 16) & 0xFF, lg = (lowCol >> 8) & 0xFF, lb = lowCol & 0xFF, la = (lowCol >> 24) & 0xFF;

        for (Map.Entry<Integer, SpellFieldDebugTracker.TrackedSpellField> entry : activeFields.entrySet()) {
            SpellFieldDebugTracker.TrackedSpellField trackedField = entry.getValue();
            Entity anchorEntity = trackedField.anchorEntity;
            if (anchorEntity == null) continue;

            Entity clientEntity = level.getEntity(anchorEntity.getId());
            if (clientEntity != null) anchorEntity = clientEntity;

            Vec3 lerpPos = anchorEntity.getPosition(partialTick);

            for (LocalVolume volume : trackedField.volumes) {
                Topology topology = volume.topology();
                if (topology == null) continue;

                double maxDim = Math.max(volume.bounds().maxX - volume.bounds().minX, Math.max(volume.bounds().maxY - volume.bounds().minY, volume.bounds().maxZ - volume.bounds().minZ));
                double step = Math.max(0.5, maxDim / 20.0);
                Vec3 absVolumeCenter = lerpPos.add(volume.volumeCenter());

                for (double x = volume.bounds().minX; x <= volume.bounds().maxX; x += step) {
                    for (double y = volume.bounds().minY; y <= volume.bounds().maxY; y += step) {
                        for (double z = volume.bounds().minZ; z <= volume.bounds().maxZ; z += step) {
                            Vec3 targetPos = lerpPos.add(x, y, z);
                            float scalar = topology.evaluateMultiplier(trackedField.enchantmentLevel, anchorEntity, absVolumeCenter, targetPos);

                            if (scalar > 0.05F) {
                                float size = 0.02F + (0.08F * scalar);

                                int r = (int) Mth.lerp(scalar, lr, hr);
                                int g = (int) Mth.lerp(scalar, lg, hg);
                                int b = (int) Mth.lerp(scalar, lb, hb);
                                int a = (int) Mth.lerp(scalar, la, ha);

                                int color = (a << 24) | (r << 16) | (g << 8) | b;
                                AABB pointBox = AABB.ofSize(targetPos, size * 2, size * 2, size * 2);
                                Gizmos.cuboid(pointBox, GizmoStyle.fill(color));
                            }
                        }
                    }
                }
            }
        }
    }

    private static void renderSurfaces(Level level, float partialTick, Map<Integer, SpellFieldDebugTracker.TrackedSpellField> activeFields) {
        int planeCol = enforceAlpha(Config.TOPOLOGY_PLANE_COLOR.get(), 0x40FFFFFF);
        int sphCol = enforceAlpha(Config.TOPOLOGY_SPHERE_COLOR.get(), 0x40FFFFFF);

        for (Map.Entry<Integer, SpellFieldDebugTracker.TrackedSpellField> entry : activeFields.entrySet()) {
            SpellFieldDebugTracker.TrackedSpellField trackedField = entry.getValue();
            Entity anchorEntity = trackedField.anchorEntity;
            if (anchorEntity == null) continue;

            Entity clientEntity = level.getEntity(anchorEntity.getId());
            if (clientEntity != null) anchorEntity = clientEntity;

            Vec3 lerpPos = anchorEntity.getPosition(partialTick);

            for (LocalVolume volume : trackedField.volumes) {
                if (volume.topology() == null) continue;

                Topology topology = volume.topology();
                float originRange = topology.originRange().calculate(trackedField.enchantmentLevel);
                if (originRange <= 0.0F) continue;

                Vec3 localOriginOffset = topology.originOffset().orElse(SpatialVector.ZERO).resolve(trackedField.enchantmentLevel, anchorEntity);
                Vec3 localOrigin = volume.volumeCenter().add(localOriginOffset);

                if (topology.axis().isPresent()) {
                    Vec3 dir = topology.axis().get().resolve(trackedField.enchantmentLevel, anchorEntity);
                    if (dir.lengthSqr() > 0.0001D) {
                        dir = dir.normalize();
                        Vector3f up = new Vector3f(0, 1, 0);
                        Vector3f vDir = new Vector3f((float) dir.x, (float) dir.y, (float) dir.z);
                        Quaternionf rot = new Quaternionf().rotationTo(up, vDir);

                        float radialRange = topology.radialRange().map(v -> v.calculate(trackedField.enchantmentLevel)).orElse(originRange);
                        int segments = 64;

                        List<Vec3> poly = new ArrayList<>();
                        for (int i = 0; i < segments; i++) {
                            float angle = (float) i / segments * Mth.TWO_PI;
                            Vector3f vLocal = new Vector3f(Mth.cos(angle), 0, Mth.sin(angle));
                            vLocal.rotate(rot);

                            Vec3 vw = new Vec3(vLocal.x(), vLocal.y(), vLocal.z());
                            double metricScale = topology.metric().calculate(vw);
                            double c = radialRange / metricScale;

                            poly.add(localOrigin.add(vw.scale(c)));
                        }

                        poly = clipPolygon(poly, volume.bounds());

                        if (poly.size() >= 3) {
                            Vec3 v0 = poly.getFirst().add(lerpPos);
                            for (int i = 1; i < poly.size() - 1; i++) {
                                Vec3 v1 = poly.get(i).add(lerpPos);
                                Vec3 v2 = poly.get(i + 1).add(lerpPos);
                                Gizmos.line(v0, v1, planeCol, 1.0F);
                                Gizmos.line(v1, v2, planeCol, 1.0F);
                                Gizmos.line(v2, v0, planeCol, 1.0F);
                            }
                        }
                    }
                } else {
                    int stacks = 24;
                    int slices = 24;

                    for (int i = 0; i < stacks; i++) {
                        float phi1 = Mth.PI * ((float) i / stacks);
                        float phi2 = Mth.PI * ((float) (i + 1) / stacks);
                        for (int j = 0; j < slices; j++) {
                            float theta1 = Mth.TWO_PI * ((float) j / slices);
                            float theta2 = Mth.TWO_PI * ((float) (j + 1) / slices);

                            Vec3 u1 = new Vec3(Mth.sin(phi1) * Mth.cos(theta1), Mth.cos(phi1), Mth.sin(phi1) * Mth.sin(theta1));
                            Vec3 u2 = new Vec3(Mth.sin(phi1) * Mth.cos(theta2), Mth.cos(phi1), Mth.sin(phi1) * Mth.sin(theta2));
                            Vec3 u3 = new Vec3(Mth.sin(phi2) * Mth.cos(theta2), Mth.cos(phi2), Mth.sin(phi2) * Mth.sin(theta2));
                            Vec3 u4 = new Vec3(Mth.sin(phi2) * Mth.cos(theta1), Mth.cos(phi2), Mth.sin(phi2) * Mth.sin(theta1));

                            double c1 = originRange / topology.metric().calculate(u1);
                            double c2 = originRange / topology.metric().calculate(u2);
                            double c3 = originRange / topology.metric().calculate(u3);
                            double c4 = originRange / topology.metric().calculate(u4);

                            Vec3 p1 = localOrigin.add(u1.scale(c1));
                            Vec3 p2 = localOrigin.add(u2.scale(c2));
                            Vec3 p3 = localOrigin.add(u3.scale(c3));
                            Vec3 p4 = localOrigin.add(u4.scale(c4));

                            List<Vec3> quad = Arrays.asList(p1, p2, p3, p4);
                            quad = clipPolygon(quad, volume.bounds());

                            if (quad.size() >= 3) {
                                Vec3 v0 = quad.get(0).add(lerpPos);
                                for (int k = 1; k < quad.size() - 1; k++) {
                                    Vec3 vk1 = quad.get(k).add(lerpPos);
                                    Vec3 vk2 = quad.get(k + 1).add(lerpPos);
                                    Gizmos.line(v0, vk1, sphCol, 1.0F);
                                    Gizmos.line(vk1, vk2, sphCol, 1.0F);
                                    Gizmos.line(vk2, v0, sphCol, 1.0F);
                                }
                            }
                        }
                    }

                    for (int face = 0; face < 6; face++) {
                        drawIntersectionCap(lerpPos, volume.bounds(), localOrigin, originRange, topology.metric(), planeCol, face);
                    }
                }
            }
        }
    }

    private static void drawIntersectionCap(Vec3 lerpPos, AABB box, Vec3 center, float radius, DistanceMetric metric, int color, int face) {
        double planeVal;
        double dist;

        if (face == 0) { planeVal = box.maxX; dist = planeVal - center.x; }
        else if (face == 1) { planeVal = box.minX; dist = planeVal - center.x; }
        else if (face == 2) { planeVal = box.maxY; dist = planeVal - center.y; }
        else if (face == 3) { planeVal = box.minY; dist = planeVal - center.y; }
        else if (face == 4) { planeVal = box.maxZ; dist = planeVal - center.z; }
        else { planeVal = box.minZ; dist = planeVal - center.z; }

        if (Math.abs(dist) >= radius) return;

        int segments = 64;
        List<Vec3> poly = new ArrayList<>();

        for (int i = 0; i < segments; i++) {
            double angle = (double) i / segments * Mth.TWO_PI;
            double t = 0;

            if (metric == DistanceMetric.EUCLIDEAN) {
                double capR2 = radius * radius - dist * dist;
                if (capR2 > 0) t = Math.sqrt(capR2);
            } else if (metric == DistanceMetric.MANHATTAN) {
                double rem = radius - Math.abs(dist);
                if (rem > 0) t = rem / (Math.abs(Math.cos(angle)) + Math.abs(Math.sin(angle)));
            }

            if (t <= 0) continue;

            double u = Math.cos(angle) * t;
            double v = Math.sin(angle) * t;

            if (face == 0 || face == 1) {
                poly.add(new Vec3(planeVal, center.y + u, center.z + v));
            } else if (face == 2 || face == 3) {
                poly.add(new Vec3(center.x + u, planeVal, center.z + v));
            } else {
                poly.add(new Vec3(center.x + u, center.y + v, planeVal));
            }
        }

        poly = clipPolygon(poly, box.inflate(0.001));

        if (poly.size() >= 3) {
            Vec3 v0 = poly.getFirst().add(lerpPos);
            for (int i = 1; i < poly.size() - 1; i++) {
                Vec3 v1 = poly.get(i).add(lerpPos);
                Vec3 v2 = poly.get(i + 1).add(lerpPos);
                Gizmos.line(v0, v1, color, 1.0F);
                Gizmos.line(v1, v2, color, 1.0F);
                Gizmos.line(v2, v0, color, 1.0F);
            }
        }
    }

    private static List<Vec3> clipPolygon(List<Vec3> poly, AABB box) {
        poly = clipAgainstPlane(poly, 1, 0, 0, box.minX);
        poly = clipAgainstPlane(poly, -1, 0, 0, -box.maxX);
        poly = clipAgainstPlane(poly, 0, 1, 0, box.minY);
        poly = clipAgainstPlane(poly, 0, -1, 0, -box.maxY);
        poly = clipAgainstPlane(poly, 0, 0, 1, box.minZ);
        poly = clipAgainstPlane(poly, 0, 0, -1, -box.maxZ);
        return poly;
    }

    private static List<Vec3> clipAgainstPlane(List<Vec3> poly, double nx, double ny, double nz, double d) {
        if (poly.isEmpty()) return poly;
        List<Vec3> out = new ArrayList<>();
        Vec3 S = poly.getLast();
        double sDist = S.x * nx + S.y * ny + S.z * nz;
        boolean sInside = sDist >= d;

        for (Vec3 E : poly) {
            double eDist = E.x * nx + E.y * ny + E.z * nz;
            boolean eInside = eDist >= d;

            if (sInside && eInside) {
                out.add(E);
            } else if (sInside) {
                double t = (d - sDist) / (eDist - sDist);
                out.add(S.add(E.subtract(S).scale(t)));
            } else if (eInside) {
                double t = (d - sDist) / (eDist - sDist);
                out.add(S.add(E.subtract(S).scale(t)));
                out.add(E);
            }
            S = E;
            sDist = eDist;
            sInside = eInside;
        }
        return out;
    }
}