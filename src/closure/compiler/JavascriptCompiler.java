package closure.compiler;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import com.google.common.collect.ImmutableList;
import com.google.javascript.jscomp.CompilationLevel;
import com.google.javascript.jscomp.Compiler;
import com.google.javascript.jscomp.CompilerOptions;
import com.google.javascript.jscomp.Result;
import com.google.javascript.jscomp.SourceFile;
import com.google.javascript.jscomp.SourceMap;

public class JavascriptCompiler {

	static final List<SourceFile> EXTERNS = ImmutableList.of(
		      SourceFile.fromCode("externs", ""));
	
	public static void main(String[] args){
		System.out.println("Start compiling javascript");
		
		File sourceFile = new File("js/jquery.js");
		File minFile = new File("js/min/jquery.js");
		File sourceMapFile = new File("js/min/jquery.js.map");
		
		try {
			
			CompiledResult result = compile(
					sourceFile,
					minFile,
					sourceMapFile,
					CompilationLevel.SIMPLE_OPTIMIZATIONS);
			
			writeToFile(minFile, result.CompiledSource);
		    writeToFile(sourceMapFile, result.SourceMap);
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println("Done!");
	}
	
	private static CompiledResult compile(File sourceFile, File minFile, File sourceMapFile, CompilationLevel level) throws IOException {
		Compiler compiler = new Compiler();
		Compiler.setLoggingLevel(Level.INFO);

		CompilerOptions options = new CompilerOptions();
		options.sourceMapOutputPath = minFile.getName();
		options.sourceMapFormat = SourceMap.Format.V3;
		
		// Fix sourceMap to source script reference.
		options.sourceMapLocationMappings = new ArrayList<SourceMap.LocationMapping>();
		options.sourceMapLocationMappings.add(new SourceMap.LocationMapping("js/", "../"));	
		
		level.setOptionsForCompilationLevel(options);
		level.setTypeBasedOptimizationOptions(options);
		
		List<SourceFile> inputs =
		        ImmutableList.of(SourceFile.fromFile(sourceFile));
		
		Result result = compiler.compile(EXTERNS, inputs, options);
		
		CompiledResult compiledResult = new CompiledResult();
		compiledResult.CompiledSource = getCompiledSource(compiler, sourceMapFile);
		compiledResult.SourceMap = getSourceMap(result, minFile);
		
		return compiledResult;
	}
	
	/*
	 * Append sourceMappingUrl to compiled source.
	 */
	private static String getCompiledSource(Compiler compiler, File sourceMapFile) {
		String mappingUrl = String.format("//@ sourceMappingURL=%s", sourceMapFile.getName());
		String compiledSource = compiler.toSource();
		
		return String.format("%s\n/*\n%s\n*/", compiledSource, mappingUrl);
	}
	
	/*
	 * Get sourceMap content.
	 */
	private static String getSourceMap(Result result, File minFile) {
		StringBuilder sb = new StringBuilder();
	    try {
	      result.sourceMap.validate(true);
	      result.sourceMap.appendTo(sb, minFile.getName());
	    } catch (IOException e) {
	      throw new RuntimeException("unexpected exception", e);
	    }
	    String XSSI_guard = ")]}";
	    
		return String.format("%s\n%s", XSSI_guard, sb);
	}
	
	private static void writeToFile(File file, String content) throws IOException {
		
		BufferedWriter writer= new BufferedWriter(new FileWriter(file));
		
		try {
			writer.write(content);
		}
		finally {
			writer.close();
		}
	}
	
	private static class CompiledResult {
		public String CompiledSource;
		public String SourceMap;
	}
}
