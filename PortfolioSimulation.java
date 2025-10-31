package assignment;
import java.util.*;
import java.text.DecimalFormat;

/**
 * Financial instrument hierarchy + portfolio return simulation.
 * Compile: javac PortfolioSimulation.java
 * Run:     java PortfolioSimulation
 */
public class PortfolioSimulation {

    // ====== Abstract Base ======
    static abstract class FinancialInstrument {
        private final String symbol;
        private double weight; // portfolio weight (will be normalized by Portfolio)

        protected FinancialInstrument(String symbol, double weight) {
            this.symbol = symbol;
            this.weight = weight;
        }

        public String getSymbol() { return symbol; }
        public double getWeight() { return weight; }
        public void setWeight(double w) { this.weight = w; }

        /**
         * Simulate a single-period (e.g., daily/monthly) return as a decimal.
         * Example: 0.01 = +1%
         */
        public abstract double simulateReturn(Random rng);
    }

    // ====== Stock ======
    static class Stock extends FinancialInstrument {
        private final double expectedReturn; // per-period drift (e.g., 0.01 = 1%)
        private final double volatility;     // per-period volatility (std dev)

        public Stock(String symbol, double weight, double expectedReturn, double volatility) {
            super(symbol, weight);
            this.expectedReturn = expectedReturn;
            this.volatility = volatility;
        }

        @Override
        public double simulateReturn(Random rng) {
            // Simple normal (Gaussian) model: r = mu + sigma * Z
            double z = rng.nextGaussian();
            return expectedReturn + volatility * z;
        }
    }

    // ====== Bond ======
    static class Bond extends FinancialInstrument {
        private final double couponRate;     // per-period coupon as % of par (decimal)
        private final double durationYears;  // effective duration proxy (sensitivity to rate moves)
        private final double rateShockVol;   // std dev of rate shocks per period

        public Bond(String symbol, double weight, double couponRate, double durationYears, double rateShockVol) {
            super(symbol, weight);
            this.couponRate = couponRate;
            this.durationYears = durationYears;
            this.rateShockVol = rateShockVol;
        }

        @Override
        public double simulateReturn(Random rng) {
            // Very simplified:
            // Bond return ≈ coupon - duration * rateShock
            // where rateShock ~ N(0, rateShockVol)
            double rateShock = rng.nextGaussian() * rateShockVol;
            return couponRate - durationYears * rateShock;
        }
    }

    // ====== Derivative (e.g., a leveraged exposure to an underlying stock) ======
    static class Derivative extends FinancialInstrument {
        private final Stock underlying;
        private final double leverage;       // sensitivity to underlying returns
        private final double carryCost;      // per-period cost (e.g., financing, theta), decimal

        public Derivative(String symbol, double weight, Stock underlying, double leverage, double carryCost) {
            super(symbol, weight);
            this.underlying = underlying;
            this.leverage = leverage;
            this.carryCost = carryCost;
        }

        @Override
        public double simulateReturn(Random rng) {
            // Simple model: derivative return = leverage * underlying_return - carryCost
            double rU = underlying.simulateReturn(rng);
            return leverage * rU - carryCost;
        }
    }

    // ====== Portfolio ======
    static class Portfolio {
        private final List<FinancialInstrument> holdings = new ArrayList<>();

        public Portfolio add(FinancialInstrument fi) {
            holdings.add(fi);
            return this;
        }

        public void normalizeWeights() {
            double sum = holdings.stream().mapToDouble(FinancialInstrument::getWeight).sum();
            if (sum == 0.0) throw new IllegalArgumentException("Sum of weights is zero.");
            for (FinancialInstrument fi : holdings) {
                fi.setWeight(fi.getWeight() / sum);
            }
        }

        /**
         * Simulate portfolio value path.
         *
         * @param periods number of periods to simulate
         * @param initialValue starting portfolio value
         * @param seed PRNG seed for reproducibility
         * @return array of portfolio values per period (length periods+1, including t=0)
         */
        public double[] simulate(int periods, double initialValue, long seed) {
            normalizeWeights();
            Random rng = new Random(seed);

            double[] values = new double[periods + 1];
            values[0] = initialValue;

            for (int t = 1; t <= periods; t++) {
                double portfolioReturn = 0.0;
                for (FinancialInstrument fi : holdings) {
                    double r = fi.simulateReturn(rng);
                    portfolioReturn += fi.getWeight() * r;
                }
                values[t] = values[t - 1] * (1.0 + portfolioReturn);
            }
            return values;
        }

        public List<FinancialInstrument> getHoldings() {
            return Collections.unmodifiableList(holdings);
        }
    }

    // ====== Demo (Main) ======
    public static void main(String[] args) {
        // Create instruments
        // Stock: expectedReturn ~ 0.8% per period, vol ~ 5% per period
        Stock techStock = new Stock("TECH", 0.50, 0.008, 0.05);

        // Bond: 0.4% coupon per period (~ 4.8% annual if 12 periods), duration 5y, rate shock vol 0.05% per period
        Bond govBond   = new Bond("BOND10Y", 0.30, 0.004, 5.0, 0.0005);

        // Derivative on the stock: 2x leverage, 0.2% carry per period
        Derivative leveredTech = new Derivative("LEV-TECH", 0.20, techStock, 2.0, 0.002);

        // Build portfolio
        Portfolio pf = new Portfolio()
                .add(techStock)
                .add(govBond)
                .add(leveredTech);

        // Simulate 24 periods (e.g., 24 months), initial capital 100,000, seeded for reproducibility
        int periods = 24;
        double initial = 100_000.0;
        long seed = 42L;

        double[] path = pf.simulate(periods, initial, seed);

        // Pretty print results
        DecimalFormat pct = new DecimalFormat("+0.00%;-0.00%");
        DecimalFormat cur = new DecimalFormat("#,##0.00");

        System.out.println("=== Portfolio Composition (normalized) ===");
        for (FinancialInstrument fi : pf.getHoldings()) {
            System.out.printf("  %-10s  weight=%5.1f%%%n", fi.getSymbol(), fi.getWeight() * 100.0);
        }

        System.out.println("\n=== Value Path ===");
        for (int t = 0; t < path.length; t++) {
            System.out.printf("  t=%2d  value=%s%n", t, cur.format(path[t]));
        }

        double totalReturn = (path[path.length - 1] / path[0]) - 1.0;
        double cagr = Math.pow(1.0 + totalReturn, 12.0 / periods) - 1.0; // if periods ~ months

        System.out.println("\n=== Summary ===");
        System.out.println("  Initial Value : " + cur.format(path[0]));
        System.out.println("  Final Value   : " + cur.format(path[path.length - 1]));
        System.out.println("  Total Return  : " + pct.format(totalReturn));
        System.out.println("  Approx. Annualized Return (CAGR) : " + pct.format(cagr));
        System.out.println("Divya A");  
        System.out.println("2117240020094");}
}
