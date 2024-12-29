package com.defold.extension.pipeline;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.ArrayList;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import com.defold.extension.pipeline.ILuaObfuscator;

import com.dynamo.bob.Bob;
import com.dynamo.bob.Project;
import com.dynamo.bob.Platform;
import com.dynamo.bob.logging.Logger;
import com.dynamo.bob.fs.DefaultFileSystem;

public class Prometheus implements ILuaObfuscator {

	private static Logger logger = Logger.getLogger(Prometheus.class.getName());

	private static final String[] PROMETHEUS_SOURCES = new String[] {
		"/logger.lua",
		"/colors.lua",
		"/prometheus.lua",
		"/highlightlua.lua",
		"/cli.lua",
		"/config.lua",
		"/presets.lua",
		"/prometheus/steps.lua",
		"/prometheus/parser.lua",
		"/prometheus/randomStrings.lua",
		"/prometheus/bit.lua",
		"/prometheus/enums.lua",
		"/prometheus/step.lua",
		"/prometheus/scope.lua",
		"/prometheus/visitast.lua",
		"/prometheus/unparser.lua",
		"/prometheus/namegenerators/confuse.lua",
		"/prometheus/namegenerators/number.lua",
		"/prometheus/namegenerators/mangled_shuffled.lua",
		"/prometheus/namegenerators/Il.lua",
		"/prometheus/namegenerators/mangled.lua",
		"/prometheus/steps/AddVararg.lua",
		"/prometheus/steps/AntiTamper.lua",
		"/prometheus/steps/ProxifyLocals.lua",
		"/prometheus/steps/ConstantArray.lua",
		"/prometheus/steps/EncryptStrings.lua",
		"/prometheus/steps/WrapInFunction.lua",
		"/prometheus/steps/Vmify.lua",
		"/prometheus/steps/SplitStrings.lua",
		"/prometheus/steps/NumbersToExpressions.lua",
		"/prometheus/tokenizer.lua",
		"/prometheus/pipeline.lua",
		"/prometheus/namegenerators.lua",
		"/prometheus/util.lua",
		"/prometheus/compiler/compiler.lua",
		"/prometheus/randomLiterals.lua",
		"/prometheus/ast.lua",
	};

	private File unpackedRoot = null;
	private File projectRoot = null;
	private String luaJITExePath = null;

	private boolean disabled = false;

	public Prometheus() throws java.lang.InstantiationException {
		try {
			unpackedRoot = Files.createTempDirectory(null).toFile();
			logger.info("Unpacking Prometheus to %s", unpackedRoot);
			for (String filename : PROMETHEUS_SOURCES) {
				File destFile = new File(unpackedRoot, filename);
				destFile.getParentFile().mkdirs();
				destFile.deleteOnExit();
				InputStream in = getClass().getResourceAsStream(filename);
				Files.copy(in, destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
				logger.info("Unpacking %s", destFile.toPath());
			}

			Bob.initLua();
			final Platform host = Platform.getHostPlatform();
			luaJITExePath = Bob.getExe(host, host.is64bit() ? "luajit-64" : "luajit-32");

			Project project = new Project(new DefaultFileSystem());
			project.loadProjectFile();
			disabled = project.getProjectProperties().getBooleanValue("prometheus", "disabled", false);
			logger.info("Prometheus is %s", disabled ? "disabled" : "enabled");
		}
		catch (Exception e) {
			throw new java.lang.InstantiationException(e.getMessage());
		}
	}

	private File createTempFile() throws IOException {
		return File.createTempFile("prometheus", "");
	}

	private File writeToTempFile(String input) throws IOException {
		File tempFile = createTempFile();
		Files.write(tempFile.toPath(), input.getBytes());
		return tempFile;
	}

	private File writeToTempFile(InputStream in) throws IOException {
		File tempFile = createTempFile();
		Files.copy(in, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
		return tempFile;
	}

	private String readFile(File file) throws IOException {
		return Files.readString(file.toPath());
	}

	private String getCliPath() throws IOException {
		File cli = new File(unpackedRoot, "cli.lua");
		return cli.toString();
	}

	/**
	 * Find the project root by traversing the path "up" until a
	 * game.project file is found
	 * @param path The path to search
	 * @return The project root
	 */
	private File getProjectRoot(String path) {
		if (projectRoot != null) {
			return projectRoot;
		}
		File current = new File(path).getParentFile();
		while (current != null) {
			File gameProject = new File(current, "game.project");
			if (gameProject.exists()) {
				projectRoot = current;
				break;
			}
			current = current.getParentFile();
		}
		return projectRoot;
	}

	@Override
	public String obfuscate(String input, String path, String buildVariant) throws Exception {
		if (disabled) {
			return input;
		}

		try {
			File projectRoot = getProjectRoot(path);
			if (projectRoot == null) {
				throw new Exception("Unable to find project root for " + path);
			}

			File inputFile = writeToTempFile(input);
			File outputFile = createTempFile();
			File configFile = new File(projectRoot, "prometheus.lua");

			// command line arguments to launch prometheus
			List<String> options = new ArrayList<String>();
			options.add(luaJITExePath);
			options.add(getCliPath().toString());
			options.add("--config");
			options.add(configFile.getAbsolutePath());
			options.add("--out");
			options.add(outputFile.getAbsolutePath());
			options.add(inputFile.getAbsolutePath());

			logger.info("Obfuscating %s to %s", path, outputFile);

			// launch the process
			ProcessBuilder pb = new ProcessBuilder(options).redirectErrorStream(true);
			java.util.Map<String, String> env = pb.environment();
			env.put("LUA_PATH", Bob.getPath("share/luajit/") + "/?.lua");
			Process p = pb.start();
			int ret = p.waitFor();

			// get all of the output from the process
			InputStream is = p.getInputStream();
			byte[] output_bytes = new byte[is.available()];
			is.read(output_bytes);
			is.close();

			if (ret != 0) {
				System.err.println(new String(output_bytes));
				throw new Exception("Unable to run prometheus, return code: " + ret);
			}
			String o = readFile(outputFile);
			// logger.info(o);
			return o;
		}
		catch(Exception e) {
			e.printStackTrace();
			throw new Exception("Unable to run prometheus, ", e);
		}
	}
}