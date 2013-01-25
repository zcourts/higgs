package com.fillta.higgs.http.server.params;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Courtney Robinson <courtney@crlog.info>
 */
public class ResourcePathTest {
    @Test
    public void simple() {
        String uri = "/abc/123/yes";
        ResourcePath path = new ResourcePath(uri);
        assertEquals("Must create 3 components", 3, path.getComponents().length);
        for (ResourcePath.Component c : path.getComponents()) {
            assertFalse(c.isPattern());
            assertNull(c.getName());
            assertNull(c.getPattern());
        }
        assertEquals(uri, path.getUri());
    }

    @Test
    public void nameDefaultPattern() {
        String uri = "/abc/{name1}/yes/{name2}/";
        ResourcePath path = new ResourcePath(uri);
        assertEquals("Must create 3 components", 4, path.getComponents().length);
        int index = 0;
        for (ResourcePath.Component c : path.getComponents()) {
            if (index == 1) {
                assertNotNull(c.getName());
                assertEquals("name1", c.getName());
                //default pattern set if none provided
                assertTrue(c.isPattern());
                assertNotNull(c.getPattern());
            } else {
                if (index == 3) {
                    assertNotNull(c.getName());
                    assertEquals("name2", c.getName());
                    //default pattern set if none provided
                    assertTrue(c.isPattern());
                    assertNotNull(c.getPattern());
                } else {
                    assertNull(c.getName());
                    assertFalse(c.isPattern());
                    assertNull(c.getPattern());
                }
            }
            index++;
        }
        assertEquals(uri, path.getUri());
    }

    @Test
    public void nameWithPattern() {
        String uri = "/abc/{name1}/yes/{name2:[a-z0-9]}/";
        ResourcePath path = new ResourcePath(uri);
        assertEquals("Must create 3 components", 4, path.getComponents().length);
        int index = 0;
        for (ResourcePath.Component c : path.getComponents()) {
            if (index == 1) {
                assertNotNull(c.getName());
                assertEquals("name1", c.getName());
                assertNotNull(c.getPattern());
                assertTrue(c.isPattern());
            } else {
                if (index == 3) {
                    assertNotNull(c.getName());
                    assertEquals("name2", c.getName());
                    assertNotNull(c.getPattern());
                    assertTrue(c.isPattern());
                } else {
                    assertNull(c.getPattern());
                    assertFalse(c.isPattern());
                    assertNull(c.getName());
                }
            }
            index++;
        }
        assertEquals(uri, path.getUri());
    }

    @Test
    public void testMatchMixPattern() {
        String uri = "/abc/{name1}/yes/{name2:[a-z0-9]}/";
        ResourcePath path = new ResourcePath(uri);
        assertTrue("A single letter should match", path.matches("abc/random/yes/a"));
        assertTrue("A single number should match", path.matches("abc/random/yes/1"));
        assertFalse("Shouldn't match more than 1 character for last component", path.matches("abc/random/yes/ab"));
    }
}
