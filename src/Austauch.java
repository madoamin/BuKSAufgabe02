import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

/**
 * Diese Klasse ist fuer den Austauch der Packeten verantwortlich
 */

public class Austauch implements Runnable {
    private byte[] buffer;
    private DatagramSocket socket;
    private DatagramPacket packet;
    public static HashMap<String,String> map;
    private String name;

    /**
     * Konstraktor fuer die Klasse Austauch ,um die Socket, der Standort und sein Data zu initialisieren.
     * @param socket
     * @param name name des Standorts.
     */
    public Austauch(String name,DatagramSocket socket) {
        this.socket = socket;
        this.name = name;
        map = new HashMap<String,String>();
        clientAufstellen();
    }

    /**
     * Methode um der Standort mit seine Informationen zu erstellen.
     */
    public void clientAufstellen() {
        map.put(this.name,neueDataErstellen());
    }


    /**
     * Diese Methode erstellt zufaellige neue Daten..
     * @return zufaellige Daten (Temeraturen, Luftfaechtigkeit und aktueller Zeitpunkt)
     */
    public static String neueDataErstellen() {
        int temperatur = (int) (Math.random() * (18 - 15 + 1) + 15); //Temperatur in Celsius zufaellig.
        int luft =  (int) (Math.random() * 100 ); // Luftfeuchtigkeit in % zufaellig.
        long zeit = System.currentTimeMillis();
        return temperatur + "&" + luft + "&" + zeit;
    }
    /**
     * diese Methode teilt sowie das Paket in Stationen nach jeder"&" auf, als auch die Daten innerhalb der Station nach jeder "/"
     */
    private void dataSpeichern(String substring) {
        String[] sData = substring.split("%");
        for(String  station : sData){
            String name = station.split("&")[0];
            String data = station.split("&")[1] + "&" + station.split("&")[2] + "&"+ station.split("&")[3] ;
            clientAktu(name, data);
        }
    }

    /**
     * Diese Methode aktualisiert die Daten der Station, wenn die Zeitstamp neu wird.
     * @param name der Station
     * @param data der Station
     */
    public static void clientAktu(String name, String data) {
        if(map.containsKey(name)){
            if(Long.parseLong(data.split("&")[2]) > Long.parseLong(map.get(name).split("&")[2]));{ //vergleicht die Zeitstamp
                map.replace(name,data);
            }
        }
        else {
            map.put(name,data);
        }
    }

    /**
 * Diese Methode enthaelt die Bearbeitung des Paketes, sie erstellt buffer, ist fuer Status und Request Pakete verantwortlich und reagiert dementsprechend
 */
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
        }
        catch (IOException e) {
            System.out.println("IO Fehler beim Packet");
            }
        }




    }
