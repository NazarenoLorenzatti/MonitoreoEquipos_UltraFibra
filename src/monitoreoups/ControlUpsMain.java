package monitoreoups;

import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Nazareno Lorenzatti
 * @version 1.1 nl.loragro@gmail.com
 *
 */
// Clase principal de ejecucion
public class ControlUpsMain {

    public static void main(String[] args) {

        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {

                LectorDeMails ejecutar = new LectorDeMails();
                int inicio = 0;

                // Establecemos un bucle ya que el programa requiere estar siempre en ejecusion, y por cada iteracion del programa lo dormimos 5 minutos
                while (true) {

                    try {
                        // Llamamos al metodo para leer la casilla del correo
                        ejecutar.leerMails();
                        System.out.println("Retorno codigo envio = " + ejecutar.getRetorno());

                        if (!hilo.isAlive()) {
                            hilo.start();
                        }
                        //Dormimos el bucle 5 minutos
                        Thread.sleep(250000);

                    } catch (InterruptedException ex) {
                        Logger.getLogger(ControlUpsMain.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

            }

            Runnable runnable = new Runnable() {

                @Override
                public void run() {
                    String entrada = "";
                    do {
                        Scanner scanner = new Scanner(System.in);

                        entrada = scanner.nextLine();
                        entrada = entrada.toLowerCase();
                        if (entrada.equals("exit")) {
                            System.out.println("EJECUCION FINALIZADA");
                            System.exit(1);
                        }

                    } while (!entrada.equals("exit"));

                }
            };

            // Con este hilo controlamos lo que se ingrese por teclado en la consola para poder 
            // parar el proceso en el servidor linux
            // Creamos el hilo y le pasamos el runnable
            Thread hilo = new Thread(runnable);

        });

    }

}
