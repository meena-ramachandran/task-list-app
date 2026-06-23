package com.ortecfinance.tasklist;

import com.ortecfinance.tasklist.service.ProjectService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;

@SpringBootApplication
public class TaskListApplication {

    public static void main(String[] args ) {
            System.out.println("Starting console Application");
            ConfigurableApplicationContext context = SpringApplication.run(TaskListApplication.class);
            startConsole(context);
    }

    private static void startConsole(ConfigurableApplicationContext context) {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        PrintWriter out = new PrintWriter(System.out);

        ProjectService projectService = context.getBean(ProjectService.class);

        TaskList taskList = new TaskList(in, out, projectService);
        taskList.run();
    }

}
