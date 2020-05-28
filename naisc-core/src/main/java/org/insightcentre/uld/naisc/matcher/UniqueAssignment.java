package org.insightcentre.uld.naisc.matcher;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntMaps;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.ints.IntComparator;
import it.unimi.dsi.fastutil.ints.IntRBTreeSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import static java.lang.Math.max;

import java.util.*;

import org.apache.jena.rdf.model.Resource;
import org.insightcentre.uld.naisc.*;
import org.insightcentre.uld.naisc.main.ConfigurationException;
import org.insightcentre.uld.naisc.main.ExecuteListener;
import org.insightcentre.uld.naisc.util.None;
import org.insightcentre.uld.naisc.util.Option;
import org.insightcentre.uld.naisc.util.Some;

/**
 * Bipartite matching algorithm, calculates the global maximal matching assuming
 * that there are no multiple alignments.
 *
 * @author John McCrae 
 */
public class UniqueAssignment implements MatcherFactory {

    /**
     * The configuration for unique assignment
     */
    public static class Configuration {

        /**
         * The minimum threshold to accept.
         */
        @ConfigurationParameter(description = "The minimum threshold to accept")
        public double threshold = Double.NEGATIVE_INFINITY;
        /**
         * The base probability. This is the probability assigned to non-scored
         * examples. The default is 0.1, but this can be changed if the
         * classifier is more reliable.
         */
        @ConfigurationParameter(description = "The probability assigned to non-scored examples")
        public double baseProbability = 0.1;
    }

    @Override
    public String id() {
        return "unique";
    }

