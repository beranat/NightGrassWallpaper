/* moon.rs - generate shaded moon texture
Copyright (C) 2015, madRat.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <http://www.gnu.org/licenses/>.  */

#pragma version(1)
#pragma rs java_package_name(org.madrat.wallpapers.nightgrass)
#include "rs_graphics.rsh"

// gPhase defines moon's phase, where 
// -1.0   (
//  0.0   O
// +1.0   )

rs_allocation gTMoon;
int gWidth, gHeight;

float gPhase = 0.0f;

float  fadding = 0.05; 						// fadding into dark side (no strict line)
float4 dark   = {0.3f, 0.3f, 0.31f, 1.0f};	// color of dark side of moon

//------------------------------------------------------------------------------
// Moon shading splited into 'prec' and 'draft'
// 'prec' -	uses for crescent, when sun is approximated by a larger circle
// 'draft'-	when the shadow of the sun tends to infinite size circle
// EPS_ECLIPSE is a limit between prec and draft aproximations

#define EPS_ECLIPSE 1.0E-36

// returns x coordinate of shadow for phase and y
static float eclipse(float phase, float y)
{
	const float x0 = 2.0*phase + ((phase<0)?1.0:-1.0);  // x coord for shadow at y == 0
	const float absx0 = fabs(x0); 

	if (absx0 > EPS_ECLIPSE)
	{
		// PREC approximation
		const float d = 0.5*(1/absx0-absx0);
		return (sqrt(d*d+1-y*y)-d)*sign(x0);
	}

	// DRAFT approximation
	return x0*sqrt(1-y*y);
}

//------------------------------------------------------------------------------
// Generate shadded moon
void root(const uchar4 *v_in, uchar4 *v_out,  uint32_t x, uint32_t y)
{
	float phase = clamp(gPhase, -1.0f, 1.0f);

	//current relative position in [-1...1]	
	float cy = 2.0f*y/gHeight-1.0f;
	float cx = 2.0f*x/gWidth -1.0f;
	
	const float ex = eclipse(phase, cy);

	float lx = -1.0f; 	// left border 	line
	float rx =  1.0f;	// right 		line
	float mlx =-1.0f;	// miss area border line (left)
	float mrx = 1.0f;	// miss area border line (right)
	
	if (phase < 0)
	{
		rx = ex;
		mlx = ex + fadding;
	}
	else
	{
		lx = ex;
		mrx = ex - fadding;
	}

	// 01. In area
	// 02. In fadding
	// 03. Not int area

	if (lx <= cx && cx <= rx)
	{
		*v_out = *v_in; 
	}
	else
	{
		float4 pixel = rsUnpackColor8888(*v_in);
		const float4 darkside = pixel*dark;

		if (mlx <= cx && cx <= mrx) 
		{
			pixel = darkside;
		}
		else
		{
			// in fadding area 
			const float f = fabs(cx - ex)  / fadding;
			pixel = mix(pixel, darkside, f);
		}
		*v_out = rsPackColorTo8888(pixel);
	}
}
