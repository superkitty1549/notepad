import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

public class DrawingApp extends JFrame {
    private DrawingPanel drawingPanel;
    private JPanel toolPanel;
    private JPanel colorPanel;
    private JSlider brushSizeSlider;
    private JSlider opacitySlider;
    private JColorChooser colorChooser;
    private JToggleButton penTool, brushTool, eraserTool, lineTool, rectangleTool, circleTool, eyedropperTool;
    private ButtonGroup toolGroup;
    private JButton undoButton, redoButton, clearButton, saveButton, loadButton;
    private JLabel statusLabel;
    
    private Color currentColor = Color.BLACK;
    private int brushSize = 5;
    private float opacity = 1.0f;
    private Tool currentTool = Tool.PEN;
    
    enum Tool {
        PEN, BRUSH, ERASER, LINE, RECTANGLE, CIRCLE, EYEDROPPER
    }
    
    public DrawingApp() {
        setTitle("Drawing App - Kleki Style");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        
        initializeComponents();
        setupLayout();
        setupEventHandlers();
        
        setSize(1200, 800);
        setLocationRelativeTo(null);
        setVisible(true);
    }
    
    private void initializeComponents() {
        // Drawing panel
        drawingPanel = new DrawingPanel();
        
        // Tool panel
        toolPanel = new JPanel();
        toolPanel.setLayout(new BoxLayout(toolPanel, BoxLayout.Y_AXIS));
        toolPanel.setBorder(new TitledBorder("Tools"));
        toolPanel.setPreferredSize(new Dimension(200, 0));
        
        // Tool buttons
        penTool = new JToggleButton("Pen", true);
        brushTool = new JToggleButton("Brush");
        eraserTool = new JToggleButton("Eraser");
        lineTool = new JToggleButton("Line");
        rectangleTool = new JToggleButton("Rectangle");
        circleTool = new JToggleButton("Circle");
        eyedropperTool = new JToggleButton("Eyedropper");
        
        toolGroup = new ButtonGroup();
        toolGroup.add(penTool);
        toolGroup.add(brushTool);
        toolGroup.add(eraserTool);
        toolGroup.add(lineTool);
        toolGroup.add(rectangleTool);
        toolGroup.add(circleTool);
        toolGroup.add(eyedropperTool);
        
        // Brush size slider
        brushSizeSlider = new JSlider(1, 50, 5);
        brushSizeSlider.setMajorTickSpacing(10);
        brushSizeSlider.setMinorTickSpacing(5);
        brushSizeSlider.setPaintTicks(true);
        brushSizeSlider.setPaintLabels(true);
        
        // Opacity slider
        opacitySlider = new JSlider(1, 100, 100);
        opacitySlider.setMajorTickSpacing(25);
        opacitySlider.setMinorTickSpacing(10);
        opacitySlider.setPaintTicks(true);
        opacitySlider.setPaintLabels(true);
        
        // Colour chooser
        colorChooser = new JColorChooser(currentColor);
        colorChooser.setPreviewPanel(new JPanel());
        
        // Action buttons
        undoButton = new JButton("Undo");
        redoButton = new JButton("Redo");
        clearButton = new JButton("Clear");
        saveButton = new JButton("Save");
        loadButton = new JButton("Load");
        
        // Status label
        statusLabel = new JLabel("Ready");
    }
    
    private void setupLayout() {
        // Tool panel layout
        toolPanel.add(Box.createVerticalStrut(10));
        toolPanel.add(new JLabel("Drawing Tools:"));
        toolPanel.add(penTool);
        toolPanel.add(brushTool);
        toolPanel.add(eraserTool);
        toolPanel.add(lineTool);
        toolPanel.add(rectangleTool);
        toolPanel.add(circleTool);
        toolPanel.add(eyedropperTool);
        
        toolPanel.add(Box.createVerticalStrut(20));
        toolPanel.add(new JLabel("Brush Size:"));
        toolPanel.add(brushSizeSlider);
        
        toolPanel.add(Box.createVerticalStrut(10));
        toolPanel.add(new JLabel("Opacity:"));
        toolPanel.add(opacitySlider);
        
        toolPanel.add(Box.createVerticalStrut(20));
        JPanel actionPanel = new JPanel(new GridLayout(5, 1, 5, 5));
        actionPanel.add(undoButton);
        actionPanel.add(redoButton);
        actionPanel.add(clearButton);
        actionPanel.add(saveButton);
        actionPanel.add(loadButton);
        toolPanel.add(actionPanel);
        
        // Color panel
        colorPanel = new JPanel(new BorderLayout());
        colorPanel.setBorder(new TitledBorder("Colors"));
        colorPanel.add(colorChooser, BorderLayout.CENTER);
        
        // Main layout
        add(toolPanel, BorderLayout.WEST);
        add(drawingPanel, BorderLayout.CENTER);
        add(colorPanel, BorderLayout.EAST);
        add(statusLabel, BorderLayout.SOUTH);
    }
    
