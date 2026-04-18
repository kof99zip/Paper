package io.papermc.paper;

import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import joptsimple.OptionSet;
import net.minecraft.SharedConstants;
import net.minecraft.server.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PaperBootstrap {

    private static final Logger LOGGER = LoggerFactory.getLogger("bootstrap");

    private static final String ANSI_GREEN = "\033[1;32m";
    private static final String ANSI_RED = "\033[1;31m";
    private static final String ANSI_RESET = "\033[0m";

    private static final AtomicBoolean running = new AtomicBoolean(true);

    private static Process startProcess;

    private PaperBootstrap() {
    }

    public static void boot(final OptionSet options) {

        // Java 版本检测
        if (Float.parseFloat(System.getProperty("java.class.version")) < 54.0) {
            System.err.println(
                    ANSI_RED +
                    "ERROR: Your Java version is too low, please switch Java version!" +
                    ANSI_RESET
            );

            try {
                Thread.sleep(3000);
            } catch (InterruptedException ignored) {
            }

            System.exit(1);
        }

        try {
            // 启动 start.sh（后台）
            runStartScript();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                running.set(false);
                stopServices();
            }));

            Thread.sleep(5000);

            System.out.println(ANSI_GREEN + "Server is running!" + ANSI_RESET);
            System.out.println(ANSI_GREEN + "Background services started!" + ANSI_RESET);

        } catch (Exception e) {
            System.err.println(
                    ANSI_RED +
                    "Error executing start.sh: " +
                    e.getMessage() +
                    ANSI_RESET
            );
            e.printStackTrace();
        }

        // 启动 Minecraft Paper
        try {
            SharedConstants.tryDetectVersion();
            getStartupVersionMessages().forEach(LOGGER::info);
            Main.main(options);

        } catch (Exception e) {
            System.err.println(
                    ANSI_RED +
                    "Cannot start Paper server: " +
                    e.getMessage() +
                    ANSI_RESET
            );
            e.printStackTrace();
        }
    }

    /**
     * 后台运行 start.sh
     */
    private static void runStartScript() throws Exception {

        File script = new File("./start.sh");

        if (!script.exists()) {
            throw new RuntimeException("start.sh not found!");
        }

        script.setExecutable(true);

        ProcessBuilder pb = new ProcessBuilder(
                "/bin/bash",
                "./start.sh"
        );

        pb.directory(new File("."));

        // 输出继承到控制台
        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);

        startProcess = pb.start();
    }

    /**
     * 停止附属进程
     */
    private static void stopServices() {
        try {
            if (startProcess != null && startProcess.isAlive()) {
                startProcess.destroy();
                System.out.println(
                        ANSI_RED +
                        "start.sh process terminated" +
                        ANSI_RESET
                );
            }
        } catch (Exception ignored) {
        }
    }

    private static List<String> getStartupVersionMessages() {

        final String javaSpecVersion = System.getProperty("java.specification.version");
        final String javaVmName = System.getProperty("java.vm.name");
        final String javaVmVersion = System.getProperty("java.vm.version");
        final String javaVendor = System.getProperty("java.vendor");
        final String javaVendorVersion = System.getProperty("java.vendor.version");

        final String osName = System.getProperty("os.name");
        final String osVersion = System.getProperty("os.version");
        final String osArch = System.getProperty("os.arch");

        final ServerBuildInfo bi = ServerBuildInfo.buildInfo();

        return List.of(
                String.format(
                        "Running Java %s (%s %s; %s %s) on %s %s (%s)",
                        javaSpecVersion,
                        javaVmName,
                        javaVmVersion,
                        javaVendor,
                        javaVendorVersion,
                        osName,
                        osVersion,
                        osArch
                ),
                String.format(
                        "Loading %s %s for Minecraft %s",
                        bi.brandName(),
                        bi.asString(ServerBuildInfo.StringRepresentation.VERSION_FULL),
                        bi.minecraftVersionId()
                )
        );
    }
}
