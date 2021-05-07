import comp34120.ex2.PlayerImpl;
import comp34120.ex2.PlayerType;
import comp34120.ex2.Record;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Random;
import java.lang.Math;

/**
 * Extends the PlayerImpl class to create a leader for a two-player Stackelberg
 * pricing Game.
 * @author Ivan Dewerpe
 * @author Chen Bo Calvin Zhang
 */
final class PlayerPolly extends PlayerImpl {

	private ArrayList<Record> data = new ArrayList<Record>();
	private ArrayList<Double> expectedUF = new ArrayList<Double>();
	private double profitAccumulation = 0;
	private double expectedProfit = 0;
	private PolynomialRegression regression;
    private int degree = 4;

	PlayerPolly() throws RemoteException, NotBoundException {
        super(PlayerType.LEADER, "Polly Leader");
	}

	@Override
	public void startSimulation(int p_steps) throws RemoteException {
		int history_len = 100;
		
        for (int t = 0; t < history_len; t++) {
            this.data.add(m_platformStub.query(this.m_type, t+1));
		}

		System.out.println("Degree " + degree);
	}
	
	@Override
	public void endSimulation()	throws RemoteException {
		this.data.add(m_platformStub.query(this.m_type, data.size()+1));
		profitAccumulation += computeProfitOnDay(data.size());

		System.out.println("Final cumulative profit is " + profitAccumulation);
		System.out.println("Expected cumulative profit is " + expectedProfit);

		double mse = 0;
		double mape = 0;
		double mabse = 0;

		for (int t = 100; t < data.size(); t++) {
			Record u_record = data.get(t);
			double u_f_hat = expectedUF.get(t-100);
			double u_f_true = u_record.m_followerPrice;
			mse += Math.pow(u_f_true - u_f_hat, 2);
			mape += Math.abs((u_f_true - u_f_hat) / u_f_true);
			mabse += Math.abs(u_f_true - u_f_hat);
		}

		mse /= (data.size() - 100);
		mape /= (data.size() - 100);
		mabse /= (data.size() - 100);
		
		System.out.println("Mean Squared Error " + mse);
		System.out.println("Mean Absolute Percentage Error " + mape);
		System.out.println("Mean Absolute Error " + mabse);
		System.out.println("-------------------------------------------------");

		// clean memory
		this.data.clear();
		this.expectedUF.clear();
		this.profitAccumulation = 0;
		this.expectedProfit = 0;
	}

	public double demandModel(double u_l, double u_f) {
		return (double) (2 - u_l + 0.3 * u_f);
	}

	public double dailyProfit(double u_l, double u_f, double c) {
		return (u_l - c) * demandModel(u_l, u_f);
	}

	public double computeProfitOnDay(int date) {
		Record record = data.get(date - 1);
		double real_ul = record.m_leaderPrice;
		double real_uf = record.m_followerPrice;

		return dailyProfit(real_ul, real_uf, 1);
	}

	public double[] getPredictedProfit(double leaderPrice, int p_date, int n) {
		double[] price_and_profit = {1.0, 1.0};
		price_and_profit[0] = this.regression.predict(leaderPrice);
		price_and_profit[1] = dailyProfit(leaderPrice, price_and_profit[0], 1);
		
		return price_and_profit;
	
	} //getPredictedProfit
	
	public double[] getPredictedLeaderPrice(int p_date, int n) {
		double predictedProfit, followerPrice;
		double priceBound = 12;
		double[] returns = {-1000.0, 1.0, 0.0};
		
		for (double leaderPrice = 1; leaderPrice <= priceBound; leaderPrice += 0.0025) {
			
			double[] price_and_profit = getPredictedProfit(leaderPrice, p_date, n);
			followerPrice = price_and_profit[0];
			predictedProfit = price_and_profit[1];
	
			if (predictedProfit >= returns[0]) {
	
				returns[0] = predictedProfit;
				returns[1] = leaderPrice;
				returns[2] = followerPrice;
	
			} //if
		} //for

		return returns;
	} //getPredictedLeaderPrice

	/**
	 * To inform this instance to proceed to a new simulation day
	 * @param p_date The date of the new day
	 * @throws RemoteException This exception *MUST* be thrown by this method
	 */
	@Override
	public void proceedNewDay(int p_date) throws RemoteException {

		// update data array
		if (p_date > 101) {
			this.data.add(m_platformStub.query(this.m_type, p_date - 1));
			profitAccumulation += computeProfitOnDay(p_date - 1);
		}

        int array_length = this.data.size();
        double[] x = new double[array_length];
        double[] y = new double[array_length];

        for (int i = 0; i < array_length; i++) {
            Record record = data.get(i);
            x[i] = record.m_leaderPrice;
            y[i] = record.m_followerPrice;
        }

        regression = new PolynomialRegression(x, y, degree);
        
		// estimated reaction
		double[] predictedLP = getPredictedLeaderPrice(p_date, 2);
		double eProfit = predictedLP[0];
		double u_l = predictedLP[1];
		double u_f = predictedLP[2];

		expectedUF.add(u_f);
		expectedProfit += eProfit;
		
		m_platformStub.publishPrice(m_type, (float) u_l);
		
	}

    public static void main(final String[] p_args) throws RemoteException, NotBoundException {
		new PlayerPolly();
	}
}