    @Override
    public Matcher makeMatcher(Map<String, Object> params) {
        Configuration config = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).convertValue(params, Configuration.class);
        if (config.baseProbability <= 0 || config.baseProbability > 1) {
            throw new ConfigurationException("Base probability must be between 0 and 1");
        }
        return new UniqueAssignmentImpl(config.threshold, config.baseProbability);
    }

    /**
     * Matching according to weights under a bipartite assumption
     *
     * @author John McCrae
     */
    private class UniqueAssignmentImpl implements Matcher {

        private final double threshold;
        private final double baseProbability;

        public UniqueAssignmentImpl(double threshold, double baseProbability) {
            this.threshold = threshold;
            this.baseProbability = baseProbability;
        }

        @Override
        public AlignmentSet alignWith(AlignmentSet matches, AlignmentSet initial, ExecuteListener monitor) {
            Set<String> relations = new HashSet<>();
            final List<Alignment> alignmentSet = new ArrayList<>();

            for (Alignment alignment : matches.getAlignments()) {
                relations.add(alignment.property);
            }
            for (String rel : relations) {
                Set<URIRes> leftExclusion = new HashSet<>();
                Set<URIRes> rightExclusion = new HashSet<>();
                
                for(Alignment init : initial) {
                    if(init.property.equals(rel)) {
                        leftExclusion.add(init.entity1);
                        rightExclusion.add(init.entity2);
                    }
                }
                
                Object2IntMap<URIRes> lefts = new Object2IntOpenHashMap<>();
                Object2IntMap<URIRes> rights = new Object2IntOpenHashMap<>();
                Int2ObjectMap<URIRes> linv = new Int2ObjectArrayMap<>();
                Int2ObjectMap<URIRes> rinv = new Int2ObjectArrayMap<>();
                HashMap<IntStringTriple, Alignment> origAligns = new HashMap<>();

                for (Alignment alignment : matches.getAlignments()) {
                    if (rel.equals(alignment.property) && alignment.probability >= threshold &&
                            !leftExclusion.contains(alignment.entity1) &&
                            !rightExclusion.contains(alignment.entity2)) {
                        if (!lefts.containsKey(alignment.entity1)) {
                            linv.put(lefts.size(), alignment.entity1);
                            lefts.put(alignment.entity1, lefts.size());
                        }
                        if (!rights.containsKey(alignment.entity2)) {
                            rinv.put(rights.size(), alignment.entity2);
                            rights.put(alignment.entity2, rights.size());
                        }
                        relations.add(alignment.property);
                        origAligns.put(new IntStringTriple(lefts.getInt(alignment.entity1),
                                rights.getInt(alignment.entity2), alignment.property),
                                alignment);
                    }
                }
                SparseMat m = new SparseMat(lefts.size(), rights.size());
                for (Alignment alignment : matches.getAlignments()) {
                    if (rel.equals(alignment.property) && alignment.probability >= threshold) {
                        if (alignment.probability < 0) {
                            throw new RuntimeException("Invalid (negative) alignment probability generated");
                        }
                        m.add(lefts.getInt(alignment.entity1), rights.getInt(alignment.entity2),
                                alignment.probability);
                                //Math.log(alignment.probability == 0 ? 1e-6 : alignment.probability / baseProbability));
                    }
                }
                MunkRes munkRes = new MunkRes(m);
                //double[] sim = m.sim();
                for (IntPair ip : munkRes.execute()) {
                    if(linv.containsKey(ip._1) && rinv.containsKey(ip._2)) {
                        Alignment orig = origAligns.get(new IntStringTriple(ip._1, ip._2, rel));
                        if(orig != null) {
                            alignmentSet.add(new Alignment(linv.get(ip._1), rinv.get(ip._2),
                                orig.probability, rel, orig.features == null || orig.features instanceof Object2DoubleMap ? (Object2DoubleMap<String>)orig.features : new Object2DoubleOpenHashMap<>(orig.features)));
                        } else {
                            alignmentSet.add(new Alignment(linv.get(ip._1), rinv.get(ip._2),
                                    baseProbability, rel, null));
                        }
                    }
                }
            }
            alignmentSet.addAll(initial);
            return new AlignmentSet(alignmentSet);
        }

    }

    private static class IntPair {

        public final int _1, _2;

        public IntPair(int _1, int _2) {
            this._1 = _1;
            this._2 = _2;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 37 * hash + this._1;
            hash = 37 * hash + this._2;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final IntPair other = (IntPair) obj;
            if (this._1 != other._1) {
                return false;
            }
            if (this._2 != other._2) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "IntPair{" + "_1=" + _1 + ", _2=" + _2 + '}';
        }

    }

    /**
     * The Hungarian or Munk-Res algorithm
     *
     * @author John McCrae
     */
    private class MunkRes implements Serializable {

        private final Mat matrix;
        private final double[] row_mod, col_mod;
        private final Set<IntPair> starred = new HashSet<>();
        private final Set<IntPair> primed = new HashSet<>();
        private final boolean[] covered_columns;
        private final boolean[] covered_rows;
        private IntPair last_primed;
//    private final boolean[] nonempty_columns, nonempty_rows;
//    private final int[] row_indexes, col_indexes;
//    private final List<IntPair> shadow_zeros;

        public MunkRes(Mat matrix) {
            final int N;
            if (matrix.M() == matrix.N()) {
                this.matrix = matrix;
                N = matrix.M();
            } else {
                N = max(matrix.M(), matrix.N());
                this.matrix = new SparseMat(N, N, matrix.all());
            }
            covered_columns = new boolean[N];
            covered_rows = new boolean[N];
            row_mod = new double[N];
            col_mod = new double[N];
//        shadow_zeros = new ArrayList<>();
        }

        private void update_row_mod(int i, double v) {
            row_mod[i] = v;
//        Iterator<IntPair> ipIter = shadow_zeros.iterator();
//        while(ipIter.hasNext()) {
//            if(ipIter.next()._1 == i) {
//                ipIter.remove();
//            }
//        }
//        for(int j = 0; j < col_mod.length; j++) {
//            if(row_mod[i] + col_mod[j] == 0 && !matrix.contains(i, j)) {
//                shadow_zeros.add(new IntPair(i, j));
//            }
//        }
        }

        private void update_col_mod(int j, double v) {
            col_mod[j] = v;
//        Iterator<IntPair> ipIter = shadow_zeros.iterator();
//        while(ipIter.hasNext()) {
//            if(ipIter.next()._2 == j) {
//                ipIter.remove();
//            }
//        }
//        for(int i = 0; i < row_mod.length; i++) {
//            if(row_mod[i] + col_mod[j] == 0 && !matrix.contains(i, j)) {
//                shadow_zeros.add(new IntPair(i, j));
//            }
//        }
        }

//  """
//    Auxiliary class. Use the top level munkres method instead.
//    """
//    def __init__(self, values):
//        """
//        Initialize the munkres.
//        values: list of non-infinite values entries of the cost matrix
//                [(i,j,value)...]
//        """
//        self.matrix = MunkresMatrix(values)
//        self.starred = set()
//        self.primed = set()
//        self.covered_columns = [False] * self.matrix.ncols
//        self.covered_rows = [False] * self.matrix.nrows
//        self.last_primed = None
//
        private final int ITER_MAX = 1000000;

        public Collection<IntPair> execute() {
            int step = 1;
            int iters = 0;
            while (true) {
                if (iters++ > ITER_MAX) {
                    try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("munkres.tmp"))) {
                        oos.writeObject(this);
                        oos.flush();
                    } catch (IOException x) {
                        x.printStackTrace();
                    }
                    throw new RuntimeException("Iterations exceeded");
                }
//                printMatrix(matrix);
                if (step == 1) {
                    step = step1();
                } else if (step == 2) {
                    step = step2();
                } else if (step == 3) {
                    step = step3();
                } else if (step == 4) {
                    step = step4();
                } else if (step == 5) {
                    step = step5();
                } else if (step == 6) {
                    step = step6();
                } else {
                    break;
                }
            }

            return starred;
        }
