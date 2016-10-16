package bg.piggybank.model.amounts;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import bg.piggybank.model.DBConnection;
import bg.piggybank.model.accounts.Account;
import bg.piggybank.model.accounts.AccountDAO;
import bg.piggybank.model.exeptions.FailedConnectionException;
import bg.piggybank.model.user.User;
import bg.piggybank.model.user.UserDAO;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

@Component
public class AmountDAO {
	private static final String SELECT_AMOUNTS = "SELECT id, date, money, account_id FROM amounts WHERE account_id=?;";

	@Autowired
	private AccountDAO accountDao;
	@Autowired
	private UserDAO userDao;

	private static volatile int count = 0;

	public void startAmountTrackingAfterServerRestart() {
		count++;
		if (count <= 1) {
			for (int accountId : accountDao.getAllAccounts()) {
				Thread amountSaver = new Thread(new AmountSaver(accountId, accountDao));
				amountSaver.setDaemon(true);
				amountSaver.start();
			}
		} else {
			return;
		}
	}

	public List<Amount> getAllMyAmounts(String username) {
		Connection connection;
		List<Amount> amounts = new ArrayList<Amount>();
		try {
			connection = DBConnection.getInstance().getConnection();
			User user = userDao.getUserByUsername(username);
			List<Integer> accountIds = accountDao.getAccountIDByUserID(user.getId());
			for (int accountID : accountIds) {
				PreparedStatement statement = connection.prepareStatement(SELECT_AMOUNTS);
				statement.setInt(1, accountID);
				ResultSet result = statement.executeQuery();
				while (result.next()) {
					int id = result.getInt("id");
					Timestamp date = result.getTimestamp("date");
					double money = result.getDouble("money");
					int accountId = result.getInt("account_id");
					Account account = accountDao.getAccountByID(accountId, connection);
					String IBAN = account.getIBAN();
					System.out.println(IBAN);
					Amount amount = new Amount(id, date, money, IBAN);
					amounts.add(amount);
				}
			}
		} catch (FailedConnectionException | SQLException e) {
			e.printStackTrace();
			return amounts;
		}
		return amounts;
	}

	public List<Amount> getAllAmountsForAccount(String IBAN) {
		Connection connection;
		List<Amount> amounts = new ArrayList<Amount>();
		try {
			connection = DBConnection.getInstance().getConnection();
			Account account = accountDao.getAccountByIBAN(IBAN);
			PreparedStatement statement = connection.prepareStatement(SELECT_AMOUNTS);
			statement.setInt(1, account.getId());
			ResultSet result = statement.executeQuery();
			while (result.next()) {
				int id = result.getInt("id");
				Timestamp date = result.getTimestamp("date");
				double money = result.getDouble("money");
				Amount amount = new Amount(id, date, money, IBAN);
				amounts.add(amount);
			}
		} catch (FailedConnectionException | SQLException e) {
			e.printStackTrace();
			return amounts;
		}
		return amounts;
	}
}
