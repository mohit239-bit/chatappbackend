package com.example.chat.app.backend.Controller;


import com.example.chat.app.backend.Config.AppConstants;
import com.example.chat.app.backend.Respository.RoomRepository;
import com.example.chat.app.backend.Service.RoomService;
import com.example.chat.app.backend.entities.Message;
import com.example.chat.app.backend.entities.Room;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rooms")
@CrossOrigin(AppConstants.FRONT_END_BASE_URI)
public class RoomController {

    @Autowired
    private RoomService roomService;

    @PostMapping
    public ResponseEntity<?> postRoom(@RequestBody Room room){
        Room createdRoom = roomService.createRoom(room.getRoomId());

        if(createdRoom == null){
            return ResponseEntity.badRequest().body("Room already exists");
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(createdRoom);
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<?> joinRoom(@PathVariable String roomId){
        Room room = roomService.getRoom(roomId);
        if(room == null){
            return ResponseEntity.badRequest().body("Room not found");
        }
        return ResponseEntity.ok(room);
    }

    @GetMapping("/{roomId}/messages")
    public ResponseEntity<List<Message>> getMessages(
            @PathVariable String roomId,
            @RequestParam(value = "page", defaultValue = "0", required = false) int page,
            @RequestParam(value = "size", defaultValue = "20", required = false) int size
    ){
        Room room = roomService.getRoom(roomId);
        if(room == null){
            return ResponseEntity.badRequest().build();
        }

        List<Message> messages = room.getMessages();
        int start = Math.max(0, messages.size() - (page+1) * size);
        int end = Math.min(messages.size(), start + size);
        List<Message> paginatedMessages = messages.subList(start,end);
        return ResponseEntity.ok(messages);
    }

}
