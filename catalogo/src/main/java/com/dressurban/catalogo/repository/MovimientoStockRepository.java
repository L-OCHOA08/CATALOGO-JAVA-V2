package com.dressurban.catalogo.repository;

import com.dressurban.catalogo.model.MovimientoStock;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovimientoStockRepository extends JpaRepository<MovimientoStock, Long> {

    List<MovimientoStock> findByVarianteIdOrderByFechaDescIdDesc(Long varianteId);
}