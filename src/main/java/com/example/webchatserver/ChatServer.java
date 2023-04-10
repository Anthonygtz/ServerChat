package com.example.webchatserver;
import java.util.*;


import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import org.json.JSONObject;

import java.io.IOException;


/**
 * This class represents a web socket server, a new connection is created and it receives a roomID as a parameter
 * **/
@ServerEndpoint(value="/ws/{roomID}")
public class ChatServer {

    //This contains a static List of ChatRoom which is used to control the existing rooms and their users

    // you may add other attributes as you see fit
    private Map<String, String> usernames = new HashMap<String, String>();
    private static Map<String, String> roomList = new HashMap<String, String>();

    @OnOpen
    public void open(@PathParam("roomID") String roomID, Session session) throws IOException, EncodeException {
        String uniqueCode; //This is the variable for room code

        if(roomList.containsKey(roomID)){ // this is incase the room already exists if so it uses its existing room code
            uniqueCode = roomList.get(roomID); //This sets the room code to the one in the map
        }else{ // otherwise it will generate a new unique code
            uniqueCode = roomID; //this sets the code to the roomID code
            roomList.put(roomID, uniqueCode); //put it in the hashmap
            System.out.println("Room created with unique code " + uniqueCode); //displays the message in the console
        }

        roomList.put(session.getId(), uniqueCode); // The code to adding userID to a room

        System.out.println("Room joined ");

        String output = stringOutput();

        //Gives us various messages to the client  in order to display
        session.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(Room " + uniqueCode + "): Welcome to the chat room. Please state your username to begin.\"}");
        session.getBasicRemote().sendText("{\"type\": \"title\", \"message\":\"You are in room: " + uniqueCode + "\"}");
        session.getBasicRemote().sendText(output);
    }

    public String stringOutput()
    {
        Set<String> stringSet = new HashSet<>();
        StringBuilder stringBuilder = new StringBuilder();
        List<String> roomslist = new ArrayList<>();
        
        for (Map.Entry<String, String> entry : roomList.entrySet())
        {
            String value = entry.getValue();
            if (!stringSet.contains(value))
            {
                stringSet.add(value);
                roomslist.add(value);
            }
        }

        stringBuilder.append("{\" type \": \" rooms \", \" message \": [");
        for (String room : roomslist)
        {
            stringBuilder.append("\"").append(room).append("\",");
        }

        if (stringBuilder.charAt(stringBuilder.length() - 1) == ',')
        {
            stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        }
        stringBuilder.append("]}");

        return stringBuilder.toString();
    }



    @OnClose
    public void close(Session session) throws IOException, EncodeException {
        String userId = session.getId();
        // do things for when the connection closes
        if (usernames.containsKey(userId)) {
            String username = usernames.get(userId);
            String roomID = roomList.get(userId);
            usernames.remove(userId);
            // remove this user from the roomList
            roomList.remove(roomID);

            // broadcasting it to peers in the same room
            int countPeers = 0;
            for (Session peer : session.getOpenSessions()){ //broadcast this person left the server
                if(roomList.get(peer.getId()).equals(roomID)) { // broadcast only to those in the same room
                    peer.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(Room " + roomID+ "): " + username + " left the chat room.\"}");
                    countPeers++; // count how many peers are left in the room
                }
            }
        }
    }

    @OnMessage
    public void handleMessage(String comm, Session session) throws IOException, EncodeException {
//        example getting unique userID that sent this message
        String userID = session.getId();
        String roomID = roomList.get(userID); // my room
        JSONObject jsonmsg = new JSONObject(comm);
        String type = (String) jsonmsg.get("type");
        String message = (String) jsonmsg.get("msg");

        // handle the messages
        if(usernames.containsKey(userID)){ // not their first message
            String username = usernames.get(userID);
            System.out.println(username);

            // broadcasting it to peers in the same room
            for(Session peer: session.getOpenSessions()){
                // only send my messages to those in the same room
                if(roomList.get(peer.getId()).equals(roomID)) {
                    peer.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(" + roomID + ", "+ username + "): " + message + "\"}");
                }
            }
        }else{ //first message is their username
            usernames.put(userID, message);
            session.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(Room " +roomID +"): Welcome, " + message + "!\"}");


            // broadcasting it to peers in the same room
            for(Session peer: session.getOpenSessions()){
                // only announce to those in the same room as me, excluding myself
                if((!peer.getId().equals(userID)) && (roomList.get(peer.getId()).equals(roomID))){
                    peer.getBasicRemote().sendText("{\"type\": \"chat\", \"message\":\"(Room " +roomID +"): " + message + " joined the chat room.\"}");
                }
            }
        }

    }


}