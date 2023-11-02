/*
 * Copyright (c) 2016-2018, Adam <Adam@sigterm.info>
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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import com.google.archivepatcher.applier.FileByFileV1DeltaApplier;
import com.google.archivepatcher.shared.DefaultDeflateCompatibilityWindow;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Streams;
import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import com.google.common.hash.HashingOutputStream;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.function.IntConsumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import javax.annotation.Nullable;
import javax.swing.*;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import lombok.extern.slf4j.Slf4j;
import net.runelite.launcher.beans.*;
import net.runelite.launcher.mutli.FontManager;
import net.runelite.launcher.mutli.SplashScreenMultipleOptions;
import org.slf4j.LoggerFactory;

@Slf4j
public class Launcher
{
	private static final File RUNELITE_DIR = new File(System.getProperty("user.home"), "." + LauncherProperties.getApplicationName());
	public static final File LOGS_DIR = new File(RUNELITE_DIR, "logs");
	public static final File CRASH_FILES = new File(LOGS_DIR, "jvm_crash_pid_%p.log");
	private static final String USER_AGENT = LauncherProperties.getApplicationName() + "/" + LauncherProperties.getVersion();
	static final String LAUNCHER_EXECUTABLE_NAME_WIN = LauncherProperties.getApplicationName() + ".exe";
	static final String LAUNCHER_EXECUTABLE_NAME_OSX = LauncherProperties.getApplicationName();

	static HashMap<String, ClientType> clientTypes = new HashMap<>();

	public static boolean displayMultipleOptions = false;

	public static void main(String[] args)
	{
		OptionParser parser = new OptionParser(false);
		parser.allowsUnrecognizedOptions();
		parser.accepts("postinstall", "Perform post-install tasks");
		parser.accepts("debug", "Enable debug logging");
		parser.accepts("nodiff", "Always download full artifacts instead of diffs");
		parser.accepts("insecure-skip-tls-verification", "Disable TLS certificate and hostname verification");
		parser.accepts("scale", "Custom scale factor for Java 2D").withRequiredArg();
		parser.accepts("noupdate", "Skips the launcher self-update");
		parser.accepts("help", "Show this text (use -- --help for client help)").forHelp();
		parser.accepts("classpath", "Classpath for the client").withRequiredArg();
		parser.accepts("J", "JVM argument (FORK or JVM launch mode only)").withRequiredArg();
		parser.accepts("configure", "Opens configuration GUI");
		parser.accepts("launch-mode", "JVM launch method (JVM, FORK, REFLECT)")
				.withRequiredArg()
				.ofType(LaunchMode.class);
		parser.accepts("hw-accel", "Java 2D hardware acceleration mode (OFF, DIRECTDRAW, OPENGL, METAL)")
				.withRequiredArg()
				.ofType(HardwareAccelerationMode.class);
		parser.accepts("mode", "Alias of hw-accel")
				.withRequiredArg()
				.ofType(HardwareAccelerationMode.class);

		if (OS.getOs() == OS.OSType.MacOS)
		{
			// Parse macos PSN, eg: -psn_0_352342
			parser.accepts("p").withRequiredArg();
		}

		final OptionSet options;
		try
		{
			options = parser.parse(args);
		}
		catch (OptionException ex)
		{
			log.error("unable to parse arguments", ex);
			SwingUtilities.invokeLater(() ->
					new FatalErrorDialog("{name} was unable to parse the provided application arguments: " + ex.getMessage())
							.open());
			throw ex;
		}

		if (options.has("help"))
		{
			try
			{
				parser.printHelpOn(System.out);
			}
			catch (IOException e)
			{
				log.error(null, e);
			}
			System.exit(0);
		}

		if (options.has("configure"))
		{
			ConfigurationFrame.open();
			return;
		}

		final LauncherSettings settings = LauncherSettings.loadSettings();
		settings.apply(options);

		final boolean postInstall = options.has("postinstall");

		// Setup logging
		LOGS_DIR.mkdirs();
		if (settings.isDebug())
		{
			final Logger logger = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
			logger.setLevel(Level.DEBUG);
		}

		retrieveClientTypes();

		initDll();

		// RTSS triggers off of the CreateWindow event, so this needs to be in place early, prior to splash screen
		initDllBlacklist();

		try
		{
			if (options.has("classpath"))
			{
				TrustManagerUtil.setupTrustManager();

				String clientName = clientTypes.entrySet().stream().findAny().get().getValue().getName();

				File location = new File(RUNELITE_DIR, "repository/" + clientName + "/");

				// being called from ForkLauncher. All JVM options are already set.
				var classpathOpt = String.valueOf(options.valueOf("classpath"));
				var classpath = Streams.stream(Splitter.on(File.pathSeparatorChar)
								.split(classpathOpt))
						.map(name -> new File(location, name))
						.collect(Collectors.toList());
				try
				{
					ReflectionLauncher.launch(classpath, getClientArgs(settings),clientTypes.entrySet().stream().findAny().get().getValue().getName());
				}
				catch (Exception e)
				{
					log.error("error launching client", e);
				}
				return;
			}

			final Map<String, String> jvmProps = new LinkedHashMap<>();
			if (settings.scale != null)
			{
				// This calls SetProcessDPIAware(). Since the RuneLite.exe manifest is DPI unaware
				// Windows will scale the application if this isn't called. Thus the default scaling
				// mode is Windows scaling due to being DPI unaware.
				// https://docs.microsoft.com/en-us/windows/win32/hidpi/high-dpi-desktop-application-development-on-windows
				jvmProps.put("sun.java2d.dpiaware", "true");
				// This sets the Java 2D scaling factor, overriding the default behavior of detecting the scale via
				// GetDpiForMonitor.
				jvmProps.put("sun.java2d.uiScale", Double.toString(settings.scale));
			}

			final var hardwareAccelMode = settings.hardwareAccelerationMode == HardwareAccelerationMode.AUTO ?
					HardwareAccelerationMode.defaultMode(OS.getOs()) : settings.hardwareAccelerationMode;
			jvmProps.putAll(hardwareAccelMode.toParams(OS.getOs()));

			// As of JDK-8243269 (11.0.8) and JDK-8235363 (14), AWT makes macOS dark mode support opt-in so interfaces
			// with hardcoded foreground/background colours don't get broken by system settings. Considering the native
			// Aqua we draw consists a window border and an about box, it's safe to say we can opt in.
			if (OS.getOs() == OS.OSType.MacOS)
			{
				jvmProps.put("apple.awt.application.appearance", "system");
			}

			// Stream launcher version
			jvmProps.put(LauncherProperties.getVersionKey(), LauncherProperties.getVersion());

			if (settings.isSkipTlsVerification())
			{
				jvmProps.put("runelite.insecure-skip-tls-verification", "true");
			}

			log.info(LauncherProperties.getApplicationName() + " Launcher version {}", LauncherProperties.getVersion());
			log.info("Launcher configuration:" + System.lineSeparator() + "{}", settings.configurationStr());
			log.info("OS name: {}, version: {}, arch: {}", System.getProperty("os.name"), System.getProperty("os.version"), System.getProperty("os.arch"));
			log.info("Using hardware acceleration mode: {}", hardwareAccelMode);

			// java2d properties have to be set prior to the graphics environment startup
			setJvmParams(jvmProps);

			if (settings.isSkipTlsVerification())
			{
				TrustManagerUtil.setupInsecureTrustManager();
			}
			else
			{
				TrustManagerUtil.setupTrustManager();
			}

			if (displayMultipleOptions)
			{
				FontManager.init();

				List<JButton> buttons = new ArrayList<>();

				for (Map.Entry<String, ClientType> type : clientTypes.entrySet())
				{
					JButton button = SplashScreenMultipleOptions.addButton(toTitleCase(type.getKey()), type.getValue().getTooltip());
					button.addActionListener(e ->
					{
						Runnable task = () -> launch(toTitleCase(type.getKey()), args, settings,jvmProps,postInstall);
						Thread thread = new Thread(task);
						thread.start();
					});
					buttons.add(button);
				}

				SplashScreenMultipleOptions.init(buttons);
				SplashScreenMultipleOptions.barMessage(null);
				SplashScreenMultipleOptions.message(null);

			}
			else
			{
				SplashScreen.init();
				Runnable task = () -> launch(toTitleCase(clientTypes.entrySet().stream().findAny().get().getValue().getName()), args, settings,jvmProps,postInstall);
				Thread thread = new Thread(task);
				thread.start();
			}
		}
		catch (Exception e)
		{
			log.error("Failure during startup", e);
			if (!postInstall)
			{
				SwingUtilities.invokeLater(() ->
						new FatalErrorDialog("{name} has encountered an unexpected error during startup.")
								.open());
			}
		}
		catch (Error e)
		{
			// packr seems to eat exceptions thrown out of main, so at least try to log it
			log.error("Failure during startup", e);
			throw e;
		}
	}

	public static void launch(String type, String[] args, LauncherSettings settings, Map<String, String> jvmProps, boolean postInstall) {
		try {
			if (postInstall)
			{
				postInstall(type);
				return;
			}

			File location = new File(RUNELITE_DIR, "repository/" + type + "/");

			stage(0, "Preparing", "Setting up environment");

			// Print out system info
			if (log.isDebugEnabled())
			{
				final RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();

				log.debug("Command line arguments: {}", String.join(" ", args));
				// This includes arguments from _JAVA_OPTIONS, which are parsed after command line flags and applied to
				// the global VM args
				log.debug("Java VM arguments: {}", String.join(" ", runtime.getInputArguments()));
				log.debug("Java Environment:");
				final Properties p = System.getProperties();
				final Enumeration<Object> keys = p.keys();

				while (keys.hasMoreElements())
				{
					final String key = (String) keys.nextElement();
					final String value = (String) p.get(key);
					log.debug("  {}: {}", key, value);
				}
			}

			stage(.05, null, "Downloading bootstrap");
			Bootstrap bootstrap;
			try
			{
				bootstrap = getBootstrap(type);
			}
			catch (IOException | VerificationException | CertificateException | SignatureException | InvalidKeyException | NoSuchAlgorithmException ex)
			{
				log.error("error fetching bootstrap", ex);

				String extract = CertPathExtractor.extract(ex);
				if (extract != null)
				{
					log.error("untrusted certificate chain: {}", extract);
				}

				SwingUtilities.invokeLater(() -> FatalErrorDialog.showNetErrorWindow("downloading the bootstrap", ex));
				return;
			}

			stage(.07, null, "Checking for updates");

			Updater.update(bootstrap, settings, args);

			stage(.10, null, "Tidying the cache");

			if (jvmOutdated(bootstrap))
			{
				// jvmOutdated opens an error dialog
				return;
			}

			// update packr vmargs to the launcher vmargs from bootstrap.
			PackrConfig.updateLauncherArgs(bootstrap);

			if (!location.exists() && !location.mkdirs())
			{
				log.error("unable to create repo directory {}", location);
				SwingUtilities.invokeLater(() -> new FatalErrorDialog("Unable to create {name} directory " + location.getAbsolutePath() + ". Check your filesystem permissions are correct.").open());
				return;
			}

			// Determine artifacts for this OS
			List<Artifact> artifacts = Arrays.stream(bootstrap.getArtifacts())
					.filter(a ->
					{
						if (a.getPlatform() == null)
						{
							return true;
						}

						final String os = System.getProperty("os.name");
						final String arch = System.getProperty("os.arch");
						for (Platform platform : a.getPlatform())
						{
							if (platform.getName() == null)
							{
								continue;
							}

							OS.OSType platformOs = OS.parseOs(platform.getName());
							if ((platformOs == OS.OSType.Other ? platform.getName().equals(os) : platformOs == OS.getOs())
									&& (platform.getArch() == null || platform.getArch().equals(arch)))
							{
								return true;
							}
						}

						return false;
					})
					.collect(Collectors.toList());

			// Clean out old artifacts from the repository
			clean(artifacts,type);

			try
			{
				download(artifacts, settings.isNodiffs(),type);
			}
			catch (IOException ex)
			{
				log.error("unable to download artifacts", ex);
				SwingUtilities.invokeLater(() -> FatalErrorDialog.showNetErrorWindow("downloading the client", ex));
				return;
			}

			stage(.80, null, "Verifying");
			try
			{
				verifyJarHashes(artifacts,type);
			}
			catch (VerificationException ex)
			{
				log.error("Unable to verify artifacts", ex);
				SwingUtilities.invokeLater(() -> FatalErrorDialog.showNetErrorWindow("verifying downloaded files", ex));
				return;
			}

			final Collection<String> clientArgs = getClientArgs(settings);
			stage(.90, "Starting the client", "");

			var classpath = artifacts.stream()
					.map(dep -> new File(location, dep.getName()))
					.collect(Collectors.toList());

			List<String> jvmParams = new ArrayList<>();
			// Set hs_err_pid location. This is a jvm param and can't be set at runtime.
			log.debug("Setting JVM crash log location to {}", CRASH_FILES);
			jvmParams.add("-XX:ErrorFile=" + CRASH_FILES.getAbsolutePath());
			// Add VM args from cli/env
			jvmParams.addAll(getJvmArgs(settings));

			if (settings.launchMode == LaunchMode.REFLECT)
			{
				log.debug("Using launch mode: REFLECT");
				ReflectionLauncher.launch(classpath, clientArgs,"");
			}
			else if (settings.launchMode == LaunchMode.FORK || (settings.launchMode == LaunchMode.AUTO && ForkLauncher.canForkLaunch()))
			{
				log.debug("Using launch mode: FORK");
				ForkLauncher.launch(bootstrap, classpath, clientArgs, jvmProps, jvmParams);
			}
			else
			{


				if (System.getenv("APPIMAGE") != null)
				{
					// java.home is in the appimage, so we can never use the jvm launcher
					throw new RuntimeException("JVM launcher is not supported from the appimage");
				}

				// launch mode JVM or AUTO outside of packr
				log.debug("Using launch mode: JVM");
				JvmLauncher.launch(bootstrap, classpath, clientArgs, jvmProps, jvmParams,type);
			}
		} catch (Exception e) {
			log.error("Failure during startup", e);
			if (!postInstall)
			{
				SwingUtilities.invokeLater(() ->
						new FatalErrorDialog("{name} has encountered an unexpected error during startup.")
								.open());
			}
		}
		catch (Error e)
		{
			// packr seems to eat exceptions thrown out of main, so at least try to log it
			log.error("Failure during startup", e);
			throw e;
		}
		finally
		{
			close();
		}
	}


	private static void setJvmParams(final Map<String, String> params)
	{
		for (Map.Entry<String, String> entry : params.entrySet())
		{
			System.setProperty(entry.getKey(), entry.getValue());
		}
	}

	public static String toTitleCase(String givenString)
	{
		String[] arr = givenString.split(" ");
		StringBuffer sb = new StringBuffer();

		for (int i = 0; i < arr.length; i++)
		{
			sb.append(Character.toUpperCase(arr[i].charAt(0)))
					.append(arr[i].substring(1)).append(" ");
		}
		return sb.toString().trim();
	}

	public static void retrieveClientTypes()
	{

		try
		{
			ClientType[] types = getClientManifest();
			for (ClientType type : types)
			{
				clientTypes.put(type.getName(), type);
			}
		}
		catch (Exception ex)
		{
			log.error("error fetching client types", ex);
			SwingUtilities.invokeLater(() -> FatalErrorDialog.showNetErrorWindow("No Clients Found", new Exception("No Clients Found")));
		}
		if (clientTypes.size() == 0)
		{
			log.error("No Clients Found");
			SwingUtilities.invokeLater(() -> FatalErrorDialog.showNetErrorWindow("No Clients Found", new Exception("No Clients Found")));
		}
		else
		{
			displayMultipleOptions = clientTypes.size() != 1;
		}
	}

	private static ClientType[] getClientManifest() throws IOException
	{
		URL u = new URL(LauncherProperties.getRuneliteTypeManifest());

		URLConnection conn = u.openConnection();


		conn.setRequestProperty("User-Agent", USER_AGENT);

		try (InputStream i = conn.getInputStream())
		{
			byte[] bytes = ByteStreams.toByteArray(i);

			Gson g = new Gson();
			return g.fromJson(new InputStreamReader(new ByteArrayInputStream(bytes)), ClientType[].class);
		}
	}

	private static Bootstrap getBootstrap(String type) throws IOException, CertificateException, NoSuchAlgorithmException, InvalidKeyException, SignatureException, VerificationException
	{
		URL u = new URL(clientTypes.get(type).getBootstrap());
		URL signatureUrl = new URL(clientTypes.get(type).getBootstrapsig());

		URLConnection conn = u.openConnection();
		URLConnection signatureConn = signatureUrl.openConnection();

		conn.setRequestProperty("User-Agent", USER_AGENT);
		signatureConn.setRequestProperty("User-Agent", USER_AGENT);

		try (InputStream i = conn.getInputStream();
			 InputStream signatureIn = signatureConn.getInputStream())
		{
			byte[] bytes = ByteStreams.toByteArray(i);
			byte[] signature = ByteStreams.toByteArray(signatureIn);

			Certificate certificate = getCertificate();
			Signature s = Signature.getInstance("SHA256withRSA");
			s.initVerify(certificate);
			s.update(bytes);

			Gson g = new Gson();
			return g.fromJson(new InputStreamReader(new ByteArrayInputStream(bytes)), Bootstrap.class);
		}
	}

	private static boolean jvmOutdated(Bootstrap bootstrap)
	{
		boolean launcherTooOld = bootstrap.getRequiredLauncherVersion() != null &&
				compareVersion(bootstrap.getRequiredLauncherVersion(), LauncherProperties.getVersion()) > 0;

		boolean jvmTooOld = false;
		try
		{
			if (bootstrap.getRequiredJVMVersion() != null)
			{
				jvmTooOld = Runtime.Version.parse(bootstrap.getRequiredJVMVersion())
						.compareTo(Runtime.version()) > 0;
			}
		}
		catch (IllegalArgumentException e)
		{
			log.warn("Unable to parse bootstrap version", e);
		}

		if (launcherTooOld)
		{
			SwingUtilities.invokeLater(() ->
					new FatalErrorDialog("Your launcher is too old to start {name}. Please download and install a more " +
							"recent one from {link}.")
							.addButton("{link}", () -> LinkBrowser.browse(LauncherProperties.getDownloadLink()))
							.open());
			return true;
		}
		if (jvmTooOld)
		{
			SwingUtilities.invokeLater(() ->
					new FatalErrorDialog("Your Java installation is too old. {name} now requires Java " +
							bootstrap.getRequiredJVMVersion() + " to run. You can get a platform specific version from {link}," +
							" or install a newer version of Java.")
							.addButton("{link}", () -> LinkBrowser.browse(LauncherProperties.getDownloadLink()))
							.open());
			return true;
		}

		return false;
	}

	private static Collection<String> getClientArgs(LauncherSettings settings)
	{
		final var args = new ArrayList<>(settings.clientArguments);

		String clientArgs = System.getenv(LauncherProperties.getApplicationName().toUpperCase() + "_ARGS");
		if (!Strings.isNullOrEmpty(clientArgs))
		{
			args.addAll(Splitter.on(' ')
					.omitEmptyStrings()
					.trimResults()
					.splitToList(clientArgs));
		}

		if (settings.debug)
		{
			args.add("--debug");
		}

		if (settings.safemode)
		{
			args.add("--safe-mode");
		}

		return args;
	}

	private static List<String> getJvmArgs(LauncherSettings settings)
	{
		var args = new ArrayList<>(settings.jvmArguments);

		var envArgs = System.getenv(LauncherProperties.getApplicationName().toUpperCase() + "_VMARGS");
		if (!Strings.isNullOrEmpty(envArgs))
		{
			args.addAll(Splitter.on(' ')
					.omitEmptyStrings()
					.trimResults()
					.splitToList(envArgs));
		}

		return args;
	}

	private static void download(List<Artifact> artifacts, boolean nodiff, String type) throws IOException
	{
		File location = new File(RUNELITE_DIR, "repository/" + type + "/");
		List<Artifact> toDownload = new ArrayList<>(artifacts.size());
		Map<Artifact, Diff> diffs = new HashMap<>();
		int totalDownloadBytes = 0;
		final boolean isCompatible = new DefaultDeflateCompatibilityWindow().isCompatible();

		if (!isCompatible && !nodiff)
		{
			log.debug("System zlib is not compatible with archive-patcher; not using diffs");
			nodiff = true;
		}

		for (Artifact artifact : artifacts)
		{
			File dest = new File(location, artifact.getName());

			String hash;
			try
			{
				hash = hash(dest);
			}
			catch (FileNotFoundException ex)
			{
				hash = null;
			}
			catch (IOException ex)
			{
				dest.delete();
				hash = null;
			}

			if (Objects.equals(hash, artifact.getHash()))
			{
				log.debug("Hash for {} up to date", artifact.getName());
				continue;
			}

			int downloadSize = artifact.getSize();

			// See if there is a diff available
			if (!nodiff && artifact.getDiffs() != null)
			{
				for (Diff diff : artifact.getDiffs())
				{
					File old = new File(location, diff.getFrom());

					String oldhash;
					try
					{
						oldhash = hash(old);
					}
					catch (IOException ex)
					{
						continue;
					}

					// Check if old file is valid
					if (diff.getFromHash().equals(oldhash))
					{
						diffs.put(artifact, diff);
						downloadSize = diff.getSize();
					}
				}
			}

			toDownload.add(artifact);
			totalDownloadBytes += downloadSize;
		}

		final double START_PROGRESS = .15;
		int downloaded = 0;
		stage(START_PROGRESS, "Downloading", "");

		for (Artifact artifact : toDownload)
		{
			File dest = new File(location, artifact.getName());
			final int total = downloaded;

			// Check if there is a diff we can download instead
			Diff diff = diffs.get(artifact);
			if (diff != null)
			{
				log.debug("Downloading diff {}", diff.getName());

				try
				{
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					final int totalBytes = totalDownloadBytes;
					download(diff.getPath(), diff.getHash(), (completed) ->
									stage(START_PROGRESS, .80, null, diff.getName(), total + completed, totalBytes, true),
							out);
					downloaded += diff.getSize();

					File old = new File(location, diff.getFrom());
					HashCode hash;
					try (InputStream patchStream = new GZIPInputStream(new ByteArrayInputStream(out.toByteArray()));
						 HashingOutputStream fout = new HashingOutputStream(Hashing.sha256(), Files.newOutputStream(dest.toPath())))
					{
						new FileByFileV1DeltaApplier().applyDelta(old, patchStream, fout);
						hash = fout.hash();
					}

					if (artifact.getHash().equals(hash.toString()))
					{
						log.debug("Patching successful for {}", artifact.getName());
						continue;
					}

					log.debug("Patched artifact hash mismatches! {}: got {} expected {}", artifact.getName(), hash.toString(), artifact.getHash());
				}
				catch (IOException | VerificationException e)
				{
					log.warn("unable to download patch {}", diff.getName(), e);
					// Fall through and try downloading the full artifact
				}

				// Adjust the download size for the difference
				totalDownloadBytes -= diff.getSize();
				totalDownloadBytes += artifact.getSize();
			}

			log.debug("Downloading {}", artifact.getName());

			try (OutputStream fout = Files.newOutputStream(dest.toPath()))
			{
				final int totalBytes = totalDownloadBytes;
				download(artifact.getPath(), artifact.getHash(), (completed) ->
								stage(START_PROGRESS, .80, null, artifact.getName(), total + completed, totalBytes, true),
						fout);
				downloaded += artifact.getSize();
			}
			catch (VerificationException e)
			{
				log.warn("unable to verify jar {}", artifact.getName(), e);
			}
		}
	}

	private static void clean(List<Artifact> artifacts,String type)
	{
		File location = new File(RUNELITE_DIR, "repository/" + type + "/");
		File[] existingFiles = location.listFiles();

		if (existingFiles == null)
		{
			return;
		}

		Set<String> artifactNames = new HashSet<>();
		for (Artifact artifact : artifacts)
		{
			artifactNames.add(artifact.getName());
			if (artifact.getDiffs() != null)
			{
				// Keep around the old files which diffs are from
				for (Diff diff : artifact.getDiffs())
				{
					artifactNames.add(diff.getFrom());
				}
			}
		}

		for (File file : existingFiles)
		{
			if (file.isFile() && !artifactNames.contains(file.getName()))
			{
				if (file.delete())
				{
					log.debug("Deleted old artifact {}", file);
				}
				else
				{
					log.warn("Unable to delete old artifact {}", file);
				}
			}
		}
	}

	private static void verifyJarHashes(List<Artifact> artifacts, String type) throws VerificationException
	{
		File location = new File(RUNELITE_DIR, "repository/" + type + "/");
		for (Artifact artifact : artifacts)
		{
			String expectedHash = artifact.getHash();
			String fileHash;
			try
			{
				fileHash = hash(new File(location, artifact.getName()));
			}
			catch (IOException e)
			{
				throw new VerificationException("unable to hash file", e);
			}

			if (!fileHash.equals(expectedHash))
			{
				log.warn("Expected {} for {} but got {}", expectedHash, artifact.getName(), fileHash);
				throw new VerificationException("Expected " + expectedHash + " for " + artifact.getName() + " but got " + fileHash);
			}

			log.info("Verified hash of {}", artifact.getName());
		}
	}

	private static String hash(File file) throws IOException
	{
		HashFunction sha256 = Hashing.sha256();
		return com.google.common.io.Files.asByteSource(file).hash(sha256).toString();
	}

	private static Certificate getCertificate() throws CertificateException
	{
		CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
		Certificate certificate = certFactory.generateCertificate(Launcher.class.getResourceAsStream("launcher.crt"));
		return certificate;
	}

	static int compareVersion(String a, String b)
	{
		Pattern tok = Pattern.compile("[^0-9a-zA-Z]");
		return Arrays.compare(tok.split(a), tok.split(b), (x, y) ->
		{
			Integer ix = null;
			try
			{
				ix = Integer.parseInt(x);
			}
			catch (NumberFormatException e)
			{
			}

			Integer iy = null;
			try
			{
				iy = Integer.parseInt(y);
			}
			catch (NumberFormatException e)
			{
			}

			if (ix == null && iy == null)
			{
				return x.compareToIgnoreCase(y);
			}

			if (ix == null)
			{
				return -1;
			}
			if (iy == null)
			{
				return 1;
			}

			if (ix > iy)
			{
				return 1;
			}
			if (ix < iy)
			{
				return -1;
			}

			return 0;
		});
	}

	static void download(String path, String hash, IntConsumer progress, OutputStream out) throws IOException, VerificationException
	{
		URL url = new URL(path);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestProperty("User-Agent", USER_AGENT);
		conn.getResponseCode();

		InputStream err = conn.getErrorStream();
		if (err != null)
		{
			err.close();
			throw new IOException("Unable to download " + path + " - " + conn.getResponseMessage());
		}

		int downloaded = 0;
		HashingOutputStream hout = new HashingOutputStream(Hashing.sha256(), out);
		try (InputStream in = conn.getInputStream())
		{
			int i;
			byte[] buffer = new byte[1024 * 1024];
			while ((i = in.read(buffer)) != -1)
			{
				hout.write(buffer, 0, i);
				downloaded += i;
				progress.accept(downloaded);
			}
		}

		HashCode hashCode = hout.hash();
		if (!hash.equals(hashCode.toString()))
		{
			throw new VerificationException("Unable to verify resource " + path + " - expected " + hash + " got " + hashCode.toString());
		}
	}

	static boolean isJava17()
	{
		// 16 has the same module restrictions as 17, so we'll use the 17 settings for it
		return Runtime.version().feature() >= 16;
	}

	private static void postInstall(String type)
	{
		Bootstrap bootstrap;
		try
		{
			bootstrap = getBootstrap(type);
		}
		catch (IOException | VerificationException | CertificateException | SignatureException | InvalidKeyException | NoSuchAlgorithmException ex)
		{
			log.error("error fetching bootstrap", ex);
			return;
		}

		PackrConfig.updateLauncherArgs(bootstrap);

		log.info("Performed postinstall steps");
	}

	private static void initDll()
	{
		if (OS.getOs() != OS.OSType.Windows)
		{
			return;
		}

		String arch = System.getProperty("os.arch");
		if (!Set.of("x86", "amd64", "aarch64").contains(arch))
		{
			log.debug("System architecture is not supported for launcher natives: {}", arch);
			return;
		}

		try
		{
			System.loadLibrary("launcher_" + arch);
			log.debug("Loaded launcher native launcher_{}", arch);
		}
		catch (Error ex)
		{
			log.debug("Error loading launcher native", ex);
		}
	}

	private static void initDllBlacklist()
	{
		String blacklistedDlls = System.getProperty("runelite.launcher.blacklistedDlls");
		if (blacklistedDlls == null || blacklistedDlls.isEmpty())
		{
			return;
		}

		String[] dlls = blacklistedDlls.split(",");

		try
		{
			log.debug("Setting blacklisted dlls: {}", blacklistedDlls);
			setBlacklistedDlls(dlls);
		}
		catch (UnsatisfiedLinkError ex)
		{
			log.debug("Error setting dll blacklist", ex);
		}
	}

	private static native void setBlacklistedDlls(String[] dlls);

	static native String regQueryString(String subKey, String value);

	public static void stage(double startProgress, double endProgress, @Nullable String actionText, String subActionText,
							 int done, int total, boolean mib)
	{
		if (displayMultipleOptions)
		{
			SplashScreenMultipleOptions.stage(startProgress, endProgress, subActionText, done, total, mib);
		}
		else
		{
			SplashScreen.stage(startProgress, endProgress,
					actionText, subActionText, done, total, mib);
		}
	}

	public static void stage(double overallProgress, @Nullable String actionText, String subActionText)
	{
		if (displayMultipleOptions)
		{
			SplashScreenMultipleOptions.stage(overallProgress, subActionText);
		}
		else
		{
			SplashScreen.stage(overallProgress, actionText, subActionText);
		}
	}

	public static void close()
	{
		if (displayMultipleOptions)
		{
			SplashScreenMultipleOptions.close();
		}
		else
		{
			SplashScreen.stop();
		}
	}


}