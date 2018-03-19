package com.screencapture.core;

import java.awt.AWTException;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.MenuItem;
import java.awt.Point;
import java.awt.PopupMenu;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

import com.screencapture.interfaces.ImageToClipboard;
import com.screencapture.slate.Slate;
import com.screencapture.utilities.AppUtils;

public class ScreenCapture
{
  private static Point start;
  private static Point end;
  private static JFrame mainWindow;
  private static JFrame screenWindow;
  private static JDialog settingsDialog;
  private static JPanel drawPanel;
  private static JPanel screenPanel;
  private static BufferedImage screen;
  private static Map<String, String> settings = new HashMap<String, String>();
  private static TrayIcon trayIcon;
  
  public static void main(String[] args)
  {
    new ScreenCapture().init();
  }
  
  private void init()
  {
    mainWindow = new JFrame("Screen Capture!!!");
    mainWindow.setResizable(false);
    mainWindow.setSize(100, 200);
    mainWindow.setLocationRelativeTo(null);
    mainWindow.setDefaultCloseOperation(3);
    
    JPanel contentPanel = new JPanel();
    contentPanel.setLayout(new BorderLayout(2, 2));
    contentPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
    
    final JButton capture = new JButton("Capture");
    capture.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent arg0)
      {
        Runnable runnable = new Runnable()
        {
          public void run()
          {
            ScreenCapture.mainWindow.setVisible(false);
            try
            {
              Thread.sleep(300L);
            }
            catch (InterruptedException e)
            {
              e.printStackTrace();
            }
            ScreenCapture.this.initializeScreenWindow();
          }
        };
        SwingUtilities.invokeLater(runnable);
      }
    });
    contentPanel.add(capture, "West");
    
    final JButton edit = new JButton("Edit");
    edit.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent arg0)
      {
        Runnable runnable = new Runnable()
        {
          public void run()
          {
            ScreenCapture.this.sendToEdit();
            ScreenCapture.mainWindow.setVisible(false);
          }
        };
        SwingUtilities.invokeLater(runnable);
      }
    });
    contentPanel.add(edit, "Center");
    
    JButton options = new JButton("Settings");
    options.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent arg0)
      {
        Runnable runnable = new Runnable()
        {
          public void run()
          {
            ScreenCapture.this.buildSettingsDialog();
          }
        };
        SwingUtilities.invokeLater(runnable);
      }
    });
    contentPanel.add(options, "East");
    
    mainWindow.addWindowStateListener(new WindowAdapter()
    {
      public void windowStateChanged(WindowEvent e)
      {
        if (e.getNewState() == 1) {
          try
          {
            SystemTray.getSystemTray().add(ScreenCapture.trayIcon);
            ScreenCapture.mainWindow.setVisible(false);
          }
          catch (AWTException e1)
          {
            e1.printStackTrace();
          }
        }
        super.windowStateChanged(e);
      }
    });
    mainWindow.add(contentPanel);
    mainWindow.pack();
    

    final SystemTray tray = SystemTray.getSystemTray();
    trayIcon = new TrayIcon(AppUtils.getImagePatch(64, 64, Color.green, true), "Screen Capture!");
    
    ActionListener trayListener = new ActionListener()
    {
      public void actionPerformed(ActionEvent arg)
      {
        MenuItem item = (MenuItem)arg.getSource();
        if (item.getLabel().equals("Open"))
        {
          tray.remove(ScreenCapture.trayIcon);
          ScreenCapture.showMe();
        }
        else if (item.getLabel().equals("Edit"))
        {
          edit.doClick();
        }
        else if (item.getLabel().equals("Exit"))
        {
          tray.remove(ScreenCapture.trayIcon);
          System.exit(0);
        }
      }
    };
    PopupMenu menu = new PopupMenu();
    
    MenuItem open = new MenuItem("Open");
    open.addActionListener(trayListener);
    menu.add(open);
    
    MenuItem editMI = new MenuItem("Edit");
    editMI.addActionListener(trayListener);
    menu.add(editMI);
    
    MenuItem close = new MenuItem("Exit");
    close.addActionListener(trayListener);
    menu.add(close);
    

    trayIcon.setPopupMenu(menu);
    trayIcon.setImageAutoSize(true);
    
    MouseListener trayMouseListener = new MouseAdapter()
    {
      Timer timer = null;
      
      public void mouseClicked(MouseEvent e)
      {
        if (e.getButton() != 1) {
          return;
        }
        if (this.timer == null)
        {
          this.timer = new Timer(300, new ActionListener()
          {
            public void actionPerformed(ActionEvent arg0)
            {
              timer.stop();
              timer = null;
              doClickTasks(1);
            }
          });
          this.timer.start();
        }
        else
        {
          this.timer.stop();
          this.timer = null;
          doClickTasks(2);
        }
        super.mouseClicked(e);
      }
      
      private void doClickTasks(int clickType)
      {
        switch (clickType)
        {
        case 1: 
          capture.doClick();
          break;
        case 2: 
          edit.doClick();
        }
      }
    };
    trayIcon.addMouseListener(trayMouseListener);
    try
    {
      tray.add(trayIcon);
      mainWindow.setVisible(false);
    }
    catch (AWTException e)
    {
      e.printStackTrace();
    }
  }
  
  private void buildSettingsDialog()
  {
    if (settingsDialog == null)
    {
      settingsDialog = new JDialog();
      settingsDialog.setTitle("Settings");
      settingsDialog.setModal(true);
      
      JPanel contentPanel = new JPanel();
      contentPanel.setBorder(new EmptyBorder(3, 3, 3, 3));
      contentPanel.setLayout(new GridLayout(3, 2, 2, 2));
      
      contentPanel.add(new JLabel("Save Location"));
      
      ButtonGroup options = new ButtonGroup();
      
      JPanel optionsPanel = new JPanel();
      optionsPanel
        .setLayout(new BoxLayout(optionsPanel, 0));
      
      final JRadioButton clipboard = new JRadioButton("Clipboard");
      JRadioButton file = new JRadioButton("File");
      if (settings.containsKey("saveto"))
      {
        String option = (String)settings.get("saveto");
        if (option.equals("clipboard")) {
          clipboard.setSelected(true);
        } else {
          file.setSelected(true);
        }
      }
      else
      {
        settings.put("saveto", "clipboard");
        clipboard.setSelected(true);
      }
      options.add(clipboard);
      optionsPanel.add(clipboard);
      options.add(file);
      optionsPanel.add(file);
      
      contentPanel.add(optionsPanel);
      
      JLabel outline = new JLabel("Outline");
      JCheckBox outlineCheck = new JCheckBox();
      
      contentPanel.add(outline);
      contentPanel.add(outlineCheck);
      
      JButton ok = new JButton("OK");
      ok.addActionListener(new ActionListener()
      {
        public void actionPerformed(ActionEvent arg0)
        {
          String selection = clipboard.isSelected() ? "clipboard" : 
            "file";
          ScreenCapture.settings.put("saveto", selection);
          ScreenCapture.settingsDialog.setVisible(false);
        }
      });
      contentPanel.add(ok);
      
      JButton cancel = new JButton("Cancel");
      cancel.addActionListener(new ActionListener()
      {
        public void actionPerformed(ActionEvent arg0)
        {
          ScreenCapture.settingsDialog.setVisible(false);
        }
      });
      contentPanel.add(cancel);
      
      settingsDialog.getContentPane().add(contentPanel);
      settingsDialog.pack();
      settingsDialog.setLocationRelativeTo(mainWindow);
    }
    settingsDialog.setVisible(true);
  }
  
  protected void initializeScreenWindow()
  {
    Rectangle myRectangle = new Rectangle(getScreenDimension());
    screen = getScreenInRectangle(myRectangle);
    if (screen != null)
    {
      buildWindowWithScreen();
      setListeners();
      screenWindow.setVisible(true);
    }
    else
    {
      JDialog dialog = new JDialog();
      dialog.setTitle("ScreenCapture Error!!");
      dialog.setLocationRelativeTo(null);
      
      JLabel label = new JLabel(
        "There is a problem loading the application");
      label.setBorder(new EmptyBorder(10, 10, 10, 10));
      dialog.getContentPane().add(label);
      
      dialog.pack();
      dialog.setVisible(true);
    }
  }
  
  private void setListeners()
  {
    if (screenWindow.getMouseListeners().length == 0) {
      screenWindow.addMouseListener(new MouseAdapter()
      {
        public void mousePressed(MouseEvent e)
        {
          ScreenCapture.start = e.getPoint();
        }
        
        public void mouseReleased(MouseEvent e)
        {
          ScreenCapture.end = e.getPoint();
          if (ScreenCapture.start.distance(ScreenCapture.end) != 0.0D) {
            ScreenCapture.this.captureToClipboard();
          }
          if (ScreenCapture.screenWindow != null)
          {
            ScreenCapture.start = ScreenCapture.end = null;
            ScreenCapture.screenWindow.setVisible(false);
            ScreenCapture.showMe();
          }
        }
      });
    }
    if (screenWindow.getMouseMotionListeners().length == 0) {
      screenWindow.addMouseMotionListener(new MouseMotionAdapter()
      {
        public void mouseDragged(MouseEvent e)
        {
          ScreenCapture.end = e.getPoint();
          ScreenCapture.drawPanel.repaint();
        }
      });
    }
  }
  
  private void captureAndSaveToFile()
  {
    Rectangle rectangle = AppUtils.buildRectangle(start, end);
    try
    {
      BufferedImage image = getSubImage(screen, rectangle);
      byte[] contents = ImageManipulation.getDippedContents(image);
      FileOutputStream fout = new FileOutputStream(new File(
        System.currentTimeMillis() + ".jpg"));
      fout.write(contents);
      fout.close();
    }
    catch (IOException e)
    {
      e.printStackTrace();
    }
  }
  
  private void captureToClipboard()
  {
    Rectangle rectangle = AppUtils.buildRectangle(start, end);
    BufferedImage image = getSubImage(screen, rectangle);
    final ImageToClipboard img = new ImageToClipboard(image);
    screen = image;
    Runnable runnable = new Runnable()
    {
      public void run()
      {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(img, null);
      }
    };
    Thread t = new Thread(runnable);
    t.setDaemon(true);
    t.start();
  }
  
  private void buildWindowWithScreen()
  {
    if (screenWindow == null)
    {
      screenWindow = new JFrame();
      screenWindow.setBounds(new Rectangle(getScreenDimension()));
      
      screenWindow.setUndecorated(true);
      screenPanel = new JPanel()
      {
        private static final long serialVersionUID = 4179177663208901709L;
        
        public void paintComponent(Graphics g)
        {
          super.paintComponent(g);
          g.drawImage(ScreenCapture.screen, 0, 0, null);
        }
      };
      screenPanel.setSize(getScreenDimension());
      screenWindow.getLayeredPane().add(screenPanel, 0);
      
      drawPanel = new JPanel()
      {
        private static final long serialVersionUID = 87102084201915159L;
        
        public void paintComponent(Graphics g)
        {
          super.paintComponent(g);
          if ((ScreenCapture.start != null) && (ScreenCapture.end != null))
          {
            g.setXORMode(new Color(1728053247, true));
            Rectangle newRect = AppUtils.buildRectangle(ScreenCapture.start, ScreenCapture.end);
            Graphics2D g2d = (Graphics2D)g;
            g2d.setStroke(new BasicStroke(2.0F));
            g2d.fillRect(newRect.x, newRect.y, newRect.width, 
              newRect.height);
          }
        }
      };
      drawPanel.setSize(getScreenDimension());
      drawPanel.setOpaque(false);
      screenWindow.getLayeredPane().add(drawPanel, 0);
      screenWindow.setAlwaysOnTop(true);
    }
  }
  
  private BufferedImage getScreenInRectangle(Rectangle myRectangle)
  {
    try
    {
      Robot robot = new Robot();
      return robot.createScreenCapture(myRectangle);
    }
    catch (AWTException e)
    {
      e.printStackTrace();
    }
    return null;
  }
  
  private BufferedImage getSubImage(BufferedImage image, Rectangle rectangle)
  {
    BufferedImage img = image.getSubimage(rectangle.x, rectangle.y, 
      rectangle.width, rectangle.height);
    return img;
  }
  
  private Dimension getScreenDimension()
  {
    return Toolkit.getDefaultToolkit().getScreenSize();
  }
  
  public static void showMe()
  {
    TrayIcon[] icons = SystemTray.getSystemTray().getTrayIcons();
    TrayIcon[] arrayOfTrayIcon1 = icons;int j = icons.length;
    for (int i = 0; i < j; i++)
    {
      TrayIcon trayIcon1 = arrayOfTrayIcon1[i];
      if (trayIcon == trayIcon1) {
        return;
      }
    }
    mainWindow.setState(0);
    mainWindow.setVisible(true);
    mainWindow.toFront();
  }
  
  public void sendToEdit()
  {
    Slate slate = new Slate(screen);
    slate.init();
  }
}
