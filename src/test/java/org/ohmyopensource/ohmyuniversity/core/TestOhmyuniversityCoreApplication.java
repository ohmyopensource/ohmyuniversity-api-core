package org.ohmyopensource.ohmyuniversity.core;

import org.springframework.boot.SpringApplication;

public class TestOhmyuniversityCoreApplication {

	public static void main(String[] args) {
		SpringApplication.from(OhmyuniversityCoreApplication::main)
				.with(TestcontainersConfiguration.class)
				.run(args);
	}

}
