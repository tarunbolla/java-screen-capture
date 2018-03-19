package com.screencapture.slate;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.screencapture.core.ScreenCapture;
import com.screencapture.interfaces.ImageToClipboard;
import com.screencapture.utilities.AppUtils;

public class Slate
{
  JFrame scEditor;
  Point prevPoint;
  Point presentPoint;
  BufferedImage canvas;
  BufferedImage background;
  BufferedImage currentStroke;
  Graphics2D canvasGraphics;
  Graphics2D currentStrokeGraphics;
  Point translatedPointForDrawing;
  int activeTool = 5;
  float strokeThickness = 1.2F;
  int opacity = 255;
  boolean fillBrush = false;
  Color brushColor = Color.red;
  Shape presentShape;
  SlateStateMaintainance presentState = new SlateStateMaintainance();
  SlateStateMaintainance startState = this.presentState;
  
  public Slate(BufferedImage toEdit)
  {
    this.background = toEdit;
  }
  
  public void init()
  {
    if (this.scEditor == null)
    {
      this.scEditor = new JFrame("Slate");
      
      this.scEditor.addWindowListener(new WindowAdapter()
      {
        public void windowClosing(WindowEvent arg0)
        {
          ScreenCapture.showMe();
          Slate.this.scEditor.setVisible(false);
        }
      });
      this.scEditor.setLayout(new BorderLayout());
      
      JLayeredPane layers = new JLayeredPane();
      layers.setPreferredSize(new Dimension(this.background.getWidth(), this.background.getHeight()));
      
      final JPanel backgroundPane = new JPanel()
      {
        private static final long serialVersionUID = 9221732976381952814L;
        
        protected void paintComponent(Graphics g)
        {
          super.paintComponent(g);
          g.drawImage(Slate.this.background, 0, 0, null);
        }
      };
      backgroundPane.setSize(this.background.getWidth(), this.background.getHeight());
      layers.add(backgroundPane, 0);
      
      final JPanel drawingPane = new JPanel()
      {
        private static final long serialVersionUID = 3148326175058470494L;
        
        protected void paintComponent(Graphics g)
        {
          super.paintComponent(g);
          g.drawImage(Slate.this.canvas, 0, 0, null);
        }
      };
      drawingPane.setOpaque(false);
      drawingPane.setSize(this.background.getWidth(), this.background.getHeight());
      layers.add(drawingPane, 0);
      
      final JPanel currentStrokePane = new JPanel()
      {
        private static final long serialVersionUID = -2499655739116655851L;
        
        protected void paintComponent(Graphics g)
        {
          super.paintComponent(g);
          g.drawImage(Slate.this.currentStroke, 0, 0, null);
        }
      };
      currentStrokePane.setOpaque(false);
      currentStrokePane.setSize(this.background.getWidth(), this.background.getHeight());
      layers.add(currentStrokePane, 0);
      
      JPanel topPanel = new JPanel();
      
      ActionListener topPanelActionListener = new ActionListener()
      {
        public void actionPerformed(ActionEvent e)
        {
          String label = ((JButton)e.getSource()).getText();
          if (label.equalsIgnoreCase("Undo"))
          {
            Slate.this.traverseInState(-1);
            drawingPane.repaint();
          }
          else if (label.equalsIgnoreCase("Redo"))
          {
            Slate.this.traverseInState(1);
            drawingPane.repaint();
          }
          else if (label.equalsIgnoreCase("Save States"))
          {
            SlateStateMaintainance tempState = Slate.this.startState;
            int i = 1;
            while (tempState != null)
            {
              try
              {
                BufferedImage img = new BufferedImage(Slate.this.canvas.getWidth(), Slate.this.canvas.getHeight(), 2);
                tempState.paint(img.createGraphics());
                ImageIO.write(img, "png", new File(i + ".png"));
              }
              catch (IOException ex)
              {
                ex.printStackTrace();
              }
              i++;
              tempState = tempState.nextState;
            }
          }
          else if (label.equalsIgnoreCase("Copy"))
          {
            BufferedImage image = AppUtils.mergeImages(Slate.this.background, Slate.this.canvas);
            ImageToClipboard imgSel = new ImageToClipboard(image);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(imgSel, null);
          }
          else if (label.equalsIgnoreCase("GC"))
          {
            System.gc();
          }
        }
      };
      JButton undo = new JButton("Undo");
      undo.addActionListener(topPanelActionListener);
      JButton redo = new JButton("Redo");
      redo.addActionListener(topPanelActionListener);
      JButton saveAllStates = new JButton("Save States");
      saveAllStates.addActionListener(topPanelActionListener);
      JButton copyToCliboard = new JButton("Copy");
      copyToCliboard.addActionListener(topPanelActionListener);
      JButton GC = new JButton("GC");
      GC.addActionListener(topPanelActionListener);
      
      topPanel.setLayout(new GridLayout(1, 6));
      topPanel.add(undo);
      topPanel.add(redo);
      topPanel.add(saveAllStates);
      topPanel.add(copyToCliboard);
      topPanel.add(GC);
      


      this.scEditor.getRootPane().getInputMap(2).put(KeyStroke.getKeyStroke("control W"), "close");
      this.scEditor.getRootPane().getActionMap().put("close", new AbstractAction()
      {
        private static final long serialVersionUID = -3369003302369106818L;
        
        public void actionPerformed(ActionEvent arg0)
        {
          WindowEvent windowClosing = new WindowEvent(Slate.this.scEditor, 201);
          Slate.this.scEditor.dispatchEvent(windowClosing);
        }
      });
      undo.getInputMap(2).put(KeyStroke.getKeyStroke("control Z"), "undo");
      undo.getActionMap().put("undo", new AbstractAction()
      {
        private static final long serialVersionUID = -3369003302369106818L;
        
        public void actionPerformed(ActionEvent arg0)
        {
          Slate.this.traverseInState(-1);
          drawingPane.repaint();
        }
      });
      redo.getInputMap(2).put(KeyStroke.getKeyStroke("control Y"), "redo");
      redo.getActionMap().put("redo", new AbstractAction()
      {
        private static final long serialVersionUID = 8882357255077449769L;
        
        public void actionPerformed(ActionEvent arg0)
        {
          Slate.this.traverseInState(1);
          drawingPane.repaint();
        }
      });
      copyToCliboard.getInputMap(2).put(KeyStroke.getKeyStroke("control C"), "copy");
      copyToCliboard.getActionMap().put("copy", new AbstractAction()
      {
        private static final long serialVersionUID = -6787632332206209098L;
        
        public void actionPerformed(ActionEvent arg0)
        {
          BufferedImage image = AppUtils.mergeImages(Slate.this.background, Slate.this.canvas);
          ImageToClipboard imgSel = new ImageToClipboard(image);
          Toolkit.getDefaultToolkit().getSystemClipboard().setContents(imgSel, null);
        }
      });
      this.scEditor.add(topPanel, "North");
      

      JPanel toolPanel = buildToolPanel();
      this.scEditor.add(toolPanel, "East");
      

      layers.addMouseMotionListener(new MouseMotionAdapter()
      {
        public void mouseDragged(MouseEvent e)
        {
          super.mouseDragged(e);
          switch (Slate.this.activeTool)
          {
          case 3: {
            Slate.this.prevPoint = Slate.this.presentPoint;
            Slate.this.presentPoint = e.getPoint();
            Line2D.Float line = new Line2D.Float(AppUtils.diffPoint(Slate.this.prevPoint, Slate.this.translatedPointForDrawing), AppUtils.diffPoint(Slate.this.presentPoint, Slate.this.translatedPointForDrawing));
            if (Slate.this.presentShape == null) {
              Slate.this.presentShape = new GeneralPath();
            }
            ((GeneralPath)Slate.this.presentShape).append(line, true);
            Slate.this.currentStrokeGraphics.draw(line);
            break;
          }
          case 4: { 
            Slate.this.presentPoint = e.getPoint();
            AppUtils.clearImage(Slate.this.currentStroke);
            Line2D.Float line = new Line2D.Float(AppUtils.diffPoint(Slate.this.prevPoint, Slate.this.translatedPointForDrawing), AppUtils.diffPoint(Slate.this.presentPoint, Slate.this.translatedPointForDrawing));
            Slate.this.presentShape = line;
            Slate.this.currentStrokeGraphics.draw(line);
            break;
          }
          case 5: {
            Slate.this.presentPoint = e.getPoint();
            AppUtils.clearImage(Slate.this.currentStroke);
            Rectangle rectangle = AppUtils.buildRectangle(AppUtils.diffPoint(Slate.this.prevPoint, Slate.this.translatedPointForDrawing), AppUtils.diffPoint(Slate.this.presentPoint, Slate.this.translatedPointForDrawing));
            Slate.this.presentShape = rectangle;
            if (Slate.this.fillBrush) {
              Slate.this.currentStrokeGraphics.fill(rectangle);
            } else {
              Slate.this.currentStrokeGraphics.draw(rectangle);
            }
            break;
          }
          case 6: {
            Slate.this.presentPoint = e.getPoint();
            AppUtils.clearImage(Slate.this.currentStroke);
            Rectangle rectangle = AppUtils.buildRectangle(AppUtils.diffPoint(Slate.this.prevPoint, Slate.this.translatedPointForDrawing), AppUtils.diffPoint(Slate.this.presentPoint, Slate.this.translatedPointForDrawing));
            RoundRectangle2D.Float roundedRectangle = new RoundRectangle2D.Float();
            roundedRectangle.setFrame(rectangle);
            roundedRectangle.archeight = (roundedRectangle.arcwidth = 20.0F);
            Slate.this.presentShape = roundedRectangle;
            if (Slate.this.fillBrush) {
              Slate.this.currentStrokeGraphics.fill(roundedRectangle);
            } else {
              Slate.this.currentStrokeGraphics.draw(roundedRectangle);
            }
            break;
          }
          case 7: {
            Slate.this.presentPoint = e.getPoint();
            AppUtils.clearImage(Slate.this.currentStroke);
            Rectangle rectangle = AppUtils.buildRectangle(AppUtils.diffPoint(Slate.this.prevPoint, Slate.this.translatedPointForDrawing), AppUtils.diffPoint(Slate.this.presentPoint, Slate.this.translatedPointForDrawing));
            Ellipse2D.Float ellipse = new Ellipse2D.Float();
            ellipse.setFrame(rectangle);
            Slate.this.presentShape = ellipse;
            if (Slate.this.fillBrush) {
              Slate.this.currentStrokeGraphics.fill(ellipse);
            } else {
              Slate.this.currentStrokeGraphics.draw(ellipse);
            }
            break;
          }
          }
          currentStrokePane.repaint();
        }
      });
      layers.addMouseListener(new MouseAdapter()
      {
        public void mouseReleased(MouseEvent e)
        {
          Slate.this.canvasGraphics.drawImage(Slate.this.currentStroke, 0, 0, null);
          drawingPane.repaint();
          AppUtils.clearImage(Slate.this.currentStroke);
          currentStrokePane.repaint();
          
          Slate.this.prevPoint = (Slate.this.presentPoint = null);
          if (Slate.this.activeTool != 0) {
            Slate.this.recordState();
          }
          super.mouseReleased(e);
        }
        
        public void mousePressed(MouseEvent e)
        {
          Slate.this.currentStrokeGraphics.setColor(Slate.this.brushColor);
          Slate.this.currentStrokeGraphics.setStroke(new BasicStroke(Slate.this.strokeThickness));
          
          Slate.this.prevPoint = Slate.this.presentPoint;
          if (Slate.this.prevPoint == null) {
            Slate.this.prevPoint = e.getPoint();
          }
          Slate.this.presentPoint = e.getPoint();
          super.mousePressed(e);
        }
      });
      this.canvas = new BufferedImage(this.background.getWidth(), this.background.getHeight(), 2);
      this.canvasGraphics = this.canvas.createGraphics();
      this.canvasGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
        RenderingHints.VALUE_ANTIALIAS_ON);
      
      this.currentStroke = new BufferedImage(this.background.getWidth(), this.background.getHeight(), 2);
      this.currentStrokeGraphics = this.currentStroke.createGraphics();
      this.currentStrokeGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
        RenderingHints.VALUE_ANTIALIAS_ON);
      
