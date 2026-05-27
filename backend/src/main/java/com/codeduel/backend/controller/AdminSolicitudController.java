package com.codeduel.backend.controller;

import com.codeduel.backend.dto.SolicitudResponse;
import com.codeduel.backend.model.enums.EstadoSolicitud;
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
public class AdminSolicitudController {

    private final SolicitudService solicitudService;

    @GetMapping("/panel")
    public String getPanel(Model model) {
        long total = solicitudService.contarTotal();
        long pendientes = solicitudService.contarPorEstado(EstadoSolicitud.PENDIENTE);
        long aprobadas = solicitudService.contarPorEstado(EstadoSolicitud.APROBADA);
        long rechazadas = solicitudService.contarPorEstado(EstadoSolicitud.RECHAZADA);

        model.addAttribute("total", total);
        model.addAttribute("pendientes", pendientes);
        model.addAttribute("aprobadas", aprobadas);
        model.addAttribute("rechazadas", rechazadas);

        List<SolicitudResponse> solicitudes = solicitudService.getTodasSolicitudes();
        model.addAttribute("solicitudes", solicitudes);

        return "panel";
    }
}
