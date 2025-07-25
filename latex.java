import java.util.*;
import java.util.regex.*;
import java.io.*;
import java.nio.file.*;

// Token types for lexical analysis
enum TokenType {
    COMMAND, TEXT, LBRACE, RBRACE, LBRACKET, RBRACKET, NEWLINE, EOF, MATH_INLINE, MATH_DISPLAY
}

// Token class
class Token {
    TokenType type;
    String value;
    int position;
    
    Token(TokenType type, String value, int position) {
        this.type = type;
        this.value = value;
        this.position = position;
    }
    
    @Override
    public String toString() {
        return String.format("Token(%s, '%s', %d)", type, value, position);
    }
}

// AST Node types
abstract class ASTNode {
    abstract String toHTML();
}

class TextNode extends ASTNode {
    String content;
    
    TextNode(String content) {
        this.content = content;
    }
    
    @Override
    String toHTML() {
        return escapeHTML(content);
    }
    
    private String escapeHTML(String text) {
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#39;");
    }
}

class CommandNode extends ASTNode {
    String command;
    List<ASTNode> arguments;
    List<ASTNode> optionalArgs;
    List<ASTNode> body;
    
    CommandNode(String command) {
        this.command = command;
        this.arguments = new ArrayList<>();
        this.optionalArgs = new ArrayList<>();
        this.body = new ArrayList<>();
    }
    
    @Override
    String toHTML() {
        switch (command) {
            case "documentclass":
                return "<!DOCTYPE html>\n<html>\n<head>\n<meta charset=\"UTF-8\">\n";
                
            case "title":
                if (!arguments.isEmpty()) {
                    return "<title>" + arguments.get(0).toHTML() + "</title>\n";
                }
                return "";
                
            case "author":
                if (!arguments.isEmpty()) {
                    return "<meta name=\"author\" content=\"" + arguments.get(0).toHTML() + "\">\n";
                }
                return "";
                
            case "begin":
                if (!arguments.isEmpty()) {
                    String env = ((TextNode)arguments.get(0)).content;
                    switch (env) {
                        case "document":
                            return "</head>\n<body>\n";
                        case "itemize":
                            return "<ul>\n";
                        case "enumerate":
                            return "<ol>\n";
                        case "center":
                            return "<div style=\"text-align: center;\">\n";
                        default:
                            return "<div class=\"" + env + "\">\n";
                    }
                }
                return "";
                
            case "end":
                if (!arguments.isEmpty()) {
                    String env = ((TextNode)arguments.get(0)).content;
                    switch (env) {
                        case "document":
                            return "\n</body>\n</html>";
                        case "itemize":
                            return "</ul>\n";
                        case "enumerate":
                            return "</ol>\n";
                        case "center":
                            return "</div>\n";
                        default:
                            return "</div>\n";
                    }
                }
                return "";
                
            case "section":
                if (!arguments.isEmpty()) {
                    return "<h1>" + arguments.get(0).toHTML() + "</h1>\n";
                }
                return "";
                
            case "subsection":
                if (!arguments.isEmpty()) {
                    return "<h2>" + arguments.get(0).toHTML() + "</h2>\n";
                }
                return "";
                
            case "subsubsection":
                if (!arguments.isEmpty()) {
                    return "<h3>" + arguments.get(0).toHTML() + "</h3>\n";
                }
                return "";
                
            case "textbf":
                if (!arguments.isEmpty()) {
                    return "<strong>" + arguments.get(0).toHTML() + "</strong>";
                }
                return "";
                
            case "textit":
                if (!arguments.isEmpty()) {
                    return "<em>" + arguments.get(0).toHTML() + "</em>";
                }
                return "";
                
            case "texttt":
                if (!arguments.isEmpty()) {
                    return "<code>" + arguments.get(0).toHTML() + "</code>";
                }
                return "";
                
            case "item":
                return "<li>";
                
            case "maketitle":
                return "<div class=\"title-page\">\n";
                
            case "par":
                return "<p>";
                
            case "\\\\": // Line break
                return "<br>\n";
                
            default:
                return "<!-- Unknown command: " + command + " -->";
        }
    }
}

class MathNode extends ASTNode {
    String content;
    boolean isDisplayMode;
    
    MathNode(String content, boolean isDisplayMode) {
        this.content = content;
        this.isDisplayMode = isDisplayMode;
    }
    
    @Override
    String toHTML() {
        if (isDisplayMode) {
            return "<div class=\"math-display\">$$" + content + "$$</div>";
        } else {
            return "<span class=\"math-inline\">$" + content + "$</span>";
        }
    }
}

class DocumentNode extends ASTNode {
    List<ASTNode> children;
    
    DocumentNode() {
        this.children = new ArrayList<>();
    }
    
