package com.example.chat.app.backend.Controller;


import com.example.chat.app.backend.Respository.RoomRepository;
import com.example.chat.app.backend.Service.RoomService;
import com.example.chat.app.backend.entities.Message;
import com.example.chat.app.backend.entities.Room;
import com.example.chat.app.backend.payload.MessageRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import java.security.Principal;

import java.time.Instant;
import java.time.LocalDateTime;

@Controller
public class ChatController {

    private RoomRepository roomRepository;

    @Autowired
    private RoomService roomService;

    public ChatController(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    @MessageMapping("/sendMessage/{roomId}")
    @SendTo("/topic/room/{roomId}")
    public Message sendMessage(@DestinationVariable String roomId,
                               MessageRequest request,
                               Principal principal) throws Exception {

        if (principal == null) {
            throw new SecurityException("Authentication is required to send messages");
        }

        Room room = roomService.getRoom(roomId);

        Message message = new Message();
        message.setContent(request.getContent());
        message.setSender(principal.getName());
        message.setTimeStamp(Instant.now());

        if(room != null){
            room.getMessages().add(message);
            roomRepository.save(room);
        }else {
            throw new RuntimeException("Room not found");
        }

        return message;
    }
}
