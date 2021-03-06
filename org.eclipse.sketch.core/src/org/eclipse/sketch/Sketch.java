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

package org.eclipse.sketch;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;

/**
 * Sketch object is created by the Recognizer and is responsible to hold all the information
 * regarding the recognition made somewhere else.
 * @author Ugo Sangiorgi
 */
public class Sketch 
{	
	/**
	 * Defines the DNA of a sketch. It is basically a String, with some useful methods
	 * (as reverting the DNA)
	 * @author Olivier Bourdoux <olivier.bourdoux@gmail.com>
	 */
	public static class Dna
	{
		private String value;
		
		public Dna(String v)
		{ value = v; }
		
		public String toString()
		{ return value; }
		
		/**
		 * @return the reverse DNA of this instance
		 */
		public Dna reverse()
		{
			StringBuffer out = new StringBuffer();
			StringBuffer buf = new StringBuffer();
			
			int l = value.length();
			for (int i=0; i!=l; ++i)
			{
				int c = Integer.parseInt(value.substring(i, i+1));
				
				if (c==0)
				{
					if (buf.length()!=0)
					{
						out.append(buf.reverse().append(0));
						buf = new StringBuffer();
					}
				}
				else
				{
					c = (c+3)%8+1; //computes the reverse direction
					buf.append(c);
				}
			}
			
			return new Dna(out./*reverse().*/toString());
		}
	}
// ------------------------------------------------------------------------------
	
	public int[][] bitmap()
	{
		LinkedList<Integer> pathx = new LinkedList<Integer>();		
		LinkedList<Integer> pathy = new LinkedList<Integer>();	
		
		//Calculating the image size
			int 
		  left=Integer.MAX_VALUE, right=0,
		  top=Integer.MAX_VALUE, bottom=0;

			for (Point p:points)
			{
			  if (top    > p.y) top = p.y;
			  if (right  < p.x) right = p.x;
			  if (bottom < p.y) bottom = p.y;
			  if (left   > p.x) left = p.x;
				
				pathx.push(p.x);			    
			  pathy.push(p.y);
			}
			
			int W = right-left+1;
			int H = bottom-top+1;
			
			System.out.println("WH:"+W+" "+H);
			System.out.println("TLBR:"+top+" "+left+" "+bottom+" "+right);
		//End of Calculating the image size
	//End of Calculating the image size
		
	//Building the image from the sketch dna					
		BufferedImage out = new BufferedImage(W,H, BufferedImage.TYPE_BYTE_INDEXED); 
		Graphics g = out.getGraphics();
		g.setColor(Color.WHITE); g.fillRect(0, 0, W, H);
		g.setColor(Color.BLACK);
	//End of building the image
		
		int curx=points.get(0).x-left; 
		int cury=points.get(0).y-top;
	
		Iterator<Integer> itx = pathx.iterator();
		Iterator<Integer> ity = pathy.iterator();
		while (itx.hasNext())
		{
			int x = itx.next()-left;
			int y = ity.next()-top;
			
			g.drawLine(curx,cury, x,y);
			
			curx = x;
			cury = y;
		}
		
		int out2[][] = new int[H][W];
		for (int y=0; y!=H; ++y)
		{
			for (int x=0; x!=W; ++x)
				out2[y][x] = (out.getRGB(x, y)==-1)?0:1;
		}
		
		/*
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
		
		return scaledImage;*/
		return out2;
	}

	public static String ELEMENT_RESULT_KEY = "ELEMENT_RESULT_KEY_HashMap_IElementType_Integer";

	private ArrayList<Point> points = new ArrayList<Point>();
	private ArrayList<Point> quantizedPoints = new ArrayList<Point>();
	
	private Point location;
	private Dimension size;
	private Dna dna;
	
	
	//result of a computation
	private HashMap<String,Object> result = new HashMap<String,Object>();

	
	
	/**
	 * 
	 * @return the result of a computation regarding what this sketch is, in a map.
	 */
	public HashMap<String, Object> getResult() {
		return result;
	}

	/**
	 * 
	 * @return the Point relative to the diagram
	 */
	public Point getLocation() {
		if(location == null){
			location = computeLocation(quantizedPoints);
		}
		return location;
	}
	
	/**
	 * 
	 * @return the Dimension of this Sketch
	 */

	public Dimension getSize() {
		if(size == null){
			size = computeSize(quantizedPoints);
		}
		return size;
	}


	
	/**
	 * 
	 * @return the word representing this Sketch
	 */
	public String getDna() {
		if(dna==null)
			dna = buildDna(quantizedPoints);
		return dna.toString();
	}