    private void setupEventHandlers() {
        // Tool selection
        penTool.addActionListener(e -> currentTool = Tool.PEN);
        brushTool.addActionListener(e -> currentTool = Tool.BRUSH);
        eraserTool.addActionListener(e -> currentTool = Tool.ERASER);
        lineTool.addActionListener(e -> currentTool = Tool.LINE);
        rectangleTool.addActionListener(e -> currentTool = Tool.RECTANGLE);
        circleTool.addActionListener(e -> currentTool = Tool.CIRCLE);
        eyedropperTool.addActionListener(e -> currentTool = Tool.EYEDROPPER);
        
        // Sliders
        brushSizeSlider.addChangeListener(e -> {
            brushSize = brushSizeSlider.getValue();
            statusLabel.setText("Brush size: " + brushSize);
        });
        
        opacitySlider.addChangeListener(e -> {
            opacity = opacitySlider.getValue() / 100.0f;
            statusLabel.setText("Opacity: " + (int)(opacity * 100) + "%");
        });
        
        // Colour chooser
        colorChooser.getSelectionModel().addChangeListener(e -> {
            currentColor = colorChooser.getColor();
            statusLabel.setText("Color changed");
        });
        
        // Action buttons
        undoButton.addActionListener(e -> drawingPanel.undo());
        redoButton.addActionListener(e -> drawingPanel.redo());
        clearButton.addActionListener(e -> drawingPanel.clear());
        saveButton.addActionListener(e -> saveImage());
        loadButton.addActionListener(e -> loadImage());
    }
    
