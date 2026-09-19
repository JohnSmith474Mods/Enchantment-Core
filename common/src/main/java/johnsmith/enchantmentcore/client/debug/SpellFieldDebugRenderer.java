package johnsmith.enchantmentcore.client.debug;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

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
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.Mth;
import net.minecraft.util.debug.DebugValueAccess;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Primary debug renderer for Enchantment Core spell fields.
 * Implements a hybrid rendering architecture compatible with Minecraft 1.21.11.
 * Bounding boxes are delegated to the native {@link Gizmos} API.
 * Complex mathematical surfaces, origin axes, and vector fields are simulated as 3D quad volumes
 * and batched via {@link MultiBufferSource} to bypass hardware line shader restrictions.
 */
public class SpellFieldDebugRenderer implements DebugRenderer.SimpleDebugRenderer {

    /**
     * Entry point for the 1.21.11 debug rendering pipeline.
     * Executes the hybrid drawing routine for all active spell fields.
     *
     * @param cx               Camera X translation.
     * @param cy               Camera Y translation.
     * @param cz               Camera Z translation.
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

        PoseStack poseStack = new PoseStack();
        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();

        VertexConsumer quadBuffer = bufferSource.getBuffer(RenderTypes.debugQuads());

        renderOriginAxes(poseStack, level, partialTick, cx, cy, cz, activeFields, quadBuffer);
        renderVectorFields(poseStack, level, partialTick, cx, cy, cz, activeFields, quadBuffer);
        renderSurfaces(poseStack, level, partialTick, cx, cy, cz, activeFields, quadBuffer);
        renderFieldPoints(poseStack, level, partialTick, cx, cy, cz, activeFields, quadBuffer);
    }

    /**
     * Prevents configuration parsing errors from producing invisible geometry.
     * Overrides the alpha channel if it evaluates to 0.
     *
     * @param color         The parsed ARGB color integer.
     * @param fallbackColor The backup ARGB color integer.
     * @return A visible ARGB color integer.
     */
    private static int enforceAlpha(int color, int fallbackColor) {
        if ((color & 0xFF000000) == 0) {
            return fallbackColor;
        }
        return color;
    }

    /**
     * Renders AABB boundaries utilizing the native Gizmos pipeline.
     */
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

