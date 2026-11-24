package com.dressurban.catalogo.service;

import com.dressurban.catalogo.model.*;
import com.dressurban.catalogo.repository.PedidoRepository;
import com.dressurban.catalogo.repository.VarianteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private String limpiar(String s) {
        return s == null ? "" : s.trim();
    }
}