    private void saveImage() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("PNG Images", "png"));
        
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                File file = fileChooser.getSelectedFile();
                if (!file.getName().toLowerCase().endsWith(".png")) {
                    file = new File(file.getAbsolutePath() + ".png");
                }
                ImageIO.write(drawingPanel.getImage(), "PNG", file);
                statusLabel.setText("Image saved: " + file.getName());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error saving image: " + ex.getMessage());
            }
        }
    }
    
    private void loadImage() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Image Files", "png", "jpg", "jpeg", "gif"));
        
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                BufferedImage image = ImageIO.read(fileChooser.getSelectedFile());
                drawingPanel.loadImage(image);
                statusLabel.setText("Image loaded: " + fileChooser.getSelectedFile().getName());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error loading image: " + ex.getMessage());
            }
        }
    }
    
    class DrawingPanel extends JPanel {
        private BufferedImage canvas;
        private Graphics2D g2d;
        private List<BufferedImage> undoStack;
        private List<BufferedImage> redoStack;
        private Point startPoint, endPoint;
        private boolean drawing = false;
        
        public DrawingPanel() {
            setBackground(Color.WHITE);
            undoStack = new ArrayList<>();
            redoStack = new ArrayList<>();
            
            addMouseListener(new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    startPoint = e.getPoint();
                    
                    if (currentTool == Tool.EYEDROPPER) {
                        pickColor(e.getPoint());
                        return;
                    }
                    
                    saveState();
                    drawing = true;
                    
                    if (currentTool == Tool.PEN || currentTool == Tool.BRUSH || currentTool == Tool.ERASER) {
                        drawPoint(e.getPoint());
                    }
                }
                
                @Override
                public void mouseReleased(MouseEvent e) {
                    if (!drawing) return;
                    
                    endPoint = e.getPoint();
                    
                    if (currentTool == Tool.LINE) {
                        drawLine(startPoint, endPoint);
                    } else if (currentTool == Tool.RECTANGLE) {
                        drawRectangle(startPoint, endPoint);
                    } else if (currentTool == Tool.CIRCLE) {
                        drawCircle(startPoint, endPoint);
                    }
                    
                    drawing = false;
                    repaint();
                }
            });
            
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    if (!drawing) return;
                    
                    if (currentTool == Tool.PEN || currentTool == Tool.BRUSH || currentTool == Tool.ERASER) {
                        drawLine(startPoint, e.getPoint());
                        startPoint = e.getPoint();
                    }
                    
                    endPoint = e.getPoint();
                    repaint();
                }
                
                @Override
                public void mouseMoved(MouseEvent e) {
                    statusLabel.setText("Position: (" + e.getX() + ", " + e.getY() + ")");
                }
            });
        }
        
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            
            if (canvas == null) {
                canvas = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
                g2d = canvas.createGraphics();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setColor(Color.WHITE);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
            
            g.drawImage(canvas, 0, 0, null);
            
            // Draw preview for shape tools
            if (drawing && startPoint != null && endPoint != null) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(currentColor);
                g2.setStroke(new BasicStroke(brushSize));
                
                if (currentTool == Tool.LINE) {
                    g2.draw(new Line2D.Float(startPoint, endPoint));
                } else if (currentTool == Tool.RECTANGLE) {
                    Rectangle rect = getRectangle(startPoint, endPoint);
                    g2.draw(new Rectangle2D.Float(rect.x, rect.y, rect.width, rect.height));
                } else if (currentTool == Tool.CIRCLE) {
                    Rectangle rect = getRectangle(startPoint, endPoint);
                    g2.draw(new Ellipse2D.Float(rect.x, rect.y, rect.width, rect.height));
                }
                
                g2.dispose();
            }
        }
        
        private void drawPoint(Point point) {
            if (g2d == null) return;
            
            setupGraphics();
            
            if (currentTool == Tool.ERASER) {
                g2d.setComposite(AlphaComposite.Clear);
            }
            
            int size = currentTool == Tool.BRUSH ? brushSize * 2 : brushSize;
            g2d.fillOval(point.x - size/2, point.y - size/2, size, size);
            
            repaint();
        }
        
        private void drawLine(Point start, Point end) {
            if (g2d == null) return;
            
            setupGraphics();
            
            if (currentTool == Tool.ERASER) {
                g2d.setComposite(AlphaComposite.Clear);
            }
            
            g2d.draw(new Line2D.Float(start, end));
            repaint();
        }
        
        private void drawRectangle(Point start, Point end) {
            if (g2d == null) return;
            
            setupGraphics();
            Rectangle rect = getRectangle(start, end);
            g2d.draw(new Rectangle2D.Float(rect.x, rect.y, rect.width, rect.height));
        }
        
        private void drawCircle(Point start, Point end) {
            if (g2d == null) return;
            
            setupGraphics();
            Rectangle rect = getRectangle(start, end);
            g2d.draw(new Ellipse2D.Float(rect.x, rect.y, rect.width, rect.height));
        }
        
        private Rectangle getRectangle(Point start, Point end) {
            int x = Math.min(start.x, end.x);
            int y = Math.min(start.y, end.y);
            int width = Math.abs(end.x - start.x);
            int height = Math.abs(end.y - start.y);
            return new Rectangle(x, y, width, height);
        }
        
        private void setupGraphics() {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setColor(currentColor);
            g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
            
            if (currentTool == Tool.BRUSH) {
                g2d.setStroke(new BasicStroke(brushSize, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            } else {
                g2d.setStroke(new BasicStroke(brushSize));
            }
        }
        
        private void pickColor(Point point) {
            if (canvas != null && point.x >= 0 && point.y >= 0 && 
                point.x < canvas.getWidth() && point.y < canvas.getHeight()) {
                int rgb = canvas.getRGB(point.x, point.y);
                Color pickedColor = new Color(rgb);
                currentColor = pickedColor;
                colorChooser.setColor(pickedColor);
                statusLabel.setText("Color picked: " + String.format("#%06X", rgb & 0xFFFFFF));
            }
        }
        
        private void saveState() {
            if (canvas != null) {
                BufferedImage stateCopy = new BufferedImage(canvas.getWidth(), canvas.getHeight(), BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = stateCopy.createGraphics();
                g.drawImage(canvas, 0, 0, null);
                g.dispose();
                
                undoStack.add(stateCopy);
                redoStack.clear();
                
                if (undoStack.size() > 50) { // Limit undo stack
                    undoStack.remove(0);
                }
            }
        }
        
        public void undo() {
            if (!undoStack.isEmpty()) {
                BufferedImage currentState = new BufferedImage(canvas.getWidth(), canvas.getHeight(), BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = currentState.createGraphics();
                g.drawImage(canvas, 0, 0, null);
                g.dispose();
                redoStack.add(currentState);
                
                BufferedImage previousState = undoStack.remove(undoStack.size() - 1);
                canvas = previousState;
                g2d = canvas.createGraphics();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                repaint();
                statusLabel.setText("Undo performed");
            }
        }
        
        public void redo() {
            if (!redoStack.isEmpty()) {
                saveState();
                BufferedImage nextState = redoStack.remove(redoStack.size() - 1);
                canvas = nextState;
                g2d = canvas.createGraphics();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                repaint();
                statusLabel.setText("Redo performed");
            }
        }
        
        public void clear() {
            saveState();
            g2d.setComposite(AlphaComposite.Clear);
            g2d.fillRect(0, 0, getWidth(), getHeight());
            g2d.setComposite(AlphaComposite.SrcOver);
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, getWidth(), getHeight());
            repaint();
            statusLabel.setText("Canvas cleared");
        }
        
        public BufferedImage getImage() {
            return canvas;
        }
        
        public void loadImage(BufferedImage image) {
            saveState();
            canvas = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
            g2d = canvas.createGraphics();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, getWidth(), getHeight());
            g2d.drawImage(image, 0, 0, null);
            repaint();
        }
    }
    
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeel());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        SwingUtilities.invokeLater(() -> new DrawingApp());
    }
}
