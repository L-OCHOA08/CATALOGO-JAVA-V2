package com.dressurban.catalogo.controller;

import com.dressurban.catalogo.service.AlertaStockService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class StockBajoAdminController {

    private final AlertaStockService alertas;

    public StockBajoAdminController(AlertaStockService alertas) {
        this.alertas = alertas;
    }

    @GetMapping("/admin/stock-bajo")
    public String lista(Model model) {
        model.addAttribute("variantes", alertas.listar());
        model.addAttribute("umbral", alertas.getUmbral());
        return "admin/stock/bajo";
    }
}