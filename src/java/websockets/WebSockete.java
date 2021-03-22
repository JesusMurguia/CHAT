/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package websockets;

import Dominio.Alumno;
import Dominio.Mensaje;
import Dominio.TipoMensaje;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.websocket.EncodeException;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import org.json.JSONArray;
import org.json.JSONObject;
import javax.websocket.server.ServerEndpoint;

/**
 *
 * @author lv1013 Es un ejemplo de websocket donde cada mensaje que llega un
 * cliente es replicado a quienes estén conectados
 */
@ServerEndpoint("/websocketendpoint")
public class WebSockete {
    //para guardar la sesión de cada cliente y poder replicar el mensaje a cada uno
    //se hace una colección sincronizada para el manejo de la concurrencia

    private static Set<Session> clients
            = Collections.synchronizedSet(new HashSet<Session>());

    @OnOpen
    public void onOpen(Session sesion) {
        System.out.println("Open Connection ...");
        //al conectarse un cliente se abre el websocket y se guarda su sesión.
        clients.add(sesion);
        
        //mensajeHandler(mensaje, sesion);
    }

    @OnClose
    public void onClose(Session sesion) {
        System.out.println("Close Connection ...");
        //al cerrarse la conexión por parte del cliente se elimina su sesión en el servidor
        clients.remove(sesion);
        Mensaje mensaje=new Mensaje("", "", "", "MENSAJE_CLIENTES", "");
        mensajeClientes(mensaje, sesion);
    }

    @OnMessage
    public void onMessage(String message, Session sesion) {
        
        String echoMsg="";
        System.out.println("Message from the client: " + message);
        
        JSONObject json = new JSONObject(message);
        System.out.println(json.getString("mensaje"));
        Mensaje mensaje=new Mensaje(json.getString("username"), json.getString("id"), json.getString("mensaje"), json.getString("tipoMensaje"), json.getString("destinatario"));
        JSONObject jsonObject = new JSONObject(mensaje);
        System.out.println(mensaje.toString());
        mensajeHandler(mensaje,sesion);
    }

    @OnError
    public void onError(Throwable e) {
        e.printStackTrace();
    }
    
    private ArrayList<String> getClientList(){
        ArrayList<String> list=new ArrayList<>();
        for(Session c:clients){
            list.add((String) c.getUserProperties().get("username"));
        }
        return list;
    }
    
    private void mensajeHandler(Mensaje mensaje, Session sesion){
        switch (mensaje.getTipoMensaje()) {
            case "MENSAJE_LOGIN":
                mensajeLogin(mensaje,sesion);
                break;
            case "MENSAJE_NORMAL":
                mensajeNormal(mensaje,sesion);
                break;
            case "MENSAJE_PRIVADO":
                mensajePrivado(mensaje,sesion);
                break;
            case "MENSAJE_CLIENTES":
                mensajeClientes(mensaje,sesion);
                break;
            case "MENSAJE_ENTIDAD":
            mensajeEntidad(mensaje,sesion);
            break;
            default:
                break;
        }
    }
    
    private void mensajeLogin(Mensaje mensaje, Session sesion){
        sesion.getUserProperties().put("username", mensaje.getUsername());
        //se hace un bloque sincronizado para manejar la concurrencia, tal como en los sockets e hilos
        synchronized (clients) {
            // Se itera sobre la sesiones (clientes) guardados para transmitir el mensaje
            for (Session client : clients) {
                if (!client.equals(sesion)) {
                    try {
                            mensaje.setMensaje(mensaje.getMensaje());
                            JSONObject jsonObject = new JSONObject(mensaje);
                            System.out.println(jsonObject.toString());
                            client.getBasicRemote().sendText(jsonObject.toString());
                    } catch (IOException ex) {
                        System.out.println(ex);
                    }
                }
            }
        }
    }
    
    private void mensajeNormal(Mensaje mensaje, Session sesion){
        synchronized (clients) {
            // Se itera sobre la sesiones (clientes) guardados para transmitir el mensaje
            for (Session client : clients) {
                if (!client.equals(sesion)) {
                    try {
                            mensaje.setMensaje(mensaje.getUsername()+": "+mensaje.getMensaje());
                            JSONObject jsonObject = new JSONObject(mensaje);
                            System.out.println(jsonObject.toString());
                            client.getBasicRemote().sendText(jsonObject.toString());
                    } catch (IOException ex) {
                        System.out.println(ex);
                    }
                }
            }
        }
    }
    
    private void mensajePrivado(Mensaje mensaje, Session sesion){
        
        synchronized (clients) {
            // Se itera sobre la sesiones (clientes) guardados para transmitir el mensaje
            for (Session client : clients) {
                if (client.getUserProperties().get("username").equals(mensaje.getDestinatario())) {
                    try {
                            mensaje.setMensaje("(MENSAJE PRIVADO)"+mensaje.getUsername()+": "+mensaje.getMensaje());
                            JSONObject jsonObject = new JSONObject(mensaje);
                            client.getBasicRemote().sendText(jsonObject.toString());
                    } catch (IOException ex) {
                        System.out.println(ex);
                    }
                }
            }
        }
    }
    
    
    private void mensajeClientes(Mensaje mensaje, Session sesion){
        
        synchronized (clients) {
            // Se itera sobre la sesiones (clientes) guardados para transmitir el mensaje
            for (Session client : clients) {
                    try {
                            mensaje.setMensaje(getClientList().toString());
                            JSONObject jsonObject = new JSONObject(mensaje);
                            client.getBasicRemote().sendText(jsonObject.toString());
                    } catch (IOException ex) {
                        System.out.println(ex);
                    }
                }
        }
    }
    
    private void mensajeEntidad(Mensaje mensaje, Session sesion){
        
        synchronized (clients) {
            // Se itera sobre la sesiones (clientes) guardados para transmitir el mensaje
            for (Session client : clients) {
                    try {
                            JSONObject json = new JSONObject(mensaje);
                            JSONObject  msg= new JSONObject(json.toString());
                            
                            Alumno a=new Alumno(msg.getString("nombre"),msg.getString("promedio"));
                            
                            JSONObject  alumno= new JSONObject(a);
                            mensaje.setMensaje(alumno.toString());
                            
                            JSONObject  m= new JSONObject(mensaje);
                            client.getBasicRemote().sendText(m.toString());
                    } catch (IOException ex) {
                        System.out.println(ex);
                    }
                }
        }
    }
    
}
