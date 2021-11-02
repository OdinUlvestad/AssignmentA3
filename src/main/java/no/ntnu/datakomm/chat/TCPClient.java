package no.ntnu.datakomm.chat;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

public class TCPClient {
    private PrintWriter toServer;
    private BufferedReader fromServer;
    private Socket connection;

    // Hint: if you want to store a message for the last error, store it here
    private String lastError = null;

    private final List<ChatListener> listeners = new LinkedList<>();

    /**
     * Connect to a chat server.
     *
     * @param host host name or IP address of the chat server
     * @param port TCP port of the chat server
     * @return True on success, false otherwise
     */
    public boolean connect(String host, int port) {
        // TODO Step 1: implement this method
        // Hint: Remember to process all exceptions and return false on error
        // Hint: Remember to set up all the necessary input/output stream variables
        boolean connectionFlag = false; // Check if the connection was successful
        System.out.println("Attempting client boot up...");

        try { // Try to establish a connection to the server
            this.connection = new Socket(host, port); // The "three way handshake"
            toServer = new PrintWriter(this.connection.getOutputStream(),true); // sends to the server
            fromServer = new BufferedReader(new InputStreamReader(connection.getInputStream())); // reads from the server
            // update the status of the connection
            connectionFlag = true;
            System.out.println("Connection status = " + connectionFlag);

        } catch (IOException e) { // Throws an exception if code ran unsuccessfully
            System.out.println("Connection to socket failed, reason: " + e.getMessage());
        }
        // returns true if connection was a success, otherwise false.
        return connectionFlag;
    }

    /**
     * Close the socket. This method must be synchronized, because several
     * threads may try to call it. For example: When "Disconnect" button is
     * pressed in the GUI thread, the connection will get closed. Meanwhile, the
     * background thread trying to read server's response will get error in the
     * input stream and may try to call this method when the socket is already
     * in the process of being closed. with "synchronized" keyword we make sure
     * that no two threads call this method in parallel.
     */
    public synchronized void disconnect() {
        // TODO Step 4: implement this method
        // Hint: remember to check if connection is active
        if (this.connection == null) { // Check if the socket is already closed
            System.out.println("Socket is already closed...");

        } else if (isConnectionActive()) {
            try { // If the socket connection is open, close it.
                this.connection.close();
                this.toServer.close();
                this.fromServer.close();
                this.connection = null;
                // Update the server side that the connection is closed.
                onDisconnect();
            } catch (IOException e) {
                System.out.println("Error message: " + e.getMessage());
            }
        }
    }

    /**
     * @return true if the connection is active (opened), false if not.
     */
    public boolean isConnectionActive() {
        return this.connection != null;
    }

