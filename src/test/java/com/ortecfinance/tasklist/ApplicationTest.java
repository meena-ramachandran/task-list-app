package com.ortecfinance.tasklist;

import com.ortecfinance.tasklist.service.ProjectService;
import com.ortecfinance.tasklist.store.ProjectStore;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.web.servlet.MockMvc;

import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static java.lang.System.lineSeparator;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

@SpringBootTest
@AutoConfigureMockMvc
public final class ApplicationTest {
    public static final String PROMPT = "> ";
    @Autowired
    private ConfigurableApplicationContext context;

    @Autowired
    private ProjectStore store;

    private PipedOutputStream inStream;
    private PrintWriter inWriter;
    private PipedInputStream outStream;
    private BufferedReader outReader;
    private Thread applicationThread;

    @BeforeEach
    public void start_the_application() throws IOException {
        store.clear();
        inStream = new PipedOutputStream();
        inWriter = new PrintWriter(inStream, true);
        outStream = new PipedInputStream();
        outReader = new BufferedReader(new InputStreamReader(outStream));

        BufferedReader in = new BufferedReader(new InputStreamReader(new PipedInputStream(inStream)));
        PrintWriter out = new PrintWriter(new PipedOutputStream(outStream), true);

        ProjectService projectService = context.getBean(ProjectService.class);

        TaskList taskList = new TaskList(in, out, projectService);
        applicationThread = new Thread(taskList);
        applicationThread.start();
        readLines("Welcome to TaskList! Type 'help' for available commands.");
    }

    @AfterEach
    public void kill_the_application() throws IOException, InterruptedException {
        if (!stillRunning()) {
            return;
        }

        Thread.sleep(1000);
        if (!stillRunning()) {
            return;
        }

        applicationThread.interrupt();
        throw new IllegalStateException("The application is still running.");
    }

    @Test
    void it_works() throws IOException {
        execute("show");

        execute("add project secrets");
        execute("add task secrets Eat more donuts.");
        execute("add task secrets Destroy all humans.");

        execute("show");
        readLines(
            "secrets",
            "    [ ] 1: Eat more donuts.",
            "    [ ] 2: Destroy all humans.",
            ""
        );

        execute("add project training");
        execute("add task training Four Elements of Simple Design");
        execute("add task training SOLID");
        execute("add task training Coupling and Cohesion");
        execute("add task training Primitive Obsession");
        execute("add task training Outside-In TDD");
        execute("add task training Interaction-Driven Design");

        execute("check 1");
        execute("check 3");
        execute("check 5");
        execute("check 6");

        execute("show");
        readLines(
                "secrets",
                "    [x] 1: Eat more donuts.",
                "    [ ] 2: Destroy all humans.",
                "",
                "training",
                "    [x] 3: Four Elements of Simple Design",
                "    [ ] 4: SOLID",
                "    [x] 5: Coupling and Cohesion",
                "    [x] 6: Primitive Obsession",
                "    [ ] 7: Outside-In TDD",
                "    [ ] 8: Interaction-Driven Design",
                ""
        );

        execute("quit");
    }

    @Test
    void it_can_add_deadlines() throws IOException {

        execute("add project training");
        execute("add task training Four Elements of Simple Design");
        execute("add task training SOLID");
        execute("add task training Coupling and Cohesion");

        execute("deadline 1 25-11-2024");
        execute("deadline 3 30-11-2024");

        execute("show");

        readLines(
                "training",
                "    [ ] 1: Four Elements of Simple Design (25-11-2024)",
                "    [ ] 2: SOLID",
                "    [ ] 3: Coupling and Cohesion (30-11-2024)",
                ""
        );

        execute("quit");
    }

    @Test
    void deadline_for_unknown_task() throws IOException {

        execute("add project training");
        execute("add task training SOLID");

        execute("deadline 99 25-11-2024");

        readLines(
                "Could not find a task with an ID of 99."
        );

        execute("quit");
    }

    @Test
    void it_can_show_tasks_due_today() throws IOException {
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));

        execute("add project training");
        execute("add task training Four Elements of Simple Design");
        execute("add task training SOLID");
        execute("deadline 1 " + today);

        execute("today");

        readLines(
                "training",
                "    [ ] 1: Four Elements of Simple Design",
                ""
        );

        execute("quit");
    }

    @Test
    void it_shows_an_error_for_an_invalid_date_format() throws IOException {
        execute("add project training");
        execute("add task training SOLID");
        execute("deadline 1 2024-11-25");
        readLines(
                "Please enter the deadline in the format: dd-MM-yyyy"
        );
        execute("quit");
    }

    @Test
    void it_can_view_tasks_grouped_by_deadline_and_project() throws IOException {

        execute("add project secrets");
        execute("add task secrets Eat more donuts.");
        execute("add task secrets Destroy all humans.");

        execute("add project training");
        execute("add task training Four Elements of Simple Design");
        execute("add task training Interaction-Driven Design");

        execute("deadline 1 25-11-2024");
        execute("deadline 3 25-11-2024");
        execute("deadline 4 30-11-2024");

        execute("view-by-deadline");

        readLines(
                "25-11-2024:",
                "    secrets",
                "        1: Eat more donuts.",
                "    training",
                "        3: Four Elements of Simple Design",
                "30-11-2024:",
                "    training",
                "        4: Interaction-Driven Design",
                "No deadline:",
                "    secrets",
                "        2: Destroy all humans."
        );

        execute("quit");
    }

    @Test
    void rejectsEmptyProjectAndTaskDetails() throws IOException {
        execute("add project");
        readLines("Project/Task details cannot be empty.");

        execute("add project secrets");

        execute("add task secrets ");
        readLines("Task description cannot be empty.");

        execute("quit");
    }

    @Test
    void canRemoveDeadline() throws IOException {
        execute("add project training");
        execute("add task training SOLID");

        execute("deadline 1 25-11-2024");
        execute("show");
        readLines(
                "training",
                "    [ ] 1: SOLID (25-11-2024)",
                ""
        );

        // Remove deadline
        execute("deadline 1 null");
        execute("show");
        readLines(
                "training",
                "    [ ] 1: SOLID",
                ""
        );

        execute("quit");
    }

    private void execute(String command) throws IOException {
        read(PROMPT);
        write(command);
    }

    private void read(String expectedOutput) throws IOException {
        int length = expectedOutput.length();
        char[] buffer = new char[length];
        outReader.read(buffer, 0, length);
        assertThat(String.valueOf(buffer), is(expectedOutput));
    }

    private void readLines(String... expectedOutput) throws IOException {
        for (String line : expectedOutput) {
            read(line + lineSeparator());
        }
    }

    private void write(String input) {
        inWriter.println(input);
    }

    private boolean stillRunning() {
        return applicationThread != null && applicationThread.isAlive();
    }
}
