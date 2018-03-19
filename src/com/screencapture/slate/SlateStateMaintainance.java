package com.screencapture.slate;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;

public class SlateStateMaintainance
{
  public SlateStateMaintainance previousState;
  public SlateStateMaintainance nextState;
  public Shape shape;
  public Color color;
  public boolean fill;
  public float strokeThickness;
  
  public void paint(Graphics2D g)
  {
    if (this.shape != null)
    {
      g.setStroke(new BasicStroke(this.strokeThickness));
      Color colorNow = g.getColor();
      g.setColor(this.color);
      if ((this.fill) && (!(this.shape instanceof Line2D)) && (!(this.shape instanceof GeneralPath))) {
        g.fill(this.shape);
      } else {
        g.draw(this.shape);
      }
      g.setColor(colorNow);
    }
  }
}