//    def munkres(self):
//        """
//        Executes the munkres algorithm.
//        Returns the optimal matching.
//        """
//        next_step = self._step_1
//        while next_step:
//            next_step = next_step()
//
//        # Transform the mapping back to the input domain
//        return self.matrix.remap(self.starred)
//

        /**
         * For each row of the matrix, find the smallest element and subtract it
         * from every element in its row. Go to Step 2.
         */
        private int step1() {
            for (int i = 0; i < matrix.N(); i++) {
                double m = Double.NEGATIVE_INFINITY;
                for (ME me : matrix.row(i)) {
                    assert (me.i == i);
                    m = max(m, me.v);
                }
                if (m != Double.NEGATIVE_INFINITY) {
                    update_row_mod(i, row_mod[i] - m);
                    //row_mod[i] = -m;
                }
            }
            return 2;
        }
//    def _step_1(self):
//        """
//        For each row of the matrix, find the smallest element and subtract it
//        from every element in its row.  Go to Step 2.
//        """
//        # TODO: This can probably be done much better than using .row(i),
//        # but it is executed only once, so the performance penalty is low.
//        for i in self.matrix.rowindices:
//            minimum = min(self.matrix.row(i))[0]
//            self.matrix.add_row(i, -minimum)
//        return self._step_2
//

        private int step2() {
            IntSet starred_rows = new IntRBTreeSet();
            IntSet starred_cols = new IntRBTreeSet();
            ELEM:
            for (ME me : matrix.all()) {
                if (!starred_rows.contains(me.i) && !starred_cols.contains(me.j)
                        && cost(me) == 0) {
                    starred.add(new IntPair(me.i, me.j));
                    starred_rows.add(me.i);
                    starred_cols.add(me.j);
                }
            }
            int[] col_indexes = new int[col_mod.length], row_indexes = new int[row_mod.length];

            for (int i = 0; i < col_indexes.length; i++) {
                col_indexes[i] = i;
            }
            for (int i = 0; i < row_indexes.length; i++) {
                row_indexes[i] = i;
            }
            IntArrays.quickSort(row_indexes, new Compare(row_mod, 1));
            IntArrays.quickSort(col_indexes, new Compare(col_mod, -1));
            int i = 0;
            int j = 0;
            while (i < row_indexes.length && j < col_indexes.length) {
                if (starred_rows.contains(row_indexes[i])) {
                    i++;
                } else if (starred_cols.contains(col_indexes[j])) {
                    j++;
                } else {
                    double r = row_mod[row_indexes[i]];
                    double c = col_mod[col_indexes[j]];
                    if (r + c == 0) {
                        if (!matrix.contains(row_indexes[i], col_indexes[j])) {
                            starred.add(new IntPair(row_indexes[i], col_indexes[j]));
                            starred_rows.add(row_indexes[i]);
                            starred_cols.add(col_indexes[j]);
                        }
                        i++;
                    } else if (r < c) {
                        i++;
                    } else /*if(c < r)*/ {
                        j++;
                    }
                }
            }

            return 3;
        }

