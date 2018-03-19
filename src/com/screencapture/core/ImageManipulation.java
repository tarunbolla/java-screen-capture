package com.screencapture.core;

import com.sun.image.codec.jpeg.ImageFormatException;
import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGEncodeParam;
import com.sun.image.codec.jpeg.JPEGImageEncoder;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ImageManipulation
{
  public static byte[] getDippedContents(BufferedImage image)
    throws ImageFormatException, IOException
  {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    JPEGImageEncoder jpegEncoder = JPEGCodec.createJPEGEncoder(out);
    JPEGEncodeParam jpegEncodeParam = jpegEncoder
      .getDefaultJPEGEncodeParam(image);
    jpegEncodeParam.setDensityUnit(1);
    jpegEncodeParam.setXDensity(96);
    jpegEncodeParam.setYDensity(96);
    jpegEncoder.encode(image, jpegEncodeParam);
    return out.toByteArray();
  }
}
