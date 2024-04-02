package com.example.webchatserver;

import java.io.*;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import org.apache.commons.lang3.RandomStringUtils;

/**
 * This is a class that has services
 * In our case, we are using this to generate unique room IDs**/
@WebServlet(name = "chatServlet", value = {"/chat-servlet", "/chat-servlet/room_list"})
public class ChatServlet extends HttpServlet {
    private String message;

    //static so this set is unique
    public static Set<String> rooms = new HashSet<>();



    /**
     * Method generates unique room codes
     * **/
    public String generatingRandomUpperAlphanumericString(int length) {
        String generatedString = RandomStringUtils.randomAlphanumeric(length).toUpperCase();
        // generating unique room code
        while (rooms.contains(generatedString)){
            generatedString = RandomStringUtils.randomAlphanumeric(length).toUpperCase();
        }
        rooms.add(generatedString);

        return generatedString;
    }

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        PrintWriter out = response.getWriter();

        String path = request.getServletPath();
        if("/chat-servlet/room_list".equals(path)) {
            response.setContentType("application/json");
            //convert to json object
//            ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
            ObjectWriter ow = new ObjectMapper().writer();
            String json = ow.writeValueAsString(rooms);

            out.println(json);
        } else {
            // send the random code as the response's content
            response.setContentType("text/plain");
            out.println(generatingRandomUpperAlphanumericString(5));
        }

    }

    public void destroy() {
    }
}