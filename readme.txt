1. Installation
	<server_IP> <server_Port> are optional

addressMap - Hash Table <username, address> used to store all the addresses that this client can private to.
privateMap - Hash Table <username, private requests> stored all the private requests from other client that has not been handled.

3.2 Server Side
3.2.1 client room (ClientManager.java)
	BlackList - Hash Set <username> stored all the user names of users who has been blocked
	mailBox - queue <reqType> stored all the offline requests.
3.2.2 Client account (ClientObj.java)
	ClientTable stores all valid <username, password>

	OnlineTable stores all online <username, ClientTalk> where ClientTalk is
				the runnable Object that deals with corresponding client

	LoginTable stores all <user, ClientTalk> who is on the process of login.
		Note: if a client is not logged in within 60 sec, the thread will be terminated.

	BlockTable stores all <user, block_time_stamp> users will be put into this
		table, if the entered invalid password for 3 times.
	for (ClientTalk.java)
	queue is used to store and get message requests

3.3 classes Summary
ServerSide:
ClientManager.java
	This is the class dealing with interaction among clients as well as job
	distribution. It distributed all messages received by the main server to 
	corresponding client thread. And whenever a client needs information from
	other clients or need to communicate to other client, it should talk to thisclass first.
	This is the class used to manage account for each client including offline mail box
	and black list.
	This class deals with communication to each specific client.
	private variable "queue" is the message queue for this specific client.
	Every time ClientManager Class tries to distribute a message to this
	specific client, it need to insert message to this queue.
	This queue works in a producer and consumer pattern where ClientManager
	is the message producer while this specific client is the consumer.
	(After finishing the request, it will close the current socket)
	encapsulate all the requests containing a JSON containing information and socket to be responsed
	This thread scans every second to check the Block List, the Online List,
	as well as the login List
WelcomServer.java
	clients. Once it receive a message, it will simply deliver it for the 
	corresponding client thread to deal with.

Client Side:
clientHeartBeat.java
	Class to sending LIVE message to keep online
	Main class for client
	Waiting for user input and send it to other client or server.
	(every time it receives a message it will close the socket)
	Waiting for messages from server or other clients to display.
	(every time it sends a message it will close the socket)

3.4 Dependency
org.json.jar

4 Commands
to start with
“make run <server_Port>” in Server folder to run Server.
“make run <server_IP> <server_Port>” in client folder to run client
Now focus on terminal for client:
4.1 Login
	>username: columbia
	>password: 116bway
	>
At this moment, if there are other users online, that user will see:
	> columbia is online

	>username: columbia
	>password: 1
	invalid password
	>password: 2
	invalid password
	>password: 3
	login fail need to wait 60 minutes
(This means login fails)
if this client tries to log in immediately
	>username: columbia
	>password: 116bway
	Due to mutiple failure, you are blocked, try sometime later

if google is already online, and another user login using the same username
	>Your account is online at another place, press ENTER to exit
	Exiting...
	>Press ENTER to exit
client will exit after you type the “ENTER”

4.2 message/broadcast
In terminal of columbia:
	>message google yes baby!
	>broadcast yes baby
In terminal of google:
	>columbia: yes baby!
	>columbia(broadcast):yes baby
In other terminals:
	>columbia(broadcast):yes baby

If terminal google is offline:
In terminal of columbia:
	>message google are you online?
	User google is already offline
when google logged in:
	>username: google
	>password: hasglasses
	>columbia: are you online?

4.3 online
if google,columbia,foobar are online
	>online
	google
	columbia
	foobar

4.4 block/unblock
in columbia’s terminal
	>block
	error
	You cannot block yourself
	>block google
	User google is blocked
in google’s terminal
	>You are blocked by columbia
	>message columbia are you online?
	Your message to columbia is blocked
	>getaddress columbia
	Sorry, you are blocked by user columbia
(If google broadcast or login or logout, columbia will not be notified)

in columbia’s terminal
	>unblock google
	>You are unblocked by columbia
	>message columbia yes!

4.5 logout
in columbia’s terminal
	>logout
	Exiting...
	>
int googles’ terminal
	>columbia is offline

The following is the P2P function with 2 Bonus features
4.6 getaddress
This command is implemented with privacy and consent feature
To implement P2P privacy and consent feature I add two more commands:
	privateRequests      // to list all the private requests from other client
 	accept <username>    // to accept a private request from other client
in columbia’s terminal
	>getaddress google
in google’s terminal
	>columbia wants to talk to you privately
	>privateRequests    // This command just shows unhanded private requests
	columbia            // So this command is optional, we can skip this command
	>accept columbia
Then in columbia’s terminal
	>Your private invitation to google is accepted
If columbia is blocked by google
	>You are blocked by google
	>getaddress google
	Sorry, you are blocked by user google

4.7 private
This command is implemented with Guaranteed Message Delivery feature
in columbia’s terminal
	>private google where are you ?
in google’s terminal
	>columbia(private): where are you ?

But if google disconnects abruptly
in columbia’s terminal
	>private google are you connected?
	>google is offline     // At this moment server is still waiting for time out for google, and tries
			       // to forward private message to google, and find it disconnected. So the server
			       // will set google offline, and other client terminals will be notified
if google logout
	>private google are you online?
	User google is already offline
google will see the private message when he is online again.
But if google is online again, its IP is already changed, columbia needs to resend the “getaddress” request to google
in columbia’s terminal
	>private google is online?
	google has changed IP, please try getaddress again

5. Bonus Part Summary
Implementation of P2P privacy and consent feature I add two more commands
Implementation of Guaranteed Message Delivery feature
