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
		"/prometheus/compiler_secure/instructionkind.lua",
		"/prometheus/compiler_secure/ir.lua",
		"/prometheus/compiler_secure/vmstrings.lua",
		"/prometheus/compiler_secure/compiler.lua",
		"/prometheus/compiler_secure/bytecode.lua",
		"/prometheus/steps.lua",
		"/prometheus/parser.lua",
		"/prometheus/randomStrings.lua",
		"/prometheus/bit.lua",
		"/prometheus/enums.lua",
		"/prometheus/step.lua",
		"/prometheus/scope.lua",
		"/prometheus/visitast.lua",
		"/prometheus/unparser.lua",
		"/prometheus/namegenerators/number.lua",
		"/prometheus/namegenerators/mangled_shuffled.lua",
		"/prometheus/namegenerators/Il.lua",
		"/prometheus/namegenerators/mangled.lua",
		"/prometheus/steps/ProxifyLocals.lua",
		"/prometheus/steps/ConstantArray.lua",
		"/prometheus/steps/WrapInFunction.lua",
		"/prometheus/steps/LocalsToTable.lua",
		"/prometheus/steps/Vmify.lua",
		"/prometheus/steps/SplitStrings.lua",
		"/prometheus/tokenizer.lua",
		"/prometheus/pipeline.lua",
		"/prometheus/namegenerators.lua",
		"/prometheus/util.lua",
		"/prometheus/randomLiterals.lua",
		"/prometheus/ast.lua",
	};


	private static String minifierPath = null;

	private static boolean unpackedSource = false;
	private static File unpackedRoot = null;


	private static File writeToTempFile(String input) throws IOException {
		File tempFile = File.createTempFile("luamin", "");
		Files.write(tempFile.toPath(), input.getBytes());
		return tempFile;
	}

	private static File writeToTempFile(InputStream in) throws IOException {
		File tempFile = File.createTempFile("luamin", "");
		Files.copy(in, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
		return tempFile;
	}

	private String getMinifierPath() throws IOException {
		if (minifierPath != null) {
			return minifierPath;
		}
		// InputStream in = getClass().getResourceAsStream("/minify.lua");
		File out = writeToTempFile(LuaMinifierSource.get());
		minifierPath = out.getAbsolutePath();
		return minifierPath;
	}


	private void unpackSource() throws IOException {
		if (unpackedSource) {
			return;
		}

		unpackedRoot = Files.createTempDirectory(null).toFile();
		Bob.verbose("unpacked_root " + unpackedRoot);
		unpackedRoot = new File(".");

		for (String source : PROMETHEUS_SOURCES) {
			File sourceFile = new File(unpackedRoot, source);
			Bob.verbose("unpack " + sourceFile);
			Files.createDirectories(sourceFile.getParentFile().toPath());
			InputStream in = getClass().getResourceAsStream(source);
			Files.copy(in, sourceFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}
		unpackedSource = true;
	}

	@Override
	public String build(String input) throws Exception {
		try {
			Bob.verbose("build");
			unpackSource();


			File inputFile = writeToTempFile(input);

			// command line arguments to launch lua-minify
			List<String> options = new ArrayList<String>();
			options.add("lua");
			options.add(getMinifierPath());
			options.add("minify");
			options.add(inputFile.getAbsolutePath());

			// launch the process
			ProcessBuilder pb = new ProcessBuilder(options).redirectErrorStream(true);
			Process p = pb.start();
			int ret = p.waitFor();

			// get all of the output from the process
			InputStream is = p.getInputStream();
			byte[] output_bytes = new byte[is.available()];
			is.read(output_bytes);
			is.close();

			// this is either the obfuscated code or the error output
			String output = new String(output_bytes);

			Bob.verbose(output);

			inputFile.delete();

			if (ret != 0) {
				System.err.println(output);
				throw new Exception("Unable to run lua-minify, return code: " + ret);
			}
			return output;
		}
		catch(Exception e) {
			throw new Exception("Unable to run lua-minify, ", e);
		}
	}
}