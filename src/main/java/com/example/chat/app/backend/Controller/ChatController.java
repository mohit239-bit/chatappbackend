package com.example.chat.app.backend.Controller;


import com.example.chat.app.backend.Config.AppConstants;
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
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.Instant;
import java.time.LocalDateTime;

@Controller
@CrossOrigin(AppConstants.FRONT_END_BASE_URI)
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
                               MessageRequest request) throws Exception {

        Room room = roomService.getRoom(request.getRoomId());

        Message message = new Message();
        message.setContent(request.getContent());
        message.setSender(request.getSender());
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
