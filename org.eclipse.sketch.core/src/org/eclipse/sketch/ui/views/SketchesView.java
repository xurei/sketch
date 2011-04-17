/*******************************************************************************
 * Copyright (c) 2010 Ugo Sangiorgi and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Ugo Sangiorgi <ugo.sangiorgi@gmail.com> - Initial contribution
 *******************************************************************************/
package org.eclipse.sketch.ui.views;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.nebula.widgets.gallery.DefaultGalleryGroupRenderer;
import org.eclipse.nebula.widgets.gallery.DefaultGalleryItemRenderer;
import org.eclipse.nebula.widgets.gallery.Gallery;
import org.eclipse.nebula.widgets.gallery.GalleryItem;
import org.eclipse.sketch.Sketch.Dna;
import org.eclipse.sketch.SketchBank;
import org.eclipse.swt.SWT;
//import org.eclipse.swt.graphics.Image;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.*;

import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;



/**
 * This sample class demonstrates how to plug-in a new
 * workbench view. The view shows data obtained from the
 * model. The sample creates a dummy model on the fly,
 * but a real implementation would connect to the model
 * available either in this or another plug-in (e.g. the workspace).
 * The view is connected to the model using a content provider.
 * <p>
 * The view uses a label provider to define how model
 * objects should be presented in the view. Each
 * view can present the same model objects using
 * different labels and icons, if needed. Alternatively,
 * a single label provider can be shared between views
 * in order to ensure that objects of the same type are
 * presented in the same way everywhere.
 * <p>
 */

public class SketchesView extends ViewPart {

	

	 /**
    * The ID of the view as specified by the extension.
    */
   public static final String ID = "org.eclipse.sketch.ui.views.SketchesView";
   Gallery gallery_1;
   Composite parent;
   private static SketchBank bank = SketchBank.getInstance(); 
	
	public SketchesView() {
	}

	@Override
	public void createPartControl(Composite parent) {
		this.parent = parent;
		gallery_1 = new Gallery(parent, SWT.BORDER);
		gallery_1.setVertical(true);
		gallery_1.setGroupRenderer(new DefaultGalleryGroupRenderer());
		gallery_1.setItemRenderer(new DefaultGalleryItemRenderer());
		
		GalleryItem galleryItem = new GalleryItem(gallery_1, SWT.NONE);
		galleryItem.setText("New Item");
		
		loadGallery();
	}
	
	private BufferedImage buildImageFromDna(Dna dna)
	{
		int l = dna.toString().length();
		
		LinkedList<Integer> pathx = new LinkedList<Integer>();		
		LinkedList<Integer> pathy = new LinkedList<Integer>();	
		LinkedList<Boolean> pathpen = new LinkedList<Boolean>();
		boolean must_draw = true;
		
		//Calculating the image size
			int curx=0, cury=0,
		  left=0, right=0,
		  top=0, bottom=0;
		
			for (int i=0; i!=l; ++i)
			{
			  char dir = dna.toString().charAt(i);
			  switch(dir)
			  {
			    case '[':
			    	must_draw = false;    
				  break;
			    case ']':
			    	must_draw = true;			    
			    break;
			    
			    case '1':cury++;         break;
			    case '2':curx++;cury++; break;
			    case '3':curx++;         break;
			    case '4':curx++;cury--; break;
			    case '5':cury--;         break;
			    case '6':curx--;cury--; break;
			    case '7':curx--;         break;
			    case '8':curx--;cury++; break;
			    default:
			    	//TODO throw something
			      System.err.println("ERROR : don't understand this DNA : "+dir);
			  }
			  
			  if (top    < cury) top = cury;
			  if (right  < curx) right = curx;
			  if (bottom > cury) bottom = cury;
			  if (left   > curx) left = curx;
		    
			  pathx.push(curx);			    
			  pathy.push(cury);
			  pathpen.push(must_draw);
			}
			
			final int border = 2; //Size of the border, in dna's distance unit
			final int grid_size = 2;
			int realW = (right-left + border*2)*grid_size;
			int realH = (top-bottom + border*2)*grid_size;
			int W, H;
			W = H = Math.max(realW, realH);
			int offsetx = border-left;
			int offsety = border-bottom;
			
			if (realW != W) //Centering in x-axis
				offsetx += (W-realW)/4;
			if (realH != H) //Centering in y-axis
				offsety += (H-realH)/4;
		//End of Calculating the image size
			
		//Building the image from the sketch dna					
			BufferedImage out = new BufferedImage(W,H, BufferedImage.TYPE_BYTE_INDEXED); 
			Graphics g = out.getGraphics();
			g.setColor(Color.WHITE); g.fillRect(0, 0, W, H);
			
			g.setColor(Color.BLACK);
		//End of building the image
			
		curx=(offsetx)*grid_size; 
		cury=H-(offsety)*grid_size;

		Iterator<Integer> itx = pathx.iterator();
		Iterator<Integer> ity = pathy.iterator();
		Iterator<Boolean> itpen = pathpen.iterator();
		while (itx.hasNext())
		{
			int x = (itx.next() + offsetx)*grid_size;
			int y = H-((ity.next() + offsety))*grid_size;
			
			if (itpen.next())
				g.drawLine(curx,cury, x,y);
			
			curx = x;
			cury = y;
		}
		
		// Create new (blank) image of required (scaled) size
		
		final int thumbsize = 88; //FIXME This should be done dynamically : it is the size of a thumbnail in the view
		
		BufferedImage scaledImage = new BufferedImage(
			thumbsize, thumbsize, BufferedImage.TYPE_BYTE_INDEXED);

		// Paint scaled version of image to new image

		Graphics2D graphics2D = scaledImage.createGraphics();
		graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
			RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		graphics2D.drawImage(out, 0, 0, thumbsize, thumbsize, null);

		// clean up
		graphics2D.dispose();
		g.dispose();
		
		return scaledImage;
	}

