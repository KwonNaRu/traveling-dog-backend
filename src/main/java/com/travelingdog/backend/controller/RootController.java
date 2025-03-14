package com.travelingdog.backend.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.view.RedirectView;

@Controller
public class RootController {

    @GetMapping("/")
    public RedirectView redirectToFrontend() {
        return new RedirectView("http://travelingdog.duckdns.org:3000");
    }

}
