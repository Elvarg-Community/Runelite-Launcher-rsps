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

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import lombok.extern.slf4j.Slf4j;
import net.runelite.launcher.beans.Bootstrap;

@Slf4j
class ForkLauncher {
    static boolean canForkLaunch() {
        OS.OSType os = OS.getOs();

        if (os == OS.OSType.Linux) {
            String appimage = System.getenv("APPIMAGE");
            if (appimage != null) {
                return true;
            }
        }

        if (os == OS.OSType.Windows || os == OS.OSType.MacOS) {
            String command = System.getProperty("sun.java.command");
            if (command != null && !command.isEmpty()) {
                Path path = Paths.get(command);
                String name = path.getFileName().toString();
                return name.equals(Launcher.LAUNCHER_EXECUTABLE_NAME_WIN)
                        || name.equals(Launcher.LAUNCHER_EXECUTABLE_NAME_OSX);
            }
        }

        return false;
    }

    static void launch(Bootstrap bootstrap, List<File> classpath, Collection<String> clientArgs,
                       Map<String, String> jvmProps, List<String> jvmArgs) throws IOException {
        String command = System.getProperty("sun.java.command");
        Path path = Paths.get(command);

        ArrayList<String> commands = new ArrayList<>();
        commands.add(path.toAbsolutePath().toString());
        commands.add("-c");

        // bootstrap vm args
        String[] clientJvmArgs = JvmLauncher.getJvmArguments(bootstrap);
        if (clientJvmArgs != null) {
            for (String arg : clientJvmArgs) {
                commands.add("-J");
                commands.add(arg);
            }
        }
        // launcher vm props
        for (Map.Entry<String, String> prop : jvmProps.entrySet()) {
            commands.add("-J");
            commands.add("-D" + prop.getKey() + "=" + prop.getValue());
        }
        // launcher vm args
        commands.addAll(jvmArgs);

        // program arguments
        commands.add("--");

        if (classpath.isEmpty()) {
            // avoid looping
            throw new RuntimeException("Cannot fork launch with an empty classpath");
        }

        commands.add("--classpath");
        StringBuilder sb = new StringBuilder();
        for (File f : classpath) {
            if (sb.length() > 0) {
                sb.append(File.pathSeparatorChar);
            }
            sb.append(f.getName());
        }
        commands.add(sb.toString());

        commands.addAll(clientArgs);

        System.out.println("Running process: " + commands);

        ProcessBuilder builder = new ProcessBuilder(commands);
        builder.start();
    }
}