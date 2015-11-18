import com.sun.deploy.util.SessionState;
import com.sun.security.ntlm.Client;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Scanner;

/**
 * Created by DucNguyenMinh on 17.11.15.
 */
public class ChatClient
{
    /* Portnummer */
    private final int serverPort;

    /* Hostname */
    private final String hostname;

    private final String clientName;

    private Socket clientSocket;
    private boolean isLoggedOut;
    private DataOutputStream outToServer; // Ausgabestream zum Server
    private BufferedReader inFromServer; // Eingabestream vom Server

    private boolean serviceRequested = true; // ChatClient beenden?

    public ChatClient(String hostname, int serverPort, String userName)
    {
        this.serverPort = serverPort;
        this.hostname = hostname;
        this.clientName = userName;
    }


    public void startJob()
    {

        Scanner inFromUser;
        String chatMsg = "";
        try {
            clientSocket = new Socket(hostname, serverPort);
            System.out.println("Connection established " + clientSocket.getInetAddress() + ":" + clientSocket.getPort());

            inFromServer = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            outToServer = new DataOutputStream(clientSocket.getOutputStream());

            new ListenFromServer().start();

            outToServer.writeBytes(this.clientName);

            isLoggedOut = false;
        } catch (IOException e) {
            System.out.println("Error connecting to server: " + e);
        }

        inFromUser = new Scanner(System.in);

        while (!isLoggedOut) {
            System.out.print("> ");

           /* String vom Benutzer (Konsoleneingabe) holen */
            chatMsg = inFromUser.nextLine();

            if (chatMsg.equalsIgnoreCase("LOGOUT.")) {
                isLoggedOut = true;
                sendToServer("LOGOUT.");
            }
        }

        logOut();
    }

    private void sendToServer(String request)
    {
        try {
            outToServer.writeBytes(request + '\r' + '\n');
        } catch (IOException e) {
            System.out.println("Exception writing to server: " + e);
        }
    }

    private void logOut()
    {
        try {
            if (inFromServer != null)
                inFromServer.close();
            if (outToServer != null) outToServer.close();
            if (clientSocket != null) clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args)
    {
        Scanner inFromUser = new Scanner(System.in);
        String inStr = "";

        /* Default Werte */
        int portNummer = 1234;
        String hostname = "localhost";
        String username = "someone";

        switch (args.length) {
            // > javac Client username portNumber serverAddr
            case 0:
                System.out.print("Username: ");
                inStr = inFromUser.nextLine();
                if (!inStr.equals("")) username = inStr;
                System.out.print("Hostname: ");
                inStr = inFromUser.nextLine();
                if (!inStr.equals("")) hostname = inStr;
                System.out.print("Port number: ");
                inStr = inFromUser.nextLine();
                boolean valid = false;
                while (!valid) {
                    if (!inStr.equals("")) {
                        try {
                            portNummer = Integer.parseInt(inStr);
                            valid = true;
                        } catch (Exception e) {
                            System.out.println("Invalid port number.");
                            System.out.print("Port number: ");
                            inStr = inFromUser.nextLine();
                        }
                    }
                }
                break;
            case 3:
                hostname = args[2];
                // > javac Client username portNumber
            case 2:
                try {
                    portNummer = Integer.parseInt(args[1]);
                } catch (Exception e) {
                    System.out.println("Invalid port number.");
                    System.out.println("Usage is: > java Client [username] [portNumber] [serverAddress]");
                    return;
                }
                // > javac Client username
            case 1:
                username = args[0];
                // > java Client
                break;
                // invalid number of arguments
            default:
                System.out.println("Usage is: > java Client [username] [portNumber] {serverAddress]");
                return;
        }


        ChatClient client = new ChatClient(hostname,portNummer,username);

        client.startJob();
    }


    /**
     *
     */
    class ListenFromServer extends Thread
    {
        @Override
        public void run()
        {
            while (true) {
                try {
                    String message = inFromServer.readLine();
                    System.out.println(message);
                    System.out.print(">");
                } catch (IOException e) {
                    System.out.print("Server has closed connection");
                }
            }
        }

    }
}
