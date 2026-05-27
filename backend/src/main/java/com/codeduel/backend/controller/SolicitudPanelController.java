package com.codeduel.backend.controller;

import com.codeduel.backend.model.enums.EstadoSolicitud;
import com.codeduel.backend.service.SolicitudService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/solicitudes")
@RequiredArgsConstructor
public class SolicitudPanelController {

    private final SolicitudService solicitudService;

    @GetMapping("/panel")
    public String panel(Model model) {
        model.addAttribute("total", solicitudService.contarTotal());
        model.addAttribute("pendientes", solicitudService.contarPorEstado(EstadoSolicitud.PENDIENTE));
        model.addAttribute("aprobadas", solicitudService.contarPorEstado(EstadoSolicitud.APROBADA));
        model.addAttribute("rechazadas", solicitudService.contarPorEstado(EstadoSolicitud.RECHAZADA));
        model.addAttribute("solicitudes", solicitudService.getTodasLasSolicitudes());

        return "admin/solicitudes-panel";
    }
}
