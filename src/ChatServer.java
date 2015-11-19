import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.concurrent.Semaphore;

/**
 * Chat-Server der Chat-Raum mit mehreren Nutzern zur Verfügung stellt.
 * Verwaltet Clients (durch hinzufügen & entfernen in eine Liste)
 * Stellt Chat-Verlauf zur Verfügung
 * Verteilt Nachrichten von einem Client an alle anderen angemeldeten Clients
 *
 * Created by DucNguyenMinh on 18.11.15.
 */
public class ChatServer
{
    /* TCP-Server, der Verbindungsanfragen entgegennimmt */
    
    /* Semaphore begrenzt die Anzahl parallel laufender Worker-Threads */
//    public Semaphore                workerThreadsSem;
    /* Portnummer */
    public final int                serverPort;
                                    
    /* Anzeige, ob der Server-Dienst weiterhin benoetigt wird */
    public boolean                  serviceRequested = true;
                                                     
    public LinkedList<String>       chatHistory;
    private ArrayList<ClientThread> clients;
                                    
    private SimpleDateFormat        dateFormat;
                                    
    /**
     * @param serverPort Port auf dem ChatServer lauscht
     */
    public ChatServer(int serverPort)
    {
        this.serverPort = serverPort;
//        this.workerThreadsSem = new Semaphore(2);
        this.chatHistory = new LinkedList<String>();
        this.clients = new ArrayList<>();
        this.dateFormat = new SimpleDateFormat("HH:mm:ss");
    }

    /**
     * Leitet die Nachricht an alle Clients, die sich im Chatraum befinden weiter.
     * @param message zu verteilende Nachricht
     */

    public synchronized void broadcast(String message)
    {
        String time = dateFormat.format(new Date());
        String msg = time + " " + message + "\n";
        
        chatHistory.add(message);
        System.out.print(msg);
        
        for (ClientThread client : clients)
        {
            client.writeToClient(msg);
        }
     }

    /**
     * Sendet den Chatverlauf ausschließlich an den ClientThread, der den spezifizierten "HISTORY."-Befehl aufgerufen hat
     * @param clientthread ClientThread der Chatverlauf angefordert hat
     */
    private synchronized void printChatHistory(ClientThread clientthread)
    {
        for (String s : chatHistory)
            clientthread.writeToClient(s + "\r\n");
    }
    
    private synchronized void addClient(ClientThread clientThread)
    {
        clients.add(clientThread);
    }
    
    private synchronized void removeClient(ClientThread clientThread)
    {
//        workerThreadsSem.release();
        clients.remove(clientThread);
    }
    
    public void startServer()
    {
        serviceRequested = true;
        ServerSocket welcomeSocket; // TCP-Server-Socketklasse
        Socket clientSocket; // TCP-Standard-Socketklasse
        
        int nextThreadNumber = 0;
        
        try
        {
            /* Server-Socket erzeugen */
            welcomeSocket = new ServerSocket(serverPort);
            
            while (serviceRequested)
            {
                // workerThreadsSem.acquire(); // Blockieren, wenn max. Anzahl
                // Worker-Threads erreicht
                
                System.out.println(
                        "Chat-Server is waiting for connection - listening TCP port "
                                + serverPort);
                /*
                 * Blockiert auf Verbindungsanfrage warten --> nach
                 * Verbindungsaufbau Standard-Socket erzeugen und an
                 * connectionSocket zuweisen
                 */
//                workerThreadsSem.acquire();
                clientSocket = welcomeSocket.accept();
                /*
                 * Neuen Arbeits-Thread erzeugen und die Nummer, den Socket
                 * sowie das Serverobjekt uebergeben
                 */
                ClientThread clientThread = new ClientThread(clientSocket);
                addClient(clientThread);
                clientThread.start();
            }
            
            try
            {
                welcomeSocket.close();
                for (ClientThread clientThread : clients)
                {
                    clientThread.closeSocket();
                }
            }
            catch (Exception e)
            {
            
            }
        }
        catch (Exception e)
        {
            System.err.println(e.toString());
        }
        
    }
    
    public static void main(String[] args)
    {
        /* Erzeuge Server und starte ihn */
        ChatServer myServer = new ChatServer(56789);
        myServer.startServer();
    }

    /**
     * Für jeden  angemeldeten Client wird ein seperater ClientThrad erstellt.
     * Der ClientThread verwaltet das dem Client zugeordneten Socket.
     * Reagiert auf spezielle Clientbefehle ("LOGOUT.", "HISTORY.")
     */
    class ClientThread extends Thread
    {
        
        private Socket           clientSocket;
                                 
        private BufferedReader   inFromClient;
                                 
        private DataOutputStream outToClient;
                                 
        String                   username;
                                 
        String                   date;
                                 
        public ClientThread(Socket clientSk)
        {
            this.clientSocket = clientSk;
            
            try
            {
                inFromClient = new BufferedReader(
                        new InputStreamReader(clientSocket.getInputStream()));
                outToClient = new DataOutputStream(clientSk.getOutputStream());
                username = inFromClient.readLine();
                System.out.println(username + " has just joined the chatroom.");
                
            }
            catch (IOException e)
            {
                System.out.println(
                        "Exception creating new Input/output Streams: " + e);
                return;
            }
            
            date = new Date().toString() + "\n";
        }

        /**
         * Fragt solange der ClientThread aktiv bleiben soll neue Nachrichten vom Nutzer ab.
         * Interpretiert und reagiert auf spezielle Befehle.
         *
         */
        @Override
        public void run()
        {
            broadcast(username + " has just joined chatroom.");
            boolean isLogoutRequested = false;
            String chatMessage = "";
            
            while (!isLogoutRequested)
            {
                try
                {
                    chatMessage = inFromClient.readLine();
                }
                catch (IOException e)
                {
                    System.out.println(
                            username + " Exception reading Streams: " + e);
                    break;
                }
                
                if (chatMessage.equalsIgnoreCase("LOGOUT."))
                {
                    String msg = username + " has left the room.";
                    broadcast(msg);
                    isLogoutRequested = true;
                }
                else if (chatMessage.equals("HISTORY."))
                {
                    printChatHistory(this);
                }
                else
                {
                    broadcast(username + ": " + chatMessage);
                }
            }

            closeSocket();
            removeClient(this);
        }
        
        /**
         * Sendet Nachricht an Client
         * 
         * @param message
         */
        private void writeToClient(String message)
        {
            try
            {
                outToClient.writeBytes(message);
            }
            catch (Exception e)
            {
                e.getMessage();
            }
        }
        
        /**
         * Schließt alle Input/Output-Streams und das Socket
         */
        private void closeSocket()
        {
            try
            {
                if (inFromClient != null)
                    inFromClient.close();
                if (outToClient != null)
                    outToClient.close();
                if (clientSocket != null)
                    clientSocket.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }
}
