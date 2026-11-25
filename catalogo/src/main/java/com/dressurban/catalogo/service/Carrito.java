package com.dressurban.catalogo.service;

import jakarta.servlet.http.HttpSession;

import java.io.Serializable;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class Carrito implements Serializable {

    public static final int MAX_POR_LINEA = 10;
    public static final int MAX_LINEAS = 20;
    private static final String CLAVE = "carrito";

    // varianteId -> cantidad
    private final Map<Long, Integer> lineas = new LinkedHashMap<>();

    public static Carrito de(HttpSession session) {
        Carrito c = (Carrito) session.getAttribute(CLAVE);
        if (c == null) {
            c = new Carrito();
            session.setAttribute(CLAVE, c);
        }
        return c;
    }

    /** Para el contador del encabezado: no crea sesión si no existe. */
    public static int cantidadEn(HttpSession session) {
        if (session == null) {
            return 0;
        }
        Object c = session.getAttribute(CLAVE);
        return c instanceof Carrito ? ((Carrito) c).cantidadTotal() : 0;
    }

    public void agregar(Long varianteId, int cantidad) {
        if (!lineas.containsKey(varianteId) && lineas.size() >= MAX_LINEAS) {
            throw new IllegalArgumentException(
                    "El carrito admite hasta " + MAX_LINEAS + " prendas distintas");
        }
        int nueva = lineas.getOrDefault(varianteId, 0) + cantidad;
        if (nueva > MAX_POR_LINEA) {
            throw new IllegalArgumentException(
                    "Podés pedir hasta " + MAX_POR_LINEA + " unidades de cada prenda por pedido");
        }
        lineas.put(varianteId, nueva);
    }

    public void cambiar(Long varianteId, int cantidad) {
        if (!lineas.containsKey(varianteId)) {
            return;
        }
        if (cantidad <= 0) {
            lineas.remove(varianteId);
            return;
        }
        if (cantidad > MAX_POR_LINEA) {
            throw new IllegalArgumentException(
                    "Podés pedir hasta " + MAX_POR_LINEA + " unidades de cada prenda por pedido");
        }
        lineas.put(varianteId, cantidad);
    }

    public void quitar(Long varianteId) {
        lineas.remove(varianteId);
    }

    public void vaciar() {
        lineas.clear();
    }

    public boolean vacio() {
        return lineas.isEmpty();
    }

    public int cantidadTotal() {
        return lineas.values().stream().mapToInt(Integer::intValue).sum();
    }

    public Map<Long, Integer> getLineas() {
        return Collections.unmodifiableMap(lineas);
    }
}