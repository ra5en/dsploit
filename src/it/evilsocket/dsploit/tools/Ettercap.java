/*
 * This file is part of the dSploit.
 *
 * Copyleft of Simone Margaritelli aka evilsocket <evilsocket@gmail.com>
 *
 * dSploit is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * dSploit is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with dSploit.  If not, see <http://www.gnu.org/licenses/>.
 */
package it.evilsocket.dsploit.tools;

import it.evilsocket.dsploit.core.System;
import it.evilsocket.dsploit.core.Shell.OutputReceiver;
import it.evilsocket.dsploit.net.Target;
import android.content.Context;
import android.util.Log;

public class Ettercap extends Tool
{
	private static final String TAG = "ETTERCAP";
	
	public Ettercap( Context context ){
		super( "ettercap/ettercap", context );		
	}

	// ettercap does not start immediately, so we need a listener to know
	// when we can re-enable ip forwarding and so on ...
	public static abstract class OnReadyListener implements OutputReceiver
	{
		@Override
		public void onStart(String command) {
			
		}

		@Override
		public void onNewLine(String line) {	
			if( line.toLowerCase().contains("for inline help") )
				onReady();			
		}

		@Override
		public void onEnd(int exitCode) {
			
		}
		
		public abstract void onReady();
	}
	
	public Thread spoof( Target target, OnReadyListener listener ) {
		String commandLine;

		// poison the entire network
		if( target.getType() == Target.Type.NETWORK )
			commandLine = "// //";
		// router -> target poison
		else
			commandLine = "/" + target.getCommandLineRepresentation() + "/ //"; 
		
		try
		{
			commandLine = "-Tq -M arp:remote -i " + System.getNetwork().getInterface().getDisplayName() + " " + commandLine;
		}
		catch( Exception e )
		{
			Log.e( TAG, e.toString() );
		}
		
		return super.async( commandLine, listener );
	}
	
	public Thread drop( Target target, OnReadyListener listener ) {
		String commandLine = ""; 
		
		try
		{			
			EtterFilter filter   = new EtterFilter( mAppContext, "drop" );
			String		compiled = mDirName + "/drop.ef";
			
			filter.setVariable( "$target_ip", "'" + target.getCommandLineRepresentation() + "'" );
			filter.compile( compiled );
			
			commandLine = "-Tq -M arp:remote -F " + compiled + " -i " + System.getNetwork().getInterface().getDisplayName() + " " + "/" + target.getCommandLineRepresentation() + "/ //";
		}
		catch( Exception e )
		{
			Log.e( TAG, e.toString() );
		}
		
		return super.async( commandLine, listener );
	}
	
	public boolean kill( ) {
		// Ettercap needs SIGINT ( ctrl+c ) to restore arp table.
		return super.kill( "SIGINT" );
	}
}