// code inspired by https://introcs.cs.princeton.edu/java/97data/PolynomialRegression.java.html
// technique https://en.wikipedia.org/wiki/QR_decomposition

import Jama.Matrix;
import Jama.QRDecomposition;

public class PolynomialRegression {
    private final int N;
    private final int degree;
    private final Matrix beta;

    public PolynomialRegression(double[] x, double[] y, int degree) {
        this.degree = degree;
        N = x.length;

        // build Vandermonde matrix
        double[][] vandermonde = new double[N][degree+1];
        for (int i = 0; i < N; i++) {
            for (int j = 0; j <= degree; j++) {
                vandermonde[i][j] = Math.pow(x[i], j);
            }
        }
        Matrix X = new Matrix(vandermonde);

        // create matrix from vector
        Matrix Y = new Matrix(y, N);

        // find least squares solution
        QRDecomposition qr = new QRDecomposition(X);
        beta = qr.solve(Y);

    }

    public double beta(int j) {
        return beta.get(j, 0);
    }

    // predicted y value corresponding to x
    public double predict(double x) {
        // horner's method
        double y = 0.0;
        for (int j = degree; j >= 0; j--)
            y = beta(j) + (x * y);
        return y;
    }
}
