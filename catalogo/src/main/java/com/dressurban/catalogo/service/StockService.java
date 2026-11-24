package com.dressurban.catalogo.service;

import com.dressurban.catalogo.model.MovimientoStock;
import com.dressurban.catalogo.model.TipoMovimiento;
import com.dressurban.catalogo.model.Variante;
import com.dressurban.catalogo.repository.MovimientoStockRepository;
import com.dressurban.catalogo.repository.VarianteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockService {

    private final VarianteRepository variantes;
    private final MovimientoStockRepository movimientos;

    public StockService(VarianteRepository variantes, MovimientoStockRepository movimientos) {
        this.variantes = variantes;
        this.movimientos = movimientos;
    }

    /** Descuenta stock por una venta. Si no alcanza, lanza StockInsuficienteException. */
    @Transactional
    public void descontarPorVenta(Long varianteId, int cantidad, Long pedidoId, String usuario) {
        validarCantidad(cantidad);
        if (variantes.descontar(varianteId, cantidad) == 0) {
            throw new StockInsuficienteException(mensajeSinStock(varianteId));
        }
        registrar(varianteId, TipoMovimiento.VENTA, -cantidad, null, usuario, pedidoId);
    }

    /** Devuelve al stock las unidades de un pedido cancelado. */
    @Transactional
    public void devolverPorCancelacion(Long varianteId, int cantidad, Long pedidoId, String usuario) {
        validarCantidad(cantidad);
        variantes.sumar(varianteId, cantidad);
        registrar(varianteId, TipoMovimiento.CANCELACION, cantidad, "Pedido cancelado", usuario, pedidoId);
    }

    /** Ajuste manual: delta positivo suma, negativo resta. El motivo es obligatorio. */
    @Transactional
    public void ajustarManual(Long varianteId, int delta, String motivo, String usuario) {
        if (delta == 0) {
            throw new IllegalArgumentException("La cantidad no puede ser 0");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("El motivo es obligatorio");
        }
        if (delta > 0) {
            variantes.sumar(varianteId, delta);
        } else if (variantes.descontar(varianteId, -delta) == 0) {
            throw new StockInsuficienteException(
                    "No se puede restar " + (-delta) + ": " + mensajeSinStock(varianteId));
        }
        registrar(varianteId, TipoMovimiento.AJUSTE_MANUAL, delta, motivo.trim(), usuario, null);
    }

    private void validarCantidad(int cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor a 0");
        }
    }

    private void registrar(Long varianteId, TipoMovimiento tipo, int cantidad,
                           String motivo, String usuario, Long pedidoId) {
        MovimientoStock m = new MovimientoStock();
        m.setVariante(variantes.getReferenceById(varianteId));
        m.setTipo(tipo);
        m.setCantidad(cantidad);
        m.setStockResultante(variantes.stockActual(varianteId));
        m.setMotivo(motivo);
        m.setUsuario(usuario);
        m.setPedidoId(pedidoId);
        movimientos.save(m);
    }

    private String mensajeSinStock(Long varianteId) {
        return variantes.findById(varianteId)
                .map(v -> "No hay stock suficiente de " + v.getProducto().getNombre()
                        + " (talle " + v.getTalle()
                        + (v.getColor().isBlank() ? "" : ", " + v.getColor())
                        + "). Disponible: " + variantes.stockActual(varianteId))
                .orElse("La variante no existe");
    }
}
