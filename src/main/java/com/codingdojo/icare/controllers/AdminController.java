package com.codingdojo.icare.controllers;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpSession;
import javax.servlet.jsp.PageContext;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.codingdojo.icare.models.Order;
import com.codingdojo.icare.models.Product;
import com.codingdojo.icare.models.User;
import com.codingdojo.icare.requests.FileUploadUtil;
import com.codingdojo.icare.services.OrderService;
import com.codingdojo.icare.services.ProductService;
import com.codingdojo.icare.services.UserService;


import javax.imageio.ImageIO;
import javax.servlet.ServletContext;


@Controller
public class AdminController {
	
	@Autowired
	private UserService userService;
	
	@Autowired
	private ProductService productService;
	
	@Autowired
	private OrderService orderService;
	
	//needed to upload imgs
	@Autowired
	ServletContext servletContext;
	
	@GetMapping("/admin")
	public String adminHome(Model model, HttpSession session, RedirectAttributes redirectAttributes) {
		if( !session.getAttribute("role").equals("admin")) {
			redirectAttributes.addFlashAttribute("error", "Must be authorized first");
			return "redirect:/";
		}
		List<Product> products = productService.findAllProduct();
		List<Order> orders = orderService.findAllOrder();
		Long id= (Long) session.getAttribute("user_id");
		User user = userService.findUser(id);
		model.addAttribute("user",user);
		model.addAttribute("products",products);
		model.addAttribute("orders",orders);
		
		return "adminHome.jsp";
	}
	
	// product create product form
	@GetMapping("/product/new")
	public String addProductForm(Model model) {
		if(!model.containsAttribute("product")) {
			model.addAttribute("product", new Product());
		}
		
		return"adminNewProduct.jsp";
	}
	
	// create a product
	@PostMapping(value="/product")
	public String add(Model model, @Valid @ModelAttribute("product") Product product, 
			BindingResult result, 
			RedirectAttributes redirectAttributes,
			HttpSession session
			) throws IOException {
		if (result.hasErrors()) {
			redirectAttributes.addFlashAttribute("product",product);
			redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.product",result);
			return "redirect:/product/new";
        } 
		
		
//		String PROFILE_UPLOAD_LOCATION = servletContext.getRealPath("/")
//				+ "resources" + File.separator
//				+ "images" + File.separator;
//
//		System.out.println("PROFILE_UPLOAD_LOCATION===================");
//		System.out.println(PROFILE_UPLOAD_LOCATION);
//
//		BufferedImage photo = ImageIO.read(new ByteArrayInputStream(product
//				.getProductImg().getBytes()));
//		System.out.print(product
//				.getProductImg());
//		File destination = new File(PROFILE_UPLOAD_LOCATION
//				+ product.getId() + "_photo" + ".png");
//		ImageIO.write(photo, "png", destination);
		
		// list to store full paths
		List<String> pathImgs = new ArrayList<String>();

		product = productService.createProduct(product);

		// loop on the list of imgs
		for (MultipartFile img: product.getProductImg()) {
			
			//store filename
			String fileName = StringUtils.cleanPath(img.getOriginalFilename());
			
			//add filename to list of paths which will be stored in db
			pathImgs.add(fileName);
			
			// create a path 
			String uploadDir = "photos/" + product.getId();
			
			// save the img on the above path
			FileUploadUtil.saveFile(uploadDir, fileName, img);
		}
		
		// save the paths in db
		product.setPhotos(pathImgs);
		product = productService.save(product);

    	redirectAttributes.addFlashAttribute("success", "product was created successfully");
        return "redirect:/admin";
	}
	
	@GetMapping("products/{id}")
	public String product(@PathVariable(value="id") Long id, Model model, HttpSession session, 
			RedirectAttributes redirectAttributes) throws URISyntaxException {
		
		// if user did not register or logged in 
		if(session.getAttribute("user_id") == null) {
			redirectAttributes.addFlashAttribute("error", "you need to login/register first");
			return "redirect:/";
		}	
		Product product = productService.findProduct(id);
		model.addAttribute("product", product);
		return "/view_product.jsp";
		}

}
