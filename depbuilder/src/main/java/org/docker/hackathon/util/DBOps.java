package org.docker.hackathon.util;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.DriverManager;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.apache.log4j.Logger;
import org.apache.log4j.Level;

import org.docker.hackathon.depbuilder.Constants;

/* 
 * Class to get a connection to BuildDB and perform some CRUD operations.
 * 
 */

public class DBOps {
	String sqlStmt;
	String db_connection_url = new String("");
	String db_user = new String("");
	String db_pass = new String("");
	Connection connection = null;

	Level logLevel = Level.toLevel("INFO");
	static Logger logger = Logger.getLogger(DBOps.class.getName());

	public Connection getConnection() {
		
		try{
			if(connection!=null) {
			if(connection.isClosed())
				connect();
			}
			else
				connect();
		}
		catch (Exception e) {e.printStackTrace();}
		return connection;
	}

	public void setConnection(Connection connection) {
		this.connection = connection;
	}
	
	public void releaseConnection() {
		this.connection = null;
	}

	public DBOps(String dbURL, String dbUser, String dbPass) {
		db_connection_url = dbURL;
		db_user = dbUser;
		db_pass = dbPass;
		connect();
	}

	private boolean connect() {
		boolean retval = false;
		try {
			Connection conn= null;
			Class.forName("com.mysql.jdbc.Driver");
			conn = DriverManager.getConnection(db_connection_url, db_user, db_pass);
			if(conn != null) {
				setConnection(conn);
				retval = true;
			} 
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return retval;
	}

	public String getSqlStmt() {
		return sqlStmt;
	}

	public void setSqlStmt(String sqlStmt) {
		this.sqlStmt = sqlStmt;
	}

	public boolean run() {
		boolean retval = false;
		retval = run(getSqlStmt());
		return retval;
	}

	public ResultSet query(String sqlStmt) {
		ResultSet rs = null;
		try {
			Statement stmt = getConnection().createStatement();
			logger.debug("run(sqlStmt) : Executing statement : "+sqlStmt);
			rs = stmt.executeQuery(sqlStmt);
			if(rs == null) {
				logger.error("run : Error while running the SQL statement : "+sqlStmt);
			} else {
				logger.debug("run : Successfully executed "+sqlStmt);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return rs;
	}

	public boolean run(String sqlStmt) {
		boolean retval = false;
		try {
			Statement stmt = getConnection().createStatement();
			logger.debug("run(sqlStmt) : Executing statement : "+sqlStmt);
			ResultSet rSet=stmt.executeQuery(sqlStmt);
			if(rSet.next()) {
				retval = Constants.SUCCESS;
				logger.info("run : Successfully executed "+sqlStmt+" retval: "+retval);	
			} else {
				retval = Constants.FAILURE;
				logger.info("run : NO result found while running : "+sqlStmt+" retval: "+retval);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return retval;
	}
	public boolean runUpdateInsertDelete(String sqlStmt) {
		boolean retval = false;
		try {
			Statement stmt = getConnection().createStatement();
			logger.debug("run(sqlStmt) : Executing statement : "+sqlStmt);
			int count=stmt.executeUpdate(sqlStmt);
			
			if(count==0) {
				retval = Constants.FAILURE;
				logger.error("run : Error while running the SQL statement : "+sqlStmt+" retval: "+retval);
				
			} else {
				retval = Constants.SUCCESS;
				logger.info("run : Successfully executed "+sqlStmt+" retval: "+retval);
				
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return retval;
	}
	
	

	public String getHostEntryFromBuildDB() {
		String hostid = null;
		try {
			String localhostname = java.net.InetAddress.getLocalHost().getHostName();
			String localIPAddr = java.net.InetAddress.getLocalHost().getHostAddress();
			String hostQueryStmt = "SELECT idkey from host where hostname=\""+localhostname+"\" LIMIT 1";
			ResultSet rs = query(hostQueryStmt);
			while ( rs.next() ) {
				hostid = rs.getString("idkey");
			}
			
			if(hostid == null) {
				// Could not find the host entry in the BuildDB. Add it.
				String hostInsertStmt = "INSERT INTO host (hostname,ip,platform,init_date,connect_date) "+
										"VALUES ('"+localhostname+"','"+localIPAddr+"',\"2.6\",Now(),Now())";
				boolean retval = run(hostInsertStmt);
				if(retval == Constants.FAILURE) {
					logger.error("Error while inserting host entry for "+localhostname+ "into the Build database.");
					return null;
				} else {
					logger.info("Successfully inserted host entry for "+localhostname+ "into the Build database.");
				}
				// Insertion should have been successful. Querying the DB again for the host entry
				hostQueryStmt = "SELECT idkey from host where hostname=\""+localhostname+"\" LIMIT 1";
				rs = query(hostQueryStmt);
				while ( rs.next() ) {
					hostid = rs.getString("idkey");
				}
			}
			return hostid;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return hostid;
	}
	
	public String getCurrentTime() {
		return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Calendar.getInstance().getTime());
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
