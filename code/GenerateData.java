import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

// Generation and export.
public final class GenerateData {
    public static void main(String[] args) throws Exception {
        if (args.length != 11) throw new IllegalArgumentException(
                "output distribution cvLow cvHigh caseSeed I J H queries oosCount selectionFraction");
        Path output = Path.of(args[0]);
        if (Files.exists(output)) throw new IllegalStateException("Refusing to overwrite " + output);
        var distribution = TRBSVUSyntheticDemandGenerator.Distribution.valueOf(args[1]);
        double low = Double.parseDouble(args[2]), high = Double.parseDouble(args[3]);
        long caseSeed = Long.parseLong(args[4]);
        int carriers = Integer.parseInt(args[5]), lanes = Integer.parseInt(args[6]);
        int history = Integer.parseInt(args[7]), queryCount = Integer.parseInt(args[8]);
        int oosCount = Integer.parseInt(args[9]);
        double fraction = Double.parseDouble(args[10]);
        if (!(fraction > 0 && fraction <= 1) || oosCount < 0 || queryCount < 1
                || carriers < 2 || lanes < 1 || history < 1 || !Double.isFinite(low)
                || !Double.isFinite(high) || low < 0 || high < low)
            throw new IllegalArgumentException("Invalid generation settings");
        SplittableRandom random = new SplittableRandom(caseSeed);
        long parameterSeed = random.nextLong(), procurementSeed = random.nextLong();
        long contextSeed = random.nextLong(), historySeed = random.nextLong(), oosSeed = random.nextLong();
        var p = TRBSVUSyntheticDemandGenerator.sampleParameters(lanes, history, parameterSeed, 10.0, 0.3, 0.5);
        double[] cv = p.volatilityParameters(low, high);
        var generated = oosCount == 0
                ? TRBSVUSyntheticDemandGenerator.generateLinearTrendQueriesWithoutOos(
                    p, distribution, cv, queryCount, contextSeed, historySeed)
                : TRBSVUSyntheticDemandGenerator.generateMultiQueryWithLinearTrend(
                    p, distribution, cv, queryCount, oosCount, contextSeed, historySeed, oosSeed);
        var market = TRBSVUProcurementGenerator.generate(
                carriers, p.linearTrendTypicalDemand(), procurementSeed, fraction);
        Files.createDirectories(output);
        for (int k = 0; k < queryCount; k++) {
            var query = generated.queries.get(k);
            try (BufferedWriter writer = Files.newBufferedWriter(output.resolve(
                    String.format(Locale.ROOT, "query_%03d.instance.tsv", k)), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE_NEW)) {
                row(writer, "TRBSVU_INSTANCE_TSV_V1");
                row(writer, "SEEDS", parameterSeed, procurementSeed, contextSeed, historySeed, oosSeed);
                row(writer, "DIMENSIONS", market.I, market.J, market.alpha, market.beta);
                for (int j = 0; j < lanes; j++) row(writer, "LANE", j, "L" + (j + 1), market.e[j]);
                for (int i = 0; i < carriers; i++) {
                    row(writer, "CARRIER", i, market.carriers.get(i), market.p[i], market.h[i]);
                    for (int j = 0; j < lanes; j++)
                        row(writer, "PAIR", i, j, market.eligible[i][j], market.q[i][j], market.r[i][j]);
                }
                row(writer, "TEST_CONTEXT", vector(query.context.values()));
                samples(writer, "HISTORY", generated.history);
                samples(writer, "OOS", query.oos);
                row(writer, "END");
            }
        }
        writeParameters(output.resolve("demand_parameters.csv"), p, cv);
        System.out.println("DATA_GENERATED queries=" + queryCount + " oosPerQuery=" + oosCount + " seed=" + caseSeed);
    }

