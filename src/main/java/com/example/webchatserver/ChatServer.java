package com.example.webchatserver;


import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.json.JSONObject;

import java.io.IOException;
import java.util.*;


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

    //stores the chat history as a <roomID, List<List<times, msgs>>>
    private static Map<String, List<String>> chatHistory = new HashMap<String, List<String>>();


    @OnOpen
    public void open(@PathParam("roomID") String roomID, Session session) throws IOException, EncodeException {

        //basically userAlert just prevents it from going into the chatHistory
        sendMessageToClient(session, "userAlert", "(Server): Welcome to the chat room. Please state your username to begin.", roomID);

        //create entry in hashmap, example: ("6d5976c1-99c5-45cb-ada0-8b983f3efbcc", "12345")
        String userID = session.getId();
        roomList.put(userID, roomID);
    }

    @OnClose
    public void close(Session session) throws IOException, EncodeException {
        String userId = session.getId();
        // do things for when the connection closes
        //TODO figure out what todo when connection terminates
    }

    @OnMessage
    public void handleMessage(String comm, Session session) throws IOException, EncodeException {

    //        Example conversion of json messages from the client
    //        JSONObject jsonmsg = new JSONObject(comm);
    //        String val1 = (String) jsonmsg.get("attribute1");
    //        String val2 = (String) jsonmsg.get("attribute2");

        // handle the messages
        String userID = session.getId();
        JSONObject jsonmsg = new JSONObject(comm);
        String type = (String) jsonmsg.get("type");
        String message = (String) jsonmsg.get("msg");
        String roomID = roomList.get(userID);

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
//                peer.getBasicRemote().sendText(makeMessageJSON("chat", "(" + username + "): " + message));
                sendMessageToClient(peer, "chatUser","(" + username + "): " + message, roomId);
            }
        }
    }

    private void firstMsg(String userID, String roomId, String message, Session session) throws IOException {
        usernames.put(userID, message);
//        session.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(Server): Welcome, " + message + "!\"}");
//        session.getBasicRemote().sendText(makeMessageJSON("chat", "(Server): Welcome, " + message + "!"));
        sendMessageToClient(session, "chat", "(Server): Welcome, " + message + "!", roomId);

        //broadcast this person joined the server to the rest
        String username = usernames.get(userID);

        //broadcast to each peer
        for(Session peer: session.getOpenSessions()){
            if((!peer.getId().equals(userID)) && (roomList.get(peer.getId()).equals(roomId))){ //not the client that just connected
//                peer.getBasicRemote().sendText(makeMessageJSON("chat", "(Server): " + message + " joined the chat room."));
                sendMessageToClient(peer, "chat", "(Server): " + message + " joined the chat room.", roomId);
            }
        }
    }

    private void sendMessageToClient(Session session, String type, String message, String roomId) throws IOException {

        //check if element is userAlert
        if(type.equals("userAlert")) { //don't create the entry
            session.getBasicRemote().sendText(makeMessageJSON("chat", message));
            return;
        }

        session.getBasicRemote().sendText(makeMessageJSON(type, message));

        //check if entry exists
        if(chatHistory.containsKey(roomId)){
            //append message
            List<String> history;
            history = chatHistory.get(roomId);
            history.add(message);
            chatHistory.put(roomId, history);
        } else {
            //create entry
            List<String> messages = new ArrayList<>();
            messages.add(message);
            chatHistory.put(roomId, messages);
        }
    }

    private String makeMessageJSON(String type, String message) {
        return "{\"type\": \"" + type + "\", \"message\":\"" + message + "\"}";

    }

}