    /**
     * Renders the XYZ coordinate origin cross as volumetric quads.
     */
    private static void renderOriginAxes(PoseStack poseStack, Level level, float partialTick, double cx, double cy, double cz, Map<Integer, SpellFieldDebugTracker.TrackedSpellField> activeFields, VertexConsumer buffer) {
        int xCol = enforceAlpha(Config.ORIGIN_X_COLOR.get(), 0xFFFF0000);
        int xr = (xCol >> 16) & 0xFF; int xg = (xCol >> 8) & 0xFF; int xb = xCol & 0xFF;

        int yCol = enforceAlpha(Config.ORIGIN_Y_COLOR.get(), 0xFF00FF00);
        int yr = (yCol >> 16) & 0xFF; int yg = (yCol >> 8) & 0xFF; int yb = yCol & 0xFF;

        int zCol = enforceAlpha(Config.ORIGIN_Z_COLOR.get(), 0xFF0000FF);
        int zr = (zCol >> 16) & 0xFF; int zg = (zCol >> 8) & 0xFF; int zb = zCol & 0xFF;

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

                poseStack.pushPose();
                poseStack.translate(absOrigin.x - cx, absOrigin.y - cy, absOrigin.z - cz);
                Matrix4f pose = poseStack.last().pose();

                float s = 0.5F;

                drawThickLine(buffer, pose, new Vec3(-s, 0, 0), new Vec3(s, 0, 0), 0.015F, xr, xg, xb, 255);
                drawThickLine(buffer, pose, new Vec3(0, -s, 0), new Vec3(0, s, 0), 0.015F, yr, yg, yb, 255);
                drawThickLine(buffer, pose, new Vec3(0, 0, -s), new Vec3(0, 0, s), 0.015F, zr, zg, zb, 255);

                poseStack.popPose();
            }
        }
    }

    /**
     * Evaluates and renders directional impulse vectors as thick 3D quads.
     */
    private static void renderVectorFields(PoseStack poseStack, Level level, float partialTick, double cx, double cy, double cz, Map<Integer, SpellFieldDebugTracker.TrackedSpellField> activeFields, VertexConsumer buffer) {
        int stemCol = enforceAlpha(Config.VECTOR_STEM_COLOR.get(), 0x96FFFFFF);
        int sr = (stemCol >> 16) & 0xFF, sg = (stemCol >> 8) & 0xFF, sb = stemCol & 0xFF, sa = (stemCol >> 24) & 0xFF;

        int tipCol = enforceAlpha(Config.VECTOR_TIP_COLOR.get(), 0xFFFFFF00);
        int tr = (tipCol >> 16) & 0xFF, tg = (tipCol >> 8) & 0xFF, tb = tipCol & 0xFF, ta = (tipCol >> 24) & 0xFF;

        for (Map.Entry<Integer, SpellFieldDebugTracker.TrackedSpellField> entry : activeFields.entrySet()) {
            SpellFieldDebugTracker.TrackedSpellField trackedField = entry.getValue();
            if (trackedField.vectorFields.isEmpty()) continue;

            Entity anchorEntity = trackedField.anchorEntity;
            if (anchorEntity == null) continue;

            Entity clientEntity = level.getEntity(anchorEntity.getId());
            if (clientEntity != null) anchorEntity = clientEntity;

            Vec3 lerpPos = anchorEntity.getPosition(partialTick);
            Vec3 absEpicenter = lerpPos.add(0, anchorEntity.getBbHeight() / 2.0, 0);

            poseStack.pushPose();
            poseStack.translate(lerpPos.x - cx, lerpPos.y - cy, lerpPos.z - cz);
            Matrix4f pose = poseStack.last().pose();

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
                                Vec3 start = new Vec3(x, y, z);
                                Vec3 end = start.add(direction.scale(length));
                                Vec3 tipEnd = end.add(direction.scale(length * 0.2F));

                                drawThickLine(buffer, pose, start, end, 0.015F, sr, sg, sb, sa);
                                drawThickLine(buffer, pose, end, tipEnd, 0.045F, tr, tg, tb, ta);
                            }
                        }
                    }
                }
            }
            poseStack.popPose();
        }
    }

    /**
     * Renders mathematical boundaries and geometric plane intersections utilizing parametric equations.
     */
    private static void renderSurfaces(PoseStack poseStack, Level level, float partialTick, double cx, double cy, double cz, Map<Integer, SpellFieldDebugTracker.TrackedSpellField> activeFields, VertexConsumer buffer) {
        int planeCol = enforceAlpha(Config.TOPOLOGY_PLANE_COLOR.get(), 0x40FFFFFF);
        int pr = (planeCol >> 16) & 0xFF; int pg = (planeCol >> 8) & 0xFF; int pb = planeCol & 0xFF; int pa = (planeCol >> 24) & 0xFF;

        int sphCol = enforceAlpha(Config.TOPOLOGY_SPHERE_COLOR.get(), 0x40FFFFFF);
        int sr = (sphCol >> 16) & 0xFF; int sg = (sphCol >> 8) & 0xFF; int sb = sphCol & 0xFF; int sa = (sphCol >> 24) & 0xFF;

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
                            poseStack.pushPose();
                            poseStack.translate(lerpPos.x - cx, lerpPos.y - cy, lerpPos.z - cz);
                            Matrix4f pose = poseStack.last().pose();

                            Vec3 v0 = poly.getFirst();
                            for (int i = 1; i < poly.size() - 1; i++) {
                                Vec3 v1 = poly.get(i);
                                Vec3 v2 = poly.get(i + 1);

                                buffer.addVertex(pose, (float) v0.x, (float) v0.y, (float) v0.z).setColor(pr, pg, pb, pa);
                                buffer.addVertex(pose, (float) v1.x, (float) v1.y, (float) v1.z).setColor(pr, pg, pb, pa);
                                buffer.addVertex(pose, (float) v2.x, (float) v2.y, (float) v2.z).setColor(pr, pg, pb, pa);
                                buffer.addVertex(pose, (float) v2.x, (float) v2.y, (float) v2.z).setColor(pr, pg, pb, pa);
                            }
                            poseStack.popPose();
                        }
                    }
                } else {
                    poseStack.pushPose();
                    poseStack.translate(lerpPos.x - cx, lerpPos.y - cy, lerpPos.z - cz);
                    Matrix4f pose = poseStack.last().pose();

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
                                Vec3 v0 = quad.get(0);
                                for (int k = 1; k < quad.size() - 1; k++) {
                                    Vec3 vk1 = quad.get(k);
                                    Vec3 vk2 = quad.get(k + 1);

                                    buffer.addVertex(pose, (float) v0.x, (float) v0.y, (float) v0.z).setColor(sr, sg, sb, sa);
                                    buffer.addVertex(pose, (float) vk1.x, (float) vk1.y, (float) vk1.z).setColor(sr, sg, sb, sa);
                                    buffer.addVertex(pose, (float) vk2.x, (float) vk2.y, (float) vk2.z).setColor(sr, sg, sb, sa);
                                    buffer.addVertex(pose, (float) vk2.x, (float) vk2.y, (float) vk2.z).setColor(sr, sg, sb, sa);
                                }
                            }
                        }
                    }

                    for (int face = 0; face < 6; face++) {
                        drawIntersectionCap(buffer, pose, volume.bounds(), localOrigin, originRange, topology.metric(), pr, pg, pb, pa, face);
                    }

                    poseStack.popPose();
                }
            }
        }
    }

    /**
     * Clips the volumetric mathematical sphere against the AABB walls to create precise boundary intersections.
     */
    private static void drawIntersectionCap(VertexConsumer buffer, Matrix4f pose, AABB box, Vec3 center, float radius, DistanceMetric metric, int r, int g, int b, int a, int face) {
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
            Vec3 v0 = poly.getFirst();
            for (int i = 1; i < poly.size() - 1; i++) {
                Vec3 v1 = poly.get(i);
                Vec3 v2 = poly.get(i + 1);

                buffer.addVertex(pose, (float) v0.x, (float) v0.y, (float) v0.z).setColor(r, g, b, a);
                buffer.addVertex(pose, (float) v1.x, (float) v1.y, (float) v1.z).setColor(r, g, b, a);
                buffer.addVertex(pose, (float) v2.x, (float) v2.y, (float) v2.z).setColor(r, g, b, a);
                buffer.addVertex(pose, (float) v2.x, (float) v2.y, (float) v2.z).setColor(r, g, b, a);
            }
        }
    }

    /**
     * Executes Sutherland-Hodgman polygon clipping against all 6 planes of the AABB bounding box.
     */
    private static List<Vec3> clipPolygon(List<Vec3> poly, AABB box) {
        poly = clipAgainstPlane(poly, 1, 0, 0, box.minX);
        poly = clipAgainstPlane(poly, -1, 0, 0, -box.maxX);
        poly = clipAgainstPlane(poly, 0, 1, 0, box.minY);
        poly = clipAgainstPlane(poly, 0, -1, 0, -box.maxY);
        poly = clipAgainstPlane(poly, 0, 0, 1, box.minZ);
        poly = clipAgainstPlane(poly, 0, 0, -1, -box.maxZ);
        return poly;
    }

    /**
     * Evaluates spatial coordinates against a specific geometric plane to discard out-of-bounds geometry.
     */
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

    /**
     * Evaluates matrix offsets and plots cubic point clusters matching spatial intensity values.
     */
    private static void renderFieldPoints(PoseStack poseStack, Level level, float partialTick, double cx, double cy, double cz, Map<Integer, SpellFieldDebugTracker.TrackedSpellField> activeFields, VertexConsumer buffer) {
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

            poseStack.pushPose();
            poseStack.translate(lerpPos.x - cx, lerpPos.y - cy, lerpPos.z - cz);
            Matrix4f pose = poseStack.last().pose();

            for (LocalVolume volume : trackedField.volumes) {
                Topology topology = volume.topology();
                if (topology == null) continue;

                AABB box = volume.bounds();
                double maxDim = Math.max(box.maxX - box.minX, Math.max(box.maxY - box.minY, box.maxZ - box.minZ));
                double step = Math.max(0.5, maxDim / 20.0);

                Vec3 absVolumeCenter = lerpPos.add(volume.volumeCenter());

                for (double x = box.minX; x <= box.maxX; x += step) {
                    for (double y = box.minY; y <= box.maxY; y += step) {
                        for (double z = box.minZ; z <= box.maxZ; z += step) {
                            Vec3 targetPos = lerpPos.add(x, y, z);
                            float scalar = topology.evaluateMultiplier(trackedField.enchantmentLevel, anchorEntity, absVolumeCenter, targetPos);

                            if (scalar > 0.05F) {
                                float size = 0.02F + (0.08F * scalar);

                                int r = (int) Mth.lerp(scalar, lr, hr);
                                int g = (int) Mth.lerp(scalar, lg, hg);
                                int b = (int) Mth.lerp(scalar, lb, hb);
                                int a = (int) Mth.lerp(scalar, la, ha);

                                Vec3 pt = new Vec3(x, y, z);
                                Vec3 p0 = pt.add(new Vec3(-size, size, -size));
                                Vec3 p1 = pt.add(new Vec3(-size, size, size));
                                Vec3 p2 = pt.add(new Vec3(size, size, size));
                                Vec3 p3 = pt.add(new Vec3(size, size, -size));
                                Vec3 p4 = pt.add(new Vec3(-size, -size, -size));
                                Vec3 p5 = pt.add(new Vec3(-size, -size, size));
                                Vec3 p6 = pt.add(new Vec3(size, -size, size));
                                Vec3 p7 = pt.add(new Vec3(size, -size, -size));

                                addQuad(buffer, pose, p0, p1, p2, p3, r, g, b, a);
                                addQuad(buffer, pose, p4, p7, p6, p5, r, g, b, a);
                                addQuad(buffer, pose, p5, p6, p2, p1, r, g, b, a);
                                addQuad(buffer, pose, p4, p0, p3, p7, r, g, b, a);
                                addQuad(buffer, pose, p4, p5, p1, p0, r, g, b, a);
                                addQuad(buffer, pose, p7, p3, p2, p6, r, g, b, a);
                            }
                        }
                    }
                }
            }
            poseStack.popPose();
        }
    }

    /**
     * Replaces standard 1D lines with 3D quad tubes to bypass hardware line shader limitations.
     */
    private static void drawThickLine(VertexConsumer buffer, Matrix4f pose, Vec3 p1, Vec3 p2, float thickness, int r, int g, int b, int a) {
        Vec3 dir = p2.subtract(p1);
        if (dir.lengthSqr() < 1e-5) return;
        dir = dir.normalize();

        Vec3 up = new Vec3(0, 1, 0);
        if (Math.abs(dir.dot(up)) > 0.99) up = new Vec3(1, 0, 0);
        Vec3 right = dir.cross(up).normalize().scale(thickness / 2.0);
        up = right.cross(dir).normalize().scale(thickness / 2.0);

        Vec3 v0 = p1.add(right).add(up);
        Vec3 v1 = p1.subtract(right).add(up);
        Vec3 v2 = p1.subtract(right).subtract(up);
        Vec3 v3 = p1.add(right).subtract(up);
        Vec3 v4 = p2.add(right).add(up);
        Vec3 v5 = p2.subtract(right).add(up);
        Vec3 v6 = p2.subtract(right).subtract(up);
        Vec3 v7 = p2.add(right).subtract(up);

        addQuad(buffer, pose, v0, v1, v2, v3, r, g, b, a);
        addQuad(buffer, pose, v7, v6, v5, v4, r, g, b, a);
        addQuad(buffer, pose, v4, v5, v1, v0, r, g, b, a);
        addQuad(buffer, pose, v3, v2, v6, v7, r, g, b, a);
        addQuad(buffer, pose, v0, v3, v7, v4, r, g, b, a);
        addQuad(buffer, pose, v5, v6, v2, v1, r, g, b, a);
    }

    /**
     * Submits an isolated quad face to the active vertex buffer.
     */
    private static void addQuad(VertexConsumer buffer, Matrix4f pose, Vec3 v1, Vec3 v2, Vec3 v3, Vec3 v4, int r, int g, int b, int a) {
        buffer.addVertex(pose, (float)v1.x, (float)v1.y, (float)v1.z).setColor(r, g, b, a);
        buffer.addVertex(pose, (float)v2.x, (float)v2.y, (float)v2.z).setColor(r, g, b, a);
        buffer.addVertex(pose, (float)v3.x, (float)v3.y, (float)v3.z).setColor(r, g, b, a);
        buffer.addVertex(pose, (float)v4.x, (float)v4.y, (float)v4.z).setColor(r, g, b, a);
    }
}