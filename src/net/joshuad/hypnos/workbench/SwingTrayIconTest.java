/*
 * Copyright (c) 1995, 2008, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle or the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package net.joshuad.hypnos.workbench;
/*
 * TrayIconDemo.java
 */

import java.awt.Image;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.swing.*;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class SwingTrayIconTest extends Application {
	public static void main ( String[] args ) {
		Application.launch( args );
	}

	@Override
	public void start ( Stage stage ) throws Exception {
		ContextMenu menu = new ContextMenu ();
		MenuItem a = new MenuItem ( "AAAA" );
		MenuItem b = new MenuItem ( "BBBB" );
		MenuItem c = new MenuItem ( "CCCC" );
		MenuItem d = new MenuItem ( "DDDD" );
		menu.getItems().addAll( a, b, c, d );
		stage.show();
		menu.setWidth( 500 );
		menu.setHeight( 500 );
		
		StackPane root = new StackPane();
		
		stage.setScene(new Scene( root, 300, 250));
		
		System.out.println ( "System tray supported: " + SystemTray.isSupported() );
		
		SwingUtilities.invokeLater( new Runnable() {
			public void run () {
				try {
					
					URL url = new URL("http://icons.iconarchive.com/icons/hopstarter/sleek-xp-basic/24/Home-icon.png");
					Image image = ImageIO.read(url);
					ImageIcon ii = new ImageIcon( image );
					final TrayIcon trayIcon = new TrayIcon( ii.getImage(), null );

					trayIcon.addMouseListener( new MouseAdapter() {

						public void mousePressed ( MouseEvent e ) {
							
							if ( e.getButton() == MouseEvent.BUTTON3 ) {	
								Platform.runLater( () -> {
									menu.setX( e.getX() );
									menu.setY( e.getY() );
									menu.show ( stage );
								});
							}
						}
					} );

					trayIcon.setImageAutoSize( true );
					SystemTray.getSystemTray().add( trayIcon );
				} catch ( Exception e ) {
					e.printStackTrace();
				}
			}
		} );
	}
}