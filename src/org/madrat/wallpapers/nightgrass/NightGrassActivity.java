package org.madrat.wallpapers.nightgrass;

import android.app.Activity;
import android.os.Bundle;

public class NightGrassActivity extends Activity 
{
	GrassView view = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		view = new GrassView(this); 
		setContentView(view);
	}
	
	@Override
	protected void onResume()
	{
		super.onResume();
		view.resume();
	}
	
	@Override
	protected void onPause()
	{
		view.pause();
		super.onPause();		
	}	
}
