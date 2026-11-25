package com.dressurban.catalogo.config;

import com.dressurban.catalogo.service.ImagenStorageService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final ImagenStorageService storage;
    private final Modulos modulos;

    public WebConfig(ImagenStorageService storage, Modulos modulos) {
        this.storage = storage;
        this.modulos = modulos;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String ubicacion = storage.getCarpeta().toUri().toString();
        if (!ubicacion.endsWith("/")) {
            ubicacion += "/";
        }
        registry.addResourceHandler("/uploads/**").addResourceLocations(ubicacion);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Módulo de stock (planes CONTROL y TIENDA)
        registry.addInterceptor(new ModuloInterceptor(modulos::isStock))
                .addPathPatterns("/admin/variantes/**",
                        "/admin/productos/*/variantes/**",
                        "/admin/pedidos/**",
                        "/admin/stock-bajo/**");

        // Módulo de tienda online (plan TIENDA)
        registry.addInterceptor(new ModuloInterceptor(modulos::isTienda))
                .addPathPatterns("/producto/**", "/carrito/**", "/checkout/**", "/pedido/**");

        // Extra: reporte de ventas
        registry.addInterceptor(new ModuloInterceptor(modulos::isReportes))
                .addPathPatterns("/admin/reportes/**");
    }
}