package net.openstage;

import net.openstage.core.math.InverseKinematicsSolver;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InverseKinematicsTest {
    @Test
    void testSingleBoneReachingStraightDown() {
        Vector3f hip = new Vector3f(0.0f, 12.0f, 0.0f);
        Vector3f foot = new Vector3f(0.0f, 0.0f, 0.0f);
        float[] rot = InverseKinematicsSolver.solveSingleBone(hip, foot);
        assertEquals(0.0f, rot[0], 0.01f);
        assertEquals(0.0f, rot[1], 0.01f);
    }

    @Test
    void testTwoBoneSolver() {
        Vector3f root = new Vector3f(0.0f, 12.0f, 0.0f);
        Vector3f target = new Vector3f(0.0f, 4.0f, 3.0f);
        var result = InverseKinematicsSolver.solveTwoBone(root, target, 6.0f, 6.0f, true);
        assertNotNull(result);
        assertTrue(!Float.isNaN(result.rootPitch()));
        assertTrue(!Float.isNaN(result.childPitch()));
    }
}

