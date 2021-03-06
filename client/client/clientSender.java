package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.net.InetAddress;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.Hashtable;
import java.util.Iterator;
import org.json.JSONArray;

public class clientSender implements Runnable {
	private String username;
	private String servIp;
	private int servPort;
	private String localIP;
	private int listenPort;

	private Socket socket = null;
	private Hashtable<String, String> addressMap = null;
	private Hashtable<String, JSONObject> privateMap = null;
	
	private int wrongTimes = 0;
	private final static int WRONGTRY = 3;
	private boolean loggedIn = false;
	
	BufferedReader input = new BufferedReader(new InputStreamReader(System.in));

	public clientSender(String newServIp, int newServPort, int newlistenPort) {
		servIp = newServIp;
		servPort = newServPort;
		listenPort = newlistenPort;
		addressMap = new Hashtable<String, String>();        //stores all the address that this client can privately contact to
		privateMap = new Hashtable<String, JSONObject>();    //stores all the private requests from other clients
		localIP = "";
		try {
			localIP = InetAddress.getLocalHost().toString().split("/")[1];
		} catch (Exception e) {}
	}

	public void insertPrivateReq(String fromname, JSONObject reqJSON) {
		try {
			privateMap.put(fromname, reqJSON);
		} catch (Exception e) {}
	}

	public void removePrivateReq(String fromname) {
		try {
			privateMap.remove(fromname);
		} catch (Exception e) {}
	}

	private void printRequests() {
		for (Iterator iter = privateMap.keySet().iterator(); iter.hasNext();) {
			String key = (String)iter.next();
			System.out.println(key);
		}
	}

	private String composeMSG(String[] inputParser, int start) {
		String message = "";
		for (int i = start; i < inputParser.length; i++) {
			message += inputParser[i];
			message += " ";
		}
		return message;
	}

	/**************************************************************************
	This function analyzes the input from client, and compose the string into
	a JSON format
	**************************************************************************/
	private JSONObject resolveInput(String inputStr) throws JSONException {
		JSONObject reqJSON = new JSONObject();
		String[] fields = inputStr.split(" ");
		if (fields.length <= 0) {
			try {
				reqJSON.put("type", "none");
			} catch (Exception e) {}
			return reqJSON;
		}
		
		reqJSON.put("username", username);
		reqJSON.put("address", localIP + ":" + listenPort);
		if (fields[0].equals("privateRequests")) {
			if (fields.length != 1) {
				reqJSON.put("type", "input_error");
			} else {
				printRequests();
			}
		} else if (fields[0].equals("accept")) {
			if (fields.length != 2) {
				reqJSON.put("type", "input_error");
			} else {
				reqJSON.put("type", fields[0]);
				reqJSON.put("toname", fields[1]);
			}
		} else if (fields[0].equals("message")) {
			if (fields.length < 3) {
				reqJSON.put("type", "input_error");
			} else {
				reqJSON.put("type", fields[0]);
				reqJSON.put("toname", fields[1]);
				reqJSON.put("message", composeMSG(fields, 2));
			}
		} else if (fields[0].equals("block")) {
			if (fields.length != 2) {
				reqJSON.put("type", "input_error");
			} else {
				reqJSON.put("type", fields[0]);
				reqJSON.put("toname", fields[1]);
			}
		} else if (fields[0].equals("unblock")) {
			if (fields.length != 2) {
				reqJSON.put("type", "input_error");
			} else {
				reqJSON.put("type", fields[0]);
				reqJSON.put("toname", fields[1]);
			}
		}else if (fields[0].equals("getaddress")) {
			if (fields.length != 2) {
				reqJSON.put("type", "input_error");
			} else {
				reqJSON.put("type", fields[0]);
				reqJSON.put("toname", fields[1]);
			}
		} else if (fields[0].equals("private")) {
			if (fields.length < 3) {
				reqJSON.put("type", "input_error");
			} else {
				reqJSON.put("type", fields[0]);
				reqJSON.put("toname", fields[1]);
				reqJSON.put("message", composeMSG(fields, 2));
			}
		}else if (fields[0].equals("broadcast")) {
			if (fields.length < 2) {
				reqJSON.put("type", "input_error");
			} else {
				reqJSON.put("type", fields[0]);
				reqJSON.put("message", composeMSG(fields, 1));
			}
		} else if (fields[0].equals("logout")) {
			if (fields.length != 1) {
				reqJSON.put("type", "input_error");
			} else {
				reqJSON.put("type", fields[0]);
			}
		} else if (fields[0].equals("online")) {
			if (fields.length != 1) {
				reqJSON.put("type", "input_error");
			} else {
				reqJSON.put("type", fields[0]);
			}
		} else if (fields[0].equals("getaddress")) {
			if (fields.length != 2) {
				reqJSON.put("type", "input_error");
			} else {
				reqJSON.put("type", fields[0]);
				reqJSON.put("toname", fields[1]);
			}
		} else {
			reqJSON.put("type", "input_error");
		}
		return reqJSON;
	}

