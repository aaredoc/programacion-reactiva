package com.bolsadeideas.springboot.webflux.app.controllers;

import java.time.Duration;

import com.bolsadeideas.springboot.webflux.app.models.services.ProductoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.thymeleaf.spring5.context.webflux.ReactiveDataDriverContextVariable;

import com.bolsadeideas.springboot.webflux.app.models.dao.ProductoDao;
import com.bolsadeideas.springboot.webflux.app.models.documents.Producto;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.validation.Valid;

@SessionAttributes("producto")
@Controller
public class ProductoController {

	@Autowired
	private ProductoService productoService;
	
	private static final Logger log = LoggerFactory.getLogger(ProductoController.class);
	
	@GetMapping({"/listar", "/"})
	public  Mono<String> listar(Model model) {
		
		Flux<Producto> productos = productoService.findAllConNombreUpperCase();
		
		productos.subscribe(prod -> log.info(prod.getNombre()));
		
		model.addAttribute("productos", productos);
		model.addAttribute("titulo", "Listado de productos");
		return Mono.just("listar");
	}

	@GetMapping("/form")
	public Mono<String> crear(Model model){
		model.addAttribute("producto", new Producto());
		model.addAttribute("boton", "crear");
		model.addAttribute("titulo", "Formulario de producto");
		return Mono.just("form");
	}

	@GetMapping("/formv2/{id}")
	public Mono<String> editarv2(@PathVariable String id, Model model){
		return productoService.findById(id)
				.doOnNext(producto -> {
					log.info(producto.getNombre());
					model.addAttribute("titulo", "Editar Producto");
					model.addAttribute("producto", producto);
					model.addAttribute("boton", "editar");
				})
				.defaultIfEmpty(new Producto())
				.flatMap(producto -> {
					if(producto.getId() == null){
						return Mono.error(new InterruptedException("No existe el producto"));
					}
					return Mono.just(producto);
				})
				.thenReturn("form")
				.onErrorResume(exception -> Mono.just("redirect:/listar?error=no+existe+el+producto"));
	}

	@GetMapping("/form/{id}")
	public Mono<String> editar(@PathVariable String id, Model model){
		Mono<Producto> productoMono = productoService.findById(id)
				.doOnNext(producto -> log.info(producto.getNombre())).defaultIfEmpty(new Producto());

		model.addAttribute("titulo", "Editar Producto");
		model.addAttribute("producto", productoMono);
		model.addAttribute("boton", "editar");
		return Mono.just("form");
	}

	@PostMapping("/form")
	public Mono<String> guardar(@Valid Producto producto, BindingResult result, SessionStatus sessionStatus, Model model){
		if(result.hasErrors()){
			model.addAttribute("titulo", "Editar Producto");
			model.addAttribute("boton", "editar");
			return Mono.just("form");
		}
		sessionStatus.setComplete();
		return productoService.save(producto).doOnNext(prod -> log.info("Producto guardado: {} {}", prod.getId(), prod.getNombre()))
				.thenReturn("redirect:/listar");
	}
	
	@GetMapping("/listar-datadriver")
	public String listarDataDriver(Model model) {
		
		Flux<Producto> productos = productoService.findAllConNombreUpperCase().delayElements(Duration.ofSeconds(1));
		
		productos.subscribe(prod -> log.info(prod.getNombre()));
		
		model.addAttribute("productos", new ReactiveDataDriverContextVariable(productos, 1));
		model.addAttribute("titulo", "Listado de productos");
		return "listar";
	}
	
	@GetMapping("/listar-full")
	public String listarFull(Model model) {
		
		Flux<Producto> productos = productoService.findAllConNombreUpperCaseRepeat();
		
		model.addAttribute("productos", productos);
		model.addAttribute("titulo", "Listado de productos");
		return "listar";
	}
	
	@GetMapping("/listar-chunked")
	public String listarChunked(Model model) {
		
		Flux<Producto> productos = productoService.findAllConNombreUpperCaseRepeat();
		
		model.addAttribute("productos", productos);
		model.addAttribute("titulo", "Listado de productos");
		return "listar-chunked";
	}
}
