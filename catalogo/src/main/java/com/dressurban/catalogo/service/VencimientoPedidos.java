package com.dressurban.catalogo.service;

import com.dressurban.catalogo.config.Modulos;
import com.dressurban.catalogo.model.EstadoPedido;
import com.dressurban.catalogo.repository.PedidoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class VencimientoPedidos {

    private static final Logger log = LoggerFactory.getLogger(VencimientoPedidos.class);

    private final PedidoRepository pedidos;
    private final PedidoService pedidoService;
    private final Modulos modulos;
    private final int horas;

    public VencimientoPedidos(PedidoRepository pedidos, PedidoService pedidoService,
                              Modulos modulos,
                              @Value("${app.pedidos.vencimiento-horas:48}") int horas) {
        this.pedidos = pedidos;
        this.pedidoService = pedidoService;
        this.modulos = modulos;
        this.horas = horas;
    }

    @Scheduled(initialDelay = 60000, fixedDelayString = "${app.pedidos.revision-ms:900000}")
    public void cancelarVencidos() {
        if (!modulos.isTienda() || horas <= 0) {
            return;
        }
        LocalDateTime limite = LocalDateTime.now().minusHours(horas);
        List<Long> ids = pedidos.idsAnterioresA(EstadoPedido.PENDIENTE, limite, PageRequest.of(0, 200));

        int cancelados = 0;
        for (Long id : ids) {
            try {
                if (pedidoService.cancelarSiSigueSinConfirmar(id, "sistema")) {
                    cancelados++;
                }
            } catch (RuntimeException e) {
                log.warn("No se pudo vencer el pedido #{}: {}", id, e.getMessage());
            }
        }
        if (cancelados > 0) {
            log.info("Pedidos pendientes vencidos y cancelados: {}", cancelados);
        }
    }
}