	/* Olivier : DNA should not be changed outside of a sketch
	   public void setDna(String dna) {
		this.dna = dna;
	}
	
	public void appendDna(String character){
		dna += character;
	}*/
	
	/**
	 * turn this sketch into a word based on cardinal points:
	 * 1 means North, 2 NorthEast, 3 East, 4 SouthEast, 5 South, 6 SouthWest, 7 West, 8 NorthWest 
	 * 0 means a pen lift (another stroke)
	 * Based on work from Adrien Coyette, Sascha Schimke, Jean Vanderdonckt, and Claus Vielhauer - http://www.isys.ucl.ac.be/bchi/publications/2007/Schimke-Interact2007.pdf
	 * @return
	 */
	private static Dna buildDna(ArrayList<Point> quantizedPoints){
		StringBuilder s = new StringBuilder();
		
		for(int i=0;i<quantizedPoints.size()-1;i++){

			Point p0 = quantizedPoints.get(i);
			Point p1 = quantizedPoints.get(i+1);
			
			int x0 = p0.x;
			int y0 = p0.y;

			int x1 = p1.x;
			int y1 = p1.y;

			int x = x1-x0;
			int y = y1-y0;

			if(x1==-1)
			{
				if (x0!=-1)
					s.append('[');
			}
			else if(x1==-2)
			{
				if (x0!=-2)
					s.append(']');
			}
			else{
				if(x>0 && y>0){
					s.append('4');
				}else if(x>0 && y==0){
					s.append('3');
				}else if(x>0 && y<0){
					s.append('2');
				}else if(x==0 && y<0){
					s.append('1');
				}else if(x<0 && y<0){				
					s.append('8');
				}else if(x<0 && y==0){
					s.append('7');
				}else if(x<0 && y>0){
					s.append('6');
				}else if(x==0 && y>0){
					s.append('5');
				}
			}
		}


		return new Dna(s.toString());
	}
	
	/**
	 * 
	 * @return an ArrayList with ALL the points (Point) of this Sketch
	 */
	public ArrayList<Point> getPoints() {
		return points;
	}

	public void setPoints(ArrayList<Point> points) {
		this.points = points;
	}

	/**
	 * 
	 * @return the ArrayList with the points on a grid (less points)
	 */
	public ArrayList<Point> getQuantizedPoints() {
		return quantizedPoints;
	}

	public void setQuantizedPoints(ArrayList<Point> quantizedPoints) {
		this.quantizedPoints = quantizedPoints;
	}
	
	/**
	 * Computes the size of the sketch based on the smallest and the biggest points
	 * @param points
	 * @return the size of the sketch 
	 */
	private Dimension computeSize(ArrayList<Point> points){
		Point p = points.get(0);
				
		int sx = p.x,sy = p.y;
		int bx = p.x,by = p.y;

		for(int i=0;i<points.size();i++){
			Point point = points.get(i);
			
			if(point.x>0){			
				if(point.x<=sx)
					sx = point.x;
			
				if(point.y<=sy)
					sy = point.y;
			
				if(point.x>=bx) 
					bx = point.x;
				if(point.y>=by)				
					by = point.y;
			}			
		}
		
		//relax by 5 pixels
		sx = sx-5;
		bx = bx+5;
		sy = sy-5;
		by = by+5;
		
		
		return new Dimension(bx-sx,by-sy);
	}
	
	private Point computeLocation(ArrayList<Point> points){
		if (points.size()<1)
			return new Point(-1,-1);
		Point p = points.get(1);
		
		int sx = p.x,sy = p.y;

		for(int i=0;i<points.size();i++){
			Point point = points.get(i);
			
			if(point.x>0){			
				if(point.x<=sx)
					sx = point.x;
			
				if(point.y<=sy)
					sy = point.y;

			}			
		}
		
		return new Point(sx,sy);		
	}

	@Override
	public String toString() {
		String s = "[Sketch]";
		s += "\n\tWord (dna): [" + getDna() + "]";
		s += "\n\tpoints: "
			+ getPoints().size() + " registered, "
			+ getQuantizedPoints().size()
			+ " processed";
		s += "\n\tSize: "+getSize() +" | Location: "+getLocation();		
		s += "\n\tComputed Result: "+result;
		return s;
	}
	
//	/**
//	 * Test method for Sketch.Dna
//	 */
//	public static void main(String args[])
//	{
//		Sketch.Dna dna = new Sketch.Dna("11111012340");
//		Sketch.Dna dna2 = new Sketch.Dna("123450");
//		System.out.println(dna);
//		System.out.println(dna.reverse());
//		System.out.println(dna.reverse().reverse());
//	}

	
}
