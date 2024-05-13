/*
 * Copyright (c) 2022, Adam <Adam@sigterm.info>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.launcher;

import com.google.common.escape.Escaper;
import com.google.common.escape.Escapers;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import lombok.extern.slf4j.Slf4j;
import static net.runelite.launcher.Launcher.LAUNCHER_EXECUTABLE_NAME_OSX;
import static net.runelite.launcher.Launcher.LAUNCHER_EXECUTABLE_NAME_WIN;
import static net.runelite.launcher.Launcher.compareVersion;
import static net.runelite.launcher.Launcher.download;
import static net.runelite.launcher.Launcher.regQueryString;
import net.runelite.launcher.beans.Bootstrap;
import net.runelite.launcher.beans.Update;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@Slf4j
class Updater
{
    private static final String RUNELITE_APP = "/Applications/OpenRune.app";

    static void update(Bootstrap bootstrap, LauncherSettings launcherSettings, String[] args)
    {
        if (OS.getOs() == OS.OSType.Windows)
        {
            updateWindows(bootstrap, launcherSettings, args);
        }
        else if (OS.getOs() == OS.OSType.MacOS)
        {
            updateMacos(bootstrap, launcherSettings, args);
        }
    }

    private static void updateMacos(Bootstrap bootstrap, LauncherSettings launcherSettings, String[] args) {

        Optional<String> command = Optional.ofNullable(System.getProperty("sun.java.command"));
        if (!command.isPresent() || command.get().isEmpty()) {
            log.debug("Running process has no command");
            return;
        }

        Path path = Paths.get(command.get());

        // Adjust path for macOS specific behavior
        path = path.normalize()
                .resolveSibling(Paths.get("..", "MacOS", path.getFileName().toString()))
                .normalize();

        if (!path.getFileName().toString().equals(LAUNCHER_EXECUTABLE_NAME_OSX) || !path.startsWith(RUNELITE_APP)) {
            log.debug("Skipping update check due to not running from installer, command is {}", command.get());
            return;
        }

        log.debug("Running from installer");

        Update newestUpdate = findAvailableUpdate(bootstrap);
        if (newestUpdate == null) {
            return;
        }

        final boolean noupdate = launcherSettings.isNoupdates();
        if (noupdate) {
            log.info("Skipping update {} due to noupdate being set", newestUpdate.getVersion());
            return;
        }

        if (System.getenv("RUNELITE_UPGRADE") != null) {
            log.info("Skipping update {} due to launching from an upgrade", newestUpdate.getVersion());
            return;
        }

        LauncherSettings settings = LauncherSettings.loadSettings();
        int hours = 1 << Math.min(9, settings.lastUpdateAttemptNum); // 512 hours = ~21 days
        if (newestUpdate.getHash().equals(settings.lastUpdateHash) &&
                Instant.ofEpochMilli(settings.lastUpdateAttemptTime).isAfter(Instant.now().minus(hours, ChronoUnit.HOURS))) {
            log.info("Previous upgrade attempt to {} was at {} (backoff: {} hours), skipping", newestUpdate.getVersion(),
                    LocalTime.from(Instant.ofEpochMilli(settings.lastUpdateAttemptTime).atZone(ZoneId.systemDefault())),
                    hours);
            return;
        }

        // Check if rollout allows this update
        // There is no installer on macOS to write install_id, so just use random()
        if (newestUpdate.getRollout() > 0. && Math.random() > newestUpdate.getRollout()) {
            log.info("Skipping update {} due to rollout", newestUpdate.getVersion());
            return;
        }

        // From here and below the update will be attempted. Update settings early so a failed
        // download counts as an attempt.
        settings.lastUpdateAttemptTime = System.currentTimeMillis();
        settings.lastUpdateHash = newestUpdate.getHash();
        settings.lastUpdateAttemptNum++;
        LauncherSettings.saveSettings(settings);

        try {
            log.info("Downloading launcher {} from {}", newestUpdate.getVersion(), newestUpdate.getUrl());

            Path file = Files.createTempFile("rlupdate", "dmg");
            try (OutputStream fout = Files.newOutputStream(file)) {
                final String name = newestUpdate.getName();
                final int size = newestUpdate.getSize();
                try {
                    download(newestUpdate.getUrl(), newestUpdate.getHash(), (completed) ->
                                    SplashScreen.stage(.07, 1., null, name, completed, size, true),
                            fout);
                } catch (VerificationException e) {
                    log.error("Unable to verify update", e);
                    Files.deleteIfExists(file);
                    return;
                }
            }

            log.debug("Mounting dmg {}", file);

            ProcessBuilder pb = new ProcessBuilder(
                    "hdiutil",
                    "attach",
                    "-nobrowse",
                    "-plist",
                    file.toAbsolutePath().toString()
            );
            Process process = pb.start();
            if (!process.waitFor(5, TimeUnit.SECONDS)) {
                // Timeout
                process.destroy();
                log.error("Timeout waiting for hdiutil to attach dmg");
                return;
            }
            if (process.exitValue() != 0) {
                log.error("Error running hdiutil attach");
                return;
            }
            String mountPoint;
            try (InputStream in = process.getInputStream()) {
                mountPoint = parseHdiutilPlist(in);
            }

            // Point of no return
            log.debug("Removing old install from {}", RUNELITE_APP);
            delete(Paths.get(RUNELITE_APP));

            log.debug("Copying new install from {}", mountPoint);
            copy(Paths.get(mountPoint, "OpenRune.app"), Paths.get(RUNELITE_APP));

            log.debug("Unmounting dmg");
            pb = new ProcessBuilder(
                    "hdiutil",
                    "detach",
                    mountPoint
            );
            pb.start();

            log.debug("Done! Launching...");

            List<String> launchCmd = new ArrayList<>(args.length + 1);
            launchCmd.add(path.toAbsolutePath().toString());
            launchCmd.addAll(Arrays.asList(args));
            pb = new ProcessBuilder(launchCmd);
            pb.environment().put("RUNELITE_UPGRADE", "1");
            pb.start();

            System.exit(0);
        } catch (Exception e) {
            log.error("Error performing upgrade", e);
        }
    }

    static String parseHdiutilPlist(InputStream in) throws Exception
    {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(in);
        doc.getDocumentElement().normalize();

        Element plist = (Element) doc.getElementsByTagName("plist").item(0);
        Element dict = (Element) plist.getElementsByTagName("dict").item(0);
        Element arr = (Element) dict.getElementsByTagName("array").item(0);
        NodeList dicts = arr.getElementsByTagName("dict");
        for (int i = 0; i < dicts.getLength(); ++i)
        {
            NodeList dict2 = (NodeList) dicts.item(i);
            String lastKey = null;

            for (int j = 0; j < dict2.getLength(); ++j)
            {
                Node node = dict2.item(j);

                if (node.getNodeType() == Node.ELEMENT_NODE)
                {
                    if (node.getNodeName().equals("key"))
                    {
                        lastKey = node.getTextContent();
                    }
                    else if (lastKey != null)
                    {
                        if (lastKey.equals("mount-point"))
                        {
                            return node.getTextContent();
                        }

                        lastKey = null;
                    }
                }
            }
        }
        return null;
    }

    private static void updateWindows(Bootstrap bootstrap, LauncherSettings launcherSettings, String[] args) {
        String command = System.getProperty("sun.java.command", "");
        if (command.isEmpty()) {
            log.debug("Running process has no command");
            return;
        }

        String installLocation;

        try {
            installLocation = regQueryString("Software\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\OpenRune Launcher_is1", "InstallLocation");
        } catch (UnsatisfiedLinkError | RuntimeException ex) {
            log.debug("Skipping update check, error querying install location", ex);
            return;
        }

        Path path = Paths.get(command);
        if (!path.startsWith(installLocation) || !path.getFileName().toString().equals(LAUNCHER_EXECUTABLE_NAME_WIN)) {
            log.debug("Skipping update check due to not running from installer, command is {}", command);
            return;
        }

        log.debug("Running from installer");

        Update newestUpdate = findAvailableUpdate(bootstrap);
        if (newestUpdate == null) {
            return;
        }

        final boolean noupdate = launcherSettings.isNoupdates();
        if (noupdate) {
            log.info("Skipping update {} due to noupdate being set", newestUpdate.getVersion());
            return;
        }

        if (System.getenv("RUNELITE_UPGRADE") != null) {
            log.info("Skipping update {} due to launching from an upgrade", newestUpdate.getVersion());
            return;
        }

        LauncherSettings settings = LauncherSettings.loadSettings();
        int hours = 1 << Math.min(9, settings.lastUpdateAttemptNum); // 512 hours = ~21 days
        if (newestUpdate.getHash().equals(settings.lastUpdateHash) &&
                Instant.ofEpochMilli(settings.lastUpdateAttemptTime).isAfter(Instant.now().minus(hours, ChronoUnit.HOURS))) {
            log.info("Previous upgrade attempt to {} was at {} (backoff: {} hours), skipping", newestUpdate.getVersion(),
                    LocalTime.from(Instant.ofEpochMilli(settings.lastUpdateAttemptTime).atZone(ZoneId.systemDefault())), hours);
            return;
        }

        List<String> allProcesses = getRunningProcesses();
        for (String ph : allProcesses) {
            if (ph.equals(command)) {
                continue;
            }

            if (ph.equals(command)) {
                log.info("Skipping update {} due to {} process {}", newestUpdate.getVersion(), LAUNCHER_EXECUTABLE_NAME_WIN, ph);
                return;
            }
        }

        if (newestUpdate.getRollout() > 0. && installRollout() > newestUpdate.getRollout()) {
            log.info("Skipping update {} due to rollout", newestUpdate.getVersion());
            return;
        }

        settings.lastUpdateAttemptTime = System.currentTimeMillis();
        settings.lastUpdateHash = newestUpdate.getHash();
        settings.lastUpdateAttemptNum++;
        LauncherSettings.saveSettings(settings);

        try {
            log.info("Downloading launcher {} from {}", newestUpdate.getVersion(), newestUpdate.getUrl());

            Path file = Files.createTempFile("rlupdate", "exe");
            try (OutputStream fout = Files.newOutputStream(file)) {
                final String name = newestUpdate.getName();
                final int size = newestUpdate.getSize();
                try {
                    download(newestUpdate.getUrl(), newestUpdate.getHash(), (completed) ->
                            SplashScreen.stage(.07, 1., null, name, completed, size, true), fout);
                } catch (VerificationException e) {
                    log.error("unable to verify update", e);
                    file.toFile().delete();
                    return;
                }
            }

            log.info("Launching installer version {}", newestUpdate.getVersion());

            String[] commandArray = new String[]{file.toFile().getAbsolutePath(), "/SILENT"};
            ProcessBuilder pb = new ProcessBuilder(commandArray);
            Map<String, String> env = pb.environment();

            StringBuilder argStr = new StringBuilder();
            Escaper escaper = Escapers.builder()
                    .addEscape('"', "\\\"")
                    .build();
            for (String arg : args) {
                if (argStr.length() > 0) {
                    argStr.append(' ');
                }
                if (arg.contains(" ") || arg.contains("\"")) {
                    argStr.append('"').append(escaper.escape(arg)).append('"');
                } else {
                    argStr.append(arg);
                }
            }

            env.put("RUNELITE_UPGRADE", "1");
            env.put("RUNELITE_UPGRADE_PARAMS", argStr.toString());
            pb.start();

            System.exit(0);
        } catch (IOException e) {
            log.error("io error performing upgrade", e);
        }
    }

    private static List<String> getRunningProcesses() {
        List<String> processes = new ArrayList<>();
        try {
            Process process = Runtime.getRuntime().exec(System.getenv("windir") + "\\system32\\" + "tasklist.exe");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                processes.add(line);
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return processes;
    }

    private static Update findAvailableUpdate(Bootstrap bootstrap)
    {
        Update[] updates = bootstrap.getUpdates();
        if (updates == null)
        {
            return null;
        }

        final String os = System.getProperty("os.name");
        final String arch = System.getProperty("os.arch");
        final String ver = System.getProperty("os.version");
        final String launcherVersion = LauncherProperties.getVersion();
        if (os == null || arch == null || launcherVersion == null)
        {
            return null;
        }

        Update newestUpdate = null;
        for (Update update : updates)
        {
            OS.OSType updateOs = OS.parseOs(update.getOs());
            if ((updateOs == OS.OSType.Other ? update.getOs().equals(os) : updateOs == OS.getOs()) &&
                    (update.getOsName() == null || update.getOsName().equals(os)) &&
                    (update.getOsVersion() == null || update.getOsVersion().equals(ver)) &&
                    (update.getArch() == null || arch.equals(update.getArch())) &&
                    compareVersion(update.getVersion(), launcherVersion) > 0 &&
                    (update.getMinimumVersion() == null || compareVersion(launcherVersion, update.getMinimumVersion()) >= 0) &&
                    (newestUpdate == null || compareVersion(update.getVersion(), newestUpdate.getVersion()) > 0))
            {
                log.info("Update {} is available", update.getVersion());
                newestUpdate = update;
            }
        }

        return newestUpdate;
    }

    private static double installRollout()
    {
        try (BufferedReader reader = new BufferedReader(new FileReader("install_id.txt")))
        {
            String line = reader.readLine();
            if (line != null)
            {
                line = line.trim();
                int i = Integer.parseInt(line);
                log.debug("Loaded install id {}", i);
                return (double) i / (double) Integer.MAX_VALUE;
            }
        }
        catch (IOException | NumberFormatException ex)
        {
            log.warn("unable to get install rollout", ex);
        }
        return Math.random();
    }

    // https://stackoverflow.com/a/27917071
    private static void delete(Path directory) throws IOException {
        Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    // https://stackoverflow.com/a/60621544
    private static void copy(Path source, Path target, CopyOption... options) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path targetDir = target.resolve(source.relativize(dir).toString());
                try {
                    Files.copy(dir, targetDir, options);
                } catch (FileAlreadyExistsException e) {
                    if (!Files.isDirectory(targetDir)) {
                        throw e;
                    }
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.copy(file, target.resolve(source.relativize(file).toString()), options);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
