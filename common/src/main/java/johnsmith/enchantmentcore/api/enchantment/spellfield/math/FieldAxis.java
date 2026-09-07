package johnsmith.enchantmentcore.api.enchantment.spellfield.math;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Defines spatial alignment vectors and corresponding impulse calculation vectors based on entity reference frames.
 */
public enum FieldAxis implements StringRepresentable {
    /**
     * Aligns with the absolute global Y-axis.
     */
    VERTICAL("vertical") {
        @Override
        public Vec3 getReferenceAxis(Entity spatialReference) {
            return spatialReference != null ? spatialReference.getUpVector(1.0F).normalize() : new Vec3(0, 1, 0);
        }

        @Override
        public Vec3 getImpulseVector(Entity spatialReference, Vec3 offsetEpicenter, Vec3 targetPos) {
            return this.getReferenceAxis(spatialReference);
        }
    },
    /**
     * Aligns with the entity's direct line of sight (forward vector).
     */
    LONGITUDINAL("longitudinal") {
        @Override
        public Vec3 getReferenceAxis(Entity spatialReference) {
            return spatialReference != null ? spatialReference.getViewVector(1.0F).normalize() : new Vec3(0, 0, 1);
        }

        @Override
        public Vec3 getImpulseVector(Entity spatialReference, Vec3 offsetEpicenter, Vec3 targetPos) {
            return this.getReferenceAxis(spatialReference);
        }
    },
    /**
     * Aligns with the entity's horizontal lateral plane (right vector).
     */
    LATERAL("lateral") {
        @Override
        public Vec3 getReferenceAxis(Entity spatialReference) {
            if (spatialReference != null) {
                Vec3 fwd = spatialReference.getViewVector(1.0F).normalize();
                Vec3 up = spatialReference.getUpVector(1.0F).normalize();
                return fwd.cross(up).normalize();
            }
            return new Vec3(1, 0, 0);
        }

        @Override
        public Vec3 getImpulseVector(Entity spatialReference, Vec3 offsetEpicenter, Vec3 targetPos) {
            return this.getReferenceAxis(spatialReference);
        }
    },
    /**
     * Aligns with the vector extending outward from the evaluation epicenter toward the target.
     */
    RADIAL("radial") {
        @Override
        public Vec3 getReferenceAxis(Entity spatialReference) {
            return VERTICAL.getReferenceAxis(spatialReference);
        }

        @Override
        public Vec3 getImpulseVector(Entity spatialReference, Vec3 offsetEpicenter, Vec3 targetPos) {
            Vec3 vectorToVictim = targetPos.subtract(offsetEpicenter);
            return vectorToVictim.lengthSqr() > 0.0001D ? vectorToVictim.normalize() : Vec3.ZERO;
        }
    },
    /**
     * Defines a vector tangent to the radius on the horizontal plane (orbital motion).
     */
    AZIMUTHAL("azimuthal") {
        @Override
        public Vec3 getReferenceAxis(Entity spatialReference) {
            return VERTICAL.getReferenceAxis(spatialReference);
        }

        @Override
        public Vec3 getImpulseVector(Entity spatialReference, Vec3 offsetEpicenter, Vec3 targetPos) {
            Vec3 vectorToVictim = targetPos.subtract(offsetEpicenter);
            Vec3 tangent = vectorToVictim.cross(this.getReferenceAxis(spatialReference));
            return tangent.lengthSqr() > 0.0001D ? tangent.normalize() : Vec3.ZERO;
        }
    },
    /**
     * Defines a vector tangent to the radius on the longitudinal plane (pitching orbit).
     */
    NUTATIONAL("nutational") {
        @Override
        public Vec3 getReferenceAxis(Entity spatialReference) {
            return LATERAL.getReferenceAxis(spatialReference);
        }

        @Override
        public Vec3 getImpulseVector(Entity spatialReference, Vec3 offsetEpicenter, Vec3 targetPos) {
            Vec3 vectorToVictim = targetPos.subtract(offsetEpicenter);
            Vec3 tangent = vectorToVictim.cross(this.getReferenceAxis(spatialReference));
            return tangent.lengthSqr() > 0.0001D ? tangent.normalize() : Vec3.ZERO;
        }
    },
    /**
     * Defines a vector tangent to the radius on the lateral plane (rolling orbit).
     */
    TORSIONAL("torsional") {
        @Override
        public Vec3 getReferenceAxis(Entity spatialReference) {
            return LONGITUDINAL.getReferenceAxis(spatialReference);
        }

        @Override
        public Vec3 getImpulseVector(Entity spatialReference, Vec3 offsetEpicenter, Vec3 targetPos) {
            Vec3 vectorToVictim = targetPos.subtract(offsetEpicenter);
            Vec3 tangent = vectorToVictim.cross(this.getReferenceAxis(spatialReference));
            return tangent.lengthSqr() > 0.0001D ? tangent.normalize() : Vec3.ZERO;
        }
    };

    public static final Codec<FieldAxis> CODEC = StringRepresentable.fromEnum(FieldAxis::values);
    private final String name;

    FieldAxis(String name) {
        this.name = name;
    }

    /**
     * Retrieves the core directional vector mapping for this axis based on the entity's orientation.
     *
     * @param spatialReference The entity defining the reference frame.
     * @return The normalized direction vector.
     */
    public abstract Vec3 getReferenceAxis(Entity spatialReference);

    /**
     * Calculates the kinetic impulse vector generated along this axis.
     *
     * @param spatialReference The entity defining the reference frame.
     * @param offsetEpicenter  The absolute origin point of the impulse force.
     * @param targetPos        The absolute spatial position of the target.
     * @return The normalized impulse vector.
     */
    public abstract Vec3 getImpulseVector(Entity spatialReference, Vec3 offsetEpicenter, Vec3 targetPos);

    @Override
    public String getSerializedName() {
        return this.name;
    }
}