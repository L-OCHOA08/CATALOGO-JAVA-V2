package com.dressurban.catalogo.controller;

import com.dressurban.catalogo.model.Producto;
import com.dressurban.catalogo.model.Variante;
import com.dressurban.catalogo.repository.MovimientoStockRepository;
import com.dressurban.catalogo.repository.ProductoRepository;
import com.dressurban.catalogo.repository.VarianteRepository;
import com.dressurban.catalogo.service.StockInsuficienteException;
import com.dressurban.catalogo.service.StockService;
import com.dressurban.catalogo.service.VarianteService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/admin")
public class VarianteAdminController {

    private final ProductoRepository productos;
    private final VarianteRepository variantes;
    private final MovimientoStockRepository movimientos;
    private final VarianteService varianteService;
    private final StockService stock;

    public VarianteAdminController(ProductoRepository productos, VarianteRepository variantes,
                                   MovimientoStockRepository movimientos,
                                   VarianteService varianteService, StockService stock) {
        this.productos = productos;
        this.variantes = variantes;
        this.movimientos = movimientos;
        this.varianteService = varianteService;
        this.stock = stock;
    }

    private Variante buscarVariante(Long id) {
        return variantes.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @GetMapping("/productos/{productoId}/variantes")
    public String lista(@PathVariable Long productoId, Model model) {
        Producto p = productos.findById(productoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("producto", p);
        model.addAttribute("variantes",
                variantes.findByProductoIdOrderByTalleAscColorAsc(productoId));
        return "admin/variantes/lista";
    }

    @PostMapping("/productos/{productoId}/variantes")
    public String crear(@PathVariable Long productoId,
                        @RequestParam String talle,
                        @RequestParam(defaultValue = "") String color,
                        @RequestParam(defaultValue = "0") int stockInicial,
                        Principal principal, RedirectAttributes ra) {
        try {
            varianteService.crear(productoId, talle, color, stockInicial, principal.getName());
            ra.addFlashAttribute("mensaje", "Variante creada");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/productos/" + productoId + "/variantes";
    }

    @PostMapping("/variantes/{id}/activar")
    public String alternar(@PathVariable Long id, RedirectAttributes ra) {
        Variante v = buscarVariante(id);
        Long productoId = v.getProducto().getId();
        boolean activa = varianteService.alternarActivo(id);
        ra.addFlashAttribute("mensaje", activa ? "Variante activada" : "Variante desactivada");
        return "redirect:/admin/productos/" + productoId + "/variantes";
    }

    @GetMapping("/variantes/{id}")
    public String detalle(@PathVariable Long id, Model model) {
        model.addAttribute("variante", buscarVariante(id));
        model.addAttribute("movimientos",
                movimientos.findByVarianteIdOrderByFechaDescIdDesc(id));
        return "admin/variantes/detalle";
    }

    @PostMapping("/variantes/{id}/ajustar")
    public String ajustar(@PathVariable Long id,
                          @RequestParam String operacion,
                          @RequestParam int cantidad,
                          @RequestParam String motivo,
                          Principal principal, RedirectAttributes ra) {
        try {
            if (cantidad <= 0) {
                throw new IllegalArgumentException("La cantidad debe ser mayor a 0");
            }
            int delta = "RESTAR".equals(operacion) ? -cantidad : cantidad;
            stock.ajustarManual(id, delta, motivo, principal.getName());
            ra.addFlashAttribute("mensaje", "Stock actualizado");
        } catch (IllegalArgumentException | StockInsuficienteException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/variantes/" + id;
    }

    @PostMapping("/variantes/{id}/eliminar")
    public String eliminar(@PathVariable Long id, RedirectAttributes ra) {
        Variante v = buscarVariante(id);
        Long productoId = v.getProducto().getId();
        try {
            varianteService.eliminar(id);
            ra.addFlashAttribute("mensaje", "Variante eliminada");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/productos/" + productoId + "/variantes";
    }

}