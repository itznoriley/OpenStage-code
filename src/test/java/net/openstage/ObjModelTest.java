package net.openstage;

import org.junit.jupiter.api.Test;
import java.io.File;
import static org.junit.jupiter.api.Assertions.*;

public class ObjModelTest {
    @Test
    public void testSolarPanelObjParsing() {
        File f = new File("run/openstage/models/solarpanel.obj");
        if (!f.exists()) {
            return;
        }

        net.openstage.client.model.ObjModel model = net.openstage.client.model.ObjModel.load(f);
        assertNotNull(model);
        assertFalse(model.getTriangles().isEmpty());
        assertEquals("solarpanel.obj", model.getName());
        assertEquals(96, model.getTriangles().size());
        assertTrue(model.getWidth() > 0.0f);
        assertTrue(model.getHeight() > 0.0f);
        assertTrue(model.getDepth() > 0.0f);
    }
}

