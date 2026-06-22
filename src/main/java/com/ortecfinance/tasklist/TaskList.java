package com.ortecfinance.tasklist;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

public final class TaskList implements Runnable {
    private static final String QUIT = "quit";

    private final Map<String, List<Task>> tasks = new LinkedHashMap<>();
    private final BufferedReader in;
    private final PrintWriter out;

    private long lastId = 0;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    public static void startConsole() {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        PrintWriter out = new PrintWriter(System.out);
        new TaskList(in, out).run();
    }

    public TaskList(BufferedReader reader, PrintWriter writer) {
        this.in = reader;
        this.out = writer;
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
        for (Map.Entry<String, List<Task>> project : tasks.entrySet()) {
            out.println(project.getKey());
            for (Task task : project.getValue()) {
                String deadlineText = "";
                if (task.getDeadline() != null) {
                    deadlineText = " (" + task.getDeadline().format(FORMATTER) + ")";
                }
                out.printf("    [%c] %d: %s%s%n", (task.isDone() ? 'x' : ' '), task.getId(), task.getDescription(), deadlineText);
            }
            out.println();
        }
    }

    private void add(String commandLine) {
        String[] subcommandRest = commandLine.split(" ", 2);
        String subcommand = subcommandRest[0];
        if (subcommand.equals("project")) {
            addProject(subcommandRest[1]);
        } else if (subcommand.equals("task")) {
            String[] projectTask = subcommandRest[1].split(" ", 2);
            addTask(projectTask[0], projectTask[1]);
        }
    }

    private void addProject(String name) {
        tasks.put(name, new ArrayList<Task>());
    }

    private void addTask(String project, String description) {
        List<Task> projectTasks = tasks.get(project);
        if (projectTasks == null) {
            out.printf("Could not find a project with the name \"%s\".", project);
            out.println();
            return;
        }
        projectTasks.add(new Task(nextId(), description, false, null));
    }

    private void check(String idString) {
        setDone(idString, true);
    }

    private void uncheck(String idString) {
        setDone(idString, false);
    }

    private void setDone(String idString, boolean done) {
        int id = Integer.parseInt(idString);
        for (Map.Entry<String, List<Task>> project : tasks.entrySet()) {
            for (Task task : project.getValue()) {
                if (task.getId() == id) {
                    task.setDone(done);
                    return;
                }
            }
        }
        out.printf("Could not find a task with an ID of %d.", id);
        out.println();
    }

    private void deadline(String commandLine) {
        String[] parts = commandLine.split(" ", 2);
        int id = Integer.parseInt(parts[0]);
        try {
            LocalDate deadline = LocalDate.parse(parts[1], FORMATTER);
            for (Map.Entry<String, List<Task>> project : tasks.entrySet()) {
                for (Task task : project.getValue()) {
                    if (task.getId() == id) {
                        task.setDeadline(deadline);
                        return;
                    }
                }
            }
        }catch (DateTimeParseException e) {
            out.printf("Please enter the deadline in the format: dd-MM-yyyy");
            out.println();
            return;
        }

        out.printf("Could not find a task with an ID of %d.", id);
        out.println();
    }

    public void viewByDeadline(){
        Map<Optional<LocalDate>, Map<String, List<Task>>> groupedTasks = new TreeMap<>(
                Comparator.comparing(d -> d.orElse(LocalDate.MAX))
        );

        for (Map.Entry<String, List<Task>> projectEntry : tasks.entrySet()) {
            String project = projectEntry.getKey();
            for (Task task : projectEntry.getValue()) {
                groupedTasks.computeIfAbsent(Optional.ofNullable(task.getDeadline()), d -> new LinkedHashMap<>()).computeIfAbsent(project, p -> new ArrayList<>()).add(task);
            }
        }

        for (Map.Entry<Optional<LocalDate>, Map<String, List<Task>>> deadlineEntry : groupedTasks.entrySet()) {
            String label = deadlineEntry.getKey()
                    .map(d -> d.format(FORMATTER) + ":")
                    .orElse("No deadline:");
            out.println(label);
            for (Map.Entry<String, List<Task>> project : deadlineEntry.getValue().entrySet()) {
                out.println("    " + project.getKey());
                for (Task task : project.getValue()) {
                    out.printf("        %d: %s%n",  task.getId(), task.getDescription());
                }
            }
        }
    }

    private void today() {
        for (Map.Entry<String, List<Task>> project : tasks.entrySet()) {
            List<Task> todayTasks = new ArrayList<>();
            for (Task task : project.getValue()) {
                if (LocalDate.now().equals(task.getDeadline())) {
                    todayTasks.add(task);
                }
            }
            if (!todayTasks.isEmpty()) {
                out.println(project.getKey());
                for (Task task : todayTasks) {
                    out.printf("    [%c] %d: %s%n", task.isDone() ? 'x' : ' ', task.getId(), task.getDescription());
                }
                out.println();
            }
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

    private void error(String command) {
        out.printf("I don't know what the command \"%s\" is.", command);
        out.println();
    }

    private long nextId() {
        return ++lastId;
    }
}
