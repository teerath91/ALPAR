/*
 * AutoRefactor - Eclipse plugin to automatically refactor Java code bases.
 *
 * Copyright (C) 2015 Jean-Noël Rouvignac - initial API and implementation
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program under LICENSE-GNUGPL.  If not, see
 * <http://www.gnu.org/licenses/>.
 *
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution under LICENSE-ECLIPSE, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.autorefactor.refactoring.rules.samples_out;

import static org.testng.Assert.*;

import org.testng.Assert;

public class TestNGAssertSample {

    public void shouldRefactorWithPrimitives(int i1, int i2) throws Exception {
        Assert.assertEquals(i1, i2);
        Assert.assertEquals(i1, i2, "Failure message to keep");
        Assert.assertNotEquals(i1, i2);
        Assert.assertNotEquals(i1, i2, "Failure message to keep");
        Assert.assertEquals(i1, i2);
        Assert.assertEquals(i1, i2, "Failure message to keep");
        Assert.assertNotEquals(i1, i2);
        Assert.assertNotEquals(i1, i2, "Failure message to keep");

        assertEquals(i1, i2);
        assertEquals(i1, i2, "Failure message to keep");
        assertNotEquals(i1, i2);
        assertNotEquals(i1, i2, "Failure message to keep");
        assertEquals(i1, i2);
        assertEquals(i1, i2, "Failure message to keep");
        assertNotEquals(i1, i2);
        assertNotEquals(i1, i2, "Failure message to keep");
    }

    public void shouldRefactorFailures() throws Exception {
        Assert.fail();
        Assert.fail("Failure message to keep");
        Assert.fail();
        Assert.fail("Failure message to keep");

        fail();
        fail("Failure message to keep");
        fail();
        fail("Failure message to keep");
    }

    public void shouldRemoveDeadChecks() throws Exception {
    }

    public void shouldRefactorNegatedConditions(boolean b) throws Exception {
        Assert.assertFalse(b);
        Assert.assertFalse(b, "Failure message to keep");
        Assert.assertTrue(b);
        Assert.assertTrue(b, "Failure message to keep");

        assertFalse(b);
        assertFalse(b, "Failure message to keep");
        assertTrue(b);
        assertTrue(b, "Failure message to keep");
    }

    public void shouldRefactorWithObjectReferences(Object o1, Object o2) throws Exception {
        Assert.assertSame(o1, o2);
        Assert.assertSame(o1, o2, "Failure message to keep");
        Assert.assertNotSame(o1, o2);
        Assert.assertNotSame(o1, o2, "Failure message to keep");
        Assert.assertSame(o1, o2);
        Assert.assertSame(o1, o2, "Failure message to keep");
        Assert.assertNotSame(o1, o2);
        Assert.assertNotSame(o1, o2, "Failure message to keep");

        assertSame(o1, o2);
        assertSame(o1, o2, "Failure message to keep");
        assertNotSame(o1, o2);
        assertNotSame(o1, o2, "Failure message to keep");
        assertSame(o1, o2);
        assertSame(o1, o2, "Failure message to keep");
        assertNotSame(o1, o2);
        assertNotSame(o1, o2, "Failure message to keep");
    }

    public void shouldRefactorWithObjects(Object o1, Object o2) throws Exception {
        Assert.assertEquals(o1, o2);
        Assert.assertEquals(o1, o2, "Failure message to keep");
        Assert.assertNotEquals(o1, o2);
        Assert.assertNotEquals(o1, o2, "Failure message to keep");
        Assert.assertEquals(o1, o2);
        Assert.assertEquals(o1, o2, "Failure message to keep");
        Assert.assertNotEquals(o1, o2);
        Assert.assertNotEquals(o1, o2, "Failure message to keep");

        assertEquals(o1, o2);
        assertEquals(o1, o2, "Failure message to keep");
        assertNotEquals(o1, o2);
        assertNotEquals(o1, o2, "Failure message to keep");
        assertEquals(o1, o2);
        assertEquals(o1, o2, "Failure message to keep");
        assertNotEquals(o1, o2);
        assertNotEquals(o1, o2, "Failure message to keep");
    }

    public void shouldRefactorNullCheckFirstArg(Object o) throws Exception {
        Assert.assertNull(o);
        Assert.assertNull(o, "Failure message to keep");
        Assert.assertNotNull(o);
        Assert.assertNotNull(o, "Failure message to keep");
        Assert.assertNull(o);
        Assert.assertNull(o, "Failure message to keep");
        Assert.assertNotNull(o);
        Assert.assertNotNull(o, "Failure message to keep");

        assertNull(o);
        assertNull(o, "Failure message to keep");
        assertNotNull(o);
        assertNotNull(o, "Failure message to keep");
        assertNull(o);
        assertNull(o, "Failure message to keep");
        assertNotNull(o);
        assertNotNull(o, "Failure message to keep");
    }

    public void shouldRefactorNullCheckSecondArg(Object o) throws Exception {
        Assert.assertNull(o);
        Assert.assertNull(o, "Failure message to keep");
        Assert.assertNotNull(o);
        Assert.assertNotNull(o, "Failure message to keep");
        Assert.assertNull(o);
        Assert.assertNull(o, "Failure message to keep");
        Assert.assertNotNull(o);
        Assert.assertNotNull(o, "Failure message to keep");

        assertNull(o);
        assertNull(o, "Failure message to keep");
        assertNotNull(o);
        assertNotNull(o, "Failure message to keep");
        assertNull(o);
        assertNull(o, "Failure message to keep");
        assertNotNull(o);
        assertNotNull(o, "Failure message to keep");
    }

    public void shouldRefactorNullCheckFirstArgWithEquals(Object o) throws Exception {
        Assert.assertNull(o);
        Assert.assertNull(o, "Failure message to keep");
        Assert.assertNotNull(o);
        Assert.assertNotNull(o, "Failure message to keep");

        assertNull(o);
        assertNull(o, "Failure message to keep");
        assertNotNull(o);
        assertNotNull(o, "Failure message to keep");
    }

    public void shouldRefactorNullCheckSecondArgWithEquals(Object o) throws Exception {
        Assert.assertNull(o);
        Assert.assertNull(o, "Failure message to keep");
        Assert.assertNotNull(o);
        Assert.assertNotNull(o, "Failure message to keep");

        assertNull(o);
        assertNull(o, "Failure message to keep");
        assertNotNull(o);
        assertNotNull(o, "Failure message to keep");
    }

    public void shouldMoveConstantAsExpectedArgInWithEquals(Object o) throws Exception {
        Assert.assertEquals(o, 42);
        Assert.assertEquals(o, 42, "Failure message to keep");
        Assert.assertNotEquals(o, 42);
        Assert.assertNotEquals(o, 42, "Failure message to keep");

        assertEquals(o, 42);
        assertEquals(o, 42, "Failure message to keep");
        assertNotEquals(o, 42);
        assertNotEquals(o, 42, "Failure message to keep");
    }

    public void shouldMoveExpectedVariableAsExpectedArgWithEquals(Object o, int expected) throws Exception {
        Assert.assertEquals(o, expected);
        Assert.assertEquals(o, expected, "Failure message to keep");
        Assert.assertNotEquals(o, expected);
        Assert.assertNotEquals(o, expected, "Failure message to keep");

        assertEquals(o, expected);
        assertEquals(o, expected, "Failure message to keep");
        assertNotEquals(o, expected);
        assertNotEquals(o, expected, "Failure message to keep");

        // tests that this works according to levenshtein distance 
        int expceted = 0;
        assertEquals(o, expceted);
    }

    public void shouldRefactorIfPrimitiveThenFail(int i1, int i2) throws Exception {
        Assert.assertNotEquals(i1, i2);
        Assert.assertNotEquals(i1, i2, "Failure message to keep");
        Assert.assertEquals(i1, i2);
        Assert.assertEquals(i1, i2, "Failure message to keep");

        assertNotEquals(i1, i2);
        assertNotEquals(i1, i2, "Failure message to keep");
        assertEquals(i1, i2);
        assertEquals(i1, i2, "Failure message to keep");
    }

    public void shouldRefactorIfSameObjectThenFail(Object o1, Object o2) throws Exception {
        Assert.assertNotSame(o1, o2);
        Assert.assertNotSame(o1, o2, "Failure message to keep");
        Assert.assertSame(o1, o2);
        Assert.assertSame(o1, o2, "Failure message to keep");

        assertNotSame(o1, o2);
        assertNotSame(o1, o2, "Failure message to keep");
        assertSame(o1, o2);
        assertSame(o1, o2, "Failure message to keep");
    }

    public void shouldRefactorIfNullThenFail(Object o1) throws Exception {
        Assert.assertNotNull(o1);
        Assert.assertNotNull(o1, "Failure message to keep");
        Assert.assertNull(o1);
        Assert.assertNull(o1, "Failure message to keep");
        Assert.assertNotNull(o1);
        Assert.assertNotNull(o1, "Failure message to keep");
        Assert.assertNull(o1);
        Assert.assertNull(o1, "Failure message to keep");

        assertNotNull(o1);
        assertNotNull(o1, "Failure message to keep");
        assertNull(o1);
        assertNull(o1, "Failure message to keep");
        assertNotNull(o1);
        assertNotNull(o1, "Failure message to keep");
        assertNull(o1);
        assertNull(o1, "Failure message to keep");
    }

    public void shouldRefactorIfObjectThenFail(Object o1, Object o2) throws Exception {
        Assert.assertNotEquals(o1, o2);
        Assert.assertNotEquals(o1, o2, "Failure message to keep");
        Assert.assertEquals(o1, o2);
        Assert.assertEquals(o1, o2, "Failure message to keep");

        assertNotEquals(o1, o2);
        assertNotEquals(o1, o2, "Failure message to keep");
        assertEquals(o1, o2);
        assertEquals(o1, o2, "Failure message to keep");
    }
}
