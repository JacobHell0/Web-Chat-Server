package com.example.webchatserver;


import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


/**
 * This class represents a web socket server, a new connection is created and it receives a roomID as a parameter
 * **/
@ServerEndpoint(value="/ws/{roomID}")
public class ChatServer {


    // contains a static List of ChatRoom used to control the existing rooms and their users
    private static Map<String, String> roomList = new HashMap<String, String>();

    // contains a map for this instance that maps a users UUID to the username they set
    private Map<String, String> usernames = new HashMap<String, String>();

    //variable used to grab the rooms from the Servlet
    private static Set<String> rooms = ChatServlet.rooms;


    @OnOpen
    public void open(@PathParam("roomID") String roomID, Session session) throws IOException, EncodeException {


        //session.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(Server): Welcome to the chat room. Please state your username to begin.\"}");
        //functionally equivalent TODO remove these comments
        session.getBasicRemote().sendText(makeMessageJSON("chat", "(Server): Welcome to the chat room. Please state your username to begin."));

        //accessing the roomID parameter
        System.out.println(roomID);

    }

    @OnClose
    public void close(Session session) throws IOException, EncodeException {
        String userId = session.getId();
        // do things for when the connection closes
    }

    @OnMessage
    public void handleMessage(String comm, Session session, @PathParam("roomID") String roomID) throws IOException, EncodeException {

    //        Example conversion of json messages from the client
    //        JSONObject jsonmsg = new JSONObject(comm);
    //        String val1 = (String) jsonmsg.get("attribute1");
    //        String val2 = (String) jsonmsg.get("attribute2");

        // handle the messages
        String userID = session.getId();
        JSONObject jsonmsg = new JSONObject(comm);
        String type = (String) jsonmsg.get("type");
        String message = (String) jsonmsg.get("msg");


        // not their first message
        if(usernames.containsKey(userID)){
            standardMsg(userID, session, message);
        } else { //User's first message: send welcome message and update hashmaps
            firstMsg(userID, roomID, message, session);
        }



    }

    private void standardMsg(String userID, Session session, String message) throws IOException {
        String username = usernames.get(userID);
        System.out.println(username);
        String roomId = roomList.get(userID);
        for(Session peer: session.getOpenSessions()){
            //for chatrooms with different rooms //TODO Write some meaningfull comments @jacob
            //if statement saying if the userID is in the roomID
            //since the roomList uses UUID, not username
            if(roomList.get(peer.getId()).equals(roomId))
            {
//                    peer.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(" + username + "): " + message+"\"}");
                peer.getBasicRemote().sendText(makeMessageJSON("chat", "(" + username + "): " + message));
            }
        }
    }

    private void firstMsg(String userID, String roomID, String message, Session session) throws IOException {
        roomList.put(userID, roomID);
        usernames.put(userID, message);
//        session.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(Server): Welcome, " + message + "!\"}");
        session.getBasicRemote().sendText(makeMessageJSON("chat", "(Server): Welcome, " + message + "!"));
        //broadcast this person joined the server to the rest
        String username = usernames.get(userID);
        String roomId = roomList.get(username);

        //broadcast to each peer
        for(Session peer: session.getOpenSessions()){
            if((!peer.getId().equals(userID)) && (roomList.get(usernames.get(peer.getId())).equals(roomId))){ //not the client that just connected
//                peer.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(Server): " + message + " joined the chat room.\"}");
                peer.getBasicRemote().sendText(makeMessageJSON("chat", "(Server): " + message + " joined the chat room."));
            }
        }
    }

    private String makeMessageJSON(String type, String message) {
        return "{\"type\": \"" + type + "\", \"message\":\"" + message + "\"}";
    }

}