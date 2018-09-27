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

import android.os.SystemClock;
import android.renderscript.Sampler;
import static android.renderscript.ProgramStore.DepthFunc.*;
import static android.renderscript.ProgramStore.BlendSrcFunc;
import static android.renderscript.ProgramStore.BlendDstFunc;
import android.renderscript.*;
import static android.renderscript.Element.*;
import android.renderscript.Mesh.Primitive;
import android.util.Log;
import android.view.MotionEvent;
import static android.renderscript.Sampler.Value.*;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;

import org.madrat.wallpapers.nightgrass.ScriptC_grass;
import org.madrat.wallpapers.nightgrass.ScriptField_Blade;
import org.madrat.wallpapers.nightgrass.ScriptField_Vertex;
import org.madrat.wallpapers.nightgrass.R;
import org.madrat.wallpapers.RenderScriptScene;

import java.util.Random;

class GrassRenderScene extends RenderScriptScene
				implements SharedPreferences.OnSharedPreferenceChangeListener
{
	private static final long MILLISECONDS = 1000;
    private static final float TESSELATION = 0.5f;
    private static final int BLADES_COUNT = 200;
    private static final Random rng = new Random();

    private ScriptField_Blade mBlades;
    private ScriptField_Vertex mVertexBuffer;
    private ProgramVertexFixedFunction.Constants mPvOrthoAlloc;

    private Allocation mBladesIndicies;
    private Mesh mBladesMesh;

    private ScriptC_grass	scriptGrass;    
    private ScriptC_moon	scriptMoon;

    private int mVerticies;
    private int mIndicies;
    private int[] mBladeSizes;

    private SharedPreferences prefs;
    
    private float moonMinPhase = Parameters.moonMinPhaseDefault;
    private float moonMaxPhase = Parameters.moonMaxPhaseDefault;
    private int   animationTime= Parameters.animationTimeDefault;
    
    private static float random(float max)
    {
    	return rng.nextFloat() * max;
    }
    
    private static int random(int min, int max)
    {
    	if (min >= max)
    		return min;
    	return (int)(random(max-min)+min);
    }

    GrassRenderScene(Context context, int width, int height)
    {
        super(width, height);
        prefs = context.getSharedPreferences(Parameters.sharedPreferences, Context.MODE_PRIVATE);
        prefs.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void start()  {
        super.start();
        updateAnimation();
    }

    @Override
    public void stop()   {
    	super.stop();
    }

    @Override
    public void resize(int width, int height)
    {
        super.resize(width, height);

        scriptGrass.set_gWidth(width);
        scriptGrass.set_gHeight(height);
        scriptGrass.invoke_updateBlades();
        
        Matrix4f proj = new Matrix4f();
        proj.loadOrthoWindow(width, height);
        mPvOrthoAlloc.setProjection(proj);
        
        updateAnimation();
    }

    @Override
    protected ScriptC createScript()
    {
    	// Moon 
    	scriptMoon = new ScriptC_moon(mRS, mResources, R.raw.moon);
    	scriptGrass = new ScriptC_grass(mRS, mResources, R.raw.grass);    	        
   	
    	// load textures
    	Bitmap moon = BitmapFactory.decodeResource(mResources, R.drawable.moon);
    	
    	Log.i(Parameters.TAG, String.format("Moon's geom %dx%d %ddpi", moon.getWidth(), moon.getHeight(), moon.getDensity()));
    	
    	scriptMoon.set_gWidth(moon.getWidth());
    	scriptMoon.set_gHeight(moon.getHeight()); 
    	
    	scriptMoon.set_gTMoon(
    			Allocation.createFromBitmap(mRS, moon, Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT));
    	
    	scriptGrass.set_gTMoon(
    			Allocation.createFromBitmap(mRS, moon, Allocation.MipmapControl.MIPMAP_NONE, 
    					Allocation.USAGE_SCRIPT + Allocation.USAGE_GRAPHICS_TEXTURE));
    	
        scriptGrass.set_gTNight(
        		Allocation.createFromBitmapResource(mRS, mResources, R.drawable.night));

        scriptGrass.set_gTAa(generateTextureAlpha());
    	
        createProgramVertex();
        createProgramFragmentStore();        
       
        createProgramFragment();
        createBlades();

        scriptGrass.set_gBladesCount(BLADES_COUNT);
        scriptGrass.set_gIndexCount(mIndicies);
        scriptGrass.set_gWidth(mWidth);
        scriptGrass.set_gHeight(mHeight);       
        scriptGrass.set_gBladesMesh(mBladesMesh);

        scriptGrass.bind_Blades(mBlades);
        scriptGrass.bind_Verticies(mVertexBuffer);
        
        loadSettings();
        
        return scriptGrass;
    }

    private void updateMoonPhase(float offset)
    {
    	final float phase = moonMinPhase + offset*(moonMaxPhase-moonMinPhase);    	
    	scriptMoon.set_gPhase(phase);    	
    	scriptMoon.forEach_root(scriptMoon.get_gTMoon(), scriptGrass.get_gTMoon());
    	scriptGrass.get_gTMoon().syncAll(Allocation.USAGE_SCRIPT);
    }
    
    @Override
    public void setOffset(float xOffset, float yOffset, int xPixels, int yPixels)
    {
    	updateMoonPhase(xOffset);
    	updateAnimation();
    	scriptGrass.set_gXOffset(xOffset);    	
    }
    
    @Override
    public void 	onTouchEvent(MotionEvent event)
    {
    	updateAnimation();
    }

    private void createBlades() {
        mVerticies = 0;
        mIndicies = 0;

        mBlades = new ScriptField_Blade(mRS, BLADES_COUNT);

        mBladeSizes = new int[BLADES_COUNT];
        for (int i = 0; i < BLADES_COUNT; i++) {
            ScriptField_Blade.Item item = new ScriptField_Blade.Item();
            createBlade(item);
            mBlades.set(item, i, false);

            mIndicies += item.size * 2 * 3;
            mVerticies += item.size + 2;
            mBladeSizes[i] = item.size;
        }
        mBlades.copyAll();

        createMesh();
    }

    private void createMesh() {
        mVertexBuffer = new ScriptField_Vertex(mRS, mVerticies * 2);

        final Mesh.AllocationBuilder meshBuilder = new Mesh.AllocationBuilder(mRS);
        meshBuilder.addVertexAllocation(mVertexBuffer.getAllocation());

        mBladesIndicies = Allocation.createSized(mRS, Element.U16(mRS), mIndicies);
        meshBuilder.addIndexSetAllocation(mBladesIndicies, Primitive.TRIANGLE);

        mBladesMesh = meshBuilder.create();

        short[] idx = new short[mIndicies];
        int idxIdx = 0;
        int vtxIdx = 0;
        for (int i = 0; i < mBladeSizes.length; i++) {
            for (int ct = 0; ct < mBladeSizes[i]; ct ++) {
                idx[idxIdx + 0] = (short)(vtxIdx + 0);
                idx[idxIdx + 1] = (short)(vtxIdx + 1);
                idx[idxIdx + 2] = (short)(vtxIdx + 2);
                idx[idxIdx + 3] = (short)(vtxIdx + 1);
                idx[idxIdx + 4] = (short)(vtxIdx + 3);
                idx[idxIdx + 5] = (short)(vtxIdx + 2);
                idxIdx += 6;
                vtxIdx += 2;
            }
            vtxIdx += 2;
        }

        mBladesIndicies.copyFrom(idx);
    }

    private void createBlade(ScriptField_Blade.Item blades) {
        final float size = random(4.0f) + 4.0f;
        final int xpos = random(-mWidth, mWidth);

        //noinspection PointlessArithmeticExpression
        blades.angle = 0.0f;
        blades.size = (int)(size / TESSELATION);
        blades.xPos = xpos;
        blades.yPos = mHeight;
        blades.offset = random(0.2f) - 0.1f;
        blades.scale = 4.0f / (size / TESSELATION) + (random(0.6f) + 0.2f) * TESSELATION;
        blades.lengthX = (random(4.5f) + 3.0f) * TESSELATION * size;
        blades.lengthY = (random(5.5f) + 2.0f) * TESSELATION * size;
        blades.hardness = (random(1.0f) + 0.2f) * TESSELATION;
        blades.h = random(0.02f) + 0.2f;
        blades.s = random(0.22f) + 0.78f;
        blades.b = random(0.65f) + 0.35f;
        blades.turbulencex = xpos * 0.006f;
    }

    private Allocation generateTextureAlpha() {
        final Type.Builder builder = new Type.Builder(mRS, A_8(mRS));
        builder.setX(4);
        builder.setY(1);
        builder.setMipmaps(true);

        final Allocation allocation = Allocation.createTyped(mRS, builder.create(),
                                                       Allocation.USAGE_GRAPHICS_TEXTURE);
        byte[] mip0 = new byte[] {0, -1, -1, 0};
        byte[] mip1 = new byte[] {64, 64};
        byte[] mip2 = new byte[] {0};

        AllocationAdapter a = AllocationAdapter.create2D(mRS, allocation);
        a.setLOD(0);
        a.copyFrom(mip0);
        a.setLOD(1);
        a.copyFrom(mip1);
        a.setLOD(2);
        a.copyFrom(mip2);

        return allocation;
    }

    private void createProgramFragment() 
    {
        Sampler.Builder samplerBuilder = new Sampler.Builder(mRS);
        {
        samplerBuilder.setMinification(LINEAR_MIP_LINEAR);
        samplerBuilder.setMagnification(LINEAR);
        samplerBuilder.setWrapS(WRAP);
        samplerBuilder.setWrapT(WRAP);

        ProgramFragmentFixedFunction.Builder builder = new ProgramFragmentFixedFunction.Builder(mRS);
        builder.setTexture(ProgramFragmentFixedFunction.Builder.EnvMode.REPLACE,
                           ProgramFragmentFixedFunction.Builder.Format.ALPHA, 0);
        
        builder.setVaryingColor(true);
        ProgramFragment pf = builder.create();        
        pf.bindSampler(samplerBuilder.create(), 0);
        
        scriptGrass.set_gPFGrass(pf);
        }
        
        {
            samplerBuilder.setMinification(LINEAR);
            samplerBuilder.setMagnification(LINEAR);
            samplerBuilder.setWrapS(CLAMP);
            samplerBuilder.setWrapT(CLAMP);
            
            ProgramFragmentFixedFunction.Builder builder = new ProgramFragmentFixedFunction.Builder(mRS);
            builder.setTexture(ProgramFragmentFixedFunction.Builder.EnvMode.DECAL,
                               ProgramFragmentFixedFunction.Builder.Format.RGBA, 0);
            ProgramFragment pf = builder.create();        
            pf.bindSampler(samplerBuilder.create(), 0);        
            scriptGrass.set_gPFMoon(pf);        	
        }

        {
       	ProgramFragmentFixedFunction.Builder builder = new ProgramFragmentFixedFunction.Builder(mRS);
        builder.setTexture(ProgramFragmentFixedFunction.Builder.EnvMode.REPLACE,
                           ProgramFragmentFixedFunction.Builder.Format.RGB, 0);
        samplerBuilder.setMinification(NEAREST);
        samplerBuilder.setMagnification(NEAREST);

        ProgramFragment pf = builder.create();        
        pf.bindSampler(samplerBuilder.create(), 0);
        scriptGrass.set_gPFBackground(pf);
        }
    }

    private void createProgramFragmentStore() {
        ProgramStore.Builder builder = new ProgramStore.Builder(mRS);
        builder.setDepthFunc(ALWAYS);
        builder.setBlendFunc(BlendSrcFunc.SRC_ALPHA, BlendDstFunc.ONE_MINUS_SRC_ALPHA);
        builder.setDitherEnabled(false);
        builder.setDepthMaskEnabled(false);
        scriptGrass.set_gPS(builder.create());
    }

    private void createProgramVertex() {
        mPvOrthoAlloc = new ProgramVertexFixedFunction.Constants(mRS);
        Matrix4f proj = new Matrix4f();
        proj.loadOrthoWindow(mWidth, mHeight);
        mPvOrthoAlloc.setProjection(proj);

        ProgramVertexFixedFunction.Builder pvb = new ProgramVertexFixedFunction.Builder(mRS);
        ProgramVertexFixedFunction pv = pvb.create();
        pv.bindConstants(mPvOrthoAlloc);
        scriptGrass.set_gPVBackground(pv);
    }

	@Override
	public void onSharedPreferenceChanged(SharedPreferences prefs, String arg)
	{
		loadSettings();
		
		updateAnimation();
		updateMoonPhase(scriptGrass.get_gXOffset());
	}
	
	private void updateAnimation()
	{
		if (0 != animationTime)
			scriptGrass.set_gAnimationStopTime(animationTime*MILLISECONDS + SystemClock.uptimeMillis());
		else
			scriptGrass.set_gAnimationStopTime(0);
	}
	
	private void loadSettings()
	{
	    moonMinPhase = Parameters.getFloat(prefs, Parameters.moonMinPhase,Parameters.moonMinPhaseDefault, -1.0f, 1.0f);
	    moonMaxPhase = Parameters.getFloat(prefs, Parameters.moonMaxPhase,Parameters.moonMaxPhaseDefault, -1.0f, 1.0f);
		
		final float fadding = Parameters.getFloat(prefs, Parameters.moonFadding,Parameters.moonFaddingDefault, 0.0f, 1.0f);
		scriptMoon.set_fadding(fadding);

		final int color = Parameters.getInt(prefs, Parameters.moonDarkSideColor,Parameters.moonDarkSideColorDefault, Integer.MIN_VALUE, Integer.MAX_VALUE);
		Float4 darkSide = new Float4(Color.red(color) / 255.f, Color.green(color) / 255.f, Color.blue(color) / 255.f, Color.alpha(color) / 255.f);	
		scriptMoon.set_dark(darkSide);

		final int rate = Parameters.getInt(prefs,Parameters.rate, Parameters.rateDefault, 0, 1000); 
		scriptGrass.set_gAnimationRate(rate);
		
		animationTime = Parameters.getInt(prefs,Parameters.animationTime, Parameters.animationTimeDefault, 0, 3600); // 0 ... 1 hour?
		
		if (isPreview())
		{
			final float offset = Parameters.getFloat(prefs, Parameters.previewOffset, Parameters.previewOffsetDefault, 0.0f, 1.0f);
			setOffset(offset, 0.0f, 0, 0);
		}
	}
}