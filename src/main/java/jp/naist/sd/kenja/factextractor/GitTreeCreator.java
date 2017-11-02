package jp.naist.sd.kenja.factextractor;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.List;
import java.io.IOException;

import jp.naist.sd.kenja.factextractor.ast.ASTCompilation;
import jp.naist.sd.kenja.factextractor.ast.CommentVisitor;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepository;
import org.eclipse.jdt.core.dom.Comment;
import org.eclipse.jdt.core.dom.LineComment;
import org.eclipse.jdt.core.dom.BlockComment;

public class GitTreeCreator {
  private Tree root = new Tree("");

  private ASTCompilation compilation;

  public GitTreeCreator() {
  }

  private void parseSourcecode(char[] src) {
    ASTParser parser = ASTParser.newParser(AST.JLS4);
    parser.setSource(src);
  
    NullProgressMonitor nullMonitor = new NullProgressMonitor();
    CompilationUnit unit = (CompilationUnit) parser.createAST(nullMonitor);
    
    List<?> commentList = unit.getCommentList();
    System.out.println(commentList.toString());
    
    String[] splitted = String.valueOf(src).split("\n"); 
    CommentVisitor cv = new CommentVisitor(unit, splitted);
    // 標準入力で与えたソースコードの表示
    for(int i=0; i < splitted.length; i++) {
    	System.out.println(splitted[i]);
    }
    
    // LineCommentの表示
    for(int i = 0;i < commentList.size();i++) {
    	if(((Comment) commentList.get(i)).isLineComment()) {
    		cv.visit((LineComment)commentList.get(i));
    	}else if(((Comment) commentList.get(i)).isBlockComment()){
    		cv.visit((BlockComment)commentList.get(i));
    	}
    	
    		
    } 
    
    compilation = new ASTCompilation(unit, root);
  }

  private void parseSourcecodeAndWriteSyntaxTree(char[] src, String outputPath) {
    File outputFile = new File(outputPath);
    parseSourcecodeAndWriteSyntaxTree(src, outputFile);
  }

  private void parseSourcecodeAndWriteSyntaxTree(char[] src, File outputFile) {
    parseSourcecode(src);
    writeASTAsFileTree(outputFile);
  }

  private void parseBlobs(String repositoryPath, String syntaxTreeDirPath) {
    File repoDir = new File(repositoryPath);
    try {
      Repository repo = new FileRepository(repoDir);

      BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
      String line;
      while ( (line = br.readLine()) != null) {
        root = new Tree("");

        ObjectId obj = ObjectId.fromString(line);
        ObjectLoader loader = repo.open(obj);

        char[] src = IOUtils.toCharArray(loader.openStream());
        File outputFile = new File(syntaxTreeDirPath, line);
        parseSourcecodeAndWriteSyntaxTree(src, outputFile);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  public void writeASTAsFileTree(File outputFile) {
    try {
      TreeWriter writer = new TextFormatTreeWriter(outputFile);
      writer.writeTree(compilation.getTree());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public static void main(String[] args) {
    if (args.length > 2) {
      System.out.println("Usage(1): path_of_output_file");
      System.out.println("Usage(2); path_of_git_repository path_of_syntax_trees_dir");
      return;
    }

    GitTreeCreator creator = new GitTreeCreator();

    if (args.length == 1) {
      try {
        char[] src = IOUtils.toCharArray(System.in);
        creator.parseSourcecodeAndWriteSyntaxTree(src, args[0]);
      } catch (IOException e) {
        e.printStackTrace();
      }
    } else {
      creator.parseBlobs(args[0], args[1]);
    }
  }
}
