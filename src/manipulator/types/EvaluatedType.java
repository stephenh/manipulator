package manipulator.types;

import org.antlr.v4.runtime.ANTLRFileStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStreamRewriter;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import manipulator.antlr.JavaBaseListener;
import manipulator.antlr.JavaLexer;
import manipulator.antlr.JavaParser;
import manipulator.antlr.JavaParser.FormalParameterContext;
import manipulator.antlr.JavaParser.FormalParametersContext;
import manipulator.antlr.JavaParser.InterfaceDeclarationContext;
import manipulator.antlr.JavaParser.InterfaceMethodDeclarationContext;

public class EvaluatedType {

  private final List<InterfaceMethodDeclarationContext> interfaceMethodContexts = new ArrayList<>();
  private final TreeMap<String, String> importedSimpleTypeNames = new TreeMap<>();
  private final CommonTokenStream tokenStream;

  private String packageName = null;
  private String canonicalName = null;
  private String simpleName = null;
  private String concreteName = null;
  private InterfaceDeclarationContext intfDeclaration = null;

  public EvaluatedType(File typeFile) {
    try {
      JavaLexer lexer = new JavaLexer(new ANTLRFileStream(typeFile.getCanonicalPath()));
      this.tokenStream = new CommonTokenStream(lexer);
      this.parseTypeFile();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void parseTypeFile() {
    new ParseTreeWalker().walk(new JavaBaseListener() {
      @Override
      public void exitPackageDeclaration(JavaParser.PackageDeclarationContext ctx) {
        packageName = ctx.qualifiedName().getText();
      }

      @Override
      public void exitImportDeclaration(JavaParser.ImportDeclarationContext ctx) {
        importedSimpleTypeNames.put(ctx.qualifiedName().stop.getText(), ctx.qualifiedName().getText());
      }

      @Override
      public void enterInterfaceDeclaration(JavaParser.InterfaceDeclarationContext ctx) {
        // Note that this bound *enter* rather than *exit* so that the canonical type name is immediately available for other purposes
        simpleName = ctx.Identifier().getText();
        concreteName = simpleName.replaceAll("^Is", "");
        canonicalName = packageName + "." + simpleName;
        intfDeclaration = ctx;
      }

      @Override
      public void exitInterfaceMethodDeclaration(JavaParser.InterfaceMethodDeclarationContext ctx) {
        interfaceMethodContexts.add(ctx);
      }
    }, new JavaParser(this.tokenStream).compilationUnit());
  }

  public String getJavadocBoilerPlatedEdition() {
    TokenStreamRewriter rewriter = new TokenStreamRewriter(tokenStream);
    deleteExistingClassComment(rewriter);
    addNewClassComment(rewriter);
    for (InterfaceMethodDeclarationContext imdc : interfaceMethodContexts) {
      deleteExistingMethodComment(rewriter, imdc);
      if (imdc.Identifier().getText().equals("as" + concreteName)) {
        addAsMethodComment(rewriter, imdc);
      } else {
        addNewMethodComment(rewriter, imdc);
      }
    }
    return rewriter.getText();
  }

  private String resolveType(String potentiallyAlreadyCanonicalTypeName) {
    // Is it already qualified?
    if (potentiallyAlreadyCanonicalTypeName.contains(".")) {
      return potentiallyAlreadyCanonicalTypeName; // presume so if there is a dot in the name
    }

    String[] arrayTypeSplit = potentiallyAlreadyCanonicalTypeName.split("\\[\\]");
    boolean isArrayType = potentiallyAlreadyCanonicalTypeName.contains("[]");
    String nonArraySimpleName = arrayTypeSplit[0];

    // Is this an imported type?
    if (importedSimpleTypeNames.containsKey(nonArraySimpleName)) {
      return importedSimpleTypeNames.get(nonArraySimpleName) + (isArrayType ? "[]" : "");
    }

    // Is it a self-referential type?
    if (nonArraySimpleName.equals(simpleName)) {
      return canonicalName + (isArrayType ? "[]" : "");
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

  private void addNewClassComment(TokenStreamRewriter rewriter) {
    StringBuilder javadoc = new StringBuilder();
    javadoc.append("/**\n");
    javadoc.append(" * An interface for {@link " + concreteName + "}.\n");
    javadoc.append(" */\n");
    rewriter.insertBefore(intfDeclaration.parent.getSourceInterval().a, javadoc.toString());
  }

  private void addNewMethodComment(TokenStreamRewriter rewriter, InterfaceMethodDeclarationContext imdc) {
    String methodName = imdc.Identifier().getText();
    String signature = generateFriendlyMethodSignature(methodName, imdc.formalParameters());
    StringBuilder javadoc = new StringBuilder();
    javadoc.append("/**\n");
    javadoc.append("   * See {@link " + concreteName + "#" + signature + "}.\n");
    javadoc.append("   */\n  ");
    rewriter.insertBefore(imdc.parent.parent.getSourceInterval().a, javadoc.toString());
  }

  private void addAsMethodComment(TokenStreamRewriter rewriter, InterfaceMethodDeclarationContext imdc) {
    StringBuilder javadoc = new StringBuilder();
    javadoc.append("/**\n");
    javadoc.append("   * @return the concrete {@link " + concreteName + "}\n");
    javadoc.append("   */\n  ");
    rewriter.insertBefore(imdc.parent.parent.getSourceInterval().a, javadoc.toString());
  }

  private void deleteExistingMethodComment(TokenStreamRewriter rewriter, InterfaceMethodDeclarationContext imdc) {
    // imdc InterfaceMethodDeclarationContext -> interfaceMemberDecl -> interfaceBodyDecl
    List<Token> hidden = tokenStream.getHiddenTokensToLeft(imdc.getParent().getParent().start.getTokenIndex());
    deleteAfterFirstComment(rewriter, hidden);
  }

  private void deleteExistingClassComment(TokenStreamRewriter rewriter) {
    // ctx interfaceDeclaration -> parent typeDeclaration
    List<Token> hidden = tokenStream.getHiddenTokensToLeft(intfDeclaration.getParent().start.getTokenIndex());
    deleteAfterFirstComment(rewriter, hidden);
  }

  private void deleteAfterFirstComment(TokenStreamRewriter rewriter, List<Token> hidden) {
    if (hidden != null && !hidden.isEmpty()) {
      // could have: \n\n, /** blah */, \n -- and we want to keep the first \n\n
      boolean foundComment = false;
      for (Token token : hidden) {
        if (token.getText().contains("/")) {
          foundComment = true;
        }
        if (foundComment) {
          rewriter.delete(token);
        }
      }
    }
  }

  private String generateFriendlyMethodSignature(String methodName, FormalParametersContext fpc) {
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
