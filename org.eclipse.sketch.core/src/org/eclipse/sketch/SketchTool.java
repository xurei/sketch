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


import java.util.ArrayList;

import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.tools.AbstractTool;
import org.eclipse.gmf.runtime.diagram.ui.parts.DiagramEditor;
import org.eclipse.gmf.runtime.emf.type.core.IElementType;
import org.eclipse.sketch.clientobserver.ISketchListener;
import org.eclipse.sketch.ui.views.SketchRecognizerControlView;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;


/**
 * Basic SketchTool, provides some abstract methods to be overwritten by client tools
 * @author  Ugo Sangiorgi
 */
public abstract class SketchTool extends AbstractTool{
	
	//the points of the current sketch
	private ArrayList<Point> points = new ArrayList<Point>();

	private ArrayList<Point> quantizedPoints = new ArrayList<Point>();
	
	protected SketchManager manager = new SketchManager();

	private MonitorThread thread = new MonitorThread(Display.getCurrent());
	
	private Color color;
	private Color sampleColor;
	private Color interpColor;
	private Color nodrawColor;
	private static final int NODRAW_POINT=0, INTERPOLATED_POINT=1;
	
	
	//Default Parameters
	int grid = 6;
	long penupdownTolerance = 1500;
	
	boolean showSamples = false;
	
	private GC gc;
	
	private long penuptime = -1;
	
	//abstract methods 
	public abstract ArrayList getTypes();
	public abstract IElementType getConnection();
	public abstract IElementType getDashedConnection();
	
	public abstract ISketchListener getClient();
	
	public abstract RGB getStrokeColor();
	public abstract Cursor getCursor();
	
	
	public SketchTool() {
		super();
		
		System.out.println("SketchTool is activated");
		
		setDefaultCursor(getCursor());		
		
		manager.setEditor((DiagramEditor) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor());
		manager.attach(getClient());
		

		SketchRecognizerControlView control = ((SketchRecognizerControlView) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView("org.eclipse.sketch.ui.views.SketchRecognizerControlView"));
		penupdownTolerance  = control!=null?control.getTolerance():1600;		
		grid = control!=null?control.getGridSize():2;
		showSamples = control!=null?control.getShowSamples():false;
		
		
		gc = new GC(manager.getEditor().getDiagramGraphicalViewer().getControl());
		gc.setAntialias(SWT.ON);
		
		int linewidth = control!=null?control.getLineWidth():1;	
		gc.setLineWidth(linewidth);
		color = new Color(gc.getDevice(),getStrokeColor());
		gc.setForeground(color);

		sampleColor = new Color(gc.getDevice(),160,0,60);
		interpColor = new Color(gc.getDevice(),0,160,60);
		nodrawColor = new Color(gc.getDevice(),160,160,160);
		
		SketchBank.getInstance().setTypes(getTypes());
		
		
		manager.setTypeForConnection(getConnection());
		manager.setTypeForDashedConnection(getDashedConnection());
		
		
		thread.start();
	}

	
	
	
	@Override
	public void deactivate() {
		thread.done = true;

		try{
			
			SketchRecognizerControlView view = (SketchRecognizerControlView) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView("org.eclipse.sketch.ui.views.SketchRecognizerControlView");			
			if(view != null){
			manager.detach(view.getControl());
			}
			}catch(Exception e){
				//e.printStackTrace();
			}
		super.deactivate();
	}


	@Override
	public void activate() {
		try{
			SketchRecognizerControlView view = (SketchRecognizerControlView) PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findView("org.eclipse.sketch.ui.views.SketchRecognizerControlView");
			if(view != null){
				view.getControl().setTypes(SketchBank.getInstance().getAvailableTypes());
				manager.attach(view.getControl());
			}
			}catch(Exception e){
				//e.printStackTrace();
			}
		super.activate();
	}

	
	
	@Override
	public boolean handleButtonDown(int button) {
		penuptime=-1;

		Point qp = new Point(Math.round(getLocation().x / grid), Math.round(getLocation().y / grid));
		//quantizedPoints.add(qp);
		boolean must_draw_begin = (prev_qp!=null);
		addQP(qp, SketchTool.NODRAW_POINT);
		if (must_draw_begin)
			quantizedPoints.add(new Point(-2,-2));

		points.add(getLocation());
		
		return super.handleButtonDown(button);
	}
		
