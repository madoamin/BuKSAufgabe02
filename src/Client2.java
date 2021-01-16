import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class Client2 {
    private int[] ports = {50001, 50002, 50003, 50004, 50005, 50006, 50007, 50008, 50009, 50010};
    private String name;
    private InetAddress adresse;
    private DatagramSocket socket;
    private int port;
    public static Map<String, String> map;

    /**
     * Der Constructor dient zur Nutzereingabe des Standorts
     */
    public Client2() {
        Scanner eingabe = new Scanner(System.in);
        System.out.println("Geben Sie bitte den Standort ein!");
        this.name = eingabe.nextLine();
        eingabe.close();
    }

    /**
     * diese Methode sucht einen freien Port und oeffnet ein datagramsocket auf lokalhost
     *
     * @throws SocketException
     * @throws UnknownHostException
     */
    public void portSuchen() {
        for (int s : ports) {
            try {
                adresse = InetAddress.getByName("localhost");
                socket = new DatagramSocket(s);
                this.port = s;
                break;
            }catch (SocketException e){
                if (s == 50010){
                    System.out.println("Keine freie Ports verfuegbar!!");
                    System.exit(1);
                }
                continue;
            }catch (UnknownHostException a){
                System.out.println("Host wurde nicht gefunden!!");
                System.exit(1);
            }
        }
    }

    public void clientStarten() {
        portSuchen();
        new Thread(new Austauch(name,socket)).start();
        try {


        while (true) {
            for (int s : ports) {
                if (s != this.port) {
                    try {
                    aktuellAusgeben();
                    String testen = "--REQUEST--";
                    byte[] data = testen.getBytes(StandardCharsets.UTF_8);
                    DatagramPacket paket = new DatagramPacket(data, data.length, adresse, s);
                    socket.send(paket);
                    } catch (IOException e) {
                        try {
                            TimeUnit.MILLISECONDS.sleep(100);
                            continue;
                        }catch (InterruptedException ee){
                            Thread.currentThread().interrupt();
                        }
                        continue;
                    }
                }
        }
            Austauch.clientAktu(this.name,Austauch.neueDataErstellen());
            try {
                TimeUnit.MILLISECONDS.sleep(1000);
            }catch (InterruptedException ee){
                Thread.currentThread().interrupt();
            }
            }
    }catch (Exception e){
            System.out.println("Unbekanter Fehler getroffen!!");
            System.exit(1);
        }
    }

    /**
     * Diese Methode gibt die Daten auf die Console aus.
     */
    private void aktuellAusgeben() {
        System.out.println("--------------------------Port-------------------------------");
        System.out.println("Die Station: " + name + " ist am Port " + this.port);
        System.out.println("------------------- --Stationsdaten---------------------------");

        HashMap<String,String> map = new HashMap<String, String>(Austauch.map);
        for (Map.Entry<String,String> e : map.entrySet()){
            String name = e.getKey();
            String data = e.getValue();
            String[] vs = data.split("&");
            System.out.println("Stationsname: " + name);
            System.out.println("Temperatur: " + vs[0] + " C");
            System.out.println("Luftfaeuchtigkeit: " + vs[1] + " %");
            System.out.println("Zeitstamp: " + new Timestamp(Long.parseLong(vs[2])));
            System.out.println("----------------------------------------------------------");
        }
    }

    public static void main(String[] args) {
        Client2 c = new Client2();
        c.clientStarten();
    }
}