    private static void writeParameters(Path file, TRBSVUSyntheticDemandGenerator.Parameters p, double[] cv) throws IOException {
        double[] base = p.base(), m = p.market(), t = p.trend(), promo = p.promotion(), a = p.attention();
        double[] loading = p.commonLoading(), typical = p.linearTrendTypicalDemand();
        try (BufferedWriter writer = Files.newBufferedWriter(file,
                StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW)) {
            writer.write("lane,a,beta_M,beta_T,beta_P,beta_A,cv_parameter,common_loading,procurement_baseline\n");
            for (int j = 0; j < base.length; j++) {
                writer.write("L" + (j + 1) + "," + base[j] + "," + m[j] + "," + t[j] + "," + promo[j]
                        + "," + a[j] + "," + cv[j] + "," + loading[j] + "," + typical[j] + "\n");
            }
        }
    }

    private static void samples(BufferedWriter writer, String group, List<Sample> samples) throws IOException {
        for (Sample sample : samples) {
            PeriodData p = sample.period;
            row(writer, "SAMPLE", group, sample.id, p.tIndex,
                    vector(sample.theta.values()), vector(sample.demand()));
        }
    }

    private static void row(BufferedWriter writer, Object... fields) throws IOException {
        for (int k = 0; k < fields.length; k++) {
            if (k > 0) writer.write('\t');
            writer.write(String.valueOf(fields[k]));
        }
        writer.newLine();
    }

    private static String vector(double[] values) {
        StringJoiner joiner = new StringJoiner(",");
        for (double value : values) joiner.add(Double.toString(value));
        return joiner.toString();
    }
}

// Data records.
class CovariateVector {
    private final double[] values;

    public CovariateVector(double[] values) {
        this.values = values;
    }

    public double[] values() { return values; }

    public CovariateVector copy() {
        return new CovariateVector(Arrays.copyOf(values, values.length));
    }
}

class PeriodData {
    public final int tIndex;
    public final double[] demandSum;

    public PeriodData(int tIndex, double[] demandSum) {
        this.tIndex = tIndex;
        this.demandSum = demandSum;
    }
}

class Sample {
    public final int id;
    public final PeriodData period;
    public CovariateVector theta;

    public Sample(int id, PeriodData period, CovariateVector theta) {
        this.id = id;
        this.period = period;
        this.theta = theta;
    }

    public double[] demand() {
        return period.demandSum;
    }
}

class ProcurementParams {
    public final List<String> carriers;
    public final int I;
    public final int J;

    public final double[] e;

    public final double[] p;
    public final double[] h;

    public final double[][] q;
    public final double[][] r;
    public final boolean[][] eligible;

    public final int alpha;
    public final int beta;

    public ProcurementParams(List<String> carriers,
                             int J,
                             double[] e,
                             double[] p,
                             double[] h,
                             double[][] q,
                             double[][] r,
                             boolean[][] eligible,
                             int alpha,
                             int beta) {
        this.carriers = carriers;
        this.I = carriers.size();
        this.J = J;

        this.e = e;
        this.p = p;
        this.h = h;

        this.q = q;
        this.r = r;
        this.eligible = eligible;

        this.alpha = alpha;
        this.beta = beta;

        validate();
    }

    public void validate() {

        if (J <= 0 || I <= 0) throw new IllegalArgumentException("I and J must be positive.");

        if (e == null || e.length != J) throw new IllegalArgumentException("e size mismatch.");
        if (p == null || p.length != I) throw new IllegalArgumentException("p size mismatch.");
        if (h == null || h.length != I) throw new IllegalArgumentException("h size mismatch.");

        checkMatrix(r, I, J, "r");
        checkMatrix(q, I, J, "q");
        checkBoolMatrix(eligible, I, J, "eligible");

        if (alpha < 1 || alpha > I) throw new IllegalArgumentException("alpha out of range.");
        if (beta < 1 || beta > I) throw new IllegalArgumentException("beta out of range.");
        if (alpha > beta) throw new IllegalArgumentException("alpha cannot exceed beta.");

        for (int j = 0; j < J; j++) {
            if (e[j] < 0) throw new IllegalArgumentException("e[j] must be nonnegative.");
        }
        for (int i = 0; i < I; i++) {
            if (p[i] < 0) throw new IllegalArgumentException("p[i] must be nonnegative.");
            if (h[i] < 0) throw new IllegalArgumentException("h[i] must be nonnegative.");

            for (int j = 0; j < J; j++) {
                if (!eligible[i][j]) {
                    if (q[i][j] != 0.0) q[i][j] = 0.0;
                    if (r[i][j] != 0.0) r[i][j] = 0.0;
                }
            }
        }
    }

