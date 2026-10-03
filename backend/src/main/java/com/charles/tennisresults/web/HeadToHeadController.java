package com.charles.tennisresults.web;

import com.charles.tennisresults.dto.HeadToHeadDto;
import com.charles.tennisresults.service.HeadToHeadService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HeadToHeadController {

    private final HeadToHeadService headToHeadService;

    public HeadToHeadController(HeadToHeadService headToHeadService) {
        this.headToHeadService = headToHeadService;
    }

    @GetMapping("/api/head-to-head")
    public HeadToHeadDto headToHead(@RequestParam Long player1Id, @RequestParam Long player2Id) {
        return headToHeadService.headToHead(player1Id, player2Id);
    }
}