//    private List<IntPair> findShadowZeros(int[] row_indexes, int[] col_indexes) {
//        final List<IntPair> pairs = new ArrayList<>();
//        IntArrays.quickSort(row_indexes, new Compare(row_mod, 1));
//        IntArrays.quickSort(col_indexes, new Compare(col_mod, -1));
//        int i = 0;
//        int j = 0;
//        while(i < row_indexes.length && j < col_indexes.length) {
//            double r = row_mod[row_indexes[i]];
//            double c = col_mod[col_indexes[j]];
//            if(r + c == 0) {
//                if(!matrix.contains(row_indexes[i], col_indexes[j])) {
//                    pairs.add(new IntPair(row_indexes[i], col_indexes[j]));
//                }
//                for(int j2 = j + 1; j2 < col_indexes.length; j2++) {
//                     c = col_mod[col_indexes[j2]];
//                    if(r + c != 0) break;
//                    if(!matrix.contains(row_indexes[i], col_indexes[j2])) {
//                        pairs.add(new IntPair(row_indexes[i], col_indexes[j2]));
//                    }
//                }
//                i++;
//            } else if(r < c) {
//                i++;
//            } else /*if(c < r)*/ {
//                j++;
//            }
//        }
//
//        return pairs;
//    }
//
//    def _step_2(self):
//        """
//        Find a zero (Z) in the resulting matrix.  If there is no starred zero
//        in its row or column, star Z.
//        Repeat for each element in the matrix. Go to Step 3.
//        """
//        zeros = self.matrix.zeros()
//        for (i, j) in zeros:
//            for (i1, j1) in self.starred:
//                if (i1 == i or j1 == j):
//                    break
//            else:
//                self.starred.add((i, j))
//        return self._step_3
//
        private boolean all_true() {
            for (int i = 0; i < covered_columns.length; i++) {
                if (!covered_columns[i]) { // && nonempty_columns[i]) {
                    return false;
                }
            }
            return true;
        }

        private int step3() {
            for (IntPair ip : starred) {
                covered_columns[ip._2] = true;
            }
            if (all_true()) {
                return 7;
            }
            return 4;
        }
//    def _step_3(self):
//        """
//        Cover each column containing a starred zero.  If K columns are covered,
//        the starred zeros describe a complete set of unique assignments.  In
//        this case, Go to DONE, otherwise, Go to Step 4.
//        """
//        for (_, j) in self.starred:
//            self.covered_columns[j] = True
//        if sum(self.covered_columns) == self.matrix.K:
//            return None
//        else:
//            return self._step_4
//
//      private Option<IntPair> find_uncovered_zero() {
//          for(ME me : matrix.all()) {
//              if(cost(me) == 0 &&
//                  !covered_columns[me.j] && !covered_rows[me.i]) {
//                  return new Some(new IntPair(me.i, me.j));
//              }
//          }
//          for(IntPair ip : shadow_zeros) {
//                if(!covered_columns[ip._2] && !covered_rows[ip._1]) {
//                  return new Some(new IntPair(ip._1, ip._2));
//              }
//          }
//          return new None();
//      }

