package net.openstage;

import net.openstage.core.math.TransformRig;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("TransformRig Hierarchy, Alias, and Euler Rotation Tests")
class TransformRigTest {
    private static final float EPSILON = 1e-5f;

    @Test
    @DisplayName("Verify bone hierarchy lookups for canonical bones")
    void testBoneHierarchyLookups() {
        assertNull(TransformRig.getParentBone(TransformRig.BONE_ROOT));

        assertEquals(TransformRig.BONE_ROOT, TransformRig.getParentBone(TransformRig.BONE_BODY));
        assertEquals(TransformRig.BONE_BODY, TransformRig.getParentBone(TransformRig.BONE_HEAD));
        assertEquals(TransformRig.BONE_BODY, TransformRig.getParentBone(TransformRig.BONE_RIGHT_ARM));
        assertEquals(TransformRig.BONE_BODY, TransformRig.getParentBone(TransformRig.BONE_LEFT_ARM));
        assertEquals(TransformRig.BONE_BODY, TransformRig.getParentBone(TransformRig.BONE_RIGHT_LEG));
        assertEquals(TransformRig.BONE_BODY, TransformRig.getParentBone(TransformRig.BONE_LEFT_LEG));

        assertEquals(TransformRig.BONE_HEAD, TransformRig.getParentBone(TransformRig.BONE_HAT));
        assertEquals(TransformRig.BONE_BODY, TransformRig.getParentBone(TransformRig.BONE_JACKET));
        assertEquals(TransformRig.BONE_RIGHT_ARM, TransformRig.getParentBone(TransformRig.BONE_RIGHT_SLEEVE));
        assertEquals(TransformRig.BONE_LEFT_ARM, TransformRig.getParentBone(TransformRig.BONE_LEFT_SLEEVE));
        assertEquals(TransformRig.BONE_RIGHT_LEG, TransformRig.getParentBone(TransformRig.BONE_RIGHT_PANTS));
        assertEquals(TransformRig.BONE_LEFT_LEG, TransformRig.getParentBone(TransformRig.BONE_LEFT_PANTS));

        assertNull(TransformRig.getParentBone("unknown_bone"));
        assertNull(TransformRig.getParentBone(null));
    }

    @Test
    @DisplayName("Hierarchy lookup resolves aliases to parent bone")
    void testParentBoneWithAliases() {
        assertEquals(TransformRig.BONE_ROOT, TransformRig.getParentBone("torso"));
        assertEquals(TransformRig.BONE_ROOT, TransformRig.getParentBone("chest"));

        assertEquals(TransformRig.BONE_HEAD, TransformRig.getParentBone("helmet"));

        assertEquals(TransformRig.BONE_BODY, TransformRig.getParentBone("rightarm"));
    }

    @ParameterizedTest
    @CsvSource({
            "torso, body",
            "chest, body",
            "rightarm, right_arm",
            "leftarm, left_arm",
            "rightleg, right_leg",
            "leftleg, left_leg",
            "right_hand, right_arm",
            "left_hand, left_arm",
            "helmet, hat"
    })
    @DisplayName("Verify alias resolution to canonical bone names")
    void testAliasResolution(String alias, String expectedCanonical) {
        assertEquals(expectedCanonical, TransformRig.resolveAlias(alias));

        assertEquals(expectedCanonical, TransformRig.resolveAlias(alias.toUpperCase()));
    }

    @Test
    @DisplayName("Canonical names and unknown names remain unchanged by resolveAlias")
    void testResolveAliasEdgeCases() {
        assertEquals("body", TransformRig.resolveAlias("body"));
        assertEquals("head", TransformRig.resolveAlias("head"));
        assertEquals("non_existent_bone", TransformRig.resolveAlias("non_existent_bone"));
        assertEquals("", TransformRig.resolveAlias(""));
        assertNull(TransformRig.resolveAlias(null));
    }

    @Test
    @DisplayName("Verify overlay bone classification")
    void testOverlayBones() {
        assertTrue(TransformRig.isOverlayBone(TransformRig.BONE_HAT));
        assertTrue(TransformRig.isOverlayBone(TransformRig.BONE_JACKET));
        assertTrue(TransformRig.isOverlayBone(TransformRig.BONE_RIGHT_SLEEVE));
        assertTrue(TransformRig.isOverlayBone(TransformRig.BONE_LEFT_SLEEVE));
        assertTrue(TransformRig.isOverlayBone(TransformRig.BONE_RIGHT_PANTS));
        assertTrue(TransformRig.isOverlayBone(TransformRig.BONE_LEFT_PANTS));

        assertTrue(TransformRig.isOverlayBone("helmet"));

        assertFalse(TransformRig.isOverlayBone(TransformRig.BONE_ROOT));
        assertFalse(TransformRig.isOverlayBone(TransformRig.BONE_BODY));
        assertFalse(TransformRig.isOverlayBone(TransformRig.BONE_HEAD));
        assertFalse(TransformRig.isOverlayBone(TransformRig.BONE_RIGHT_ARM));
        assertFalse(TransformRig.isOverlayBone(TransformRig.BONE_LEFT_LEG));

        assertFalse(TransformRig.isOverlayBone("unknown"));
        assertFalse(TransformRig.isOverlayBone(null));
    }

