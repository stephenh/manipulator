package manipulator.types;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import manipulator.antlr.JavaBaseListener;
import manipulator.antlr.JavaLexer;
import manipulator.antlr.JavaParser;
import manipulator.antlr.JavaParser.FormalParameterContext;
import manipulator.antlr.JavaParser.FormalParametersContext;
import manipulator.antlr.JavaParser.InterfaceMethodDeclarationContext;

import org.antlr.v4.runtime.ANTLRFileStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.TokenStreamRewriter;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apache.commons.lang3.StringUtils;

public class EvaluatedType {
	public final String filePath;
	private CommonTokenStream tokenStream = null;
	public TreeMap<String, InterfaceMethodDeclarationContext> interfaceMethodContexts = new TreeMap<>();
	public String packageName = null;
	public String canonicalName = null;
	public String simpleName = null;
	public TreeMap<String, String> importedSimpleTypeNames = new TreeMap<>();

	public EvaluatedType(File typeFile) {
		try {
			this.filePath = typeFile.getCanonicalPath();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		this.parseTypeFile();
		/*
		 * for (String signature : this.interfaceMethodContexts.keySet()) { System.out.println(signature); }
		 */
	}

	private void parseTypeFile() {
		try {
			JavaLexer lexer = new JavaLexer(new ANTLRFileStream(this.filePath));
			this.tokenStream = new CommonTokenStream(lexer);
			JavaParser parser = new JavaParser(this.tokenStream);
			new ParseTreeWalker().walk(new JavaBaseListener() {
				@Override
				public void exitPackageDeclaration(JavaParser.PackageDeclarationContext ctx) {
					EvaluatedType.this.packageName = ctx.qualifiedName().getText();
				}

				@Override
				public void exitImportDeclaration(JavaParser.ImportDeclarationContext ctx) {
					EvaluatedType.this.importedSimpleTypeNames.put(ctx.qualifiedName().stop.getText(), ctx.qualifiedName().getText());
				}

				@Override
				public void enterInterfaceDeclaration(JavaParser.InterfaceDeclarationContext ctx) {
					// Note that this bound *enter* rather than *exit* so that the canonical type name is immediately available for other purposes
					EvaluatedType.this.simpleName = ctx.Identifier().getText();
					EvaluatedType.this.canonicalName = EvaluatedType.this.packageName + "." + EvaluatedType.this.simpleName;
				}

				@Override
				public void exitInterfaceMethodDeclaration(JavaParser.InterfaceMethodDeclarationContext ctx) {
					String methodName = ctx.Identifier().getText();
					EvaluatedType.this.interfaceMethodContexts.put(EvaluatedType.this.generateFriendlyMethodSignature(methodName, ctx.formalParameters()), ctx);
				}
			}, parser.compilationUnit());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public String getJavadocBoilerPlatedEdition() {
		TokenStreamRewriter rewriter = new TokenStreamRewriter(this.tokenStream);
		for (Map.Entry<String, InterfaceMethodDeclarationContext> interfaceMethod : this.interfaceMethodContexts.entrySet()) {
			InterfaceMethodDeclarationContext imdc = interfaceMethod.getValue();
			String friendlyMethodDescriptor = interfaceMethod.getKey();
			StringBuilder javadoc = new StringBuilder();
			javadoc.append("/**\n");
			javadoc.append("\t* @see SomeTypeHere.");
			javadoc.append(friendlyMethodDescriptor);
			javadoc.append("\n\t*/\n\t");
			rewriter.insertBefore(imdc.parent.parent.getSourceInterval().a, javadoc.toString());
		}
		return rewriter.getText();
	}

	public String resolveType(String potentiallyAlreadyCanonicalTypeName) {
		// Is it already qualified?
		if (potentiallyAlreadyCanonicalTypeName.contains(".")) {
			return potentiallyAlreadyCanonicalTypeName; // presume so if there is a dot in the name
		}

		String[] arrayTypeSplit = potentiallyAlreadyCanonicalTypeName.split("\\[\\]");
		boolean isArrayType = potentiallyAlreadyCanonicalTypeName.contains("[]");
		String nonArraySimpleName = arrayTypeSplit[0];

		// Is this an imported type?
		if (this.importedSimpleTypeNames.containsKey(nonArraySimpleName)) {
			return this.importedSimpleTypeNames.get(nonArraySimpleName) + (isArrayType ? "[]" : "");
		}

		// Is it a self-referential type?
		if (nonArraySimpleName.equals(this.simpleName)) {
			return this.canonicalName + (isArrayType ? "[]" : "");
		}

		// Is it on the search path?
		try {
			String searchPackageTypeName = "java.lang." + nonArraySimpleName;
			Class.forName(searchPackageTypeName);
			return searchPackageTypeName + (isArrayType ? "[]" : "");
		} catch (Exception e) {
			// Nope.
		}

		// Give up and just hand back the original value.
		return potentiallyAlreadyCanonicalTypeName;
	}

	public String generateFriendlyMethodSignature(String methodName, FormalParametersContext fpc) {
		ArrayList<String> types = new ArrayList<>();
		StringBuilder typeSignature = new StringBuilder();
		if (fpc.formalParameterList() != null) {
			for (FormalParameterContext parameter : fpc.formalParameterList().formalParameter()) {
				types.add(this.resolveType(parameter.type().getText()));
			}
			if (fpc.formalParameterList().lastFormalParameter() != null) {
				types.add(this.resolveType(fpc.formalParameterList().lastFormalParameter().type().getText()) + "...");
			}
			typeSignature.append(StringUtils.join(types, ", "));
		}
		return String.format("%s(%s)", methodName, typeSignature.toString());
	}
}
