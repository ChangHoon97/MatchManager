package com.matchmanager.controller;

import com.matchmanager.dto.DrawRequestDto;
import com.matchmanager.model.Court;
import com.matchmanager.model.Player;
import com.matchmanager.service.DrawService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@Controller
public class DrawController {

    @Autowired
    private DrawService drawService;

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @PostMapping("/api/draw")
    @ResponseBody
    public List<Court> generateDrawApi(@Valid @RequestBody DrawRequestDto request) {
        List<Player> players = new ArrayList<>();
        for (DrawRequestDto.PlayerDto dto : request.getPlayers()) {
            players.add(new Player(dto.getName(), dto.getGrade()));
        }
        return drawService.generateDraw(players);
    }
}