    @Test
    @DisplayName("Verify isKnownBone for canonical and aliased bones")
    void testIsKnownBone() {
        assertTrue(TransformRig.isKnownBone("body"));
        assertTrue(TransformRig.isKnownBone("torso"));
        assertTrue(TransformRig.isKnownBone("TORSO"));
        assertTrue(TransformRig.isKnownBone("helmet"));
        assertFalse(TransformRig.isKnownBone("unknown_bone"));
        assertFalse(TransformRig.isKnownBone(null));
    }

    @Test
    @DisplayName("getAllBoneNames returns all canonical bones defensively")
    void testGetAllBoneNames() {
        String[] bones1 = TransformRig.getAllBoneNames();
        assertEquals(17, bones1.length);
        List<String> list = Arrays.asList(bones1);
        assertTrue(list.contains(TransformRig.BONE_ROOT));
        assertTrue(list.contains(TransformRig.BONE_BODY));
        assertTrue(list.contains(TransformRig.BONE_HEAD));
        assertTrue(list.contains(TransformRig.BONE_HAT));
        assertTrue(list.contains(TransformRig.BONE_RIGHT_FOREARM));
        assertTrue(list.contains(TransformRig.BONE_LEFT_LOWER_LEG));
        bones1[0] = "mutated";
        String[] bones2 = TransformRig.getAllBoneNames();
        assertNotEquals("mutated", bones2[0]);
    }

    @Test
    @DisplayName("eulerYZXToQuaternion: (0,0,0) produces identity quaternion")
    void testEulerIdentity() {
        Quaternionf q = TransformRig.eulerYZXToQuaternion(0.0f, 0.0f, 0.0f);
        assertEquals(0.0f, q.x, EPSILON);
        assertEquals(0.0f, q.y, EPSILON);
        assertEquals(0.0f, q.z, EPSILON);
        assertEquals(1.0f, q.w, EPSILON);
        Vector3f v = new Vector3f(1.0f, 2.0f, 3.0f);
        q.transform(v);
        assertEquals(1.0f, v.x, EPSILON);
        assertEquals(2.0f, v.y, EPSILON);
        assertEquals(3.0f, v.z, EPSILON);
    }

    @Test
    @DisplayName("eulerYZXToQuaternion: single axis rotations (Y, Z, X)")
    void testEulerSingleAxisRotations() {
        Quaternionf qY = TransformRig.eulerYZXToQuaternion(90.0f, 0.0f, 0.0f);
        Vector3f vY = new Vector3f(1.0f, 0.0f, 0.0f);
        qY.transform(vY);
        assertEquals(0.0f, vY.x, EPSILON);
        assertEquals(0.0f, vY.y, EPSILON);
        assertEquals(-1.0f, vY.z, EPSILON);
        Quaternionf qZ = TransformRig.eulerYZXToQuaternion(0.0f, 90.0f, 0.0f);
        Vector3f vZ = new Vector3f(1.0f, 0.0f, 0.0f);
        qZ.transform(vZ);
        assertEquals(0.0f, vZ.x, EPSILON);
        assertEquals(1.0f, vZ.y, EPSILON);
        assertEquals(0.0f, vZ.z, EPSILON);
        Quaternionf qX = TransformRig.eulerYZXToQuaternion(0.0f, 0.0f, 90.0f);
        Vector3f vX = new Vector3f(0.0f, 1.0f, 0.0f);
        qX.transform(vX);
        assertEquals(0.0f, vX.x, EPSILON);
        assertEquals(0.0f, vX.y, EPSILON);
        assertEquals(1.0f, vX.z, EPSILON);
    }

    @Test
    @DisplayName("eulerYZXToQuaternion: intrinsic YZX rotation order validation")
    void testEulerIntrinsicYZXOrder() {
        Quaternionf q = TransformRig.eulerYZXToQuaternion(90.0f, 90.0f, 0.0f);
        Vector3f v1 = new Vector3f(1.0f, 0.0f, 0.0f);
        q.transform(v1);
        assertEquals(0.0f, v1.x, EPSILON);
        assertEquals(1.0f, v1.y, EPSILON);
        assertEquals(0.0f, v1.z, EPSILON);
        Vector3f v2 = new Vector3f(0.0f, 1.0f, 0.0f);
        q.transform(v2);
        assertEquals(0.0f, v2.x, EPSILON);
        assertEquals(0.0f, v2.y, EPSILON);
        assertEquals(1.0f, v2.z, EPSILON);
        Vector3f v3 = new Vector3f(0.0f, 0.0f, 1.0f);
        q.transform(v3);
        assertEquals(1.0f, v3.x, EPSILON);
        assertEquals(0.0f, v3.y, EPSILON);
        assertEquals(0.0f, v3.z, EPSILON);
    }

    @Test
    @DisplayName("eulerYZXToQuaternion: quaternion normalization invariant is preserved")
    void testQuaternionUnitLength() {
        float[][] angles = {
                {45.0f, 30.0f, 60.0f},
                {-120.0f, 45.0f, -90.0f},
                {180.0f, 0.0f, -180.0f},
                {360.0f, 720.0f, -360.0f}
        };
        for (float[] a : angles) {
            Quaternionf q = TransformRig.eulerYZXToQuaternion(a[0], a[1], a[2]);
            float lengthSquared = q.x * q.x + q.y * q.y + q.z * q.z + q.w * q.w;
            assertEquals(1.0f, lengthSquared, EPSILON,
                    "Quaternion must be normalized for angles: " + Arrays.toString(a));
        }
    }
}

