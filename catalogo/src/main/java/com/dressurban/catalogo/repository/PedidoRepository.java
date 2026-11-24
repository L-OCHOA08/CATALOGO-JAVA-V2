package com.dressurban.catalogo.repository;

import com.dressurban.catalogo.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {
}