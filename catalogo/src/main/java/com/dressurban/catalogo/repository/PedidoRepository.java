package com.dressurban.catalogo.repository;

import com.dressurban.catalogo.model.EstadoPedido;
import com.dressurban.catalogo.model.Pedido;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    Page<Pedido> findAllByOrderByFechaDescIdDesc(Pageable pageable);

    Page<Pedido> findByEstadoOrderByFechaDescIdDesc(EstadoPedido estado, Pageable pageable);

    long countByEstado(EstadoPedido estado);

    // Cambio ATÓMICO: solo se aplica si el pedido sigue en el estado "actual".
    // Devuelve 1 si cambió y 0 si alguien lo cambió antes.
    @Modifying(flushAutomatically = true)
    @Query("update Pedido p set p.estado = :nuevo where p.id = :id and p.estado = :actual")
    int cambiarEstado(@Param("id") Long id,
                      @Param("actual") EstadoPedido actual,
                      @Param("nuevo") EstadoPedido nuevo);

    @Query("select p.id from Pedido p where p.estado = :estado and p.fecha < :limite order by p.id")
    List<Long> idsAnterioresA(@Param("estado") EstadoPedido estado,
                              @Param("limite") LocalDateTime limite,
                              Pageable pageable);
}