package com.dressurban.catalogo.config;

import com.dressurban.catalogo.service.Carrito;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class VistaGlobal {

    private final String whatsapp;
    private final Modulos modulos;
    private final int vencimientoHoras;

    public VistaGlobal(@Value("${app.whatsapp.numero:}") String whatsapp, Modulos modulos,
                       @Value("${app.pedidos.vencimiento-horas:48}") int vencimientoHoras) {
        this.whatsapp = whatsapp.replaceAll("\\D", "");
        this.modulos = modulos;
        this.vencimientoHoras = modulos.isTienda() ? Math.max(vencimientoHoras, 0) : 0;
    }

    @ModelAttribute("whatsappNumero")
    public String whatsappNumero() {
        return whatsapp;
    }

    @ModelAttribute("modulos")
    public Modulos modulos() {
        return modulos;
    }

    @ModelAttribute("vencimientoHoras")
    public int vencimientoHoras() {
        return vencimientoHoras;
    }

    @ModelAttribute("carritoCantidad")
    public int carritoCantidad(HttpServletRequest request) {
        return Carrito.cantidadEn(request.getSession(false));
    }
}