    /**
     * Send a command to server.
     *
     * @param cmd A command. It should include the command word and optional attributes, according to the protocol.
     * @return true on success, false otherwise
     */
    private boolean sendCommand(String cmd) {
        // TODO Step 2: Implement this method
        // Hint: Remember to check if connection is active
        boolean cmdSuccess = false; // Check if command was successful

        if (isConnectionActive()) { // check connection to the socket
            try { // send command to the server
                // Accepted commands/protocols are:
                // login, msg, privmsg, supported, users.
                this.toServer.println(cmd);
                System.out.println("Command to send: " + cmd);
                cmdSuccess = true;

            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
            // returns true, if the command was sent successfully.
        } return cmdSuccess;
    }

    /**
     * Send a public message to all the recipients.
     *
     * @param message Message to send
     * @return true if message sent, false on error
     */
    public boolean sendPublicMessage(String message) {
        // TODO Step 2: implement this method
        // Hint: Reuse sendCommand() method
        // Hint: update lastError if you want to store the reason for the error.
        /** You can choose to write your method like this or simply
         * just do all the necessary operations in one sentence.
         *
         * You'll see I've used both ways to write the code.
         *
         * boolean sentMsg = false; // Check to see if successful
        if (isConnectionActive()) { // check connection
            try { // Send request to the server
                sendCommand("msg " + message); // send protocol and message to server
                sentMsg = true; // set check to success (true)
            } catch (Exception e) {
                System.out.println("Error message: " + e.getMessage());
            }
        } // returns true if successful
        return sentMsg; */
        return sendCommand("msg " + message);
    }

    /**
     * Send a login request to the chat server.
     *
     * @param username Username to use
     */
    public void tryLogin(String username) {
        // TODO Step 3: implement this method
        // Hint: Reuse sendCommand() method
        if (isConnectionActive()) {
            try {  // sending the login protocol.
                sendCommand("login " + username);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
    }

    /**
     * Send a request for latest user list to the server. To get the new users,
     * clear your current user list and use events in the listener.
     */
    public void refreshUserList() {
        // TODO Step 5: implement this method
        // Hint: Use Wireshark and the provided chat client reference app to find out what commands the
        // client and server exchange for user listing.
        if (isConnectionActive()) {
            try { // sending the users protocol
                sendCommand("users");
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }

    }

    /**
     * Send a private message to a single recipient.
     *
     * @param recipient username of the chat user who should receive the message
     * @param message   Message to send
     * @return true if message sent, false on error
     */
    public boolean sendPrivateMessage(String recipient, String message) {
        // TODO Step 6: Implement this method
        // Hint: Reuse sendCommand() method
        // Hint: update lastError if you want to store the reason for the error.
        boolean sentMessage = false;
        if (isConnectionActive()) { // Sending the private message protocol.
            sendCommand("privmsg " + recipient + " " + message);
            sentMessage = true;
        } // returns true if sent correctly
        return sentMessage;

        /** You can also choose to write the method like this
         *
         * return sendCommand("privmsg " + recipient + " " + message); */
    }


    /**
     * Send a request for the list of commands that server supports.
     */
    public void askSupportedCommands() {
        // TODO Step 8: Implement this method
        // Hint: Reuse sendCommand() method
        if (isConnectionActive()) {
            // Sending the help protocol
            sendCommand("help");
        }
    }


    /**
     * Wait for chat server's response
     *
     * @return one line of text (one command) received from the server
     */
    private String waitServerResponse() {
        // TODO Step 3: Implement this method
        // TODO Step 4: If you get I/O Exception or null from the stream, it means that something has gone wrong
        // with the stream and hence the socket. Probably a good idea to close the socket in that case.
        String response = "";
        if (isConnectionActive()) {
            try { // reads the response from the server
                response = this.fromServer.readLine();
            // catches an exception if unable to read
            } catch (Exception e) {
                System.out.println(e.getMessage());
                disconnect();
                onDisconnect();
            }
        }
        System.out.println("Server: " + response);
        return response;
    }

    /**
     * Get the last error message
     *
     * @return Error message or "" if there has been no error
     */
    public String getLastError() {
        if (lastError != null) {
            return lastError;
        } else {
            return "";
        }
    }

    /**
     * Start listening for incoming commands from the server in a new CPU thread.
     */
    public void startListenThread() {
        // Call parseIncomingCommands() in the new thread.
        Thread t = new Thread(() -> {
            parseIncomingCommands();
        });
        t.start();
    }

    /**
     * Read incoming messages one by one, generate events for the listeners. A loop that runs until
     * the connection is closed.
     */
    private void parseIncomingCommands() {
        // TODO Step 3: Implement this method
        // Hint: Reuse waitServerResponse() method
        // Hint: Have a switch-case (or other way) to check what type of response is received from the server
        // and act on it.
        // Hint: In Step 3 you need to handle only login-related responses.
        // Hint: In Step 3 reuse onLoginResult() method
        // Checks the login messages
        // TODO Step 5: update this method, handle user-list response from the server
        // TODO Step 7: add support for incoming chat messages from other users (types: msg, privmsg)
        // TODO Step 7: add support for incoming message errors (type: msgerr)
        // TODO Step 7: add support for incoming command errors (type: cmderr)
        // TODO Step 8: add support for incoming supported command list (type: supported)

        while (isConnectionActive()) {
            // Variable to store the response from the server
            String serverResponse = waitServerResponse();
            // list of strings split into "protocol, user, text-message"
            String[] splitServerResponse = serverResponse.split(" ", 3);
            // list of strings split to keep track of users
            String[] splitSingleServerResponse = serverResponse.split(" ");

            // Switch case that uses command-words to execute different protocols,
            // reads the first string from the server as an identifier.
            switch (splitServerResponse[0]) {
                case "loginok": // Successful login! aka the user was logged in.
                    onLoginResult(true, "Login was a success!");
                    System.out.println("Logged in correctly");
                    break;

                case "loginerr": // Error on login.. aka the user did not log in.
                    onLoginResult(false, "Login failed, reason: " + lastError);
                    System.out.println("login failed, reason: " + lastError);
                    break;

                case "users": // Prints the list of users!
                    onUsersList(splitSingleServerResponse);
                    System.out.println("List updated and received!");
                    break;

                case "msg": // Receives public messages from other users
                    onMsgReceived(false, splitServerResponse[1], splitServerResponse[2]);
                    break;

                case "privmsg": // Receives private message from a single user
                    onMsgReceived(true, splitServerResponse[1], splitServerResponse[2]);
                    break;

                case "msgerr": // Error when receiving or sending a massage
                    onMsgError(serverResponse);
                    System.out.println("A public message wasn't sent or received...");
                    break;

                case "cmderr": // Error when trying to use an unknown command
                    onCmdError(serverResponse);
                    System.out.println("Command not recognized...");
                    break;

                case "supported": // Displays a list over all command words
                    onSupported(splitSingleServerResponse);
                    System.out.println("The help command was printed out successfully!");
                    break;
            }
        }
    }

    /**
     * Register a new listener for events (login result, incoming message, etc)
     *
     * @param listener
     */
    public void addListener(ChatListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    /**
     * Unregister an event listener
     *
     * @param listener
     */
    public void removeListener(ChatListener listener) {
        listeners.remove(listener);
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////////////
    // The following methods are all event-notificators - notify all the listeners about a specific event.
    // By "event" here we mean "information received from the chat server".
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Notify listeners that login operation is complete (either with success or
     * failure)
     *
     * @param success When true, login successful. When false, it failed
     * @param errMsg  Error message if any
     */
    private void onLoginResult(boolean success, String errMsg) {
        for (ChatListener l : listeners) {
            l.onLoginResult(success, errMsg);
        }
    }

    /**
     * Notify listeners that socket was closed by the remote end (server or
     * Internet error)
     */
    private void onDisconnect() {
        // TODO Step 4: Implement this method
        // Hint: all the onXXX() methods will be similar to onLoginResult()
        for (ChatListener l : listeners) {
            l.onDisconnect();
        }
    }

    /**
     * Notify listeners that server sent us a list of currently connected users
     *
     * @param users List with usernames
     */
    private void onUsersList(String[] users) {
        // TODO Step 5: Implement this method
        for (ChatListener l : listeners) {
            l.onUserList(users);
        }
    }

    /**
     * Notify listeners that a message is received from the server
     *
     * @param priv   When true, this is a private message
     * @param sender Username of the sender
     * @param text   Message text
     */
    private void onMsgReceived(boolean priv, String sender, String text) {
        // TODO Step 7: Implement this method
        TextMessage receivedMessage = new TextMessage(sender, priv, text );
        for (ChatListener l : listeners) {
            l.onMessageReceived(receivedMessage);
        }
    }

    /**
     * Notify listeners that our message was not delivered
     *
     * @param errMsg Error description returned by the server
     */
    private void onMsgError(String errMsg) {
        // TODO Step 7: Implement this method
        for (ChatListener l : listeners){
            l.onMessageError(errMsg);
        }
    }

    /**
     * Notify listeners that command was not understood by the server.
     *
     * @param errMsg Error message
     */
    private void onCmdError(String errMsg) {
        // TODO Step 7: Implement this method
        for (ChatListener l : listeners) {
            l.onCommandError(errMsg);
        }
    }

    /**
     * Notify listeners that a help response (supported commands) was received
     * from the server
     *
     * @param commands Commands supported by the server
     */
    private void onSupported(String[] commands) {
        // TODO Step 8: Implement this method
            for (ChatListener l : listeners) {
            l.onSupportedCommands(commands);
        }
    }
}
