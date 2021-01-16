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
    private static DatagramPacket packet;

    /**
     * Der Constructor dient zur Nutzereingabe des Standorts
     */
    public Client2() {
        Scanner eingabe = new Scanner(System.in);
        System.out.println("Geben Sie bitte den Standort ein!");
        this.name = eingabe.nextLine();
        this.map = new HashMap<String,String>();
        eingabe.close();
    }

    /**
     * Diese Methode aktualisiert die Daten der Station, wenn die Zeitstamp neu wird.
     *
     * @param name der Station
     * @param data der Station
     */
    public void clientAktu(String name, String data) {
        if (map.containsKey(name)) {
            if (Long.parseLong(data.split("&")[2]) > Long.parseLong(map.get(name).split("&")[2])) ;
            { //vergleicht die Zeitstamp
                map.replace(name, data);
            }
        } else {
            map.put(name, data);
        }
    }
    /**
     * Diese Methode erstellt zufaellige neue Daten..
     *
     * @return zufaellige Daten (Temeraturen, Luftfaechtigkeit und aktueller Zeitpunkt)
     */
    public String neueDataErstellen() {
        int temperatur = (int) (Math.random() * (18 - 15 + 1) + 15); //Temperatur in Celsius zufaellig.
        int luft = (int) (Math.random() * 100); // Luftfeuchtigkeit in % zufaellig.
        long zeit = System.currentTimeMillis();
        return temperatur + "&" + luft + "&" + zeit;
    }


    /**
     * Methode um der Standort mit seine Informationen zu erstellen.
     */
    public void clientAufstellen() {
        map.put(name, neueDataErstellen());
    }
    /**
     * diese Methode teilt sowie das Paket in Stationen nach jeder"&" auf, als auch die Daten innerhalb der Station nach jeder "/"
     */
    private void dataSpeichern(String substring) {
        String[] sData = substring.split("%");
        for (String station : sData) {
            String name = station.split("&")[0];
            String data = station.split("&")[1] + "&" + station.split("&")[2] + "&" + station.split("&")[3];
            clientAktu(name, data);
        }
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
        new Thread(this.austauchAusfuehren()).start();
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
            this.clientAktu(this.name,this.neueDataErstellen());
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

        HashMap<String,String> map = new HashMap<String, String>(this.map);
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



    // New Austauch


    public Runnable austauchAusfuehren(){
        clientAufstellen();
        Runnable austauch = new Runnable() {


        private byte[] buffer;


        @Override
        public void run() {
            try {
                while (true) {
                    buffer = new byte[1024];
                    packet = new DatagramPacket(buffer, buffer.length);

                    socket.receive(packet);
                    String dataToString = new String(packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8); // um die Daten von Bytes into Strings umzuwandeln
                    String controlle = dataToString.substring(0, 10);

                    if (dataToString.equals("--REQUEST--")) {
                        String antwort = "--STATUS--";
                        HashMap<String, String> mapcopy = new HashMap<String, String>(map);

                        for (HashMap.Entry<String, String> eingang : mapcopy.entrySet()) {
                            String name = eingang.getKey();
                            String data = eingang.getValue();
                            antwort = antwort + name + "&" + data + "%";
                        }
                        byte[] dataAntwort = antwort.getBytes(StandardCharsets.UTF_8);
                        packet.setData(dataAntwort);
                        packet.setLength(dataAntwort.length);
                        socket.send(packet);

                    } else if (controlle.equals("--STATUS--")) {
                        dataSpeichern(dataToString.substring(10));

                    }
                }
            } catch (IOException e) {
                System.out.println("IO Fehler beim Packet");
            }
        }
    };
    return austauch;
    }
}