//    def _find_uncovered_zero(self):
//        """
//        Returns the (row, column) of one of the uncovered zeros in the matrix.
//        If there are no uncovered zeros, returns None
//        """
//        zeros = self.matrix.zeros()
//        for (i, j) in zeros:
//            if not self.covered_columns[j] and not self.covered_rows[i]:
//                return (i, j)
//        return None
//
        private List<ME> prep_uncovered_zeros(int[] col_indexes, int[] row_indexes) {
            for (int i = 0; i < col_indexes.length; i++) {
                col_indexes[i] = i;
            }
            for (int i = 0; i < row_indexes.length; i++) {
                row_indexes[i] = i;
            }
            IntArrays.quickSort(row_indexes, new Compare(row_mod, 1));
            IntArrays.quickSort(col_indexes, new Compare(col_mod, -1));
            List<ME> zeros = new ArrayList<>();
            for (ME me : matrix.all()) {
                if (cost(me) == 0) {
                    zeros.add(me);
                }
            }
            return zeros;
        }

        private Option<IntPair> find_uncovered_zero(List<ME> matrixZeros,
                int[] col_indexes, int[] row_indexes) {
            for (ME me : matrixZeros) {
                if (!covered_columns[me.j] && !covered_rows[me.i]) {
                    return new Some<>(new IntPair(me.i, me.j));
                }
            }
            int i = 0;
            int j = 0;
            while (i < row_indexes.length && j < col_indexes.length) {
                if (covered_columns[col_indexes[j]]) {
                    j++;
                } else if (covered_rows[row_indexes[i]]) {
                    i++;
                } else {
                    double r = row_mod[row_indexes[i]];
                    double c = col_mod[col_indexes[j]];
                    if (r + c == 0) {
                        if (!matrix.contains(row_indexes[i], col_indexes[j])) {
                            return new Some(new IntPair(row_indexes[i], col_indexes[j]));
                        }
                        for (int j2 = j + 1; j2 < col_indexes.length; j2++) {
                            c = col_mod[col_indexes[j2]];
                            if (r + c != 0) {
                                break;
                            }
                            if (!matrix.contains(row_indexes[i], col_indexes[j2])) {
                                return new Some(new IntPair(row_indexes[i], col_indexes[j2]));
                            }
                        }
                        i++;
                    } else if (r < c) {
                        i++;
                    } else /*if(c < r)*/ {
                        j++;
                    }
                }
            }
            return new None<>();
        }

        private int step4() {
            int[] row_indexes = new int[row_mod.length], col_indexes = new int[col_mod.length];
            List<ME> zeros = prep_uncovered_zeros(col_indexes, row_indexes);
            boolean done = false;
            int iters = 0;
            ITER:
            while (!done) {
                if (++iters > ITER_MAX) {
                    throw new RuntimeException();
                }
                //Option<IntPair> zero = find_uncovered_zero();
                Option<IntPair> zero = find_uncovered_zero(zeros, col_indexes, row_indexes);
                if (zero.has()) {
                    int i = zero.get()._1;
                    primed.add(zero.get());
                    last_primed = zero.get();
                    for (IntPair st : starred) {
                        if (st._1 == i) {
                            int j1 = st._2;
                            covered_rows[i] = true;
                            covered_columns[j1] = false;
                            continue ITER;
                        }
                    }
                    return 5;
                } else {
                    done = true;
                }
            }
            return 6;
        }

//    def _step_4(self):
//        """
//        Find a noncovered zero and prime it.  If there is no starred zero in
//        the row containing this primed zero, Go to Step 5.  Otherwise, cover
//        this row and uncover the column containing the starred zero. Continue
//        in this manner until there are no uncovered zeros left. Save the
//        smallest uncovered value and Go to Step 6.
//        """
//        done = False
//        while not done:
//            zero = self._find_uncovered_zero()
//            if zero:
//                i, j = zero
//                self.primed.add((i, j))
//                self.last_primed = (i, j)
//                st = [(i1, j1) for (i1, j1) in self.starred if i1 == i]
//                if not st:
//                    return self._step_5
//                assert len(st) == 1
//                i1, j1 = st[0]
//                self.covered_rows[i] = True
//                self.covered_columns[j1] = False
//            else:
//                done = True
//        return self._step_6
//
        private int step5() {
            IntPair last_primed = this.last_primed;
            IntPair last_starred;
            Set<IntPair> local_primed = new HashSet<>();
            local_primed.add(last_primed);
            Set<IntPair> local_starred = new HashSet<>();
            int iters = 0;
            ITER:
            while (true) {
                if (++iters > ITER_MAX) {
                    throw new RuntimeException();
                }
                final IntPair lp = last_primed;
                for (IntPair ip : starred) {
                    if (ip._2 == lp._2) {
                        last_starred = ip;
                        local_starred.add(last_starred);
                        final IntPair ls = last_starred;
                        for (IntPair ip2 : primed) {
                            if (ip2._1 == ls._1) {
                                last_primed = ip2;
                                local_primed.add(last_primed);
                                continue ITER;
                            }
                        }
                    }
                }
                break;
            }
            starred.removeAll(local_starred);
            starred.addAll(local_primed);
            primed.clear();
            Arrays.fill(covered_rows, false);

            return 3;
        }

