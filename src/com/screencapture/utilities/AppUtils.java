package com.screencapture.utilities;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

public class AppUtils
{
  public static BufferedImage mergeImages(BufferedImage bg, BufferedImage fg, Point fromPoint)
  {
    BufferedImage finalImage = new BufferedImage(bg.getWidth(), 
      bg.getHeight(), 2);
    Graphics2D g = finalImage.createGraphics();
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, 
      RenderingHints.VALUE_ANTIALIAS_ON);
    g.drawImage(bg, 0, 0, null);
    g.drawImage(fg, fromPoint.x, fromPoint.y, null);
    
    g.dispose();
    
    return finalImage;
  }
  
  public static BufferedImage mergeImages(BufferedImage bg, BufferedImage fg)
  {
    return mergeImages(bg, fg, new Point(0, 0));
  }
  
  public static Point diffPoint(Point point1, Point point2)
  {
    return new Point(point1.x - point2.x, point1.y - point2.y);
  }
  
  public static void clearImage(BufferedImage img)
  {
    Graphics2D g = (Graphics2D)img.getGraphics();
    Color bg = g.getBackground();
    g.setBackground(new Color(255, 255, 255, 0));
    g.clearRect(0, 0, img.getWidth(), img.getHeight());
    g.setBackground(bg);
  }
  
  public static BufferedImage copyImage(BufferedImage image)
  {
    BufferedImage finalImage = new BufferedImage(image.getWidth(), 
      image.getHeight(), 2);
    finalImage.createGraphics().drawImage(image, image.getWidth(), image.getHeight(), null);
    return finalImage;
  }
  
  public static BufferedImage getImagePatch(int width, int height, Color color, boolean fill)
  {
    BufferedImage img = new BufferedImage(width, height, 2);
    Graphics2D g = img.createGraphics();
    g.setStroke(new BasicStroke(2.0F));
    g.setColor(color);
    if (fill) {
      g.fillRect(0, 0, width, height);
    } else {
      g.drawRect(0, 0, width, height);
    }
    g.dispose();
    return img;
  }
  
  public static Rectangle buildRectangle(Point p1, Point p2)
  {
    if ((p1.x < p2.x) && (p1.y < p2.y)) {
      return new Rectangle(p1.x, p1.y, p2.x - p1.x, p2.y - p1.y);
    }
    if ((p1.x < p2.x) && (p1.y > p2.y)) {
      return new Rectangle(p1.x, p2.y, p2.x - p1.x, p1.y - p2.y);
    }
    if ((p1.x > p2.x) && (p1.y > p2.y)) {
      return new Rectangle(p2.x, p2.y, p1.x - p2.x, p1.y - p2.y);
    }
    return new Rectangle(p2.x, p1.y, p1.x - p2.x, p2.y - p1.y);
  }
}
