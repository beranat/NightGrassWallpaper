/*
 * Parameters.java - changeble parameters for wallpaper, and its setter/getter functions
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

import junit.framework.Assert;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.util.Log;

final class Parameters {
	static final public String TAG = Parameters.class.getName();
	
	static final public String  sharedPreferences		= "nightgrass_wallpaper";

	static final public String 	rate 					= "refresh_rate";
	static final public int 	rateDefault				= 50;

	static final public String 	animationTime			= "animation_time";
	static final public int 	animationTimeDefault	= 5;

	static final public String 	moonFadding 			= "moon_fadding";
	static final public float	moonFaddingDefault		= 0.07f;

	static final public String	moonDarkSideColor    		= "moon_darkside_color";
	static final public int		moonDarkSideColorDefault 	= Color.argb(255, 76, 76, 76);

	static final public String	previewOffset			= "preview_offset";
	static final public float 	previewOffsetDefault	= 0.75f;

	static final public String	moonMinPhase			= "moon_phase_min";
	static final public float 	moonMinPhaseDefault		= -0.8f;

	static final public String	moonMaxPhase			= "moon_phase_max";
	static final public float 	moonMaxPhaseDefault		= 0.8f;


	static public float getFloat(SharedPreferences prefs, String name, float def, float min, float max)
	{
		if (BuildConfig.DEBUG)
			Assert.assertTrue(min <= def && def <= max);

		try
		{
			final float value = prefs.getFloat(name, def);
			if (min <= value && value <= max)
				return value;
			Log.e(TAG, String.format("Invalid value %d for %s, reset to default value", value, name));
		}
		catch (Exception e)
		{
			Log.e(TAG, String.format("Exception for %s, %s", name, e.toString()));
		}

		SharedPreferences.Editor editor = prefs.edit();
		editor.putFloat(name, def);
		editor.apply();
		return def;
	}

	static public int getInt(SharedPreferences prefs, String name, int def, int min, int max)
	{
		if (BuildConfig.DEBUG)
			Assert.assertTrue(min <= def && def <= max);

		try
		{
			final int value = prefs.getInt(name, def);
			if (min <= value && value <= max)
				return value;
			Log.e(TAG, String.format("Invalid value %d for %s, reset to default value", value, name));
		}
		catch (Exception e)
		{
			Log.e(TAG, String.format("Exception for %s, %s", name, e.toString()));
		}

		SharedPreferences.Editor editor = prefs.edit();
		editor.putInt(name, def);
		editor.apply();
		return def;
	}

}