//    def _step_5(self):
//        """
//        Construct a series of alternating primed and starred zeros as follows.
//        Let Z0 represent the uncovered primed zero found in Step 4. Let Z1
//        denote the starred zero in the column of Z0 (if any). Let Z2 denote
//        the primed zero in the row of Z1 (there will always be one). Continue
//        until the series terminates at a primed zero that has no starred zero
//        in its column.  Unstar each starred zero of the series, star each
//        primed zero of the series, erase all primes and uncover every line in
//        the matrix. Return to Step 3.
//        """
//        last_primed = self.last_primed
//        last_starred = None
//        primed = [last_primed]
//        starred = []
//        while True:
//            # find the starred zero in the same column of last_primed
//            t = [(i, j) for (i, j) in self.starred if j == last_primed[1]]
//            if not t:
//                break
//            assert len(t) == 1
//            last_starred = t[0]
//            starred.append(last_starred)
//            t = [(i, j) for (i, j) in self.primed if i == last_starred[0]]
//            assert len(t) == 1
//            last_primed = t[0]
//            primed.append(last_primed)
//        for s in starred:
//            self.starred.remove(s)
//        for p in primed:
//            self.starred.add(p)
//        self.primed.clear()
//        for i in xrange(len(self.covered_rows)):
//            self.covered_rows[i] = False
//
//        return self._step_3
//
        private int step6() {
            double maxval = Double.NEGATIVE_INFINITY;
            for (int i = 0; i < matrix.N(); i++) {
                if (!covered_rows[i]) {
                    for (int j = 0; j < matrix.M(); j++) {
                        if (!covered_columns[j] && !matrix.contains(i, j)) {
                            maxval = max(maxval, row_mod[i] + col_mod[j]);
                        }
                    }
                }
            }
            for (ME me : matrix.all()) {
                boolean covered = covered_rows[me.i] || covered_columns[me.j];
                if (!covered && maxval < cost(me)) {
                    maxval = cost(me);
                }
            }
            if (Double.isInfinite(maxval)) {
                // Every value is covered
                return 7;
            }
            for (int i = 0; i < matrix.N(); i++) {
                if (covered_rows[i]) {
                    update_row_mod(i, row_mod[i] + maxval);
                    //row_mod[i] += maxval;
                }
            }
            for (int j = 0; j < matrix.M(); j++) {
                if (!covered_columns[j]) {
                    update_col_mod(j, col_mod[j] - maxval);
                    //col_mod[j] -= maxval;
                }
            }
            return 4;
        }
