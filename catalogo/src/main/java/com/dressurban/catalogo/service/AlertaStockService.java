package com.dressurban.catalogo.service;

import com.dressurban.catalogo.model.Variante;
import com.dressurban.catalogo.repository.VarianteRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AlertaStockService {

    private final VarianteRepository variantes;
    private final int umbral;

    public AlertaStockService(VarianteRepository variantes,
                              @Value("${app.stock.minimo:3}") int umbral) {
        this.variantes = variantes;
        this.umbral = Math.max(umbral, 0);
    }

    public int getUmbral() {
        return umbral;
    }

    public List<Variante> listar() {
        return variantes.stockBajo(umbral);
    }

    public long contar() {
        return variantes.contarStockBajo(umbral);
    }
}