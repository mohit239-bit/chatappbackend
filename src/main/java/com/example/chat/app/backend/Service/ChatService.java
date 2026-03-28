package com.example.chat.app.backend.Service;

import com.example.chat.app.backend.Respository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    @Autowired
    private RoomRepository roomRepository;

}
