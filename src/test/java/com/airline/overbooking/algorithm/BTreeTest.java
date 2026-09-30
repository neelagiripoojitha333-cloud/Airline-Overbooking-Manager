package com.airline.overbooking.algorithm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BTreeTest {
    @Test
    void insertsAndSearchesAcrossMultipleSplits() {
        BTree<Integer, String> tree = new BTree<>(2);

        for (int key = 1; key <= 30; key++) {
            tree.insert(key, "value-" + key);
        }

        assertEquals(30, tree.size());
        for (int key = 1; key <= 30; key++) {
            assertEquals("value-" + key, tree.search(key));
        }
        assertNull(tree.search(31));
    }

    @Test
    void rejectsDuplicateKeysWithoutReplacingTheirValues() {
        BTree<Integer, String> tree = new BTree<>(2);
        tree.insert(4, "first");

        assertThrows(IllegalArgumentException.class, () -> tree.insert(4, "second"));

        assertEquals("first", tree.search(4));
        assertEquals(1, tree.size());
    }

    @Test
    void deletesLeafAndInternalKeysAndCollapsesEmptyRoot() {
        BTree<Integer, String> tree = new BTree<>(2);
        for (int key = 1; key <= 30; key++) {
            tree.insert(key, "value-" + key);
        }

        for (int key = 1; key <= 30; key++) {
            assertEquals("value-" + key, tree.delete(key));
            assertNull(tree.search(key));
        }

        assertTrue(tree.isEmpty());
        assertEquals(0, tree.size());
        assertNull(tree.delete(31));
    }

    @Test
    void deletesKeysInDescendingOrder() {
        BTree<Integer, String> tree = new BTree<>(2);
        for (int key = 1; key <= 30; key++) {
            tree.insert(key, "value-" + key);
        }

        for (int key = 30; key >= 1; key--) {
            assertEquals("value-" + key, tree.delete(key));
        }

        assertTrue(tree.isEmpty());
        assertEquals(0, tree.size());
    }

    @Test
    void updatesExistingKeysOnly() {
        BTree<Integer, String> tree = new BTree<>(2);
        tree.insert(4, "old");

        assertTrue(tree.update(4, "new"));
        assertFalse(tree.update(5, "missing"));
        assertEquals("new", tree.search(4));
        assertEquals(1, tree.size());
    }
}