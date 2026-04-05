package com.storygenerator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class StoryGeneratorApplication {

    public static void main(String[] args) {
        SpringApplication.run(StoryGeneratorApplication.class, args);
        System.out.println("\n✨ Intelligent Story Generator is running!");
        System.out.println("🌐 Open http://localhost:8080 in your browser\n");
    }
}
