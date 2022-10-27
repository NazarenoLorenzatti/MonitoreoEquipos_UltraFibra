package monitoreoups;


import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Nazareno Lorenzatti
 * @version 1.1
 * nl.loragro@gmail.com
 * 
 */

// Clase principal de ejecucion
public class ControlUpsMain {

    public static void main(String[] args) {

        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                
                LectorDeMails ejecutar = new LectorDeMails();

                // Establecemos un bucle ya que el programa requiere estar siempre en ejecusion, y por cada iteracion del programa lo dormimos 5 minutos
                while (true) {
                    try {
                        // Llamamos al metodo para leer la casilla del correo
                        ejecutar.leerMails();
                        //Dormimos el bucle 5 minutos
                        Thread.sleep(300000);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(ControlUpsMain.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

            }

        });

    }

}
