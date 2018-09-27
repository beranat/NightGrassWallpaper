/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.madrat.wallpapers.nightgrass;

import android.renderscript.RSSurfaceView;
import android.renderscript.RenderScriptGL;
import android.view.SurfaceHolder;
import android.view.View;
import android.app.Activity;

class GrassView extends RSSurfaceView 
{
	GrassRenderScene scene = null;
	
	Activity activity = null;
	
    public GrassView(Activity a) 
    {    	
        super(a);
        activity = a;
        activity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }
    
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) 
    {
        super.surfaceChanged(holder, format, w, h);
        if (scene == null)
        {
            RenderScriptGL.SurfaceConfig sc = new RenderScriptGL.SurfaceConfig();
            RenderScriptGL RS = createRenderScriptGL(sc);
            RS.setSurface(holder, w, h);
            
            scene = new GrassRenderScene(getContext(), w, h);
            scene.init(RS, getResources(), true);
            scene.start();
            scene.setOffset(0.5f, 0.0f, w/2, 0);
        }
    }    
       
    @Override
    protected void onDetachedFromWindow() 
    {
    	if (null != scene)
    	{
    		scene.stop();
    		scene = null;
    		destroyRenderScriptGL();
    	}

    	super.onDetachedFromWindow();
    }
}