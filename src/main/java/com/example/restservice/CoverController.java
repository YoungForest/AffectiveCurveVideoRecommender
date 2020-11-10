package com.example.restservice;

import java.util.concurrent.atomic.AtomicLong;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.core.io.FileSystemResource;
import java.io.File;

@RestController
public class CoverController {
	final String coverRoot = "/Users/yngsen/workspace/bishe-spider/douyin/";

	@GetMapping("/cover")
	public FileSystemResource greeting(@RequestParam(value = "name") String name) {
		File file = new File(coverRoot + name);
		return new FileSystemResource(file);
	}
}