    void addChild(ASTNode child) {
        children.add(child);
    }
    
    @Override
    String toHTML() {
        StringBuilder sb = new StringBuilder();
        for (ASTNode child : children) {
            sb.append(child.toHTML());
        }
        return sb.toString();
    }
}

// Lexer class
class LaTeXLexer {
    private String input;
    private int position;
    private List<Token> tokens;
    
    LaTeXLexer(String input) {
        this.input = input;
        this.position = 0;
        this.tokens = new ArrayList<>();
    }
    
    List<Token> tokenize() {
        while (position < input.length()) {
            char ch = input.charAt(position);
            
            if (ch == '\\') {
                readCommand();
            } else if (ch == '{') {
                tokens.add(new Token(TokenType.LBRACE, "{", position));
                position++;
            } else if (ch == '}') {
                tokens.add(new Token(TokenType.RBRACE, "}", position));
                position++;
            } else if (ch == '[') {
                tokens.add(new Token(TokenType.LBRACKET, "[", position));
                position++;
            } else if (ch == ']') {
                tokens.add(new Token(TokenType.RBRACKET, "]", position));
                position++;
            } else if (ch == '$') {
                readMath();
            } else if (ch == '\n') {
                tokens.add(new Token(TokenType.NEWLINE, "\n", position));
                position++;
            } else if (Character.isWhitespace(ch)) {
                position++; // Skip whitespace except newlines
            } else {
                readText();
            }
        }
        
        tokens.add(new Token(TokenType.EOF, "", position));
        return tokens;
    }
    
    private void readCommand() {
        int start = position;
        position++; // Skip backslash
        
        if (position < input.length() && input.charAt(position) == '\\') {
            // Double backslash
            position++;
            tokens.add(new Token(TokenType.COMMAND, "\\\\", start));
            return;
        }
        
        StringBuilder command = new StringBuilder();
        while (position < input.length() && 
               (Character.isLetter(input.charAt(position)) || 
                (command.length() == 0 && !Character.isLetter(input.charAt(position))))) {
            command.append(input.charAt(position));
            position++;
            if (!Character.isLetter(input.charAt(position - 1))) break;
        }
        
        tokens.add(new Token(TokenType.COMMAND, command.toString(), start));
    }
    
    private void readText() {
        int start = position;
        StringBuilder text = new StringBuilder();
        
        while (position < input.length()) {
            char ch = input.charAt(position);
            if (ch == '\\' || ch == '{' || ch == '}' || ch == '[' || ch == ']' || 
                ch == '$' || ch == '\n') {
                break;
            }
            text.append(ch);
            position++;
        }
        
        if (text.length() > 0) {
            tokens.add(new Token(TokenType.TEXT, text.toString(), start));
        }
    }
    
    private void readMath() {
        int start = position;
        position++; // Skip first $
        
        boolean isDisplayMode = false;
        if (position < input.length() && input.charAt(position) == '$') {
            isDisplayMode = true;
            position++; // Skip second $
        }
        
        StringBuilder math = new StringBuilder();
        while (position < input.length()) {
            char ch = input.charAt(position);
            if (ch == '$') {
                position++;
                if (isDisplayMode && position < input.length() && input.charAt(position) == '$') {
                    position++; // Skip second $
                }
                break;
            }
            math.append(ch);
            position++;
        }
        
        TokenType type = isDisplayMode ? TokenType.MATH_DISPLAY : TokenType.MATH_INLINE;
        tokens.add(new Token(type, math.toString(), start));
    }
}

// Parser class
class LaTeXParser {
    private List<Token> tokens;
    private int position;
    
    LaTeXParser(List<Token> tokens) {
        this.tokens = tokens;
        this.position = 0;
    }
    
    DocumentNode parse() {
        DocumentNode document = new DocumentNode();
        
        while (!isAtEnd()) {
            ASTNode node = parseElement();
            if (node != null) {
                document.addChild(node);
            }
        }
        
        return document;
    }
    
    private ASTNode parseElement() {
        Token current = getCurrentToken();
        
        switch (current.type) {
            case COMMAND:
                return parseCommand();
            case TEXT:
                advance();
                return new TextNode(current.value);
            case MATH_INLINE:
                advance();
                return new MathNode(current.value, false);
            case MATH_DISPLAY:
                advance();
                return new MathNode(current.value, true);
            case NEWLINE:
                advance();
                return new TextNode(" "); // Convert newlines to spaces
            default:
                advance();
                return null;
        }
    }
    