	/**************************************************************************
	This function deals with all the communication to outside including 
	server and other client and returns the JSON response from outside
	**************************************************************************/
	private JSONObject sendReq(JSONObject reqJSON) throws JSONException{
		JSONObject respJSON = new  JSONObject();
		BufferedReader in = null;
		PrintStream out = null;
		if (reqJSON.getString("type").equals("accept")) {
			String toname = reqJSON.getString("toname");
			JSONObject privateJSON = privateMap.get(toname);
			if (privateJSON == null) {
				respJSON.put("type", "error");
				respJSON.put("reason", toname + " did not request private talk to you");
				return respJSON;
			} else {
				removePrivateReq(toname);
				saveAddress(privateJSON);
			}
		}

		if (reqJSON.getString("type").equals("private")) {
			String toname = reqJSON.getString("toname");
			String addressStr = addressMap.get(toname);
			if (addressStr != null) {
				String[] address = addressStr.split(":");
				String privateIp = address[0];
				int privatePort = Integer.parseInt(address[1]);
				try {
					socket = new Socket(privateIp, privatePort);
					//in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
					out = new PrintStream(socket.getOutputStream());
				} catch (Exception e) {
					respJSON.put("type", "error");
					respJSON.put("reason", "User " + toname + " is Offline");
					reqJSON.put("privateAddress", addressStr);
					try {
						socket = new Socket(servIp, servPort);
						in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
						out = new PrintStream(socket.getOutputStream());
					} catch (Exception eServer) {
						respJSON.put("type", "fatal");
						respJSON.put("reason", "Server may be down");
						return respJSON;
					}
				}
			} else {
				respJSON.put("type", "error");
				respJSON.put("reason", "Does not hold Address for " + reqJSON.getString("toname"));
				return respJSON;
			}
		} else {
			try {
				socket = new Socket(servIp, servPort);
				in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				out = new PrintStream(socket.getOutputStream());
			} catch (Exception e) {
				respJSON.put("type", "fatal");
				respJSON.put("reason", "Server may be down");
				return respJSON;
			}
		}
		try {
			if (out != null)
				out.println(reqJSON.toString());
			
			if (in != null) {
				String respStr = in.readLine();
				respJSON = new JSONObject(respStr);
			} else {
				respJSON.put("type", "success");
			}
		} catch (Exception e) {
			respJSON.put("type", "fatal");
			respJSON.put("reason", "connect timeout");
		}
		return respJSON;
	}

	private void printOnlineList(JSONArray onlineList) throws JSONException{
		for (int i = 0; i < onlineList.length(); i++) {  
	        System.out.println(onlineList.getString(i));   
	    }   
	}

	public void saveAddress(JSONObject addressJSON) {
		try {
			String fromname = addressJSON.getString("fromname");
			String address = addressJSON.getString("address");
			addressMap.put(fromname, address);
		} catch (Exception e) {}
	}

	private void removeAddress(JSONObject respJSON) throws JSONException {
		addressMap.remove(respJSON.getString("toname"));
	}


