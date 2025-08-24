
package com.test;

import org.springframework.web.bind.annotation.*;

@RestController
public class HelloWorldController {
	@GetMapping("/hello")
	public String hello() {
		return "Hello from Spring Boot on Cloud Run!";
	}
}
