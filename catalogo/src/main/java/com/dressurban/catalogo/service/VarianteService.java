package com.dressurban.catalogo.service;

import com.dressurban.catalogo.model.Producto;
import com.dressurban.catalogo.model.Variante;
import com.dressurban.catalogo.repository.PedidoItemRepository;
import com.dressurban.catalogo.repository.ProductoRepository;
import com.dressurban.catalogo.repository.VarianteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VarianteService {

    private final VarianteRepository variantes;
    private final ProductoRepository productos;
    private final PedidoItemRepository pedidoItems;
    private final StockService stock;

    public VarianteService(VarianteRepository variantes, ProductoRepository productos,
                           PedidoItemRepository pedidoItems, StockService stock) {
        this.variantes = variantes;
        this.productos = productos;
        this.pedidoItems = pedidoItems;
        this.stock = stock;
    }

    @Transactional
    public Variante crear(Long productoId, String talle, String color,
                          int stockInicial, String usuario) {
        String t = talle == null ? "" : talle.trim();
        String c = color == null ? "" : color.trim();

        if (t.isEmpty()) {
            throw new IllegalArgumentException("El talle es obligatorio");
        }
        if (t.length() > 20 || c.length() > 40) {
            throw new IllegalArgumentException("Talle (máx. 20 caracteres) o color (máx. 40) demasiado largo");
        }
        if (stockInicial < 0) {
            throw new IllegalArgumentException("El stock inicial no puede ser negativo");
        }
        Producto p = productos.findById(productoId)
                .orElseThrow(() -> new IllegalArgumentException("El producto no existe"));
        if (variantes.existsByProductoIdAndTalleIgnoreCaseAndColorIgnoreCase(productoId, t, c)) {
            throw new IllegalArgumentException("Ya existe esa combinación de talle y color");
        }

        Variante v = new Variante();
        v.setProducto(p);
        v.setTalle(t);
        v.setColor(c);
        variantes.save(v);

        if (stockInicial > 0) {
            stock.ajustarManual(v.getId(), stockInicial, "Stock inicial", usuario);
        }
        return v;
    }

    /** Devuelve true si la variante quedó activa. */
    @Transactional
    public boolean alternarActivo(Long id) {
        Variante v = variantes.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("La variante no existe"));
        v.setActivo(!v.isActivo());
        return v.isActivo();
    }

    @Transactional
    public void eliminar(Long id) {
        Variante v = variantes.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("La variante no existe"));
        if (pedidoItems.existsByVarianteId(id)) {
            throw new IllegalArgumentException(
                    "Tiene ventas registradas: no se puede eliminar. Ocultala para que no se pueda comprar.");
        }
        if (v.getStock() != 0) {
            throw new IllegalArgumentException(
                    "Todavía tiene " + v.getStock() + " unidades. Ajustá el stock a 0 antes de eliminarla.");
        }
        variantes.delete(v);
    }
}