      final JScrollPane scrollPane = new JScrollPane(layers);
      scrollPane.setHorizontalScrollBarPolicy(30);
      scrollPane.setVerticalScrollBarPolicy(20);
      scrollPane.addComponentListener(new ComponentAdapter()
      {
        public void componentResized(ComponentEvent e)
        {
          super.componentResized(e);
          

          int left = (-Slate.this.background.getWidth() + scrollPane.getWidth()) / 2;
          int top = (-Slate.this.background.getHeight() + scrollPane.getHeight()) / 2;
          
          top = top > 0 ? top : 0;
          left = left > 0 ? left : 0;
          
          Slate.this.translatedPointForDrawing = new Point(left, top);
          
          backgroundPane.setLocation(Slate.this.translatedPointForDrawing);
          drawingPane.setLocation(Slate.this.translatedPointForDrawing);
          currentStrokePane.setLocation(Slate.this.translatedPointForDrawing);
        }
      });
      this.scEditor.add(scrollPane, "Center");
      this.scEditor.setSize(600, 600);
      this.scEditor.setExtendedState(6);
      this.scEditor.setDefaultCloseOperation(0);
      this.scEditor.setLocationRelativeTo(null);
    }
    this.scEditor.setVisible(true);
  }
  
  private JPanel buildToolPanel()
  {
    final JPanel toolPanel = new JPanel();
    toolPanel.setLayout(new BoxLayout(toolPanel, 1));
    
    ActionListener actionListener = new ActionListener()
    {
      public void actionPerformed(ActionEvent e)
      {
        AbstractButton control = (AbstractButton)e.getSource();
        int toolType = ((Integer)control.getClientProperty("toolType")).intValue();
        switch (toolType)
        {
        case 1: 
          Color newColor = JColorChooser.showDialog(toolPanel, 
            "Choose Brush Color", Slate.this.brushColor);
          Slate.this.brushColor = (newColor == null ? Slate.this.brushColor : newColor);
          BufferedImage icon = AppUtils.getImagePatch(15, 15, Slate.this.brushColor, true);
          control.setIcon(new ImageIcon(icon));
          Slate.this.brushColor = new Color(Slate.this.brushColor.getRed(), Slate.this.brushColor.getGreen(), Slate.this.brushColor.getBlue(), Slate.this.opacity);
          break;
        case 2: 
          Slate.this.fillBrush = control.isSelected();
          if (Slate.this.fillBrush) {
            control.setToolTipText("Click to disable Fill");
          } else {
            control.setToolTipText("Click to enable Fill");
          }
          break;
        case 3: 
          Slate.this.activeTool = 3;
          break;
        case 4: 
          Slate.this.activeTool = 4;
          break;
        case 5: 
          Slate.this.activeTool = 5;
          break;
        case 6: 
          Slate.this.activeTool = 6;
          break;
        case 7: 
          Slate.this.activeTool = 7;
        }
      }
    };
    JLabel toolkitLabel = new JLabel("Toolkit");
    
    JButton color = new JButton();
    color.setToolTipText("Click to change Brush Color");
    color.putClientProperty("toolType", Integer.valueOf(1));
    BufferedImage colorIcon = AppUtils.getImagePatch(15, 15, this.brushColor, true);
    color.setIcon(new ImageIcon(colorIcon));
    color.setMargin(new Insets(6, 6, 6, 6));
    color.addActionListener(actionListener);
    
    JToggleButton fillPreference = new JToggleButton();
    fillPreference.setToolTipText("Click to enable Fill");
    fillPreference.putClientProperty("toolType", Integer.valueOf(2));
    BufferedImage strokePrefIcon = AppUtils.getImagePatch(15, 15, Color.black, false);
    fillPreference.setIcon(new ImageIcon(strokePrefIcon));
    BufferedImage fillPrefIcon = AppUtils.getImagePatch(15, 15, Color.black, true);
    fillPreference.setSelectedIcon(new ImageIcon(fillPrefIcon));
    fillPreference.setMargin(new Insets(6, 6, 6, 6));
    fillPreference.addActionListener(actionListener);
    
    ButtonGroup drawingTools = new ButtonGroup();
    
    JToggleButton freeHand = new JToggleButton();
    freeHand.setToolTipText("Draw Freehand");
    freeHand.setSelected(this.activeTool == 3);
    freeHand.putClientProperty("toolType", Integer.valueOf(3));
    freeHand.setMnemonic('f');
    freeHand.setFocusPainted(false);
    freeHand.setIcon(new ImageIcon("icons/freehand.png"));
    freeHand.setMargin(new Insets(2, 2, 2, 2));
    freeHand.addActionListener(actionListener);
    drawingTools.add(freeHand);
    
    JToggleButton lineTool = new JToggleButton();
    lineTool.setToolTipText("Draw Line");
    lineTool.setSelected(this.activeTool == 4);
    lineTool.putClientProperty("toolType", Integer.valueOf(4));
    lineTool.setMnemonic('l');
    lineTool.setFocusPainted(false);
    lineTool.setIcon(new ImageIcon("icons/line.png"));
    lineTool.setMargin(new Insets(2, 2, 2, 2));
    lineTool.addActionListener(actionListener);
    drawingTools.add(lineTool);
    
    JToggleButton rectangleTool = new JToggleButton();
    rectangleTool.setToolTipText("Draw Rectangle");
    rectangleTool.setSelected(this.activeTool == 5);
    rectangleTool.putClientProperty("toolType", Integer.valueOf(5));
    rectangleTool.setMnemonic('r');
    rectangleTool.setFocusPainted(false);
    rectangleTool.setIcon(new ImageIcon("icons/rectangle.png"));
    rectangleTool.setMargin(new Insets(2, 2, 2, 2));
    rectangleTool.addActionListener(actionListener);
    drawingTools.add(rectangleTool);
    
    JToggleButton roundedRectangle = new JToggleButton();
    roundedRectangle.setToolTipText("Draw Rounded Rectangle");
    roundedRectangle.setSelected(this.activeTool == 6);
    roundedRectangle.putClientProperty("toolType", Integer.valueOf(6));
    roundedRectangle.setMnemonic('t');
    roundedRectangle.setFocusPainted(false);
    roundedRectangle.setIcon(new ImageIcon("icons/roundedrectangle.png"));
    roundedRectangle.setMargin(new Insets(2, 2, 2, 2));
    roundedRectangle.addActionListener(actionListener);
    drawingTools.add(roundedRectangle);
    
    JToggleButton ellipseTool = new JToggleButton();
    ellipseTool.setToolTipText("Draw Oval");
    ellipseTool.setSelected(this.activeTool == 7);
    ellipseTool.putClientProperty("toolType", Integer.valueOf(7));
    ellipseTool.setMnemonic('e');
    ellipseTool.setFocusPainted(false);
    ellipseTool.setIcon(new ImageIcon("icons/ellipse.png"));
    ellipseTool.setMargin(new Insets(2, 2, 2, 2));
    ellipseTool.addActionListener(actionListener);
    drawingTools.add(ellipseTool);
    
    toolPanel.add(toolkitLabel);
    toolPanel.add(color);
    toolPanel.add(fillPreference);
    toolPanel.add(freeHand);
    toolPanel.add(lineTool);
    toolPanel.add(rectangleTool);
    toolPanel.add(roundedRectangle);
    toolPanel.add(ellipseTool);
    


    final JSpinner thicknessSpinner = new JSpinner(new SpinnerNumberModel(1.2D, 1.0D, 4.0D, 0.1D));
    JTextField thicknessField = ((JSpinner.DefaultEditor)thicknessSpinner.getEditor()).getTextField();
    thicknessField.setEditable(false);
    thicknessSpinner.setBorder(new EmptyBorder(new Insets(2, 2, 2, 2)));
    thicknessSpinner.addChangeListener(new ChangeListener()
    {
      public void stateChanged(ChangeEvent arg0)
      {
        Slate.this.strokeThickness = ((Double)thicknessSpinner.getValue()).floatValue();
      }
    });
    toolPanel.add(thicknessSpinner);
    
    final JSlider opacitySlider = new JSlider(10, 255, 255);
    opacitySlider.setOrientation(1);
    opacitySlider.setPreferredSize(new Dimension(5, 20));
    opacitySlider.addChangeListener(new ChangeListener()
    {
      public void stateChanged(ChangeEvent arg0)
      {
        Slate.this.opacity = opacitySlider.getValue();
        Slate.this.brushColor = new Color(Slate.this.brushColor.getRed(), Slate.this.brushColor.getGreen(), Slate.this.brushColor.getBlue(), Slate.this.opacity);
      }
    });
    opacitySlider.setBorder(new EmptyBorder(new Insets(2, 2, 2, 2)));
    toolPanel.add(opacitySlider);
    
    final JSpinner opacitySpinner = new JSpinner(new SpinnerNumberModel(255, 100, 255, 20));
    JTextField opacityField = ((JSpinner.DefaultEditor)opacitySpinner.getEditor()).getTextField();
    opacityField.setEditable(false);
    opacitySpinner.setBorder(new EmptyBorder(new Insets(2, 2, 2, 2)));
    opacitySpinner.addChangeListener(new ChangeListener()
    {
      public void stateChanged(ChangeEvent arg0)
      {
        Slate.this.opacity = ((Integer)opacitySpinner.getValue()).intValue();
        Slate.this.brushColor = new Color(Slate.this.brushColor.getRed(), Slate.this.brushColor.getGreen(), Slate.this.brushColor.getBlue(), Slate.this.opacity);
      }
    });
    toolPanel.add(opacitySpinner);
    
    return toolPanel;
  }
  
  protected void traverseInState(int dir)
  {
    if (dir == -1)
    {
      if (this.presentState.previousState != null)
      {
        AppUtils.clearImage(this.canvas);
        SlateStateMaintainance tempState = this.startState;
        while (tempState != this.presentState)
        {
          tempState.paint(this.canvasGraphics);
          tempState = tempState.nextState;
        }
        this.presentState = this.presentState.previousState;
      }
    }
    else if (this.presentState.nextState != null)
    {
      this.presentState = this.presentState.nextState;
      
      AppUtils.clearImage(this.canvas);
      SlateStateMaintainance tempState = this.startState;
      while (tempState != this.presentState.nextState)
      {
        tempState.paint(this.canvasGraphics);
        tempState = tempState.nextState;
      }
    }
  }
  
  protected void recordState()
  {
    SlateStateMaintainance st = new SlateStateMaintainance();
    st.previousState = this.presentState;
    if (this.presentState.nextState != null) {
      this.presentState.nextState.previousState = null;
    }
    this.presentState.nextState = st;
    this.presentState = st;
    try
    {
      this.presentState.color = this.brushColor;
      this.presentState.fill = this.fillBrush;
      this.presentState.shape = this.presentShape;
      this.presentState.strokeThickness = this.strokeThickness;
      this.presentShape = null;
    }
    catch (Exception e)
    {
      e.printStackTrace();
    }
    removenthStateLink();
  }
  
  protected void removenthStateLink() {}
}
