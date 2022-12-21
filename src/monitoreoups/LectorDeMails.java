package monitoreoups;

import com.mashape.unirest.http.*;
import com.mashape.unirest.http.exceptions.UnirestException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.mail.*;

/**
 *
 * @author Nazareno Lorenzatti
 * @version 1.1 nl.loragro@gmail.com
 *
 */
// Clase para la leectura de los mails enviados por las UPS al momento de registrar algun evento.
public class LectorDeMails {

    private static String alertaMensaje;
    private static String token;
    private static String remitente;
    private static String asunto;
    private static int retorno;

    public LectorDeMails() {

    }

    public void leerMails() {
        try {
            System.out.println("Ejecutando lector");
            Properties prop = new Properties();

            // Deshabilitamos TLS
            prop.setProperty("mail.pop3.starttls.enable", "false");

            // Usamos SSL
            prop.setProperty("mail.pop3.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            prop.setProperty("mail.pop3.socketFactory.fallback", "false");

            // Puerto 995 para conectarse.
            prop.setProperty("mail.pop3.port", "995");
            prop.setProperty("mail.pop3.socketFactory.port", "995");

            Session sesion = Session.getInstance(prop);
//            sesion.setDebug(true);

            try {

                System.out.println("PROGRAMA EN EJECUCION");

                Store store = sesion.getStore("pop3");
                store.connect("pop.gmail.com", "alertas.ups.ultrafibra@gmail.com", "*********");

                Folder folder = store.getFolder("INBOX");
                folder.open(Folder.READ_ONLY);

                Message[] mensajes = folder.getMessages();

                for (int i = 0; i < mensajes.length; i++) {
                    int ret = 0;
                    this.remitente = mensajes[i].getFrom()[0].toString();
                    this.asunto = mensajes[i].getSubject();

                    if (this.remitente.contains("ups-notificaciones@ultra.net.ar") || this.remitente.contains("nl.loragro@gmail.com")) {
                        analizaParteDeMensajeUPS(mensajes[i]);
                        ret = enviarMensaje(this.alertaMensaje);
                    } else if (this.remitente.contains("catchall@vangrow.ar") || this.remitente.contains("alertas.ups.ultrafibra@gmail.com")) {
                        analizaParteDeMensajeOLT(mensajes[i]);
                        ret = enviarMensaje(this.alertaMensaje);
                    }

                    if (ret == 401) {
                        nuevoToken();
                        i--; // reinicio i para que no se pierda el mensaje que ya paso
                    }
                    Thread.sleep(5000);

                }

                folder.close(false);
                store.close();

            } catch (NoSuchProviderException ex) {

                Logger.getLogger(LectorDeMails.class.getName()).log(Level.SEVERE, null, ex);
                System.out.println("Error al obtener el protocolo de sesion");

            } catch (InterruptedException ex) {
                Logger.getLogger(LectorDeMails.class.getName()).log(Level.SEVERE, null, ex);
                System.out.println("Error al pausar el Hilo");
            }

        } catch (MessagingException ex) {
            Logger.getLogger(LectorDeMails.class.getName()).log(Level.SEVERE, null, ex);
            System.out.println("Error al establecer la conexion y/o obtener la carpeta de correos");
        }
    }

    //ANALIZAR EL MAIL PARA OBTENER LA INFORMACION DE LA ALERTA
    private static void analizaParteDeMensajeUPS(Part unaParte) {

        try {
            // Si es multiparte, se analiza cada una de sus partes recursivamente, Obteniendo el mensaje solamente. 
            if (unaParte.isMimeType("multipart/*")) {
                Multipart multi;
                multi = (Multipart) unaParte.getContent();

                for (int j = 0; j < multi.getCount(); j++) {
                    Part bodyPart = multi.getBodyPart(j);
                    if (bodyPart.isMimeType("text/*")) {
                        alertaMensaje = (String) bodyPart.getContent();
                        break;
                    }
                    analizaParteDeMensajeUPS(multi.getBodyPart(j));
                }
            }
            // si es un texto simple se guarda directamente el mensaje
            if (unaParte.isMimeType("text/*")) {
                alertaMensaje = (String) unaParte.getContent();
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error al leer el correo");
        }

    }

    private static void analizaParteDeMensajeOLT(Part unaParte) {

        try {
            // Si es multiparte, se analiza cada una de sus partes recursivamente, Obteniendo el mensaje solamente. 
            if (unaParte.isMimeType("multipart/*")) {
                Multipart multi;
                multi = (Multipart) unaParte.getContent();

                for (int j = 0; j < multi.getCount(); j++) {
                    Part bodyPart = multi.getBodyPart(j);
                    if (bodyPart.isMimeType("text/*")) {
                        alertaMensaje = (String) bodyPart.getContent();
                        alertaMensaje = alertaMensaje.substring(14480, 14547);
                        alertaMensaje = alertaMensaje.replaceAll("</p>", "");
                        alertaMensaje = alertaMensaje.replaceAll("</td>", "").replaceAll("<", "").replaceAll(">", "");
                        alertaMensaje = alertaMensaje.replaceAll("left", "").replace("align", "").replace("=", "").replace("lign","");
                        alertaMensaje = alertaMensaje.replaceAll("\"","");
                        alertaMensaje = alertaMensaje.trim();
                        alertaMensaje = asunto + ">>>>" + alertaMensaje;
                        break;
                    }
                    analizaParteDeMensajeOLT(multi.getBodyPart(j));
                }
            }
            // si es un texto simple se guarda directamente el mensaje
            if (unaParte.isMimeType("text/*")) {
                alertaMensaje = (String) unaParte.getContent();
                alertaMensaje = alertaMensaje.substring(14480, 14547);
                alertaMensaje = alertaMensaje.replaceAll("</p>", "");
                alertaMensaje = alertaMensaje.replaceAll("</td>", "").replaceAll("<", "").replaceAll(">", "");
                alertaMensaje = alertaMensaje.replaceAll("left", "").replace("align", "").replace("=", "").replace("lign","");
                alertaMensaje = alertaMensaje.replaceAll("\"","");
                alertaMensaje = alertaMensaje.trim();                
                alertaMensaje = asunto + ">>>>" + alertaMensaje;

            }

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error al leer el correo");
        }

    }

    public static int enviarMensaje(String alerta) {
        int ret = 0;
        try {
            Unirest.setTimeouts(0, 0);
            HttpResponse<String> response = Unirest.post("https://mayten.cloud/api/Mensajes/Texto")
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .body("{\r\n    \"origen\": \"SMS_CORTO\",\r\n    \"mensajes\": [\r\n        {\r\n            \"mensaje\": \"" + alerta + "\",\r\n            \"telefono\": \"**********\", \r\n            \"identificador\": \"\"\r\n        },\r\n        {\r\n            \"mensaje\": \"" + alerta + "\",\r\n            \"telefono\": \"*********\", \r\n            \"identificador\": \"\"\r\n        },\r\n                {\r\n            \"mensaje\": \"" + alerta + "\",\r\n            \"telefono\": \"*********\", \r\n            \"identificador\": \"\"\r\n        },\r\n                {\r\n            \"mensaje\": \"" + alerta + "\",\r\n            \"telefono\": \"********\", \r\n            \"identificador\": \"\"\r\n        },\r\n                {\r\n            \"mensaje\": \"" + alerta + "\",\r\n            \"telefono\": \"********\", \r\n            \"identificador\": \"\"\r\n        },\r\n                {\r\n            \"mensaje\": \"" + alerta + "\",\r\n            \"telefono\": \"********\", \r\n            \"identificador\": \"\"\r\n        },\r\n                {\r\n            \"mensaje\": \"" + alerta + "\",\r\n            \"telefono\": \"**********\", \r\n            \"identificador\": \"\"\r\n        }\r\n    ]\r\n}")
                    .asString();
            ret = response.getStatus();
            System.out.println("retorno = " + ret);
            retorno = ret;
        } catch (UnirestException ex) {
            Logger.getLogger(LectorDeMails.class.getName()).log(Level.SEVERE, null, ex);
        }
        return ret;
    }

    //SOLICITAR TOKEN NUEVO
    public static void nuevoToken() {
        try {
            Unirest.setTimeouts(0, 0);
            HttpResponse<String> response = Unirest.post("http://mayten.cloud/auth")
                    .header("Content-Type", "application/json")
                    .body("{\r\n\t\"username\": \"*********\",\r\n\t\"password\": \"*********\"\r\n}")
                    .asString();
            // Leo el body de la llamda donde esta el token
            String body = response.getBody();
            // extraigo el token y lo guardo en el atributo
            token = body.substring(10, 314);

        } catch (UnirestException ex) {
            Logger.getLogger(LectorDeMails.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public static int getRetorno() {
        return retorno;
    }

}
