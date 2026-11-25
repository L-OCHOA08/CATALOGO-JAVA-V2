package com.dressurban.catalogo.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.function.BooleanSupplier;

public class ModuloInterceptor implements HandlerInterceptor {

    private final BooleanSupplier habilitado;

    public ModuloInterceptor(BooleanSupplier habilitado) {
        this.habilitado = habilitado;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws IOException {
        if (habilitado.getAsBoolean()) {
            return true;
        }
        response.sendError(HttpServletResponse.SC_NOT_FOUND);
        return false;
    }
}