    private CommandNode parseCommand() {
        Token commandToken = advance();
        CommandNode command = new CommandNode(commandToken.value);
        
        // Parse optional arguments [...]
        while (match(TokenType.LBRACKET)) {
            List<ASTNode> optArg = new ArrayList<>();
            while (!check(TokenType.RBRACKET) && !isAtEnd()) {
                ASTNode element = parseElement();
                if (element != null) {
                    optArg.add(element);
                }
            }
            consume(TokenType.RBRACKET, "Expected ']'");
            command.optionalArgs.addAll(optArg);
        }
        
        // Parse required arguments {...}
        while (match(TokenType.LBRACE)) {
            List<ASTNode> arg = new ArrayList<>();
            while (!check(TokenType.RBRACE) && !isAtEnd()) {
                ASTNode element = parseElement();
                if (element != null) {
                    arg.add(element);
                }
            }
            consume(TokenType.RBRACE, "Expected '}'");
            
            if (arg.size() == 1) {
                command.arguments.add(arg.get(0));
            } else if (arg.size() > 1) {
                DocumentNode group = new DocumentNode();
                for (ASTNode node : arg) {
                    group.addChild(node);
                }
                command.arguments.add(group);
            } else {
                command.arguments.add(new TextNode(""));
            }
        }
        
        return command;
    }
    
    private Token getCurrentToken() {
        if (isAtEnd()) return tokens.get(tokens.size() - 1);
        return tokens.get(position);
    }
    
    private Token advance() {
        if (!isAtEnd()) position++;
        return tokens.get(position - 1);
    }
    
    private boolean isAtEnd() {
        return position >= tokens.size() || getCurrentToken().type == TokenType.EOF;
    }
    
    private boolean match(TokenType... types) {
        for (TokenType type : types) {
            if (check(type)) {
                advance();
                return true;
            }
        }
        return false;
    }
    
    private boolean check(TokenType type) {
        if (isAtEnd()) return false;
        return getCurrentToken().type == type;
    }
    
    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        throw new RuntimeException(message + " at position " + position);
    }
}

// Main compiler class
public class LaTeXCompiler {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java LaTeXCompiler <input.tex> <output.html>");
            return;
        }
        
        try {
            String inputFile = args[0];
            String outputFile = args[1];
            
            // Read input file
            String content = Files.readString(Paths.get(inputFile));
            
            // Compile LaTeX to HTML
            String html = compile(content);
            
            // Write output file
            Files.write(Paths.get(outputFile), html.getBytes());
            
            System.out.println("Compilation successful: " + outputFile);
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static String compile(String latexSource) {
        try {
            // Lexical analysis
            LaTeXLexer lexer = new LaTeXLexer(latexSource);
            List<Token> tokens = lexer.tokenize();
            
            // Syntax analysis
            LaTeXParser parser = new LaTeXParser(tokens);
            DocumentNode ast = parser.parse();
            
            // Code generation
            String html = ast.toHTML();
            
            // Add CSS for math rendering
            String mathJaxScript = "\n<script src=\"https://polyfill.io/v3/polyfill.min.js?features=es6\"></script>\n" +
                                  "<script id=\"MathJax-script\" async src=\"https://cdn.jsdelivr.net/npm/mathjax@3/es5/tex-mml-chtml.js\"></script>\n" +
                                  "<script>\n" +
                                  "window.MathJax = {\n" +
                                  "  tex: {\n" +
                                  "    inlineMath: [['$', '$']],\n" +
                                  "    displayMath: [['$$', '$$']]\n" +
                                  "  }\n" +
                                  "};\n" +
                                  "</script>\n";
            
            // Insert MathJax before </head> if it exists
            if (html.contains("</head>")) {
                html = html.replace("</head>", mathJaxScript + "</head>");
            } else {
                html = mathJaxScript + html;
            }
            
            return html;
            
        } catch (Exception e) {
            throw new RuntimeException("Compilation failed: " + e.getMessage(), e);
        }
    }
    
    // Test method
    public static void test() {
        String sampleLatex = """
            \\documentclass{article}
            \\title{Sample Document}
            \\author{LaTeX Compiler}
            
            \\begin{document}
            \\maketitle
            
            \\section{Introduction}
            This is a \\textbf{bold} text and this is \\textit{italic}.
            
            \\subsection{Mathematics}
            Inline math: $E = mc^2$
            
            Display math:
            $$\\int_{-\\infty}^{\\infty} e^{-x^2} dx = \\sqrt{\\pi}$$
            
            \\section{Lists}
            \\begin{itemize}
            \\item First item
            \\item Second item with \\texttt{code}
            \\end{itemize}
            
            \\begin{enumerate}
            \\item Numbered item
            \\item Another numbered item
            \\end{enumerate}
            
            \\end{document}
            """;
        
        System.out.println("=== LaTeX Input ===");
        System.out.println(sampleLatex);
        System.out.println("\n=== HTML Output ===");
        System.out.println(compile(sampleLatex));
    }
}