	/**
	 * Fills up the gallery with sketch images from the database (SketchBank)
	 * TODO: retrieve sketches from the SketchBank and insert the images on the gallery
	 */
	private void loadGallery()
	{
		try
		{
			IWorkspaceRoot myWorkspaceRoot= ResourcesPlugin.getWorkspace().getRoot();

			gallery_1.removeAll();
			
			ArrayList<Object> types = bank.getAvailableTypes();
			
			for (Object t: types)			
			{
				GalleryItem group = new GalleryItem(gallery_1, SWT.NONE);
				//group.setText(RelativePath.getRelativePath(openedFile, f)); //NON-NLS-1$
				group.setText(t.toString());
				group.setExpanded(false);
				
				ArrayList<String> sketches = bank.getSketches(t);
				
				for (String dna: sketches)
				{					
					BufferedImage sketch_img = buildImageFromDna(new Dna(dna));
					
					GalleryItem item = new GalleryItem(group, SWT.NONE);
					
					Image itemImage = new Image(parent.getDisplay(), convertToSWT(sketch_img));
					item.setImage(itemImage);
				}
			}

		}catch(Exception e){
			e.printStackTrace();
		}
	}
	
	public static ImageData convertToSWT(BufferedImage bufferedImage) {
        if (bufferedImage.getColorModel() instanceof DirectColorModel) {
            DirectColorModel colorModel
                    = (DirectColorModel) bufferedImage.getColorModel();
            PaletteData palette = new PaletteData(colorModel.getRedMask(),
                    colorModel.getGreenMask(), colorModel.getBlueMask());
            ImageData data = new ImageData(bufferedImage.getWidth(),
                    bufferedImage.getHeight(), colorModel.getPixelSize(),
                    palette);
            WritableRaster raster = bufferedImage.getRaster();
            int[] pixelArray = new int[3];
            for (int y = 0; y < data.height; y++) {
                for (int x = 0; x < data.width; x++) {
                    raster.getPixel(x, y, pixelArray);
                    int pixel = palette.getPixel(new RGB(pixelArray[0],
                            pixelArray[1], pixelArray[2]));
                    data.setPixel(x, y, pixel);
                }
            }
            return data;
        }
        else if (bufferedImage.getColorModel() instanceof IndexColorModel) {
            IndexColorModel colorModel = (IndexColorModel)
                    bufferedImage.getColorModel();
            int size = colorModel.getMapSize();
            byte[] reds = new byte[size];
            byte[] greens = new byte[size];
            byte[] blues = new byte[size];
            colorModel.getReds(reds);
            colorModel.getGreens(greens);
            colorModel.getBlues(blues);
            RGB[] rgbs = new RGB[size];
            for (int i = 0; i < rgbs.length; i++) {
                rgbs[i] = new RGB(reds[i] & 0xFF, greens[i] & 0xFF,
                        blues[i] & 0xFF);
            }
            PaletteData palette = new PaletteData(rgbs);
            ImageData data = new ImageData(bufferedImage.getWidth(),
                    bufferedImage.getHeight(), colorModel.getPixelSize(),
                    palette);
            data.transparentPixel = colorModel.getTransparentPixel();
            WritableRaster raster = bufferedImage.getRaster();
            int[] pixelArray = new int[1];
            for (int y = 0; y < data.height; y++) {
                for (int x = 0; x < data.width; x++) {
                    raster.getPixel(x, y, pixelArray);
                    data.setPixel(x, y, pixelArray[0]);
                }
            }
            return data;
        }
        else
        {
        	System.err.println("Invalid color model");
        }
        return null;
    }
	
	@Override
	public void setFocus() {
		// TODO Auto-generated method stub
		
	}
}