    private static void checkMatrix(double[][] a, int I, int J, String name) {
        if (a == null || a.length != I) throw new IllegalArgumentException(name + " row mismatch.");
        for (int i = 0; i < I; i++) {
            if (a[i] == null || a[i].length != J) throw new IllegalArgumentException(name + " col mismatch at row " + i);
        }
    }

    private static void checkBoolMatrix(boolean[][] a, int I, int J, String name) {
        if (a == null || a.length != I) throw new IllegalArgumentException(name + " row mismatch.");
        for (int i = 0; i < I; i++) {
            if (a[i] == null || a[i].length != J) throw new IllegalArgumentException(name + " col mismatch at row " + i);
        }
    }
}

// Conditional demand generation.
final class TRBSVUSyntheticDemandGenerator {
    private static final long COMMON_LOADING_SALT = 0x6A09E667F3BCC909L;
    private static final long COMMON_NOISE_SALT = 0xBB67AE8584CAA73BL;

    private static final long CONTEXT_STRUCTURE_SALT = 0x3C6EF372FE94F82BL;

    private TRBSVUSyntheticDemandGenerator() {
    }

    public enum Distribution {
        NORMAL, LOGNORMAL
    }

    public static final class Parameters {
        private final int historicalPeriods;
        private final double[] base;
        private final double[] market;
        private final double[] trend;
        private final double[] promotion;
        private final double[] attention;
        private final double[] volatilityQuantile;
        private final double[] commonLoading;

        private Parameters(int historicalPeriods, int lanes) {
            this.historicalPeriods = historicalPeriods;
            base = new double[lanes];
            market = new double[lanes];
            trend = new double[lanes];
            promotion = new double[lanes];
            attention = new double[lanes];
            volatilityQuantile = new double[lanes];
            commonLoading = new double[lanes];
        }

        public int laneCount() {
            return base.length;
        }

        public int historicalPeriods() {
            return historicalPeriods;
        }

        public double[] base() { return base.clone(); }
        public double[] market() { return market.clone(); }
        public double[] trend() { return trend.clone(); }
        public double[] promotion() { return promotion.clone(); }
        public double[] attention() { return attention.clone(); }

        public double[] commonLoading() { return commonLoading.clone(); }

        public double[] linearTrendTypicalDemand() {
            double[] result = new double[laneCount()];
            double meanTrend = (historicalPeriods - 1.0) / (2.0 * historicalPeriods);
            for (int j = 0; j < result.length; j++) {
                result[j] = base[j] + 0.5 * (market[j] + promotion[j] + attention[j])
                        + meanTrend * trend[j];
            }
            return result;
        }

        public double[] nominalDemand(CovariateVector context) {
            double[] x = context.values();
            if (x.length != 4) throw new IllegalArgumentException("Context must have M,T,P,A.");
            double[] result = new double[laneCount()];
            for (int j = 0; j < result.length; j++) {
                result[j] = base[j] + market[j] * x[0] + trend[j] * x[1]
                        + promotion[j] * x[2] + attention[j] * x[3];
            }
            return result;
        }

        public double[] volatilityParameters(double lower, double upper) {
            if (!Double.isFinite(lower) || !Double.isFinite(upper)
                    || lower < 0 || upper < lower)
                throw new IllegalArgumentException("Invalid CV interval.");
            double[] result = new double[laneCount()];
            for (int j = 0; j < result.length; j++)
                result[j] = lower + (upper - lower) * volatilityQuantile[j];
            return result;
        }
    }

    public static final class ConditionalQuery {
        public final CovariateVector context;
        public final List<Sample> oos;

        private ConditionalQuery(CovariateVector context, List<Sample> oos) {
            this.context = context;
            this.oos = List.copyOf(oos);
        }
    }

    public static final class MultiQueryReplication {
        public final List<Sample> history;
        public final List<ConditionalQuery> queries;

