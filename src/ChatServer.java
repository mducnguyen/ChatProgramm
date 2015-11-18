import sun.misc.Cleaner;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.Semaphore;

/**
 * Created by DucNguyenMinh on 18.11.15.
 */
public class ChatServer
{
    /* TCP-Server, der Verbindungsanfragen entgegennimmt */

    /* Semaphore begrenzt die Anzahl parallel laufender Worker-Threads  */
    public Semaphore workerThreadsSem;

    /* Portnummer */
    public final int serverPort;

    /* Anzeige, ob der Server-Dienst weiterhin benoetigt wird */
    public boolean serviceRequested = true;

    private ArrayList<ClientThread> clients;

    private SimpleDateFormat dateFormat;

    /* Konstruktor mit Parametern: Server-Port, Maximale Anzahl paralleler Worker-Threads*/
    public ChatServer(int serverPort) {
        this.serverPort = serverPort;
//        this.workerThreadsSem = new Semaphore(maxThreads);
        this.clients = new ArrayList<>();
        this.dateFormat = new SimpleDateFormat("HH:mm:ss");
    }

    /**/
    public synchronized void broadcast(String message){
        String time = dateFormat.format(new Date());
        String msg = time + " " + message + "\n";

        System.out.print(msg);

        for(ClientThread client : clients ){
            client.writeToClient(msg);
        }
    }
    public synchronized void addClient(ClientThread clientThread){
        clients.add(clientThread);
    }

    public synchronized void removeClient(ClientThread clientThread) {
        clients.remove(clientThread);
    }

    public void startServer() {
        serviceRequested = true;
        ServerSocket welcomeSocket; // TCP-Server-Socketklasse
        Socket clientSocket; // TCP-Standard-Socketklasse

        int nextThreadNumber = 0;

        try {
         /* Server-Socket erzeugen */
            welcomeSocket = new ServerSocket(serverPort);

            while (serviceRequested) {
//                workerThreadsSem.acquire();  // Blockieren, wenn max. Anzahl Worker-Threads erreicht

                System.out.println("TCP Server is waiting for connection - listening TCP port " + serverPort);
            /*
             * Blockiert auf Verbindungsanfrage warten --> nach Verbindungsaufbau
             * Standard-Socket erzeugen und an connectionSocket zuweisen
             */
                clientSocket = welcomeSocket.accept();

            /* Neuen Arbeits-Thread erzeugen und die Nummer, den Socket sowie das Serverobjekt uebergeben */
                ClientThread clientThread = new ClientThread(clientSocket);
                addClient(clientThread);
                clientThread.start();
            }

            try {
                welcomeSocket.close();
                for(ClientThread clientThread : clients){
                    clientThread.closeSocket();
                }
            }catch (Exception e){

            }
        } catch (Exception e) {
            System.err.println(e.toString());
        }



    }


    public static void main(String[] args) {
      /* Erzeuge Server und starte ihn */
        ChatServer myServer = new ChatServer(56789);
        myServer.startServer();
    }


    class ClientThread extends Thread {

        private Socket clientSocket;

        private BufferedReader inFromClient;

        private DataOutputStream outToClient;

        String username;

        String date;


        public ClientThread(Socket clientSk)
        {
            this.clientSocket = clientSk;

            try {
                this.inFromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                this.outToClient = new DataOutputStream(clientSk.getOutputStream());
                this.username = inFromClient.readLine();
                System.out.println(username + "has just joined chatroom.");


            } catch (IOException e) {
                System.out.println("Exception creating new Input/output Streams: " + e);
                return;
            }

            date = new Date().toString() + "\n";
        }

        @Override
        public void run(){
            broadcast(username + "has just joined chatroom.");
            boolean isLogoutRequested = false;
            String chatMessage = "" ;

            while(!isLogoutRequested){
                try {
                    chatMessage = inFromClient.readLine();
                }
                catch (IOException e) {
                    System.out.println(username + " Exception reading Streams: " + e);
                    break;
                }

                if(chatMessage.equalsIgnoreCase("LOGOUT.")){
                    String msg = username + " has left the room with LOGOUT. message.";
                    System.out.println(msg);
                    broadcast(msg);
                    isLogoutRequested = true;
                }else{
                    broadcast(username + ": " + chatMessage);
                }
            }

            removeClient(this);
            closeSocket();
        }

        public void writeToClient(String message){
            try{
                this.outToClient.writeBytes(message);
            }catch(Exception e) {

            }
        }

        public void closeSocket()
        {
            try {
                if (inFromClient != null)
                    inFromClient.close();
                if (outToClient != null) outToClient.close();
                if (clientSocket != null) clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
