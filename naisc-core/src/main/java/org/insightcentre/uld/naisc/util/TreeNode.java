package org.insightcentre.uld.naisc.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 *
 * @author John McCrae
 */
public class TreeNode<R> {

    TreeNode<R> left;
    private TreeNode<R> right;
    R r;
    private double score;
    private int size = -1;

    public TreeNode() {
    }

    public TreeNode(R r, double score) {
        this.r = r;
        this.score = score;
    }

    public Iterator<R> iterator() {
        return new TreeIterator<>(this);
    }

    public boolean isEmpty() {
        return left == null && right == null && r == null;
    }

    public int size() {
        if (size >= 0) {
            return size;
        }
        size = 0;
        if (r != null) {
            size++;
        }
        if (left != null) {
            size += left.size();
        }
        if (right != null) {
            size += right.size();
        }
        return size;
    }

    public void add(R r, double score) {
        if (size >= 0) {
            size++;
        }
        if (this.r == null) {
            this.r = r;
            this.score = score;
        } else if (score < this.score) {
            if (left != null) {
                left.add(r, score);
            } else {
                left = new TreeNode(r, score);
            }
        } else {
            if (right != null) {
                right.add(r, score);
            } else {
                right = new TreeNode(r, score);
            }
        }
    }

    public ScoredQueueItem<R> poll() {
        if (size >= 0) {
            size--;
        }
        if (left == null) {
            // I am the node to be polled
            ScoredQueueItem<R> sqi = new ScoredQueueItem<>(this.r, this.score);
            if (right == null) {
                // If there are no left children either we just set this
                // node value to null so that the parent prunes it
                this.r = null;
            } else {
                // Otherwise we copy the right data to this node
                this.r = right.r;
                this.left = right.left;
                this.score = right.score;
                this.right = right.right;
            }
            return sqi;
        } else {
            ScoredQueueItem<R> sqi = left.poll();
            if (left.r == null) {
                left = null;
            }
            return sqi;
        }
    }

    public ScoredQueueItem<R> peek() {
        if (left == null) {
            return new ScoredQueueItem<>(this.r, this.score);
        } else {
            return left.peek();
        }
    }

    public TreeNode<R> trim(int n) {
        if(size >= 0 && size <= n)
            return this;
        size = -1;
        if (left != null) {
            if (left.size() >= n) {
                return left.trim(n);
            } else if (left.size() + 1 == n) {
                this.right = null;
                return this;
            } else {
                if (right != null) {
                    right = right.trim(n - left.size() - 1);
                    return this;
                } else {
                    return this;
                }
            }
        } else {
            if (right != null) {
                if(n > 1) {
                    right = right.trim(n - 1);
                    return this;
                } else {
                    return new TreeNode<>(r, score);
                }
            } else if (n > 0) {
                return this;
            } else {
                return new TreeNode<>(null, this.score);
            }
        }
    }

    private static class TreeIterator<R> implements Iterator<R> {

        private final List<TreeNode<R>> nodes = new ArrayList<>();

        private TreeIterator(TreeNode<R> root) {
            TreeNode<R> r = root;
            while (r.left != null) {
                nodes.add(r);
                r = r.left;
            }
            if (r.r != null) {
                nodes.add(r);
            }
        }

        @Override
        public boolean hasNext() {
            return !nodes.isEmpty();
        }

        @Override
        public R next() {
            if (nodes.isEmpty()) {
                throw new NoSuchElementException();
            }
            TreeNode<R> top = nodes.get(nodes.size() - 1);
            R r = top.r;
            if (top.right != null) {
                nodes.remove(nodes.size() - 1);
                nodes.add(top.right);
                TreeNode<R> r2 = top.right;
                while (r2.left != null) {
                    nodes.add(r2.left);
                    r2 = r2.left;
                }
            } else {
                nodes.remove(nodes.size() - 1);
            }
            return r;
        }

    }

    public static class ScoredQueueItem<R> implements Comparable<ScoredQueueItem<R>> {

        public final R r;
        public final double score;

        public ScoredQueueItem(R r, double score) {
            this.r = r;
            this.score = score;
        }

        @Override
        public int compareTo(ScoredQueueItem<R> o2) {
            double s1 = this.score;
            double s2 = o2.score;
            int i = Double.compare(s1, s2);
            if (i != 0) {
                return i;
            } else {
                int i2 = Integer.compare(this.hashCode(), o2.hashCode());
                if (i2 == 0 && !this.equals(o2)) {
                    return 1;
                } else {
                    return i2;
                }
            }
        }

    }

}
