package Physical;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONObject;
import java.sql.Timestamp;

import java.util.Date;

import static Enumerations.Constantes.*;

// Author Victor Pinquier

public class InterfaceMQTT implements MqttCallback{

    private static final String CONNECTION_URL = "tcp://neocampus.univ-tlse3.fr:1883";
    private static final String SUBSCRIPTION = "u4/302/#";
    private static final String USERNAME = "m2dc";
    private static final String PASSWORD = "m2dc;18";

    private MqttClient client;
    private MqttConnectOptions connOpts;

    public InterfaceMQTT() {
    }

    public void run() {

        String clientId = "JavaSample";
        MemoryPersistence persistence = new MemoryPersistence();

        try {
            client = new MqttClient(CONNECTION_URL, clientId, persistence);
            connOpts = new MqttConnectOptions();
            MqttMessage message;

            connOpts.setUserName(USERNAME);
            connOpts.setPassword(PASSWORD.toCharArray());

            connOpts.setCleanSession(true);
            System.out.println("Connecting to broker: "+CONNECTION_URL);
            client.connect(connOpts);
            System.out.println("Connected");
            client.setCallback(this);

            client.subscribe(SUBSCRIPTION);

            // Gets status de démarrage

            // Get des volets
            message = new MqttMessage();
            message.setPayload(ListeCommande.getPayloadString(COMMANDE_ALL,COMMANDE_VOLETS_STATUS).getBytes());
            client.publish(ListeCommande.getTopicString(TOPIC_VOLETS),message);

            // Get des lampes
            message = new MqttMessage();
            message.setPayload(ListeCommande.getPayloadString(COMMANDE_ALL, COMMANDE_LUMIERE_STATUS).getBytes());
            client.publish(ListeCommande.getTopicString(TOPIC_LUMIERES), message);

            // Permet de traiter les ordres stockés dans la liste "listeOrdre"
            /*while (true) {
                if (ListeCommande.possedeOrdre()) {
                    message = new MqttMessage();
                    message.setPayload(ListeCommande.getPremierOrdre().getPayload().getBytes());
                    client.publish(ListeCommande.getPremierOrdre().getTopic(), message);
                    ListeCommande.supprimerPremierOrdre();
                }
            }*/

            //client.disconnect();
            //System.out.println("Disconnected");
            //System.exit(0);
        } catch(MqttException me) {
            System.out.println("reason "+me.getReasonCode());
            System.out.println("msg "+me.getMessage());
            System.out.println("loc "+me.getLocalizedMessage());
            System.out.println("cause "+me.getCause());
            System.out.println("excep "+me);
            me.printStackTrace();
        }
    }

    /**
     *
     * @deprecated
     */
    public void publish(String topicName, int qos, byte[] payload) throws MqttException {

        // Connect to the MQTT server
        System.out.println("Connecting to "+CONNECTION_URL + " with client ID "+client.getClientId());
        client.connect(connOpts);
        System.out.println("Connected");

        String time = new Timestamp(System.currentTimeMillis()).toString();
        System.out.println("Publishing at: "+time+ " to topic \""+topicName+"\" qos "+qos);

        // Create and configure a message
        MqttMessage message = new MqttMessage(payload);
        message.setQos(qos);

        // Send the message to the server, control is not returned until
        // it has been delivered to the server meeting the specified
        // quality of service.
        client.publish(topicName, message);

        // Disconnect the client
        client.disconnect();
    }

    @Override
    public void connectionLost(Throwable throwable) {

    }

    @Override
    public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
        //System.out.println(mqttMessage);
        JSONObject obj = new JSONObject(mqttMessage.toString());
        //System.out.println(obj.toString());

        if(obj.toMap().containsKey("subID")) {
            switch (obj.getString("subID")){
                // Reception des données des capteurs de l'ilot 1
                case "ilot1":
                    // Capteur luminosité
                    if(obj.toMap().containsKey("value_units") && obj.getString("value_units").equals("lux")){
                        System.out.println("ilot1 lux : " + obj.getInt("value"));
                        Capteur.setLuminositeInt(0, obj.getInt("value"));
                    }
                    // Capteur présence
                    else if(obj.toMap().containsKey("type") && obj.getString("type").equals("presence")){
                        System.out.println("presence 1 : " + obj.getInt("value"));
                        boolean val = (obj.getInt("value")==1); // Permet de convertir un int en bool
                        Capteur.setPresence(0, val);
                        // Sauvegarde la date de présence
                        if (!val)
                            Capteur.setDatePresence(new Date());
                    }
                break;
                // Reception des données des capteurs de l'ilot 2
                case "ilot2":
                    // Capteur luminosité
                    if(obj.toMap().containsKey("value_units") && obj.getString("value_units").equals("lux")){
                        System.out.println("ilot2 lux : " + obj.getInt("value"));
                        Capteur.setLuminositeInt(1, obj.getInt("value"));
                    }
                    // Capteur présence
                    else if(obj.toMap().containsKey("type") && obj.getString("type").equals("presence")){
                        System.out.println("presence 2 : " + obj.getInt("value"));
                        boolean val = (obj.getInt("value")==1);
                        Capteur.setPresence(1, val);
                        // Sauvegarde la date de présence
                        if (!val)
                            Capteur.setDatePresence(new Date());
                    }
                break;
                // Reception des données des capteurs de l'ilot 3
                case "ilot3":
                    // Capteur luminosité
                    if(obj.toMap().containsKey("value_units") && obj.getString("value_units").equals("lux")){
                        System.out.println("ilot3 lux : " + obj.getInt("value"));
                        Capteur.setLuminositeInt(2, obj.getInt("value"));
                    }
                    // Capteur présence
                    else if(obj.toMap().containsKey("type") && obj.getString("type").equals("presence")){
                        System.out.println("presence 3 : " + obj.getInt("value"));
                        boolean val = (obj.getInt("value")==1);
                        Capteur.setPresence(2, val);
                        // Sauvegarde la date de présence
                        if (!val)
                            Capteur.setDatePresence(new Date());
                    }
                break;
                // Reception des données du capteur de luminosité exterieur
                case "ouest":
                    // Conversion de la luminosité de W/m² en lux
                    if(obj.getString("unitID").equals("outside") && obj.getString("value_units").equals("w/m2")) {
                        System.out.println("exterieur : " + obj.getInt("value") / 0.0079);
                        Capteur.setLuminositeExt((float) (obj.getInt("value") / 0.0079));
                        Capteur.getDateDifference();
                    }
                break;
            }
        }

            // PARTIE EFFECTEUR
            if(obj.toMap().containsKey("unitID")) {
                // Permet de récupérer les status des effecteurs
                switch (obj.getString("unitID")) {
                    // Volet back
                    case "back":
                        Effecteur.setVolets(VOLETS_BACK, obj.getString("status"));
                        System.out.println("back : " + obj.getString("status"));
                        break;
                    // Volet center
                    case "center":
                        Effecteur.setVolets(VOLETS_CENTER, obj.getString("status"));
                        System.out.println("center : " + obj.getString("status"));
                        break;
                    // Volet front
                    case "front":
                        Effecteur.setVolets(VOLETS_FRONT, obj.getString("status"));
                        System.out.println("front : " + obj.getString("status"));
                        break;
                    // Lampes others
                    case "others":
                        Effecteur.setLumiere(obj.getString("status"));
                        System.out.println("other : " + obj.getString("status"));
                        break;
                }
            }

    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

    }
}