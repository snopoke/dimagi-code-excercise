package com.dimagi;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Main {

    private File props;
    private Stage stage;
    private String host;
    private int port;
    private final String name = "Whereami";

    public Main(String[] args) throws IOException,
            Exception {
        parseArgs(args);
    }

    public static void main(String[] args) throws Exception {
        new Main(args).run();
    }

    private void parseArgs(String[] args) throws IOException, Exception {
        OptionParser parser = new OptionParser();

        OptionSpec<Stage> stageOpt = parser
                .accepts("stage", "PRODUCTION or DEVELOPMENT")
                .withRequiredArg().ofType(Stage.class)
                .defaultsTo(Stage.PRODUCTION);

        /*OptionSpec<String> hostOpt = parser
                .accepts("host", "Address to bind to.").withRequiredArg()
                .ofType(String.class).defaultsTo("127.0.0.1");

        OptionSpec<Integer> portOpt = parser
                .accepts("port", "Port to bind to.").withRequiredArg()
                .ofType(Integer.class).defaultsTo(9000);*/

        OptionSpec<File> propsOpt = parser
                .accepts("props", "Path to the properties file.")
                .withRequiredArg().ofType(File.class)
                .defaultsTo(new File("whereami.properties"));

        parser.acceptsAll(Arrays.asList("h", "?"), "show help").forHelp();

        try {
            OptionSet options = parser.parse(args);

            if (options.has("h")) {
                printHelpAndExit(parser);
            }

            stage = stageOpt.value(options);
//            host = hostOpt.value(options);
            //port = portOpt.value(options);/
            props = propsOpt.value(options);
        } catch (OptionException e) {
            e.printStackTrace();
            printHelpAndExit(parser);
        }
    }

    public void run() throws Exception {
        System.out.printf("Starting %s in %s mode at %s:%d ...\n", name, stage,
                host, port);
        startService();
        System.out.printf("%s successfully started.\n", name);
    }

    private void startService() throws Exception {
        Injector injector = Guice.createInjector(stage,
                new WhereamiModule(props));

        WhereamiByMail service = injector.getInstance(WhereamiByMail.class);

        ScheduledThreadPoolExecutor executor = (ScheduledThreadPoolExecutor) injector.getInstance(ExecutorService.class);
        executor.scheduleAtFixedRate(service, 2, 60, TimeUnit.SECONDS);
    }

    private static void printHelpAndExit(OptionParser parser) throws IOException {
        parser.printHelpOn(System.out);
        System.exit(0);
    }
}
