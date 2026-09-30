package com.airline.overbooking.algorithm;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BTree<K extends Comparable<? super K>, V> {
    private final int minimumDegree;
    private Node root;
    private int size;

    public BTree(int minimumDegree) {
        if (minimumDegree < 2) {
            throw new IllegalArgumentException("minimumDegree must be at least 2");
        }
        this.minimumDegree = minimumDegree;
        this.root = new Node(true);
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public boolean containsKey(K key) {
        return search(key) != null;
    }

    public V search(K key) {
        requireKey(key);
        return search(root, key);
    }

    public void insert(K key, V value) {
        requireKey(key);
        Objects.requireNonNull(value, "value must not be null");
        if (containsKey(key)) {
            throw new IllegalArgumentException("key already exists: " + key);
        }

        if (root.keys.size() == maximumKeys()) {
            Node newRoot = new Node(false);
            newRoot.children.add(root);
            splitChild(newRoot, 0);
            root = newRoot;
        }

        insertIntoNonFullNode(root, key, value);
        size++;
    }

    public boolean update(K key, V value) {
        requireKey(key);
        Objects.requireNonNull(value, "value must not be null");
        return update(root, key, value);
    }

    public V delete(K key) {
        requireKey(key);
        V existingValue = search(key);
        if (existingValue == null) {
            return null;
        }

        delete(root, key);
        size--;
        if (root.keys.isEmpty() && !root.leaf) {
            root = root.children.get(0);
        }
        return existingValue;
    }

    private V search(Node node, K key) {
        int index = findKeyIndex(node, key);
        if (index < node.keys.size() && node.keys.get(index).compareTo(key) == 0) {
            return node.values.get(index);
        }
        if (node.leaf) {
            return null;
        }
        return search(node.children.get(index), key);
    }

    private boolean update(Node node, K key, V value) {
        int index = findKeyIndex(node, key);
        if (index < node.keys.size() && node.keys.get(index).compareTo(key) == 0) {
            node.values.set(index, value);
            return true;
        }
        if (node.leaf) {
            return false;
        }
        return update(node.children.get(index), key, value);
    }

    private void insertIntoNonFullNode(Node node, K key, V value) {
        int index = node.keys.size() - 1;
        if (node.leaf) {
            while (index >= 0 && key.compareTo(node.keys.get(index)) < 0) {
                index--;
            }
            node.keys.add(index + 1, key);
            node.values.add(index + 1, value);
            return;
        }

        while (index >= 0 && key.compareTo(node.keys.get(index)) < 0) {
            index--;
        }
        index++;

        if (node.children.get(index).keys.size() == maximumKeys()) {
            splitChild(node, index);
            if (key.compareTo(node.keys.get(index)) > 0) {
                index++;
            }
        }
        insertIntoNonFullNode(node.children.get(index), key, value);
    }

    private void splitChild(Node parent, int childIndex) {
        Node fullChild = parent.children.get(childIndex);
        Node rightSibling = new Node(fullChild.leaf);

        K middleKey = fullChild.keys.remove(minimumDegree - 1);
        V middleValue = fullChild.values.remove(minimumDegree - 1);
        while (fullChild.keys.size() > minimumDegree - 1) {
            rightSibling.keys.add(fullChild.keys.remove(minimumDegree - 1));
            rightSibling.values.add(fullChild.values.remove(minimumDegree - 1));
        }
        if (!fullChild.leaf) {
            while (fullChild.children.size() > minimumDegree) {
                rightSibling.children.add(fullChild.children.remove(minimumDegree));
            }
        }

        parent.keys.add(childIndex, middleKey);
        parent.values.add(childIndex, middleValue);
        parent.children.add(childIndex + 1, rightSibling);
    }

    private void delete(Node node, K key) {
        int index = findKeyIndex(node, key);
        if (index < node.keys.size() && node.keys.get(index).compareTo(key) == 0) {
            if (node.leaf) {
                node.keys.remove(index);
                node.values.remove(index);
            } else {
                deleteFromInternalNode(node, index);
            }
            return;
        }

        if (node.leaf) {
            return;
        }

        if (node.children.get(index).keys.size() == minimumDegree - 1) {
            prepareChildForDeletion(node, index);
            if (index > node.keys.size()) {
                index--;
            }
        }
        delete(node.children.get(index), key);
    }

    private void deleteFromInternalNode(Node node, int keyIndex) {
        K key = node.keys.get(keyIndex);
        Node leftChild = node.children.get(keyIndex);
        Node rightChild = node.children.get(keyIndex + 1);

        if (leftChild.keys.size() >= minimumDegree) {
            KeyValue predecessor = maximumEntry(leftChild);
            node.keys.set(keyIndex, predecessor.key);
            node.values.set(keyIndex, predecessor.value);
            delete(leftChild, predecessor.key);
        } else if (rightChild.keys.size() >= minimumDegree) {
            KeyValue successor = minimumEntry(rightChild);
            node.keys.set(keyIndex, successor.key);
            node.values.set(keyIndex, successor.value);
            delete(rightChild, successor.key);
        } else {
            mergeChildren(node, keyIndex);
            delete(leftChild, key);
        }
    }

    private void prepareChildForDeletion(Node parent, int childIndex) {
        if (childIndex > 0
                && parent.children.get(childIndex - 1).keys.size() >= minimumDegree) {
            borrowFromPrevious(parent, childIndex);
        } else if (childIndex < parent.keys.size()
                && parent.children.get(childIndex + 1).keys.size() >= minimumDegree) {
            borrowFromNext(parent, childIndex);
        } else if (childIndex < parent.keys.size()) {
            mergeChildren(parent, childIndex);
        } else {
            mergeChildren(parent, childIndex - 1);
        }
    }

    private void borrowFromPrevious(Node parent, int childIndex) {
        Node child = parent.children.get(childIndex);
        Node leftSibling = parent.children.get(childIndex - 1);

        child.keys.add(0, parent.keys.get(childIndex - 1));
        child.values.add(0, parent.values.get(childIndex - 1));
        if (!child.leaf) {
            child.children.add(0, leftSibling.children.remove(leftSibling.children.size() - 1));
        }
        parent.keys.set(childIndex - 1, leftSibling.keys.remove(leftSibling.keys.size() - 1));
        parent.values.set(childIndex - 1, leftSibling.values.remove(leftSibling.values.size() - 1));
    }

    private void borrowFromNext(Node parent, int childIndex) {
        Node child = parent.children.get(childIndex);
        Node rightSibling = parent.children.get(childIndex + 1);

        child.keys.add(parent.keys.get(childIndex));
        child.values.add(parent.values.get(childIndex));
        if (!child.leaf) {
            child.children.add(rightSibling.children.remove(0));
        }
        parent.keys.set(childIndex, rightSibling.keys.remove(0));
        parent.values.set(childIndex, rightSibling.values.remove(0));
    }

    private void mergeChildren(Node parent, int keyIndex) {
        Node leftChild = parent.children.get(keyIndex);
        Node rightChild = parent.children.remove(keyIndex + 1);

        leftChild.keys.add(parent.keys.remove(keyIndex));
        leftChild.values.add(parent.values.remove(keyIndex));
        leftChild.keys.addAll(rightChild.keys);
        leftChild.values.addAll(rightChild.values);
        leftChild.children.addAll(rightChild.children);
    }

    private KeyValue maximumEntry(Node node) {
        Node current = node;
        while (!current.leaf) {
            current = current.children.get(current.children.size() - 1);
        }
        int lastIndex = current.keys.size() - 1;
        return new KeyValue(current.keys.get(lastIndex), current.values.get(lastIndex));
    }

    private KeyValue minimumEntry(Node node) {
        Node current = node;
        while (!current.leaf) {
            current = current.children.get(0);
        }
        return new KeyValue(current.keys.get(0), current.values.get(0));
    }

    private int findKeyIndex(Node node, K key) {
        int index = 0;
        while (index < node.keys.size() && key.compareTo(node.keys.get(index)) > 0) {
            index++;
        }
        return index;
    }

    private int maximumKeys() {
        return 2 * minimumDegree - 1;
    }

    private void requireKey(K key) {
        Objects.requireNonNull(key, "key must not be null");
    }

    private final class Node {
        private final List<K> keys = new ArrayList<>();
        private final List<V> values = new ArrayList<>();
        private final List<Node> children = new ArrayList<>();
        private final boolean leaf;

        private Node(boolean leaf) {
            this.leaf = leaf;
        }
    }

    private final class KeyValue {
        private final K key;
        private final V value;

        private KeyValue(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }
}