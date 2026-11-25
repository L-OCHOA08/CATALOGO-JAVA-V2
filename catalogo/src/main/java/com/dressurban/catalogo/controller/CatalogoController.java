package com.dressurban.catalogo.controller;

import com.dressurban.catalogo.model.Categoria;
import com.dressurban.catalogo.model.Producto;
import com.dressurban.catalogo.repository.CategoriaRepository;
import com.dressurban.catalogo.repository.ProductoRepository;
import com.dressurban.catalogo.repository.VarianteRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@Controller
public class CatalogoController {

    private final ProductoRepository productos;
    private final CategoriaRepository categorias;
    private final VarianteRepository variantes;

    public CatalogoController(ProductoRepository productos, CategoriaRepository categorias,
                              VarianteRepository variantes) {
        this.productos = productos;
        this.categorias = categorias;
        this.variantes = variantes;
    }

    private Map<Long, Long> stockPorProducto() {
        Map<Long, Long> mapa = new HashMap<>();
        for (Object[] fila : variantes.stockPorProducto()) {
            mapa.put(((Number) fila[0]).longValue(), ((Number) fila[1]).longValue());
        }
        return mapa;
    }

    @GetMapping("/")
    public String inicio(@RequestParam(defaultValue = "0") int pagina, Model model) {
        Page<Producto> page = productos.findByActivoTrue(
                PageRequest.of(pagina, 12, Sort.by("nombre")));
        model.addAttribute("productos", page);
        model.addAttribute("categorias", categorias.findAll());
        model.addAttribute("stockPorProducto", stockPorProducto());
        return "catalogo";
    }

    @GetMapping("/categoria/{slug}")
    public String porCategoria(@PathVariable String slug,
                               @RequestParam(defaultValue = "0") int pagina, Model model) {
        Categoria cat = categorias.findBySlug(slug)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        Page<Producto> page = productos.findByCategoriaIdAndActivoTrue(
                cat.getId(), PageRequest.of(pagina, 12, Sort.by("nombre")));
        model.addAttribute("productos", page);
        model.addAttribute("categorias", categorias.findAll());
        model.addAttribute("categoriaActual", cat);
        model.addAttribute("stockPorProducto", stockPorProducto());
        return "catalogo";
    }
}