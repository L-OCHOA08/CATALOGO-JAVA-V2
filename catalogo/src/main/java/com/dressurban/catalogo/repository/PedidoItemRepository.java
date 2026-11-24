package com.dressurban.catalogo.repository;

import com.dressurban.catalogo.model.PedidoItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PedidoItemRepository extends JpaRepository<PedidoItem, Long> {

    boolean existsByVarianteId(Long varianteId);

    boolean existsByVarianteProductoId(Long productoId);
}