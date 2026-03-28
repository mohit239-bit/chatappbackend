package com.example.chat.app.backend.Service;

import com.example.chat.app.backend.Respository.RoomRepository;
import com.example.chat.app.backend.entities.Room;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

@Service
public class RoomService {

    private RoomRepository roomRepository;

    public RoomService(RoomRepository roomRepository){
        this.roomRepository = roomRepository;
    }

    public Room createRoom(String roomId){
        if(roomRepository.findByRoomId(roomId) != null){
            return null;
        }
        Room room = new Room();
        room.setRoomId(roomId);
        return roomRepository.save(room);
    }

    public Room getRoom(String roomId){
        return roomRepository.findByRoomId(roomId);
    }


}
