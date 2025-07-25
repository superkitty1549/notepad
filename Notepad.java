import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.awt.print.PrinterException;
import java.io.*;

public class Notepad extends JFrame implements ActionListener {
    private JTextArea textArea;
    private JScrollPane scrollPane;
    private JMenuBar menuBar;
    private JMenu fileMenu, editMenu, formatMenu, helpMenu;
    private JMenuItem newFile, openFile, saveFile, saveAs, exit;
    private JMenuItem cut, copy, paste, selectAll, find;
    private JMenuItem wordWrap, font, about;
    private File currentFile;
    private boolean isModified = false;

    public Notepad() {
        setTitle("Notepad - Untitled");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);

        initializeComponents();
        createMenuBar();
        setupEventHandlers();
        
        setVisible(true);
    }

    private void initializeComponents() {
        textArea = new JTextArea();
        textArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        
        scrollPane = new JScrollPane(textArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        
        add(scrollPane, BorderLayout.CENTER);
    }

    private void createMenuBar() {
        menuBar = new JMenuBar();

        // File Menu
        fileMenu = new JMenu("File");
        newFile = new JMenuItem("New");
        openFile = new JMenuItem("Open");
        saveFile = new JMenuItem("Save");
        saveAs = new JMenuItem("Save As");
        exit = new JMenuItem("Exit");

        newFile.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_DOWN_MASK));
        openFile.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK));
        saveFile.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK));

        fileMenu.add(newFile);
        fileMenu.add(openFile);
        fileMenu.addSeparator();
        fileMenu.add(saveFile);
        fileMenu.add(saveAs);
        fileMenu.addSeparator();
        fileMenu.add(exit);

        // Edit Menu
        editMenu = new JMenu("Edit");
        cut = new JMenuItem("Cut");
        copy = new JMenuItem("Copy");
        paste = new JMenuItem("Paste");
        selectAll = new JMenuItem("Select All");
        find = new JMenuItem("Find");

        cut.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.CTRL_DOWN_MASK));
        copy.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK));
        paste.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK));
        selectAll.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK));
        find.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.CTRL_DOWN_MASK));

        editMenu.add(cut);
        editMenu.add(copy);
        editMenu.add(paste);
        editMenu.addSeparator();
        editMenu.add(selectAll);
        editMenu.add(find);

        // Format Menu
        formatMenu = new JMenu("Format");
        wordWrap = new JMenuItem("Word Wrap");
        font = new JMenuItem("Font");

        formatMenu.add(wordWrap);
        formatMenu.add(font);

        // Help Menu
        helpMenu = new JMenu("Help");
        about = new JMenuItem("About");
        helpMenu.add(about);

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(formatMenu);
        menuBar.add(helpMenu);

        setJMenuBar(menuBar);
    }

    private void setupEventHandlers() {
        // Add action listeners
        newFile.addActionListener(this);
        openFile.addActionListener(this);
        saveFile.addActionListener(this);
        saveAs.addActionListener(this);
        exit.addActionListener(this);
        cut.addActionListener(this);
        copy.addActionListener(this);
        paste.addActionListener(this);
        selectAll.addActionListener(this);
        find.addActionListener(this);
        wordWrap.addActionListener(this);
        font.addActionListener(this);
        about.addActionListener(this);

        // Document listener to track changes
        textArea.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { setModified(true); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { setModified(true); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { setModified(true); }
        });

        // Window closing event
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                exitApplication();
            }
        });
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();

        switch (command) {
            case "New":
                newDocument();
                break;
            case "Open":
                openDocument();
                break;
            case "Save":
                saveDocument();
                break;
            case "Save As":
                saveAsDocument();
                break;
            case "Exit":
                exitApplication();
                break;
            case "Cut":
                textArea.cut();
                break;
            case "Copy":
                textArea.copy();
                break;
            case "Paste":
                textArea.paste();
                break;
            case "Select All":
                textArea.selectAll();
                break;
            case "Find":
                findText();
                break;
            case "Word Wrap":
                toggleWordWrap();
                break;
            case "Font":
                chooseFont();
                break;
            case "About":
                showAbout();
                break;
        }
    }

    private void newDocument() {
        if (isModified && !confirmSave()) return;
        
        textArea.setText("");
        currentFile = null;
        setModified(false);
        setTitle("Notepad - Untitled");
    }

    private void openDocument() {
        if (isModified && !confirmSave()) return;

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("Text Files", "txt"));
        
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                currentFile = fileChooser.getSelectedFile();
                BufferedReader reader = new BufferedReader(new FileReader(currentFile));
                textArea.read(reader, null);
                reader.close();
                setModified(false);
                setTitle("Notepad - " + currentFile.getName());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error opening file: " + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void saveDocument() {
        if (currentFile == null) {
            saveAsDocument();
        } else {
            saveToFile(currentFile);
        }
    }

    private void saveAsDocument() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new FileNameExtensionFilter("Text Files", "txt"));
        
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".txt")) {
                file = new File(file.getAbsolutePath() + ".txt");
            }
            saveToFile(file);
            currentFile = file;
            setTitle("Notepad - " + currentFile.getName());
        }
    }

    private void saveToFile(File file) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            textArea.write(writer);
            writer.close();
            setModified(false);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error saving file: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean confirmSave() {
        int option = JOptionPane.showConfirmDialog(this,
                "Do you want to save changes?", "Notepad",
                JOptionPane.YES_NO_CANCEL_OPTION);
        
        if (option == JOptionPane.YES_OPTION) {
            saveDocument();
            return !isModified; // Return true if save was successful
        } else if (option == JOptionPane.NO_OPTION) {
            return true;
        }
        return false; // Cancel option
    }

    private void exitApplication() {
        if (isModified && !confirmSave()) return;
        System.exit(0);
    }

    private void findText() {
        String searchText = JOptionPane.showInputDialog(this, "Find:", "Find", JOptionPane.PLAIN_MESSAGE);
        if (searchText != null && !searchText.isEmpty()) {
            String content = textArea.getText();
            int index = content.indexOf(searchText);
            if (index >= 0) {
                textArea.setCaretPosition(index);
                textArea.select(index, index + searchText.length());
            } else {
                JOptionPane.showMessageDialog(this, "Text not found!", "Find", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    private void toggleWordWrap() {
        boolean wrap = !textArea.getLineWrap();
        textArea.setLineWrap(wrap);
        textArea.setWrapStyleWord(wrap);
        wordWrap.setText(wrap ? "Word Wrap: On" : "Word Wrap: Off");
    }

    private void chooseFont() {
        Font currentFont = textArea.getFont();
        String[] fontNames = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        String selectedFont = (String) JOptionPane.showInputDialog(this,
                "Choose font:", "Font", JOptionPane.PLAIN_MESSAGE, null, fontNames, currentFont.getName());
        
        if (selectedFont != null) {
            String[] sizes = {"8", "9", "10", "11", "12", "14", "16", "18", "20", "24", "28", "32", "36", "48", "72"};
            String selectedSize = (String) JOptionPane.showInputDialog(this,
                    "Choose size:", "Font Size", JOptionPane.PLAIN_MESSAGE, null, sizes, String.valueOf(currentFont.getSize()));
            
            if (selectedSize != null) {
                int size = Integer.parseInt(selectedSize);
                textArea.setFont(new Font(selectedFont, currentFont.getStyle(), size));
            }
        }
    }

    private void showAbout() {
        JOptionPane.showMessageDialog(this,
                "Java Notepad\nVersion 1.0\n\nA simple text editor built with Java Swing",
                "About Notepad", JOptionPane.INFORMATION_MESSAGE);
    }

    private void setModified(boolean modified) {
        this.isModified = modified;
        String title = getTitle();
        if (modified && !title.startsWith("*")) {
            setTitle("*" + title);
        } else if (!modified && title.startsWith("*")) {
            setTitle(title.substring(1));
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeel());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        SwingUtilities.invokeLater(() -> new Notepad());
    }
}
