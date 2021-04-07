import comp34120.ex2.PlayerImpl;
import comp34120.ex2.PlayerType;
import comp34120.ex2.Record;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Random;


/**
 * Extends the PlayerImpl class to create a leader for a two-player Stackelberg
 * pricing Game.
 * @author Ivan Dewerpe
 * @author Chen Bo Calvin Zhang
 */
final class PlayerYaoMing extends PlayerImpl {

    private ArrayList<Record> data = new ArrayList<Record>();

	private final Random m_randomizer = new Random(System.currentTimeMillis());

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
		super.endSimulation();
	}

	public float demandModel(float u_l, float u_f) {
		return 2 - u_l + 0.3 * u_f;
	}

	public float dailyProfit(float u_l, float u_f, float c) {
		return (u_l - c) * demandModel(u_l, u_f);
	}	

	/**
	 * To inform this instance to proceed to a new simulation day
	 * @param p_date The date of the new day
	 * @throws RemoteException This exception *MUST* be thrown by this method
	 */
	@Override
	public void proceedNewDay(int p_date) throws RemoteException {
		
		float price_l = 0.2191 * 1.8 + 1.3768;

		m_platformStub.publishPrice(m_type, price_l);
	}

    public static void main(final String[] p_args) throws RemoteException, NotBoundException {
		new PlayerYaoMing();
	}
}
