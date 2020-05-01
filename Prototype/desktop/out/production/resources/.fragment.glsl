#ifdef GL_ES
#define LOWP lowp
    precision mediump float;
#else
    #define LOWP
#endif

varying LOWP vec4 v_color;
varying vec2 v_texCoords;
varying vec2 v_pos;


uniform sampler2D u_texture;
uniform vec2 mousePos;
uniform float time;
uniform float deltax;
uniform float deltay;
uniform float u_rippleDistance;
uniform float u_rippleRange;


float waveHeight(vec2 p) {
    float ampFactor = 2.0;
    float distFactor = 5.0;
    //float dist = length(p);
    float dist = length(p);
    float delta = abs(u_rippleDistance - dist);
    if (delta <= u_rippleRange) {
        return cos((u_rippleDistance - dist) * distFactor) * (u_rippleRange - delta) * ampFactor;
    }
    else {
        return 0.0;
    }
}


void main()
{
    //vec2 p = v_texCoords - vec2(0.5, 0.5);
    vec2 p = v_texCoords - mousePos;
    vec2 normal = normalize(p);

    // offset texcoord along dist direction
    vec2 v_texCoord2 = v_texCoords + normal * waveHeight(p);

    gl_FragColor = texture2D(u_texture, v_texCoord2) * v_color;



}