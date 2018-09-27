/*
 * Settings.java - wallpaper settings activity
 *
 * Copyright (C) 2015, madRat
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.madrat.wallpapers.nightgrass;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

public class Settings extends Activity
{
	private class TaggedString
	{
		private Integer 	id_;
		private String 	string_;

	    public TaggedString(Integer id, String string)
	    {
	    	 id_ = id;
	    	 string_ = string;
	    }

	    @Override
	    public String toString()
	    {
	        return string_;
	    }

	    public Integer getId()
	    {
	    	return id_;
	    }
	}

	SharedPreferences prefs = null;

	private Spinner animationFps = null;
	private Spinner animationTime = null;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final Resources res = getResources();
		prefs = getSharedPreferences(Parameters.sharedPreferences, Context.MODE_PRIVATE);

		setContentView(R.layout.activity_settings);

		animationFps 	= (Spinner)findViewById(R.id.animationFps);
		animationTime 	= (Spinner)findViewById(R.id.animationTime);

		//Animation FPS spinner
		{
			final int rate = Parameters.getInt(prefs, Parameters.rate, Parameters.rateDefault, 0, 1000);

	        ArrayAdapter<TaggedString> adapter =
        			new ArrayAdapter<TaggedString>(this, android.R.layout.simple_spinner_item);
	        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

	        int pos = 0;

			for (int r100ns : res.getIntArray(R.array.rate_values_100ns))
			{
				//Ugly workaround for 'special'
				String text;

				if (0 == r100ns)
				{
					text = res.getString(R.string.no_animation);
				}
				else
				{
					final int fps = 10000 / r100ns;
					text = res.getQuantityString(R.plurals.fps, fps, fps);
				}

				final int curRate = r100ns / 10;

				if (rate == curRate)
					pos = adapter.getCount();

				TaggedString item = new TaggedString(curRate, text);
				adapter.add(item);
			}


			animationFps.setAdapter(adapter);
			animationFps.setSelection(pos);

			animationFps.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
		        public void onItemSelected(AdapterView<?> animation, View view, int position, long id)
		        {
		        	TaggedString item = (TaggedString)animationFps.getItemAtPosition(position);
		    		SharedPreferences.Editor editor = prefs.edit();
		    		editor.putInt(Parameters.rate, item.getId());
		    		editor.apply();
	    			animationTime.setEnabled(0 != item.getId());
		        }
		        public void onNothingSelected(AdapterView<?> arg0){}
		    });
		}

		//Animation Time spinner
		{
			final int curTime = Parameters.getInt(prefs, Parameters.animationTime, Parameters.animationTimeDefault, 0, 1000);
	        ArrayAdapter<TaggedString> adapter =
        			new ArrayAdapter<TaggedString>(this, android.R.layout.simple_spinner_item);
	        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

	        int pos = 0;

			for (int time : res.getIntArray(R.array.time_values))
			{
				//Ugly workaround for 'special'
				String text;

				if (0 == time)
				{
					text = res.getString(R.string.infinitely);
				}
				else
				{
					text = res.getQuantityString(R.plurals.time, time, time);
				}

				if (curTime == time)
					pos = adapter.getCount();

				TaggedString item = new TaggedString(time, text);
				adapter.add(item);
			}

			animationTime.setAdapter(adapter);
			animationTime.setSelection(pos);

			animationTime.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
		        public void onItemSelected(AdapterView<?> animation, View view, int position, long id)
		        {
		        	TaggedString item = (TaggedString)animation.getItemAtPosition(position);
		    		SharedPreferences.Editor editor = prefs.edit();
		    		editor.putInt(Parameters.animationTime, item.getId());
		    		editor.apply();
		        }
		        public void onNothingSelected(AdapterView<?> arg0){}
		    });
		}

		//About message
		{
			TextView about = (TextView)findViewById(R.id.textAbout);
			about.setText(Html.fromHtml(res.getString(R.string.about_html)));
			about.setMovementMethod(LinkMovementMethod.getInstance());
		}

		//Generate button
		{
			Button generate = (Button) findViewById(R.id.btnGenerate);
			generate.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					onGenerate();
				}
			});
		}
    }

	protected void onGenerate()
	{
		Intent intent = new Intent(getApplicationContext(), NightGrassActivity.class);
		startActivity(intent);
	}
}