	/**************************************************************************
	Display different information according to the corresponding response JSON
	**************************************************************************/
	private void displayResp (JSONObject respJSON) throws JSONException{
		String type = respJSON.getString("type");
		if (type.equals("error")) {
			System.out.println("error");
			System.out.println(respJSON.getString("reason"));
		} else if (type.equals("fatal")) {
			System.out.println(respJSON.getString("reason"));
		} else if (type.equals("blockby")) {
			System.out.println("Sorry, you are blocked by user " + respJSON.getString("fromname"));
		} else if (type.equals("onlinelist")) {
			printOnlineList(respJSON.getJSONArray("onlinelist"));
		} else if (type.equals("IpAddress")) {
			saveAddress(respJSON);
		} else if (type.equals("changeIP")) {
			System.out.println(respJSON.getString("toname") + " has changed IP, please try getaddress again");
			removeAddress(respJSON);
		} else if (type.equals("rejectby")) {
			System.out.println("Your message to " + respJSON.getString("fromname") + " is blocked");
		} else if (type.equals("useroffline")) {
			System.out.println("User " + respJSON.getString("fromname") + " is already offline");
		} else if (type.equals("blockto")) {
			System.out.println("User " + respJSON.getString("toname") + " is blocked");
		}
	}

	/**************************************************************************
	Main loop for client side, waiting for input, and calling functions above
	**************************************************************************/
	public void run() {
		if (!login()) {
			clientMain.logout(true, "sender");
			return;
		}
		clientMain.startBeat(username);
		while (!clientMain.exitFlag) {
			System.out.print(">");
			
			JSONObject reqJSON = null;
			JSONObject respJSON = null;
			try {
				//String message = input.readLine();
				String message = System.console().readLine();
				if (clientMain.exitFlag)
					return;
				reqJSON = resolveInput(message);
				if (reqJSON.getString("type").equals("none"))
					continue;
				if (reqJSON.getString("type").equals("input_error")) {
					System.out.println("invalid input");
					continue;
				}
			} catch (Exception e) {}
			if (reqJSON == null) {
				continue;
			} else {
				try {
					respJSON = sendReq(reqJSON);
				} catch (Exception e) {}
				disconnect();
			}
			if (respJSON == null) {
				continue;
			} else {
				try {
					displayResp(respJSON);
					if (respJSON.getString("type").equals("fatal"))
						clientMain.logout(true, "sender");
					else if (respJSON.getString("type").equals("logout"))
						clientMain.logout(false, "sender");
				} catch (Exception e) {}
			}
		}
	}

	/**************************************************************************
	Login function
	**************************************************************************/
	private boolean login() {
		BufferedReader in;
		PrintStream out;
		input = new BufferedReader(new InputStreamReader(System.in));

		String password = "";
		try {
			System.out.print(">username: ");
			username = input.readLine();
			System.out.print(">password: ");
			password = input.readLine();
		} catch (Exception e) {}

		while (true) {
			try {
				JSONObject reqJSON = new JSONObject();
				reqJSON.put("type", "login");
				reqJSON.put("username", username);
				reqJSON.put("password", password);
				reqJSON.put("address", localIP + ":" + listenPort);
				socket = new Socket(servIp, servPort);
				out = new PrintStream(socket.getOutputStream());
				in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				out.println(reqJSON.toString());
				String respStr = in.readLine();
				disconnect();
				JSONObject respJSON = new JSONObject(respStr);
				String type = respJSON.getString("type");
				if (type.equals("success")){
					loggedIn = true;
					break;
				} else if (type.equals("invalid name")) {
					System.out.println(type);
					System.out.print(">username: ");
					username = input.readLine();
					System.out.print(">password: ");
					password = input.readLine();
				} else if (type.equals("invalid password")) {
					System.out.println(type);
					System.out.print(">password: ");
					password = input.readLine();
				} else if (type.equals("serverBlock")) {
					System.out.println("login fail need to wait 60 minutes");
					break;
				} else if (type.equals("multiple failure")) {
					System.out.println("Due to mutiple failure, you are blocked, try sometime later");
					break;
				}
			} catch (Exception e) {
				System.out.println("Server is down");
				break;
			}
		}
		return loggedIn;
	}

	public void disconnect() {
		try {
			if (socket != null)
				socket.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
