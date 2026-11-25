package com.dressurban.catalogo.controller;

import com.dressurban.catalogo.model.*;
import com.dressurban.catalogo.repository.PedidoRepository;
import com.dressurban.catalogo.repository.ProductoRepository;
import com.dressurban.catalogo.repository.VarianteRepository;
import com.dressurban.catalogo.service.Carrito;
import com.dressurban.catalogo.service.PedidoService;
import com.dressurban.catalogo.service.StockInsuficienteException;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Controller
public class TiendaController {

    public static class LineaVista {
        private final Long varianteId;
        private final String nombre;
        private final String talle;
        private final String color;
        private final int cantidad;
        private final BigDecimal precio;
        private final int stock;
        private final boolean disponible;

        public LineaVista(Long varianteId, String nombre, String talle, String color,
                          int cantidad, BigDecimal precio, int stock, boolean disponible) {
            this.varianteId = varianteId;
            this.nombre = nombre;
            this.talle = talle;
            this.color = color;
            this.cantidad = cantidad;
            this.precio = precio;
            this.stock = stock;
            this.disponible = disponible;
        }

        public Long getVarianteId() { return varianteId; }
        public String getNombre() { return nombre; }
        public String getTalle() { return talle; }
        public String getColor() { return color; }
        public int getCantidad() { return cantidad; }
        public BigDecimal getPrecio() { return precio; }
        public int getStock() { return stock; }
        public boolean isDisponible() { return disponible; }
        public BigDecimal getSubtotal() { return precio.multiply(BigDecimal.valueOf(cantidad)); }
        public boolean isProblema() { return !disponible || cantidad > stock; }
    }

    public static class CarritoVista {
        private final List<LineaVista> lineas;

        public CarritoVista(List<LineaVista> lineas) { this.lineas = lineas; }

        public List<LineaVista> getLineas() { return lineas; }

