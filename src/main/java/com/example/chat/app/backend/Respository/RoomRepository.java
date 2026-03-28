package com.example.chat.app.backend.Respository;

import com.example.chat.app.backend.entities.Room;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RoomRepository extends MongoRepository<Room, String> {

    Room findByRoomId(String roomId);

}