	@Override
	protected final boolean handleDrag() {
		Point location = getLocation();
		points.add(location);
		
		//In order to be able to draw the beginning of the sketch
			if ( points.size()<2 || (points.size()>1 && points.get(points.size()-2).x == -1) )
				points.add(location);

		//updates the editor view
			Point point1 = points.get(points.size()-2);
			Point point2 = points.get(points.size()-1);		
			gc.drawLine(point1.x, point1.y, point2.x, point2.y);

		//count++;
		//if(count==grid)
			Point qp = new Point(Math.round(location.x / grid), Math.round(location.y / grid));
			addQP(qp, SketchTool.INTERPOLATED_POINT);
			
			//count=0;
		return true;
	}
	
	
	private Point prev_qp=null;
	/**
	 * @param qp the quantized point
	 * @param interpolated 0 if sample, 1 if interpolated, 2 if interpolated and non-drawn 
	 * (ie when the pen doesn't touch the surface for a while) 
	 */
	private void addQP(Point qp, int interpolated)
	{
			if (prev_qp!=null) //If there is some points sampled before this one, we try to interpolate
			{	
				if(showSamples)
					switch (interpolated)
					{
						case SketchTool.NODRAW_POINT:       gc.setForeground(nodrawColor); break;
						case SketchTool.INTERPOLATED_POINT: gc.setForeground(interpColor); break;
					}
				
				//Point prev_qp = quantizedPoints.get(quantizedPoints.size()-1);
				
				Dimension diff = qp.getDifference(prev_qp);
				
				int dx = Math.abs(diff.width), dy = Math.abs(diff.height);
				//If the distance between points is too big, they're interpolated
				if (dx > 1 || dy > 1)
				{					
					int max_diff = (int)Math.max(dx, dy);
					float deltax = diff.width  / (float)max_diff;
					float deltay = diff.height / (float)max_diff;
					
					for (int i=0; i<max_diff; i++)
					{
						Point interp = new Point(prev_qp);
						interp.x += deltax*i;
						interp.y += deltay*i;
						quantizedPoints.add(interp.getScaled(grid));
						if(showSamples)
							gc.drawRectangle(interp.x*grid,interp.y*grid,2,2);
					}						
				}
			}
			
			//Final point, the one that is actually sampled
			quantizedPoints.add(qp.getScaled(grid));
			prev_qp = qp;
	
			if(showSamples)
			{
				gc.setForeground(sampleColor);
				
				gc.drawRectangle(qp.x*grid,qp.y*grid,2,2);
				gc.setForeground(color);
			}
	}
	
	@Override
	public boolean handleButtonUp(int button) 
	{
		penuptime = System.currentTimeMillis();
	
		points.add(new Point(-1,-1));
		quantizedPoints.add(new Point(-1,-1));
						
		return super.handleButtonUp(button);
	}
	
	/**
	 * Cleans up the current points in buffers
	 */
	public void cleanup(){
		System.out.println("Quantized points : ");
		for (Point p : quantizedPoints)
		System.out.println(p);
		
		points = new ArrayList<Point>();
		quantizedPoints = new ArrayList<Point>();
		
		prev_qp = null;
		
		penuptime = -1;
	}


	@Override
	protected String getCommandName() {
		return null;
	}

	/**
	 * 
	 * Creates a new unprocessed Sketch and passes it to the Manager
	 * 
	 * @author ugo
	 */
	class MonitorThread extends Thread {  
		private Display d;  

		public boolean done = false;
		public MonitorThread(Display _d){  
			d = _d;  
		}  

		public void run(){  

			while (!done){  
				d.asyncExec(new Runnable(){  
					public void run(){  


						if(penuptime>0){

							if(System.currentTimeMillis()-penuptime>penupdownTolerance){

								penuptime=-1;
								Sketch sketch = new Sketch();

								sketch.setPoints(points);
								sketch.setQuantizedPoints(quantizedPoints);

								manager.newSketch(sketch);

								//erases the drawing area
								manager.getEditor().getDiagramGraphicalViewer().getControl().redraw();	 

								
								cleanup();
							}
						}
					}  

				});  
				
				try {  
					Thread.sleep(penupdownTolerance/3);  
				} catch (InterruptedException e) {  
					e.printStackTrace();  
				}  
			}  
		}  
	}  
}