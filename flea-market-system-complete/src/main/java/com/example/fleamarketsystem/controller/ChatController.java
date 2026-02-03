package com.example.fleamarketsystem.controller;

import com.example.fleamarketsystem.entity.User;
import com.example.fleamarketsystem.service.ChatService;
import com.example.fleamarketsystem.service.ItemService;
import com.example.fleamarketsystem.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/chat")
public class ChatController {

    private final ChatService chatService;
    private final ItemService itemService;
    private final UserService userService;

    public ChatController(ChatService chatService, ItemService itemService, UserService userService) {
        this.chatService = chatService;
        this.itemService = itemService;
        this.userService = userService;
    }

    @GetMapping("/{itemId}")
    public String showChatScreen(@PathVariable("itemId") Long itemId, Model model) {
        model.addAttribute("item", itemService.getItemById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found")));
        model.addAttribute("chats", chatService.getChatMessagesByItem(itemId));
        return "item_detail"; // Re-use item_detail for chat display
    }

    @PostMapping("/{itemId}")
    public String sendMessage(
            @PathVariable("itemId") Long itemId,
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("message") String message) {
        User sender = userService.getUserByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Sender not found"));
        chatService.sendMessage(itemId, sender, message);
        return "redirect:/chat/{itemId}";
    }
}