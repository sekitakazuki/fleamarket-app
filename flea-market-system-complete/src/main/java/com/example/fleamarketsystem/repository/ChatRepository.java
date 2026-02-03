package com.example.fleamarketsystem.repository;

import com.example.fleamarketsystem.entity.Chat;
import com.example.fleamarketsystem.entity.Item;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {
    List<Chat> findByItemOrderByCreatedAtAsc(Item item);
}
