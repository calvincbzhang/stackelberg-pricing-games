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
final class PlayerYaoMing extends PlayerImpl {

	private ArrayList<Record> data = new ArrayList<Record>();
	private ArrayList<Double> expectedUF = new ArrayList<Double>();
	private double profitAccumulation = 0;
	private double expectedProfit = 0;
	private double a_star;
	private double b_star;

	PlayerYaoMing() throws RemoteException, NotBoundException {
		super(PlayerType.LEADER, "Yao Ming Leader");
	}

	@Override
	public void startSimulation(int p_steps) throws RemoteException {
		int history_len = 100;
		
        for (int t=0; t < history_len; t++) {
            this.data.add(m_platformStub.query(this.m_type, t+1));
		}
	}
	
	@Override
	public void endSimulation()	throws RemoteException {
		this.data.add(m_platformStub.query(this.m_type, data.size()+1));
		profitAccumulation += computeProfitOnDay(data.size());

		System.out.println("Final cumulative profit is " + profitAccumulation);
		System.out.println("Expected cumulative profit is " + expectedProfit);

		double mse = 0;
		double mape = 0;

		for (int t=100; t < data.size(); t++) {
			Record u_record = data.get(t);
			double u_f_hat = expectedUF.get(t-100);
			double u_f_true = u_record.m_followerPrice;
			mse += Math.pow(u_f_true - u_f_hat, 2);
			mape += Math.abs((u_f_true - u_f_hat) / u_f_true);
		}

		mse /= (data.size() - 100);
		mape /= (data.size() - 100);
		
		System.out.println("Mean Squared Error " + mse);
		System.out.println("Mean Absolute Percentage Error " + mape);

		// CLEAN MEMORY
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

	public void estimateReaction() {
		int T = this.data.size();
		double sum_L = 0;
		double sum_F = 0;
		double sum_Lsq = 0;
		double sum_LF = 0;

		for (int t=0; t < T; t++) {
			Record record = this.data.get(t);
			double u_l_t = record.m_leaderPrice;
			double u_f_t = record.m_followerPrice;

			sum_L += u_l_t;
			sum_F += u_f_t;
			sum_Lsq += (u_l_t * u_l_t);
			sum_LF += (u_l_t * u_f_t);
		}

		double a_star = ((sum_Lsq * sum_F) - (sum_L * sum_LF)) / ((T * sum_Lsq) - (sum_L * sum_L));
		double b_star = ((T * sum_LF) - (sum_L * sum_F)) / ((T * sum_Lsq) - (sum_L * sum_L));

		this.a_star = a_star;
		this.b_star = b_star;

		return;
	}

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

		// estimate the coefficient for the reaction function
		estimateReaction();
		// System.out.println(this.a_star);
		// System.out.println(this.b_star);

		// estimated reaction
		double u_l = (0.3 * (this.a_star - this.b_star) - 3) / (2 * ((0.3 * this.a_star - 1)));
		double u_f = (this.a_star * u_l + this.b_star);
	
		expectedUF.add(u_f);
		expectedProfit += dailyProfit(u_l, u_f, 1);
		
		m_platformStub.publishPrice(m_type, (float) u_l);
		
	}

    public static void main(final String[] p_args) throws RemoteException, NotBoundException {
		new PlayerYaoMing();
	}
}
