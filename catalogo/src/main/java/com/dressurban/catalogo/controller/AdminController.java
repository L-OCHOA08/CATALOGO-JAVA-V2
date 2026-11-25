package com.dressurban.catalogo.controller;

import com.dressurban.catalogo.model.EstadoPedido;
import com.dressurban.catalogo.repository.PedidoRepository;
import com.dressurban.catalogo.service.AlertaStockService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;

@Controller
public class AdminController {

    private final PedidoRepository pedidos;
    private final AlertaStockService alertas;

    public AdminController(PedidoRepository pedidos, AlertaStockService alertas) {
        this.pedidos = pedidos;
        this.alertas = alertas;
    }

    @GetMapping("/admin")
    public String panel(Principal principal, Model model) {
        model.addAttribute("usuario", principal.getName());
        model.addAttribute("pendientes", pedidos.countByEstado(EstadoPedido.PENDIENTE));
        model.addAttribute("stockBajo", alertas.contar());
        return "admin/index";
    }
}