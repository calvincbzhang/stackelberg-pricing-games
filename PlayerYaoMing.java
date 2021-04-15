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
	private ArrayList<Float> expectedUF = new ArrayList<Float>();
	private float profitAccumulation = 0;
	private float a_star;
	private float b_star;

	PlayerYaoMing() throws RemoteException, NotBoundException {
		super(PlayerType.LEADER, "Yao Ming Leader");
	}

	@Override
	public void startSimulation(int p_steps) throws RemoteException {
        int history_len = 100;
		
        for (int t=0; t <= history_len; t++) {
            this.data.add(m_platformStub.query(this.m_type, t+1));
			// String date = String.format("%d", this.data.get(t).m_date);
			// String fP = String.format("%f", this.data.get(t).m_followerPrice);
			// String lP = String.format("%f", this.data.get(t).m_leaderPrice);

			// m_platformStub.log(this.m_type, date);
			// m_platformStub.log(this.m_type, fP);
			// m_platformStub.log(this.m_type, lP);
        }
	}
	
	@Override
	public void endSimulation()	throws RemoteException {
		this.data.add(m_platformStub.query(this.m_type, data.size()));

		System.out.println("Final cumulative profit is " + profitAccumulation);
		float mse = 0;
		float mape = 0;

		for (int t=101; t < data.size(); t++) {
			Record u_record = data.get(t);
			float u_f_hat = expectedUF.get(t-101);
			float u_f_true = u_record.m_followerPrice;
			System.out.println("u_hat " + u_f_hat);
			System.out.println("u_true " + u_f_true);
			mse += Math.pow(u_f_true - u_f_hat, 2);
			mape += Math.abs((u_f_true - u_f_hat) / u_f_true);
		}

		mse /= (data.size() - 101);
		mape /= (data.size() - 101);
		
		System.out.println("Mean Squared Error " + mse);
		System.out.println("Mean Absolute Percentage Error " + mape);

		// CLEAN MEMORY
		this.data.clear();
		this.expectedUF.clear();
		this.profitAccumulation = 0;
	}

	public float demandModel(float u_l, float u_f) {
		return (float) (2 - u_l + 0.3 * u_f);
	}

	public float dailyProfit(float u_l, float u_f, float c) {
		return (u_l - c) * demandModel(u_l, u_f);
	}

	public void estimateReaction() {
		int T = data.size();
		float sum_L = 0;
		float sum_F = 0;
		float sum_Lsq = 0;
		float sum_LF = 0;

		for (int t=0; t < T; t++) {
			Record record = data.get(t);
			float u_l_t = record.m_leaderPrice;
			float u_f_t = record.m_followerPrice;

			sum_L += u_l_t;
			sum_F += u_f_t;
			sum_Lsq += (u_l_t * u_l_t);
			sum_LF += (u_l_t * u_f_t);
		}

		float a_star = ((sum_Lsq * sum_F) - (sum_L * sum_LF)) / ((T * sum_Lsq) - (sum_L * sum_L));
		float b_star = ((T * sum_LF) - (sum_L * sum_F)) / ((T * sum_Lsq) - (sum_L * sum_L));

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
		}

		// estimate the coefficient for the reaction function
		estimateReaction();
		System.out.println(this.a_star);
		System.out.println(this.b_star);

		// estimated reaction
		float u_l = (float) (0.3 * (this.a_star - this.b_star) - 3) / (float) (2 * ((0.3 * this.a_star - 1)));
		float u_f = (float) (this.a_star * u_l + this.b_star);
		
		expectedUF.add(u_f);
		
		profitAccumulation += dailyProfit(u_l, u_f, 1);
		System.out.println("Current cumulative profit is " + profitAccumulation);
		
		m_platformStub.publishPrice(m_type, u_l);
		
	}

    public static void main(final String[] p_args) throws RemoteException, NotBoundException {
		new PlayerYaoMing();
	}
}
