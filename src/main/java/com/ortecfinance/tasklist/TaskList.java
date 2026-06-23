package com.ortecfinance.tasklist;

import com.ortecfinance.tasklist.domain.Project;
import com.ortecfinance.tasklist.domain.Task;
import com.ortecfinance.tasklist.service.ProjectService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

public final class TaskList implements Runnable {
    private static final String QUIT = "quit";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final BufferedReader in;
    private final PrintWriter out;
    private final ProjectService projectService;

    public TaskList(BufferedReader reader, PrintWriter writer, ProjectService projectService) {
        this.in = reader;
        this.out = writer;
        this.projectService = projectService;
    }

    public void run() {
        out.println("Welcome to TaskList! Type 'help' for available commands.");
        while (true) {
            out.print("> ");
            out.flush();
            String command;
            try {
                command = in.readLine();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (command.equals(QUIT)) {
                break;
            }
            execute(command);
        }
    }

    private void execute(String commandLine) {
        String[] commandRest = commandLine.split(" ", 2);
        String command = commandRest[0];
        switch (command) {
            case "show":
                show();
                break;
            case "add":
                add(commandRest[1]);
                break;
            case "check":
                check(commandRest[1]);
                break;
            case "uncheck":
                uncheck(commandRest[1]);
                break;
            case "deadline":
                deadline(commandRest[1]);
                break;
            case "today":
                today();
                break;
            case "view-by-deadline":
                viewByDeadline();
                break;
            case "help":
                help();
                break;
            default:
                error(command);
                break;
        }
    }

    private void show() {
        for (Project project : projectService.getProjects()) {
            out.println(project.getName());
            for (Task task : project.getTasks()) {
                printTaskLineWithDeadline(task);
            }
            out.println();
        }
    }

    private void add(String commandLine) {
        String[] subcommandRest = commandLine.split(" ", 2);
        if (subcommandRest.length < 2 || subcommandRest[1].trim().isEmpty()) {
            out.println("Project/Task details cannot be empty.");
            return;
        }
        String subcommand = subcommandRest[0];
        String details = subcommandRest[1];

        if (subcommand.equals("project")) {
            // 2. Safely capture if the project name itself is blank (e.g., "add project  ")
            if (details.trim().isEmpty()) {
                out.println("Project name cannot be empty.");
                return;
            }
            try {
                projectService.addProject(details);
            } catch (Exception e) {
                    out.println(e.getMessage());
            }
        } else if (subcommand.equals("task")) {
            String[] projectTask = details.split(" ", 2);
            if (projectTask.length < 2 || projectTask[1].trim().isEmpty()) {
                out.println("Task description cannot be empty.");
                return;
            }
            try {
                projectService.addTask(projectTask[0], projectTask[1]);
            } catch (Exception e) {
                    out.println(e.getMessage());
            }
        }
    }

    private void check(String idString) {
        setDone(idString, true);
    }

    private void uncheck(String idString) {
        setDone(idString, false);
    }

    private void setDone(String idString, boolean done) {
        long id;
        try {
            id = Long.parseLong(idString);
        } catch (NumberFormatException e) {
            out.println("Invalid task ID format.");
            return;
        }

        try {
            projectService.setDone(id, done);
        } catch (Exception e) {
                out.println(e.getMessage());
        }
    }

    private void deadline(String commandLine) {
        String[] parts = commandLine.split(" ", 2);
        if (parts.length < 2) {
            out.println("Command format is: deadline <task ID> <date>");
            return;
        }
        long id;
        try {
            id = Long.parseLong(parts[0]);
        } catch (NumberFormatException e) {
            out.println("Invalid task ID.");
            return;
        }
        String val = parts[1].trim();
        LocalDate parsedDeadline = null;
        if (!val.equalsIgnoreCase("null") && !val.equals("-") && !val.isEmpty()) {
            try {
                parsedDeadline = LocalDate.parse(val, FORMATTER);
            } catch (DateTimeParseException e) {
                out.println("Please enter the deadline in the format: dd-MM-yyyy");
                return;
            }
        }
        try {
            projectService.setDeadline(id, parsedDeadline);
        } catch (Exception e) {
                out.println(e.getMessage());
        }
    }

    private void viewByDeadline() {
        Map<Optional<LocalDate>, Map<String, List<Task>>> grouped = projectService.getTasksGroupedByDeadline();

        for (Map.Entry<Optional<LocalDate>, Map<String, List<Task>>> entry : grouped.entrySet()) {
            String label = entry.getKey()
                    .map(d -> d.format(FORMATTER) + ":")
                    .orElse("No deadline:");
            out.println(label);
            for (Map.Entry<String, List<Task>> project : entry.getValue().entrySet()) {
                out.println("    " + project.getKey());
                for (Task task : project.getValue()) {
                    out.printf("        %d: %s%n", task.getId(), task.getDescription());
                }
            }
        }
    }

    private void today() {
        Map<String, List<Task>> todayTasks = projectService.getTasksForToday(LocalDate.now());
        for (Map.Entry<String, List<Task>> entry : todayTasks.entrySet()) {
            out.println(entry.getKey());
            for (Task task : entry.getValue()) {
                printTaskLineNoDeadline(task);
            }
            out.println();
        }
    }

    private void help() {
        out.println("Commands:");
        out.println("  show");
        out.println("  add project <project name>");
        out.println("  add task <project name> <task description>");
        out.println("  check <task ID>");
        out.println("  uncheck <task ID>");
        out.println("  deadline <task ID> <date(dd-MM-yyyy)>");
        out.println("  today");
        out.println("  view-by-deadline");
        out.println();
    }

    private void printTaskLineWithDeadline(Task task) {
        String deadlineText = "";
        if (task.getDeadline() != null) {
            deadlineText = " (" + task.getDeadline().format(FORMATTER) + ")";
        }
        out.printf("    [%c] %d: %s%s%n", task.isDone() ? 'x' : ' ', task.getId(), task.getDescription(), deadlineText);
    }

    private void printTaskLineNoDeadline(Task task) {
        out.printf("    [%c] %d: %s%n", task.isDone() ? 'x' : ' ', task.getId(), task.getDescription());
    }

    private void error(String command) {
        out.printf("I don't know what the command \"%s\" is.", command);
        out.println();
    }

}