        public BigDecimal getTotal() {
            return lineas.stream().map(LineaVista::getSubtotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        public boolean isHayProblemas() {
            return lineas.stream().anyMatch(LineaVista::isProblema);
        }
    }

    private final ProductoRepository productos;
    private final VarianteRepository variantes;
    private final PedidoRepository pedidos;
    private final PedidoService pedidoService;
    private final String whatsapp;

    public TiendaController(ProductoRepository productos, VarianteRepository variantes,
                            PedidoRepository pedidos, PedidoService pedidoService,
                            @Value("${app.whatsapp.numero:}") String whatsapp) {
        this.productos = productos;
        this.variantes = variantes;
        this.pedidos = pedidos;
        this.pedidoService = pedidoService;
        this.whatsapp = whatsapp.replaceAll("\\D", "");
    }

    @GetMapping("/producto/{id}")
    public String producto(@PathVariable Long id, Model model) {
        Producto p = productos.findById(id)
                .filter(Producto::isActivo)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        List<Variante> disponibles = variantes.findByProductoIdOrderByTalleAscColorAsc(id).stream()
                .filter(v -> v.isActivo() && v.getStock() > 0)
                .toList();
        model.addAttribute("producto", p);
        model.addAttribute("variantes", disponibles);
        return "tienda/producto";
    }

    @PostMapping("/carrito/agregar")
    public String agregar(@RequestParam Long varianteId,
                          @RequestParam(defaultValue = "1") int cantidad,
                          HttpSession session, RedirectAttributes ra) {
        Variante v = variantes.findById(varianteId).orElse(null);
        if (v == null || !v.isActivo() || !v.getProducto().isActivo() || v.getStock() <= 0) {
            ra.addFlashAttribute("error", "Esa prenda ya no está disponible");
            return "redirect:/";
        }
        try {
            Carrito.de(session).agregar(varianteId, Math.max(cantidad, 1));
            ra.addFlashAttribute("mensaje", "Agregado al carrito");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/producto/" + v.getProducto().getId();
        }
        return "redirect:/carrito";
    }

    @GetMapping("/carrito")
    public String carrito(HttpSession session, Model model) {
        model.addAttribute("vista", armarVista(Carrito.de(session)));
        return "tienda/carrito";
    }

    @PostMapping("/carrito/actualizar")
    public String actualizar(@RequestParam Long varianteId, @RequestParam int cantidad,
                             HttpSession session, RedirectAttributes ra) {
        try {
            Carrito.de(session).cambiar(varianteId, cantidad);
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/carrito";
    }

    @PostMapping("/carrito/quitar")
    public String quitar(@RequestParam Long varianteId, HttpSession session) {
        Carrito.de(session).quitar(varianteId);
        return "redirect:/carrito";
    }

    @GetMapping("/checkout")
    public String checkout(HttpSession session, Model model, RedirectAttributes ra) {
        CarritoVista vista = armarVista(Carrito.de(session));
        if (vista.getLineas().isEmpty()) {
            return "redirect:/carrito";
        }
        if (vista.isHayProblemas()) {
            ra.addFlashAttribute("error", "Corregí los productos marcados antes de continuar");
            return "redirect:/carrito";
        }
        formulario(model, vista, "", "", "", "", null);
        return "tienda/checkout";
    }

    @PostMapping("/checkout")
    public String confirmar(@RequestParam String clienteNombre,
                            @RequestParam String clienteTelefono,
                            @RequestParam(defaultValue = "") String clienteEmail,
                            @RequestParam(defaultValue = "") String nota,
                            HttpSession session, Model model, RedirectAttributes ra) {
        Carrito carrito = Carrito.de(session);
        if (carrito.vacio()) {
            ra.addFlashAttribute("error", "Tu carrito está vacío");
            return "redirect:/carrito";
        }
        List<PedidoService.Linea> lineas = carrito.getLineas().entrySet().stream()
                .map(e -> new PedidoService.Linea(e.getKey(), e.getValue()))
                .toList();
        try {
            Pedido p = pedidoService.crear(OrigenPedido.WEB, clienteNombre, clienteTelefono,
                    clienteEmail, nota, lineas, "web");
            carrito.vaciar();
            session.setAttribute("ultimoPedidoId", p.getId());
            return "redirect:/pedido/gracias";
        } catch (StockInsuficienteException e) {
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/carrito";
        } catch (IllegalArgumentException e) {
            formulario(model, armarVista(carrito), clienteNombre, clienteTelefono,
                    clienteEmail, nota, e.getMessage());
            return "tienda/checkout";
        }
    }

    @GetMapping("/pedido/gracias")
    public String gracias(HttpSession session, Model model) {
        Object id = session.getAttribute("ultimoPedidoId");
        if (!(id instanceof Long)) {
            return "redirect:/";
        }
        Pedido p = pedidos.findById((Long) id).orElse(null);
        if (p == null) {
            return "redirect:/";
        }
        model.addAttribute("pedido", p);
        model.addAttribute("whatsappUrl", armarWhatsapp(p));
        return "tienda/gracias";
    }

    private void formulario(Model model, CarritoVista vista, String nombre, String telefono,
                            String email, String nota, String error) {
        model.addAttribute("vista", vista);
        model.addAttribute("clienteNombre", nombre);
        model.addAttribute("clienteTelefono", telefono);
        model.addAttribute("clienteEmail", email);
        model.addAttribute("nota", nota);
        model.addAttribute("error", error);
    }

    private CarritoVista armarVista(Carrito carrito) {
        Map<Long, Integer> copia = new LinkedHashMap<>(carrito.getLineas());
        Map<Long, Variante> porId = new HashMap<>();
        for (Variante v : variantes.findAllById(copia.keySet())) {
            porId.put(v.getId(), v);
        }
        List<LineaVista> lineas = new ArrayList<>();
        for (Map.Entry<Long, Integer> e : copia.entrySet()) {
            Variante v = porId.get(e.getKey());
            if (v == null) {
                carrito.quitar(e.getKey());
                continue;
            }
            Producto p = v.getProducto();
            lineas.add(new LineaVista(v.getId(), p.getNombre(), v.getTalle(), v.getColor(),
                    e.getValue(), p.getPrecio(), v.getStock(), v.isActivo() && p.isActivo()));
        }
        return new CarritoVista(lineas);
    }

    private String armarWhatsapp(Pedido p) {
        if (whatsapp.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder("Hola! Hice el pedido #")
                .append(p.getId()).append(" en la web:\n");
        for (PedidoItem i : p.getItems()) {
            sb.append("- ").append(i.getCantidad()).append(" x ").append(i.getProductoNombre())
                    .append(" (talle ").append(i.getTalle());
            if (!i.getColor().isBlank()) {
                sb.append(", ").append(i.getColor());
            }
            sb.append(")\n");
        }
        sb.append("Total: $ ").append(p.getTotal().setScale(2, RoundingMode.HALF_UP).toPlainString())
                .append("\nMi nombre: ").append(p.getClienteNombre());
        return "https://wa.me/" + whatsapp + "?text="
                + UriUtils.encodeQueryParam(sb.toString(), StandardCharsets.UTF_8);
    }
}