        private MultiQueryReplication(List<Sample> history, List<ConditionalQuery> queries) {
            this.history = List.copyOf(history);
            this.queries = List.copyOf(queries);
        }
    }

    public static Parameters sampleParameters(int laneCount, int historicalPeriods, long seed,
                                              double contextCoefficientScale,
                                              double commonLoadingLower,
                                              double commonLoadingUpper) {
        if (laneCount <= 0 || historicalPeriods <= 0) {
            throw new IllegalArgumentException("Lane and history counts must be positive.");
        }
        if (!(contextCoefficientScale > 0.0) || !Double.isFinite(contextCoefficientScale)) {
            throw new IllegalArgumentException("Context coefficient scale must be finite and positive.");
        }
        if (!(commonLoadingLower >= 0.0 && commonLoadingLower <= commonLoadingUpper
                && commonLoadingUpper < 1.0)) {
            throw new IllegalArgumentException("Common loading interval must lie in [0,1). ");
        }
        Parameters p = new Parameters(historicalPeriods, laneCount);
        Random random = new Random(seed);
        Random loadingRandom = new Random(seed ^ COMMON_LOADING_SALT);
        Random structureRandom = new Random(seed ^ CONTEXT_STRUCTURE_SALT);
        for (int j = 0; j < laneCount; j++) {
            p.base[j] = 10.0 + 90.0 * random.nextDouble();
            double[] ratios = new double[4];
            for (int k = 0; k < ratios.length; k++) {
                random.nextDouble();
                ratios[k] = structureRandom.nextDouble();
            }
            p.market[j] = contextCoefficientScale * p.base[j] * ratios[0];
            p.trend[j] = contextCoefficientScale * p.base[j] * ratios[1];
            p.promotion[j] = contextCoefficientScale * p.base[j] * ratios[2];
            p.attention[j] = contextCoefficientScale * p.base[j] * ratios[3];
            p.volatilityQuantile[j] = random.nextDouble();
            p.commonLoading[j] = uniform(loadingRandom, commonLoadingLower, commonLoadingUpper);
        }
        return p;
    }

    public static MultiQueryReplication generateMultiQueryWithLinearTrend(
            Parameters parameters, Distribution distribution, double[] laneCv,
            int queryCount, int oosCount, long contextSeed,
            long historyNoiseSeed, long oosNoiseSeed) {
        if (oosCount <= 0) throw new IllegalArgumentException("Positive OOS count is required.");
        return linearTrendQueries(parameters, distribution, laneCv, queryCount, oosCount,
                contextSeed, historyNoiseSeed, oosNoiseSeed);
    }

    public static MultiQueryReplication generateLinearTrendQueriesWithoutOos(
            Parameters parameters, Distribution distribution, double[] laneCv,
            int queryCount, long contextSeed, long historyNoiseSeed) {
        return linearTrendQueries(parameters, distribution, laneCv, queryCount, 0,
                contextSeed, historyNoiseSeed, 0L);
    }

