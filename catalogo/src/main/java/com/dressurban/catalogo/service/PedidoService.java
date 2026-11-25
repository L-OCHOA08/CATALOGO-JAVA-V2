package com.dressurban.catalogo.service;

import com.dressurban.catalogo.model.*;
import com.dressurban.catalogo.repository.PedidoRepository;
import com.dressurban.catalogo.repository.VarianteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Comparator;
import java.util.List;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class PedidoService {

    public record Linea(Long varianteId, int cantidad) {}

    private final PedidoRepository pedidos;
    private final VarianteRepository variantes;
    private final StockService stock;

    public PedidoService(PedidoRepository pedidos, VarianteRepository variantes,
                         StockService stock) {
        this.pedidos = pedidos;
        this.variantes = variantes;
        this.stock = stock;
    }

    @Transactional
    public Pedido crear(OrigenPedido origen, String nombre, String telefono, String email,
                        String nota, List<Linea> lineas, String usuario) {

        String n = limpiar(nombre);
        String t = limpiar(telefono);
        String e = limpiar(email);
        String no = limpiar(nota);

        if (n.isEmpty()) {
            throw new IllegalArgumentException("El nombre del cliente es obligatorio");
        }
        if (t.isEmpty()) {
            throw new IllegalArgumentException("El teléfono del cliente es obligatorio");
        }
        if (n.length() > 120 || t.length() > 40 || e.length() > 120 || no.length() > 500) {
            throw new IllegalArgumentException("Alguno de los datos es demasiado largo");
        }

        // Agrupa por variante y las ordena por id (siempre bloqueamos en el mismo orden)
        Map<Long, Integer> porVariante = new TreeMap<>();
        for (Linea l : lineas) {
            if (l.cantidad() <= 0) {
                throw new IllegalArgumentException("Las cantidades deben ser mayores a 0");
            }
            porVariante.merge(l.varianteId(), l.cantidad(), Integer::sum);
        }
        if (porVariante.isEmpty()) {
            throw new IllegalArgumentException("Agregá al menos una prenda");
        }

        Pedido p = new Pedido();
        p.setOrigen(origen);
        p.setEstado(origen == OrigenPedido.PANEL ? EstadoPedido.CONFIRMADO : EstadoPedido.PENDIENTE);
        p.setClienteNombre(n);
        p.setClienteTelefono(t);
        p.setClienteEmail(e.isEmpty() ? null : e);
        p.setNota(no.isEmpty() ? null : no);

        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Long, Integer> entrada : porVariante.entrySet()) {
            Variante v = variantes.findById(entrada.getKey())
                    .orElseThrow(() -> new IllegalArgumentException("Una de las prendas ya no existe"));
            Producto prod = v.getProducto();
            if (!v.isActivo() || (origen == OrigenPedido.WEB && !prod.isActivo())) {
                throw new IllegalArgumentException(
                        "\"" + prod.getNombre() + "\" (talle " + v.getTalle() + ") ya no está disponible");
            }
            PedidoItem item = new PedidoItem();
            item.setVariante(v);
            item.setProductoNombre(prod.getNombre());
            item.setTalle(v.getTalle());
            item.setColor(v.getColor());
            item.setCantidad(entrada.getValue());
            item.setPrecioUnitario(prod.getPrecio());
            p.agregarItem(item);
            total = total.add(item.getSubtotal());
        }
        p.setTotal(total);
        pedidos.save(p);

        // Si alguna línea no tiene stock, esto lanza la excepción y se deshace TODO
        for (Map.Entry<Long, Integer> entrada : porVariante.entrySet()) {
            stock.descontarPorVenta(entrada.getKey(), entrada.getValue(), p.getId(), usuario);
        }
        return p;
    }

    @Transactional
    public void cambiarEstado(Long pedidoId, EstadoPedido nuevo, String usuario) {
        Pedido p = pedidos.findById(pedidoId)
                .orElseThrow(() -> new IllegalArgumentException("El pedido no existe"));
        EstadoPedido actual = p.getEstado();

        if (!transicionValida(actual, nuevo)) {
            throw new IllegalArgumentException(
                    "No se puede pasar un pedido de " + actual + " a " + nuevo);
        }
        if (pedidos.cambiarEstado(pedidoId, actual, nuevo) == 0) {
            throw new IllegalArgumentException(
                    "El pedido cambió mientras lo mirabas. Recargá la página.");
        }

        if (nuevo == EstadoPedido.CANCELADO) {
            // Siempre en el mismo orden (por id) para no trabarse con otros pedidos
            List<PedidoItem> items = p.getItems().stream()
                    .sorted(Comparator.comparing(i -> i.getVariante().getId()))
                    .toList();
            for (PedidoItem i : items) {
                stock.devolverPorCancelacion(i.getVariante().getId(), i.getCantidad(),
                        pedidoId, usuario);
            }
        }
    }

    private static boolean transicionValida(EstadoPedido de, EstadoPedido a) {
        return switch (de) {
            case PENDIENTE -> a == EstadoPedido.CONFIRMADO || a == EstadoPedido.CANCELADO;
            case CONFIRMADO -> a == EstadoPedido.ENTREGADO || a == EstadoPedido.CANCELADO;
            case ENTREGADO, CANCELADO -> false;
        };
    }

    private String limpiar(String s) {
        return s == null ? "" : s.trim();
    }

    /**
     * Cancela el pedido SOLO si sigue pendiente (la condición va dentro del UPDATE).
     * Devuelve true si lo canceló y devolvió el stock.
     */
    @Transactional
    public boolean cancelarSiSigueSinConfirmar(Long pedidoId, String usuario) {
        Pedido p = pedidos.findById(pedidoId).orElse(null);
        if (p == null) {
            return false;
        }
        if (pedidos.cambiarEstado(pedidoId, EstadoPedido.PENDIENTE, EstadoPedido.CANCELADO) == 0) {
            return false; // alguien lo confirmó o cancelaron antes: no se toca
        }
        List<PedidoItem> items = p.getItems().stream()
                .sorted(Comparator.comparing(i -> i.getVariante().getId()))
                .toList();
        for (PedidoItem i : items) {
            stock.devolverPorCancelacion(i.getVariante().getId(), i.getCantidad(),
                    pedidoId, usuario);
        }
        return true;
    }
}