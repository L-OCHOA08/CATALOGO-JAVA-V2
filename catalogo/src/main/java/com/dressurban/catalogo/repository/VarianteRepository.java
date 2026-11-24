package com.dressurban.catalogo.repository;

import com.dressurban.catalogo.model.Variante;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VarianteRepository extends JpaRepository<Variante, Long> {

    List<Variante> findByProductoIdOrderByTalleAscColorAsc(Long productoId);

    // Descuento ATÓMICO: la condición "stock >= n" va dentro del propio UPDATE.
    // Devuelve 1 si descontó y 0 si no alcanzaba el stock.
    @Modifying(flushAutomatically = true)
    @Query("update Variante v set v.stock = v.stock - :n where v.id = :id and v.stock >= :n")
    int descontar(@Param("id") Long id, @Param("n") int n);

    @Modifying(flushAutomatically = true)
    @Query("update Variante v set v.stock = v.stock + :n where v.id = :id")
    int sumar(@Param("id") Long id, @Param("n") int n);

    // Lee el stock real desde la base (no desde la memoria de Hibernate)
    @Query("select v.stock from Variante v where v.id = :id")
    int stockActual(@Param("id") Long id);

    boolean existsByProductoIdAndTalleIgnoreCaseAndColorIgnoreCase(
            Long productoId, String talle, String color);

    // Stock total por producto (solo variantes activas): cada fila es [productoId, suma]
    @Query("select v.producto.id, sum(v.stock) from Variante v "
            + "where v.activo = true group by v.producto.id")
    List<Object[]> stockPorProducto();

    @Query("select v from Variante v join fetch v.producto p "
            + "where v.activo = true and p.activo = true and v.stock > 0 "
            + "order by p.nombre, v.talle, v.color")
    List<Variante> disponibles();
}