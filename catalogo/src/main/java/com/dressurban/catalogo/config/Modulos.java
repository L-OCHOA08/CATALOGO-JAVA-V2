package com.dressurban.catalogo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class Modulos {

    public enum Plan { VITRINA, CONTROL, TIENDA }

    private static final Logger log = LoggerFactory.getLogger(Modulos.class);

    private final Plan plan;
    private final boolean reportes;

    public Modulos(@Value("${app.plan:VITRINA}") String plan,
                   @Value("${app.modulos.reportes:false}") boolean reportes) {
        try {
            this.plan = Plan.valueOf(plan.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                    "app.plan inválido: \"" + plan + "\". Usá VITRINA, CONTROL o TIENDA.");
        }
        if (reportes && this.plan == Plan.VITRINA) {
            throw new IllegalStateException(
                    "El reporte de ventas requiere el plan CONTROL o TIENDA.");
        }
        this.reportes = reportes;
        log.info("Plan activo: {} | Reporte de ventas: {}", this.plan, this.reportes);
    }

    public Plan getPlan() {
        return plan;
    }

    /** Variantes, stock, alerta de stock bajo y pedidos del panel. */
    public boolean isStock() {
        return plan != Plan.VITRINA;
    }

    /** Carrito y compra para el cliente final. */
    public boolean isTienda() {
        return plan == Plan.TIENDA;
    }

    public boolean isReportes() {
        return reportes;
    }
}