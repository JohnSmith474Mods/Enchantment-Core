package johnsmith.enchantmentcore.api.enchantment.spellfield.math;

import com.mojang.serialization.Codec;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.NotNull;

/**
 * Defines local and topological spatial reference axes and their corresponding kinetic impulse
 * unit vectors relative to a reference entity's orientation and an evaluation epicenter.
 */
public enum FieldAxis implements StringRepresentable {
    /**
     * Aligns with the reference entity's local up vector (or the global $+Y$ axis if no entity is present).
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
     * Aligns with the reference entity's direct line of sight (forward view vector, or $+Z$ if no entity is present).
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
     * Aligns with the reference entity's horizontal right vector ($\text{forward} \times \text{up}$, or $+X$ if no entity is present).
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
     * Points radially outward from {@code offsetEpicenter} toward {@code targetPos}.
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
     * Points tangentially around the {@link #VERTICAL} axis ($\vec{r} \times \hat{u}_{\text{vertical}}$),
     * producing horizontal yaw-plane orbital motion.
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
     * Points tangentially around the {@link #LATERAL} axis ($\vec{r} \times \hat{u}_{\text{lateral}}$),
     * producing longitudinal pitching orbital motion.
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
     * Points tangentially around the {@link #LONGITUDINAL} axis ($\vec{r} \times \hat{u}_{\text{longitudinal}}$),
     * producing lateral rolling orbital motion.
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

    /**
     * Codec for serializing and deserializing {@link FieldAxis} constants by their lowercase identifier.
     */
    public static final Codec<FieldAxis> CODEC = StringRepresentable.fromEnum(FieldAxis::values);

    /** The serialized string identifier of this axis. */
    private final String name;

    FieldAxis(String name) {
        this.name = name;
    }

    /**
     * Retrieves the normalized primary reference axis vector based on the entity's orientation.
     *
     * @param spatialReference The entity defining the local coordinate frame, or {@code null} for world-axis defaults.
     * @return The normalized direction vector.
     */
    public abstract Vec3 getReferenceAxis(Entity spatialReference);

    /**
     * Computes the normalized kinetic impulse vector at {@code targetPos} relative to {@code offsetEpicenter}.
     *
     * @param spatialReference The entity defining the local coordinate frame, or {@code null}.
     * @param offsetEpicenter  The world-space origin point of the impulse field.
     * @param targetPos        The world-space position of the target being evaluated.
     * @return The normalized impulse direction vector, or {@link Vec3#ZERO} if degenerate.
     */
    public abstract Vec3 getImpulseVector(Entity spatialReference, Vec3 offsetEpicenter, Vec3 targetPos);

    @Override
    public @NotNull String getSerializedName() {
        return this.name;
    }
}