//    def _step_6(self):
//        """
//        Add the value found in Step 4 to every element of each covered row, and
//        subtract it from every element of each uncovered column.  Return to
//        Step 4 without altering any stars, primes, or covered lines.
//        """
//        minval = INFINITY
//        for i, j, value in self.matrix.get_values():
//            covered = self.covered_rows[i] or self.covered_columns[j]
//            if not covered and minval > value:
//                minval = value
//        assert 1e-6 < abs(minval) < INFINITY
//        for i in self.matrix.rowindices:
//            if self.covered_rows[i]:
//                self.matrix.add_row(i, minval)
//        for j in self.matrix.colindices:
//            if not self.covered_columns[j]:
//                self.matrix.add_column(j, -minval)
//        return self._step_4F

        private double cost(ME me) {
            return me.v + row_mod[me.i] + col_mod[me.j];
        }

    }

    /**
     * A matrix
     *
     * @author jmccrae
     */
    private interface Mat {

        Iterable<ME> all();

        Iterable<ME> row(int i);

        Iterable<ME> col(int j);

        double[] sim();

        int N();

        int M();

        int size();

        int ij2index(int i, int j);

        public boolean contains(int _1, int _2);

        double val(int i, int j);

    }

    private static final class Compare implements IntComparator {

        private final double[] data;
        private final int sign;

        public Compare(double[] data, int sign) {
            this.data = data;
            this.sign = sign;
        }

        @Override
        public int compare(int k1, int k2) {
            return sign * Double.compare(data[k1], data[k2]);
        }

        @Override
        public int compare(Integer o1, Integer o2) {
            return sign * Double.compare(data[o1], data[o2]);
        }

    }

    public static final class ME {

        public final int i;
        public final int j;
        public final double v;

        public ME(int i, int j, double v) {
            this.i = i;
            this.j = j;
            this.v = v;
        }

    }

    /**
     * A sparse matrix
     *
     * @author jmccrae
     */
    private final class SparseMat implements Mat {

        final int N;
        private final int M;
        final List<ME> mes;
        private final Int2ObjectMap<Int2IntMap> ijMap = new Int2ObjectOpenHashMap<>();

        public SparseMat(int N, int M) {
            this.N = N;
            this.M = M;
            this.mes = new ArrayList<>();
        }

        public SparseMat(int N, int M, Iterable<ME> mes) {
            this.N = N;
            this.M = M;
            this.mes = new ArrayList<ME>();
            int i = 0;
            for (ME me : mes) {
                this.mes.add(me);
                if (!ijMap.containsKey(me.i)) {
                    ijMap.put(me.i, new Int2IntOpenHashMap());
                }
                ijMap.get(me.i).put(me.j, i++);
            }
        }

        public void add(int i, int j, double v) {
            if (!ijMap.containsKey(i)) {
                ijMap.put(i, new Int2IntOpenHashMap());
            }
            ijMap.get(i).put(j, mes.size());
            mes.add(new ME(i, j, v));
        }

        @Override
        public boolean contains(int _1, int _2) {
            if (ijMap.containsKey(_1)) {
                return ijMap.get(_1).containsKey(_2);
            } else {
                return false;
            }
        }

        @Override
        public Iterable<ME> all() {
            return mes;
        }

        @Override
        public Iterable<ME> row(final int i) {
            return new Iterable<ME>() {
                Iterator<ME> iter = mes.iterator();

                @Override
                public Iterator<ME> iterator() {

                    return new Iterator<ME>() {
                        ME me = findNext();

                        private ME findNext() {
                            while (iter.hasNext()) {
                                ME me2 = iter.next();
                                if (me2.i == i) {
                                    return me2;
                                }
                            }
                            return null;
                        }

                        @Override
                        public boolean hasNext() {
                            return me != null;
                        }

                        @Override
                        public ME next() {
                            ME me2 = me;
                            me = findNext();
                            return me2;
                        }

                        @Override
                        public void remove() {
                        }
                    };
                }
            };
        }

        @Override
        public Iterable<ME> col(final int j) {
            return new Iterable<ME>() {
                Iterator<ME> iter = mes.iterator();

                @Override
                public Iterator<ME> iterator() {

                    return new Iterator<ME>() {
                        ME me = findNext();

                        private ME findNext() {
                            while (iter.hasNext()) {
                                ME me2 = iter.next();
                                if (me2.j == j) {
                                    return me2;
                                }
                            }
                            return null;
                        }

                        @Override
                        public boolean hasNext() {
                            return me != null;
                        }

                        @Override
                        public ME next() {
                            ME me2 = me;
                            me = findNext();
                            return me2;
                        }

                        @Override
                        public void remove() {
                        }
                    };
                }
            };
        }

        @Override
        public double[] sim() {
            double[] sim = new double[mes.size()];
            int i = 0;
            for (ME me : mes) {
                sim[i++] = me.v;
            }
            return sim;
        }

        @Override
        public int N() {
            return N;
        }

        @Override
        public int M() {
            return M;
        }

        @Override
        public int size() {
            return mes.size();
        }

        @Override
        public int ij2index(int i, int j) {
            return getOrDefault(getOrDefault(ijMap, i, Int2IntMaps.EMPTY_MAP), j, -1);
        }

        @Override
        public double val(int i, int j) {
            int k = getOrDefault(getOrDefault(ijMap, i, Int2IntMaps.EMPTY_MAP), j, -1);
            if (k < 0) {
                return 0.0;
            } else {
                return mes.get(k).v;
            }
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (ME me : mes) {
                sb.append(String.format("%d,%d=%.4f ", me.i, me.j, me.v));
            }
            return sb.toString();
        }

    }

    /**
     * Get the value or return f if not in the map
     *
     * @param <E> The key type
     * @param <F> The value type
     * @param map The map
     * @param e The key
     * @param f The default value
     * @return map.get(e) or f
     */
    private static <E, F> F getOrDefault(Map<E, F> map, E e, F f) {
        if (map.containsKey(e)) {
            return map.get(e);
        } else {
            return f;
        }
    }

    private static class IntStringTriple {

        private final int _1, _2;
        private final String _3;

        public IntStringTriple(int _1, int _2, String _3) {
            this._1 = _1;
            this._2 = _2;
            this._3 = _3;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 29 * hash + this._1;
            hash = 29 * hash + this._2;
            hash = 29 * hash + Objects.hashCode(this._3);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final IntStringTriple other = (IntStringTriple) obj;
            if (this._1 != other._1) {
                return false;
            }
            if (this._2 != other._2) {
                return false;
            }
            if (!Objects.equals(this._3, other._3)) {
                return false;
            }
            return true;
        }

    }
    
    private static void printMatrix(Mat m) {
        for(int i = 0; i < m.M(); i++) {
            for(int j = 0; j < m.N(); j++) {
                System.err.printf("%.4f ", m.val(i, j));
            }
            System.err.println();
        }
            System.err.println();
    }
}