    private static MultiQueryReplication linearTrendQueries(
            Parameters parameters, Distribution distribution, double[] laneCv,
            int queryCount, int oosCount, long contextSeed,
            long historyNoiseSeed, long oosNoiseSeed) {
        if (parameters == null || distribution == null || laneCv == null
                || queryCount <= 0 || oosCount < 0) {
            throw new IllegalArgumentException(
                    "Parameters, DGP cell, query count and OOS count are required.");
        }
        if (laneCv.length != parameters.laneCount())
            throw new IllegalArgumentException("One CV per lane is required.");
        for (double value : laneCv)
            if (!Double.isFinite(value) || value < 0)
                throw new IllegalArgumentException("Lane CV must be finite and nonnegative.");
        int h = parameters.historicalPeriods();
        Random contextRandom = new Random(contextSeed);
        Random historyRandom = new Random(historyNoiseSeed);
        Random oosRandom = new Random(oosNoiseSeed);
        Random historyCommonRandom = new Random(historyNoiseSeed ^ COMMON_NOISE_SALT);
        Random oosCommonRandom = new Random(oosNoiseSeed ^ COMMON_NOISE_SALT);
        double[] cv = laneCv.clone();
        List<Sample> history = new ArrayList<>(h);
        for (int t = 0; t < h; t++) {
            CovariateVector context = contextWithTrend(
                    contextRandom, (double) t / h);
            double[] demand = drawDemand(parameters.nominalDemand(context), cv,
                    parameters.commonLoading, distribution, historyRandom,
                    historyCommonRandom);
            history.add(sample(t, t, context, demand));
        }

        List<ConditionalQuery> queries = new ArrayList<>(queryCount);
        for (int query = 0; query < queryCount; query++) {
            CovariateVector queryContext = contextWithTrend(
                    contextRandom, 1.0);
            double[] nominal = parameters.nominalDemand(queryContext);
            List<Sample> oos = new ArrayList<>(oosCount);
            for (int draw = 0; draw < oosCount; draw++) {
                double[] demand = drawDemand(nominal, cv, parameters.commonLoading,
                        distribution, oosRandom, oosCommonRandom);
                oos.add(sample(query * oosCount + draw, h, queryContext.copy(), demand));
            }
            queries.add(new ConditionalQuery(queryContext, oos));
        }
        return new MultiQueryReplication(history, queries);
    }

    private static CovariateVector contextWithTrend(Random random, double trend) {
        double[] values = new double[4];
        for (int k = 0; k < values.length; k++) values[k] = random.nextDouble();
        values[1] = trend;
        return new CovariateVector(values);
    }

    private static double[] drawDemand(double[] nominal, double[] cv, double[] commonLoading,
                                       Distribution distribution, Random idiosyncraticRandom,
                                       Random commonRandom) {
        double[] demand = new double[nominal.length];
        double commonShock = commonRandom.nextGaussian();
        for (int j = 0; j < demand.length; j++) {
            double loading = commonLoading[j];
            double idiosyncraticLoading = Math.sqrt(1.0 - loading * loading);
            if (distribution == Distribution.NORMAL) {
                do {
                    double standardizedShock = loading * commonShock
                            + idiosyncraticLoading * idiosyncraticRandom.nextGaussian();
                    demand[j] = nominal[j] * (1.0 + cv[j] * standardizedShock);
                } while (demand[j] < 0.0);
            } else {
                double logVariance = Math.log1p(cv[j] * cv[j]);
                double standardizedShock = loading * commonShock
                        + idiosyncraticLoading * idiosyncraticRandom.nextGaussian();
                demand[j] = nominal[j] * Math.exp(-0.5 * logVariance
                        + Math.sqrt(logVariance) * standardizedShock);
            }
        }
        return demand;
    }

    private static Sample sample(int id, int period, CovariateVector context,
                                 double[] demand) {
        return new Sample(id, new PeriodData(period, demand), context);
    }

    private static double uniform(Random random, double lower, double upper) {
        return lower + (upper - lower) * random.nextDouble();
    }
}

// Procurement parameter generation.
final class TRBSVUProcurementGenerator {
    private TRBSVUProcurementGenerator() {
    }

    public static ProcurementParams generate(int carrierCount, double[] typicalDemand, long seed,
                                             double selectionFraction) {
        if (carrierCount < 6 || typicalDemand == null || typicalDemand.length == 0) {
            throw new IllegalArgumentException("At least six carriers and one lane are required.");
        }
        if (!(selectionFraction > 0.0 && selectionFraction <= 1.0)) {
            throw new IllegalArgumentException("Selection fraction must lie in (0,1].");
        }
        for (double value : typicalDemand) {
            if (!(value > 0.0) || !Double.isFinite(value)) {
                throw new IllegalArgumentException("Typical lane demand must be finite and positive.");
            }
        }
        Random random = new Random(seed);
        boolean[][] eligible = randomEligibility(carrierCount, typicalDemand.length, random);
        return buildMarket(typicalDemand, random, eligible, selectionFraction);
    }

