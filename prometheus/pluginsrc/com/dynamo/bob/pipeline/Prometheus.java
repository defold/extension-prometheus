package com.dynamo.bob.pipeline;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.ArrayList;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;


import com.dynamo.bob.Bob;
import com.dynamo.bob.Platform;
import com.dynamo.bob.pipeline.LuaBuilderPlugin;


public class Prometheus extends LuaBuilderPlugin {

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


	private static String minifierPath = null;

	private static boolean unpackedSource = false;
	private static File unpackedRoot = null;

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

	private void unpackPrometheusSource() throws IOException {
		if (unpackedSource) {
			return;
		}

		unpackedRoot = Files.createTempDirectory(null).toFile();

		for (String filename : PROMETHEUS_SOURCES) {
			File destFile = new File(unpackedRoot, filename);
			destFile.getParentFile().mkdirs();
			destFile.deleteOnExit();
			InputStream in = getClass().getResourceAsStream(filename);
			Files.copy(in, destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
		unpackedSource = true;
	}

	@Override
	public String create(String input) throws Exception {
		return input;
	}

	@Override
	public String build(String input) throws Exception {
		try {
			Bob.initLua();
			unpackPrometheusSource();

			File inputFile = writeToTempFile(input);
			File outputFile = createTempFile();

			// command line arguments to launch prometheus
			List<String> options = new ArrayList<String>();
			options.add(Bob.getExe(Platform.getHostPlatform(), "luajit-64"));
			options.add(getCliPath().toString());
			options.add("--config");
			options.add("prometheus.lua");
			options.add("--out");
			options.add(outputFile.getAbsolutePath());
			options.add(inputFile.getAbsolutePath());

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
			return readFile(outputFile);
		}
		catch(Exception e) {
			e.printStackTrace();
			throw new Exception("Unable to run prometheus, ", e);
		}
	}
}