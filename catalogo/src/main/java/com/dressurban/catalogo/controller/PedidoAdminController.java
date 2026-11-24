package com.dressurban.catalogo.controller;

import com.dressurban.catalogo.model.OrigenPedido;
import com.dressurban.catalogo.model.Pedido;
import com.dressurban.catalogo.repository.PedidoRepository;
import com.dressurban.catalogo.repository.VarianteRepository;
import com.dressurban.catalogo.service.PedidoService;
import com.dressurban.catalogo.service.StockInsuficienteException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/admin/pedidos")
public class PedidoAdminController {

    public static class Fila {
        private final String varianteId;
        private final String cantidad;

        public Fila(String varianteId, String cantidad) {
            this.varianteId = varianteId;
            this.cantidad = cantidad;
        }
        public String getVarianteId() { return varianteId; }
        public String getCantidad() { return cantidad; }
    }

    private final PedidoService pedidoService;
    private final PedidoRepository pedidos;
    private final VarianteRepository variantes;

    public PedidoAdminController(PedidoService pedidoService, PedidoRepository pedidos,
                                 VarianteRepository variantes) {
        this.pedidoService = pedidoService;
        this.pedidos = pedidos;
        this.variantes = variantes;
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        return formulario(model, new ArrayList<>(), "", "", "", "", null);
    }

    @PostMapping("/crear")
    public String crear(@RequestParam String clienteNombre,
                        @RequestParam String clienteTelefono,
                        @RequestParam(defaultValue = "") String clienteEmail,
                        @RequestParam(defaultValue = "") String nota,
                        @RequestParam(name = "varianteId", required = false) List<String> varianteIds,
                        @RequestParam(name = "cantidad", required = false) List<String> cantidades,
                        Principal principal, Model model, RedirectAttributes ra) {

        List<Fila> filas = new ArrayList<>();
        List<PedidoService.Linea> lineas = new ArrayList<>();
        int cantidadFilas = varianteIds == null ? 0 : varianteIds.size();

        for (int i = 0; i < cantidadFilas; i++) {
            String vid = varianteIds.get(i) == null ? "" : varianteIds.get(i).trim();
            String cant = (cantidades != null && i < cantidades.size() && cantidades.get(i) != null)
                    ? cantidades.get(i).trim() : "";
            filas.add(new Fila(vid, cant));
            if (vid.isEmpty()) {
                continue;
            }
            try {
                lineas.add(new PedidoService.Linea(Long.valueOf(vid), Integer.parseInt(cant)));
            } catch (NumberFormatException ex) {
                return formulario(model, filas, clienteNombre, clienteTelefono, clienteEmail, nota,
                        "Revisá las cantidades: tienen que ser números enteros");
            }
        }

        try {
            Pedido p = pedidoService.crear(OrigenPedido.PANEL, clienteNombre, clienteTelefono,
                    clienteEmail, nota, lineas, principal.getName());
            ra.addFlashAttribute("mensaje", "Pedido #" + p.getId() + " creado y stock descontado");
            return "redirect:/admin/pedidos/" + p.getId();
        } catch (IllegalArgumentException | StockInsuficienteException ex) {
            return formulario(model, filas, clienteNombre, clienteTelefono, clienteEmail, nota,
                    ex.getMessage());
        }
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable Long id, Model model) {
        Pedido p = pedidos.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("pedido", p);
        return "admin/pedidos/detalle";
    }

    private String formulario(Model model, List<Fila> filas, String nombre, String telefono,
                              String email, String nota, String error) {
        if (filas.isEmpty()) {
            filas.add(new Fila("", "1"));
        }
        model.addAttribute("variantes", variantes.disponibles());
        model.addAttribute("filas", filas);
        model.addAttribute("clienteNombre", nombre);
        model.addAttribute("clienteTelefono", telefono);
        model.addAttribute("clienteEmail", email);
        model.addAttribute("nota", nota);
        model.addAttribute("error", error);
        return "admin/pedidos/nuevo";
    }
}