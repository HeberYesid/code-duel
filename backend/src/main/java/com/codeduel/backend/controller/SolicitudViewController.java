package com.codeduel.backend.controller;

import com.codeduel.backend.dto.SolicitudResponse;
import com.codeduel.backend.service.SolicitudService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/admin/solicitudes")
@RequiredArgsConstructor
public class SolicitudViewController {

    private final SolicitudService solicitudService;

    @GetMapping("/panel")
    public String getPanel(Model model) {
        model.addAttribute("total", solicitudService.countTotal());
        model.addAttribute("pendientes", solicitudService.countPendientes());
        model.addAttribute("aprobadas", solicitudService.countAprobadas());
        model.addAttribute("rechazadas", solicitudService.countRechazadas());

        List<SolicitudResponse> solicitudes = solicitudService.obtenerTodasLasSolicitudes();
        model.addAttribute("solicitudes", solicitudes);

        return "panel";
    }
}
