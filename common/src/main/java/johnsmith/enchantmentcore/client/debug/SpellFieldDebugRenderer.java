package johnsmith.enchantmentcore.client.debug;

import com.mojang.blaze3d.vertex.*;

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

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShapeRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class SpellFieldDebugRenderer {

    public static void render(PoseStack poseStack, Camera camera, float partialTick) {
        if (!Config.ENABLE_DEBUG_RENDERER.get()) return;

        if (!Minecraft.getInstance().getEntityRenderDispatcher().shouldRenderHitBoxes()) return;

        Map<Integer, SpellFieldDebugTracker.TrackedSpellField> activeFields = SpellFieldDebugTracker.getActiveShapes();
        if (activeFields.isEmpty()) return;

        Level level = Minecraft.getInstance().level;
        if (level == null) return;

        Vec3 cameraPos = camera.getPosition();
        double cx = cameraPos.x();
        double cy = cameraPos.y();
        double cz = cameraPos.z();

        renderLines(poseStack, level, partialTick, cx, cy, cz, activeFields);
        renderSurfaces(poseStack, level, partialTick, cx, cy, cz, activeFields);
        renderFieldPoints(poseStack, level, partialTick, cx, cy, cz, activeFields);
        renderVectorFields(poseStack, level, partialTick, cx, cy, cz, activeFields);
    }

    private static void renderLines(PoseStack poseStack, Level level, float partialTick, double cx, double cy, double cz, Map<Integer, SpellFieldDebugTracker.TrackedSpellField> activeFields) {
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
        boolean hasGeometry = false;

        int bbCol = Config.BOUNDING_BOX_COLOR.get();
        float bbA = ((bbCol >> 24) & 0xFF) / 255.0F;
        float bbR = ((bbCol >> 16) & 0xFF) / 255.0F;
        float bbG = ((bbCol >> 8) & 0xFF) / 255.0F;
        float bbB = (bbCol & 0xFF) / 255.0F;

        int xCol = Config.ORIGIN_X_COLOR.get();
        int xr = (xCol >> 16) & 0xFF; int xg = (xCol >> 8) & 0xFF; int xb = xCol & 0xFF;

        int yCol = Config.ORIGIN_Y_COLOR.get();
        int yr = (yCol >> 16) & 0xFF; int yg = (yCol >> 8) & 0xFF; int yb = yCol & 0xFF;

        int zCol = Config.ORIGIN_Z_COLOR.get();
        int zr = (zCol >> 16) & 0xFF; int zg = (zCol >> 8) & 0xFF; int zb = zCol & 0xFF;

        int aCol = Config.AXIS_LINE_COLOR.get();
        int ar = (aCol >> 16) & 0xFF; int ag = (aCol >> 8) & 0xFF; int ab = aCol & 0xFF;

        for (Map.Entry<Integer, SpellFieldDebugTracker.TrackedSpellField> entry : activeFields.entrySet()) {
            SpellFieldDebugTracker.TrackedSpellField trackedField = entry.getValue();
            Entity anchorEntity = trackedField.anchorEntity;
            if (anchorEntity == null) continue;

            Entity clientEntity = level.getEntity(anchorEntity.getId());
            if (clientEntity != null) {
                anchorEntity = clientEntity;
            }

            Vec3 lerpPos = anchorEntity.getPosition(partialTick);

            for (LocalVolume volume : trackedField.volumes) {
                poseStack.pushPose();
                poseStack.translate(lerpPos.x - cx, lerpPos.y - cy, lerpPos.z - cz);
                ShapeRenderer.renderLineBox(
                        poseStack, buffer,
                        volume.bounds().minX, volume.bounds().minY, volume.bounds().minZ,
                        volume.bounds().maxX, volume.bounds().maxY, volume.bounds().maxZ,
                        bbR, bbG, bbB, bbA
                );
                poseStack.popPose();

                if (volume.topology() != null) {
                    Topology topology = volume.topology();
                    float originRange = topology.originRange().calculate(trackedField.enchantmentLevel);
                    if (originRange <= 0.0F) {
                        hasGeometry = true;
                        continue;
                    }

                    Vec3 absVolumeCenter = lerpPos.add(volume.volumeCenter());
                    Vec3 absOrigin = topology.getOrigin(trackedField.enchantmentLevel, anchorEntity, absVolumeCenter);

                    poseStack.pushPose();
                    poseStack.translate(absOrigin.x - cx, absOrigin.y - cy, absOrigin.z - cz);
                    Matrix4f pose = poseStack.last().pose();

                    float s = 0.5F;

                    buffer.addVertex(pose, -s, 0, 0).setColor(xr, xg, xb, 255).setNormal(poseStack.last(), 1, 0, 0);
                    buffer.addVertex(pose, s, 0, 0).setColor(xr, xg, xb, 255).setNormal(poseStack.last(), 1, 0, 0);
                    buffer.addVertex(pose, 0, -s, 0).setColor(yr, yg, yb, 255).setNormal(poseStack.last(), 0, 1, 0);
                    buffer.addVertex(pose, 0, s, 0).setColor(yr, yg, yb, 255).setNormal(poseStack.last(), 0, 1, 0);
                    buffer.addVertex(pose, 0, 0, -s).setColor(zr, zg, zb, 255).setNormal(poseStack.last(), 0, 0, 1);
                    buffer.addVertex(pose, 0, 0, s).setColor(zr, zg, zb, 255).setNormal(poseStack.last(), 0, 0, 1);

                    if (topology.axis().isPresent()) {
                        Vec3 dir = topology.axis().get().resolve(trackedField.enchantmentLevel, anchorEntity);
                        if (dir.lengthSqr() > 0.0001D) {
                            dir = dir.normalize();

                            float axialRange = topology.axialRange().map(v -> v.calculate(trackedField.enchantmentLevel)).orElse(originRange);

                            buffer.addVertex(pose, 0, 0, 0).setColor(ar, ag, ab, 255).setNormal(poseStack.last(), (float) dir.x, (float) dir.y, (float) dir.z);
                            buffer.addVertex(pose, (float) (dir.x * axialRange), (float) (dir.y * axialRange), (float) (dir.z * axialRange)).setColor(ar, ag, ab, 255).setNormal(poseStack.last(), (float) dir.x, (float) dir.y, (float) dir.z);
                        }
                    }
                    poseStack.popPose();
                }
                hasGeometry = true;
            }
        }

        if (hasGeometry) {
            try {
                RenderType.lines().draw(buffer.buildOrThrow());
            } catch (Exception e) {
                tesselator.clear();
            }
        } else {
            tesselator.clear();
        }
    }

    private static void renderSurfaces(PoseStack poseStack, Level level, float partialTick, double cx, double cy, double cz, Map<Integer, SpellFieldDebugTracker.TrackedSpellField> activeFields) {
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        boolean hasGeometry = false;

        int planeCol = Config.TOPOLOGY_PLANE_COLOR.get();
        int pr = (planeCol >> 16) & 0xFF; int pg = (planeCol >> 8) & 0xFF; int pb = planeCol & 0xFF; int pa = (planeCol >> 24) & 0xFF;

        int sphCol = Config.TOPOLOGY_SPHERE_COLOR.get();
        int sr = (sphCol >> 16) & 0xFF; int sg = (sphCol >> 8) & 0xFF; int sb = sphCol & 0xFF; int sa = (sphCol >> 24) & 0xFF;

        for (Map.Entry<Integer, SpellFieldDebugTracker.TrackedSpellField> entry : activeFields.entrySet()) {
            SpellFieldDebugTracker.TrackedSpellField trackedField = entry.getValue();
            Entity anchorEntity = trackedField.anchorEntity;
            if (anchorEntity == null) continue;

            Entity clientEntity = level.getEntity(anchorEntity.getId());
            if (clientEntity != null) {
                anchorEntity = clientEntity;
            }

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
                            hasGeometry = true;
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
                    hasGeometry = true;
                }
            }
        }

        if (hasGeometry) {
            try {
                RenderType.debugQuads().draw(buffer.buildOrThrow());
            } catch (Exception e) {
                tesselator.clear();
            }
        } else {
            tesselator.clear();
        }
    }

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

    private static void renderFieldPoints(PoseStack poseStack, Level level, float partialTick, double cx, double cy, double cz, Map<Integer, SpellFieldDebugTracker.TrackedSpellField> activeFields) {
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        boolean hasGeometry = false;

        int highCol = Config.FIELD_POINT_HIGH_COLOR.get();
        int hr = (highCol >> 16) & 0xFF, hg = (highCol >> 8) & 0xFF, hb = highCol & 0xFF, ha = (highCol >> 24) & 0xFF;

        int lowCol = Config.FIELD_POINT_LOW_COLOR.get();
        int lr = (lowCol >> 16) & 0xFF, lg = (lowCol >> 8) & 0xFF, lb = lowCol & 0xFF, la = (lowCol >> 24) & 0xFF;

        for (Map.Entry<Integer, SpellFieldDebugTracker.TrackedSpellField> entry : activeFields.entrySet()) {
            SpellFieldDebugTracker.TrackedSpellField trackedField = entry.getValue();
            Entity anchorEntity = trackedField.anchorEntity;
            if (anchorEntity == null) continue;

            Entity clientEntity = level.getEntity(anchorEntity.getId());
            if (clientEntity != null) {
                anchorEntity = clientEntity;
            }

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

                                float fx = (float) x;
                                float fy = (float) y;
                                float fz = (float) z;

                                buffer.addVertex(pose, fx - size, fy + size, fz - size).setColor(r, g, b, a);
                                buffer.addVertex(pose, fx - size, fy + size, fz + size).setColor(r, g, b, a);
                                buffer.addVertex(pose, fx + size, fy + size, fz + size).setColor(r, g, b, a);
                                buffer.addVertex(pose, fx + size, fy + size, fz - size).setColor(r, g, b, a);

                                buffer.addVertex(pose, fx - size, fy - size, fz - size).setColor(r, g, b, a);
                                buffer.addVertex(pose, fx + size, fy - size, fz - size).setColor(r, g, b, a);
                                buffer.addVertex(pose, fx + size, fy - size, fz + size).setColor(r, g, b, a);
                                buffer.addVertex(pose, fx - size, fy - size, fz + size).setColor(r, g, b, a);

                                buffer.addVertex(pose, fx - size, fy - size, fz + size).setColor(r, g, b, a);
                                buffer.addVertex(pose, fx + size, fy - size, fz + size).setColor(r, g, b, a);
                                buffer.addVertex(pose, fx + size, fy + size, fz + size).setColor(r, g, b, a);
                                buffer.addVertex(pose, fx - size, fy + size, fz + size).setColor(r, g, b, a);

                                buffer.addVertex(pose, fx - size, fy - size, fz - size).setColor(r, g, b, a);
                                buffer.addVertex(pose, fx - size, fy + size, fz - size).setColor(r, g, b, a);
                                buffer.addVertex(pose, fx + size, fy + size, fz - size).setColor(r, g, b, a);
                                buffer.addVertex(pose, fx + size, fy - size, fz - size).setColor(r, g, b, a);

                                buffer.addVertex(pose, fx - size, fy - size, fz - size).setColor(r, g, b, a);
                                buffer.addVertex(pose, fx - size, fy - size, fz + size).setColor(r, g, b, a);
                                buffer.addVertex(pose, fx - size, fy + size, fz + size).setColor(r, g, b, a);
                                buffer.addVertex(pose, fx - size, fy + size, fz - size).setColor(r, g, b, a);

                                buffer.addVertex(pose, fx + size, fy - size, fz - size).setColor(r, g, b, a);
                                buffer.addVertex(pose, fx + size, fy + size, fz - size).setColor(r, g, b, a);
                                buffer.addVertex(pose, fx + size, fy + size, fz + size).setColor(r, g, b, a);
                                buffer.addVertex(pose, fx + size, fy - size, fz + size).setColor(r, g, b, a);

                                hasGeometry = true;
                            }
                        }
                    }
                }
            }
            poseStack.popPose();
        }

        if (hasGeometry) {
            try {
                RenderType.debugQuads().draw(buffer.buildOrThrow());
            } catch (Exception e) {
                tesselator.clear();
            }
        } else {
            tesselator.clear();
        }
    }

    private static void renderVectorFields(PoseStack poseStack, Level level, float partialTick, double cx, double cy, double cz, Map<Integer, SpellFieldDebugTracker.TrackedSpellField> activeFields) {
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);
        boolean hasGeometry = false;

        int stemCol = Config.VECTOR_STEM_COLOR.get();
        int sr = (stemCol >> 16) & 0xFF, sg = (stemCol >> 8) & 0xFF, sb = stemCol & 0xFF, sa = (stemCol >> 24) & 0xFF;

        int tipCol = Config.VECTOR_TIP_COLOR.get();
        int tr = (tipCol >> 16) & 0xFF, tg = (tipCol >> 8) & 0xFF, tb = tipCol & 0xFF, ta = (tipCol >> 24) & 0xFF;

        for (Map.Entry<Integer, SpellFieldDebugTracker.TrackedSpellField> entry : activeFields.entrySet()) {
            SpellFieldDebugTracker.TrackedSpellField trackedField = entry.getValue();
            if (trackedField.vectorFields.isEmpty()) continue;

            Entity anchorEntity = trackedField.anchorEntity;
            if (anchorEntity == null) continue;

            Entity clientEntity = level.getEntity(anchorEntity.getId());
            if (clientEntity != null) {
                anchorEntity = clientEntity;
            }

            Vec3 lerpPos = anchorEntity.getPosition(partialTick);
            Vec3 absEpicenter = lerpPos.add(0, anchorEntity.getBbHeight() / 2.0, 0);

            poseStack.pushPose();
            poseStack.translate(lerpPos.x - cx, lerpPos.y - cy, lerpPos.z - cz);
            Matrix4f pose = poseStack.last().pose();

            for (LocalVolume volume : trackedField.volumes) {
                AABB box = volume.bounds();

                double maxDim = Math.max(box.maxX - box.minX, Math.max(box.maxY - box.minY, box.maxZ - box.minZ));
                double step = Math.max(1.0, maxDim / 6.0);

                Vec3 absVolumeCenter = lerpPos.add(volume.volumeCenter());

                for (double x = box.minX; x <= box.maxX; x += step) {
                    for (double y = box.minY; y <= box.maxY; y += step) {
                        for (double z = box.minZ; z <= box.maxZ; z += step) {
                            Vec3 targetPos = lerpPos.add(x, y, z);

                            if (volume.topology() != null) {
                                float scalar = volume.topology().evaluateMultiplier(trackedField.enchantmentLevel, anchorEntity, absVolumeCenter, targetPos);
                                if (scalar <= 0.05F) continue;
                            }

                            for (FieldAxis axis : trackedField.vectorFields) {
                                Vec3 direction = axis.getImpulseVector(anchorEntity, absEpicenter, targetPos);
                                if (direction.lengthSqr() < 0.0001D) continue;

                                float length = 0.4F;
                                float dx = (float) direction.x * length;
                                float dy = (float) direction.y * length;
                                float dz = (float) direction.z * length;

                                float px = (float) x;
                                float py = (float) y;
                                float pz = (float) z;

                                buffer.addVertex(pose, px, py, pz).setColor(sr, sg, sb, sa).setNormal(poseStack.last(), dx, dy, dz);
                                buffer.addVertex(pose, px + dx, py + dy, pz + dz).setColor(sr, sg, sb, sa).setNormal(poseStack.last(), dx, dy, dz);

                                buffer.addVertex(pose, px + dx, py + dy, pz + dz).setColor(tr, tg, tb, ta).setNormal(poseStack.last(), dx, dy, dz);
                                buffer.addVertex(pose, px + dx + (dx * 0.2F), py + dy + (dy * 0.2F), pz + dz + (dz * 0.2F)).setColor(tr, tg, tb, ta).setNormal(poseStack.last(), dx, dy, dz);

                                hasGeometry = true;
                            }
                        }
                    }
                }
            }
            poseStack.popPose();
        }

        if (hasGeometry) {
            try {
                RenderType.lines().draw(buffer.buildOrThrow());
            } catch (Exception e) {
                tesselator.clear();
            }
        } else {
            tesselator.clear();
        }
    }
}