    private static boolean[][] randomEligibility(int carrierCount, int lanes, Random random) {
        boolean[][] eligible = new boolean[carrierCount][lanes];
        int lanesPerCarrier = Math.max(1, (int) Math.round(0.50 * lanes));
        int[] laneCoverage = new int[lanes];
        for (int i = 0; i < carrierCount; i++) {
            List<Integer> shuffled = new ArrayList<>(lanes);
            for (int j = 0; j < lanes; j++) shuffled.add(j);
            Collections.shuffle(shuffled, random);
            for (int k = 0; k < lanesPerCarrier; k++) {
                int j = shuffled.get(k);
                eligible[i][j] = true;
                laneCoverage[j]++;
            }
        }
        repairUncoveredLanes(eligible, laneCoverage, random);
        return eligible;
    }

    private static void repairUncoveredLanes(
            boolean[][] eligible, int[] laneCoverage, Random random) {
        int carrierCount = eligible.length;
        int lanes = laneCoverage.length;

        for (int j = 0; j < lanes; j++) {
            if (laneCoverage[j] > 0) continue;
            List<Integer> carriers = new ArrayList<>(carrierCount);
            for (int i = 0; i < carrierCount; i++) carriers.add(i);
            Collections.shuffle(carriers, random);
            boolean repaired = false;
            for (int i : carriers) {
                List<Integer> donors = new ArrayList<>();
                for (int k = 0; k < lanes; k++)
                    if (eligible[i][k] && laneCoverage[k] > 1) donors.add(k);
                if (donors.isEmpty()) continue;
                int donor = donors.get(random.nextInt(donors.size()));
                eligible[i][donor] = false;
                eligible[i][j] = true;
                laneCoverage[donor]--;
                laneCoverage[j]++;
                repaired = true;
                break;
            }
            if (!repaired) throw new IllegalStateException("Cannot construct 50% lane coverage.");
        }
    }

    private static ProcurementParams buildMarket(
            double[] typicalDemand, Random random, boolean[][] eligible,
            double selectionFraction) {
        int carrierCount = eligible.length;
        int lanes = typicalDemand.length;
        double[][] rates = new double[carrierCount][lanes];
        double[][] capacities = new double[carrierCount][lanes];
        double[] spot = new double[lanes];
        double[] carrierFactor = new double[carrierCount];
        for (int i = 0; i < carrierCount; i++) {
            carrierFactor[i] = uniform(random, 0.7, 1.3);
        }
        for (int j = 0; j < lanes; j++) {
            double laneRate = uniform(random, 20.0, 100.0);
            List<Integer> carriers = new ArrayList<>();
            for (int i = 0; i < carrierCount; i++) {
                if (eligible[i][j]) carriers.add(i);
            }
            Collections.shuffle(carriers, random);
            double rateSum = 0.0;
            for (int i : carriers) {
                rates[i][j] = laneRate * carrierFactor[i]
                        * uniform(random, 0.9, 1.1);
                capacities[i][j] = typicalDemand[j] * uniform(random, 0.3, 0.5);
                rateSum += rates[i][j];
            }
            spot[j] = uniform(random, 2.0, 3.0) * rateSum / carriers.size();
        }

        double[] mqc = new double[carrierCount];
        double[] penalty = new double[carrierCount];
        for (int i = 0; i < carrierCount; i++) {
            double eligibleDemand = 0.0;
            double minimumRate = Double.POSITIVE_INFINITY;
            for (int j = 0; j < lanes; j++) {
                if (!eligible[i][j]) continue;
                eligibleDemand += typicalDemand[j];
                minimumRate = Math.min(minimumRate, rates[i][j]);
            }
            mqc[i] = eligibleDemand * uniform(random, 0.15, 0.35);
            penalty[i] = minimumRate;
        }
        List<String> carrierNames = new ArrayList<>(carrierCount);
        for (int i = 0; i < carrierCount; i++) carrierNames.add("C" + (i + 1));
        return new ProcurementParams(carrierNames, lanes, spot, mqc, penalty,
                capacities, rates, eligible,
                (int) Math.ceil(0.1 * carrierCount),
                (int) Math.ceil(selectionFraction * carrierCount));
    }

    private static double uniform(Random random, double lower, double upper) {
        return lower + (upper - lower) * random.nextDouble();
    }
}
