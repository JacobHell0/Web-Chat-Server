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
//    private static Set<String> rooms = ChatServlet.rooms;

    //stores the chat history as a <roomID, List<List<times, msgs>>>
    private static Map<String, List<String>> chatHistory = new HashMap<String, List<String>>();


    @OnOpen
    public void open(@PathParam("roomID") String roomID, Session session) throws IOException, EncodeException {

        //send the history to the user
        sendHistoryToClient(session, roomID);

        //basically userAlert just prevents it from going into the chatHistory
        sendMessageToClient(session, "userAlert", "(Server): Welcome to the chat room. Please state your username to begin.", roomID);

        //create entry in hashmap, example: ("6d5976c1-99c5-45cb-ada0-8b983f3efbcc", "12345")
        String userID = session.getId();
        roomList.put(userID, roomID);
    }

    @OnClose
    public void close(Session session) throws IOException, EncodeException {
        String userId = session.getId();
        String roomId = roomList.get(userId);
        String username = usernames.get(userId);

        //broadcast left message
        for (Session peer : session.getOpenSessions()){
            //filter to broadcast to users in the same room
            if(roomList.get(peer.getId()).equals(roomId)) {
                sendMessageToClient(peer, "other", "(Server): " + username + " left the chat" +
                                                                    " room.", roomId);
            }
        }

    }

    @OnMessage
    public void handleMessage(String comm, Session session) throws IOException, EncodeException {

        // handle the messages
        String userID = session.getId();
        String roomID = roomList.get(userID);

        //checks if the user sent an image
        if(comm.startsWith("data:image/png;")) {
            //sent text is an image
            //check if the user has a username
            if(!usernames.containsKey(userID)){return;} //reject request

            //send image to other users
            pictureMsg(userID, session, comm, roomID);
            return;
        }

        //the user sent a json formatted string then
        JSONObject jsonmsg = new JSONObject(comm);
        String message = (String) jsonmsg.get("msg");


        // not their first message
        if(usernames.containsKey(userID)){
            standardMsg(userID, session, message);
        } else { //User's first message: send welcome message and update hashmaps
            firstMsg(userID, roomID, message, session);
        }

    }

    //this logic is very similar to standard message but it needs to send different types
    private void pictureMsg(String userID, Session session, String comm, String roomID) throws IOException {
        String roomId = roomList.get(userID);
        for(Session peer: session.getOpenSessions()){
            //check if peer is user so it sends correct type
            if(peer.getId().equals(userID)) {
                sendMessageToClient(peer, "user-image", comm, roomID);
            } else { //actual peer
                //since the roomList uses UUID, not username
                if (roomList.get(peer.getId()).equals(roomId)) //check if peer is in same room
                {
                    sendMessageToClient(peer, "other-image", comm, roomID);
                }
            }
        }
    }

    //function to handle the logic for every message after the user's first message
    private void standardMsg(String userID, Session session, String message) throws IOException {
        String username = usernames.get(userID);
        String roomId = roomList.get(userID);
        for(Session peer: session.getOpenSessions()){
            //check if peer is user so it sends correct type
            if(peer.getId().equals(userID)) {
                sendMessageToClient(peer, "user", "(" + username + "): " + message, roomId);
            } else { //actual peer
                //since the roomList uses UUID, not username
                if (roomList.get(peer.getId()).equals(roomId)) //check if peer is in same room
                {
                    sendMessageToClient(peer, "other", "(" + username + "): " + message, roomId);
                }
            }
        }
    }

    //this function handles the logic for the user sending the backend their username
    private void firstMsg(String userID, String roomId, String message, Session session) throws IOException {
        //add user to the usernames hashmap
        usernames.put(userID, message);
        sendMessageToClient(session, "other", "(Server): Welcome, " + message + "!", roomId);

        //broadcast this person joined the server to each peer
        for(Session peer: session.getOpenSessions()){
            if((!peer.getId().equals(userID)) && (roomList.get(peer.getId()).equals(roomId))){ //not the client that just connected
                sendMessageToClient(peer, "other", "(Server): " + message + " joined the chat room.", roomId);
            }
        }
    }

    //this function sends the entire chat history for roomID to the client
    private void sendHistoryToClient(Session session, String roomId) throws IOException {
        //send the entire map for that room to the client
        if(!chatHistory.containsKey(roomId)) return;
        List<String> hist = new ArrayList<String>(chatHistory.get(roomId));
        for(String item : hist) {
            //specialized type to indicate to the javascript and to avoid recording it twice in the map
            sendMessageToClient(session, "ChatHistory", item, roomId);
        }
    }

    private void sendMessageToClient(Session session, String type, String message, String roomId) throws IOException {
        //the different tyeps are:
        //userAlert: sends a msg to the user but doesn't save it in the chat history
        //ChatHistory: indicates the backend is sending the chat history to the user, so it doesn't create
        //              another entry in the chat history map
        //all others such as 'user' or 'other' which are important on the frontend but require no special
        //logic on the backend

        //check if element is userAlert
        if(type.equals("userAlert")) { //don't create the entry in chat history
            session.getBasicRemote().sendText(makeMessageJSON("other", message));
            return;
        }

        //send message to client with a type
        session.getBasicRemote().sendText(makeMessageJSON(type, message));

        //this fixes a duplicate message from being recorded in the mpa
        if(type.equals("ChatHistory")) {return;}
        if(type.equals("other")) {return;}
        if(type.equals("other-image")) {return;}
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

    //helper function to convert the message into a json format
    private String makeMessageJSON(String type, String message) {
        return "{\"type\": \"" + type + "\", \"message\":\"" + message + "\"}";
    }

}