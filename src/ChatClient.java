import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Scanner;

/**
 * Client über den sich ein Nutzer bei Angabe von PortNr. & Host bei einem Chat-Client anmelden kann 
 * 
 * Created by DucNguyenMinh on 17.11.15.
 */
public class ChatClient
{
    /* Portnummer */
    private final int        serverPort;
                             
    /* Hostname */
    private final String     hostname;
    /* Name des Clients für Serveridentifizierung */
    private final String     clientName;
    /* Socket für Kommunikation mit ChatServer */
    private Socket           clientSocket;
    /* Flag, falls Nutzer sind ausloggen möchste */
    private boolean          isLoggedOut;
    /* Ausgabestream zum Server */
    private DataOutputStream outToServer;
    /* Eingabestream vom Server */
    private BufferedReader   inFromServer;
    
                                              
    /**
     * 
     * @param hostname
     *            Hostname des ChatServers
     * @param serverPort
     *            Port, auf dem Server lauscht
     * @param userName
     *            Angezeigte Name des Nutzers im Chat
     */
    public ChatClient(String hostname, int serverPort, String userName)
    {
        this.serverPort = serverPort;
        this.hostname = hostname;
        this.clientName = userName;
    }
    
    /**
     * Startet den Client inklusive Handshake
     */
    public void startJob()
    {
        
        Scanner inFromUser;
        String chatMsg = "";
        try
        {
            clientSocket = new Socket(hostname, serverPort);
            System.out.println(
                    "Connection established " + clientSocket.getInetAddress()
                            + ":" + clientSocket.getPort());
                            
            this.inFromServer = new BufferedReader(
                    new InputStreamReader(clientSocket.getInputStream()));
            this.outToServer = new DataOutputStream(
                    clientSocket.getOutputStream());
                    
            new ListenFromServer().start();
            
            sendToServer(clientName);
            
            isLoggedOut = false;
        }
        catch (IOException e)
        {
            System.out.println("Error connecting to server: " + e);
        }
        
        inFromUser = new Scanner(System.in);
        
        /* Schleife die Nutzereingaben in Stdin an den Server schickt */
        while (!isLoggedOut)
        {
            System.out.print("> ");
            
            /* String vom Benutzer (Konsoleneingabe) holen */
            chatMsg = inFromUser.nextLine();
            
            if (chatMsg.equalsIgnoreCase("LOGOUT."))
            {
                isLoggedOut = true;
                // logOut();
            }
            
            sendToServer(chatMsg);
            
        }
        inFromUser.close();
        logOut();
    }
    
    /**
     * Schiebt die übergebene Nachricht mit dem Socket zum Server
     * 
     * @param request
     */
    private void sendToServer(String request)
    {
        try
        {
            outToServer.writeBytes(request + '\r' + '\n');
        }
        catch (IOException e)
        {
            System.out.println("Exception writing to server: " + e);
        }
    }
    
    /**
     * Schließt die Input/Output-Streams sowie das Socket selbst
     */
    private void logOut()
    {
        try
        {
            if (inFromServer != null)
                inFromServer.close();
            if (outToServer != null)
                outToServer.close();
            if (clientSocket != null)
                clientSocket.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
    /**
     * Main-Methode. Standardmäßig werden Port, Host & Username als args-Parameter übergeben
     * @param args
     */
    public static void main(String[] args)
    {
        Scanner inFromUser = new Scanner(System.in);
        
        /* Default Werte */
        int portNummer = 56789;
        String hostname = "localhost";
        String username = "someone";

            switch (args.length)
            {
            // > javac Client username portNumber serverAddr
            case 3:
                hostname = args[2];
                // > javac Client username portNumber
            case 2:
                try
                {
                    portNummer = Integer.parseInt(args[1]);
                }
                catch (Exception e)
                {
                    System.out.println("Invalid port number.");
                    System.out.println(
                            "Usage is: > java Client [username] [portNumber] [serverAddress]");
                    return;
                }
                // > javac Client username
            case 1:
                username = args[0];
                // > java Client
                break;
            // invalid number of arguments
            // case 0:
            //
            // break;
            default:
                System.out.println(
                        "Usage is: > java Client [username] [portNumber] {serverAddress]");
                return;
            }

        ChatClient client = new ChatClient(hostname, portNummer, username);
        
        client.startJob();
        inFromUser.close();
        
    }

    /**
     * Thread-Klasse, die das Lesen vom Chatserver seperat übernimmt
     */
    class ListenFromServer extends Thread
    {
        @Override
        public void run()
        {
            while (!isLoggedOut)
            {
                try
                {
                    String message = inFromServer.readLine();
                    if (message != null)
                    {
                        System.out.println(message);
                        System.out.print("> ");
                    }
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }

    }
}
