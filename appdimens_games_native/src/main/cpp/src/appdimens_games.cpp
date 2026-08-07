#include "appdimens_games.h"
#include <algorithm>
#include <cmath>
#include <cstring>
namespace {
bool finite_positive(float v) { return std::isfinite(v) && v > 0.f; }
bool valid(const adg_screen& s) { return finite_positive(s.width_dp) && finite_positive(s.height_dp) && finite_positive(s.density); }
bool valid(const adg_config& c) { return finite_positive(c.design_width_dp) && finite_positive(c.design_height_dp) && c.sensitivity >= 0 && c.exponent > 0 && c.transition_dp > 0 && c.min_value >= 0 && c.max_value >= c.min_value && c.max_viewport_dp > c.min_viewport_dp; }
float clamp(float v,float lo,float hi){return std::max(lo,std::min(hi,v));}
float aspect(float actual,float design){return clamp(1.f+(actual/design-1.f)*.08f,.85f,1.15f);}
}
extern "C" uint32_t adg_abi_version(void){return ADG_ABI_VERSION;}
extern "C" adg_config adg_default_config(void){return {300,533,.40f,.75f,480,0,3.402823466e+38F,320,768};}
extern "C" adg_status adg_scale(float value,adg_strategy strategy,const adg_screen* s,const adg_config* c,float* out){
 if(!s||!c||!out)return ADG_NULL_ARGUMENT;
 if(!std::isfinite(value)||value<0||!valid(*s)||!valid(*c)||(int)strategy<0||(int)strategy>12)return ADG_INVALID_ARGUMENT;
 float w=std::min(s->width_dp,s->height_dp),h=std::max(s->width_dp,s->height_dp),wr=w/c->design_width_dp,hr=h/c->design_height_dp;
 float ar=aspect(h/w,c->design_height_dp/c->design_width_dp),r=value;
 switch(strategy){case ADG_NONE:break;case ADG_DEFAULT:r=value*(1+(w-c->design_width_dp)/300*.10f)*ar;break;case ADG_PERCENTAGE:r=value*wr;break;
 case ADG_BALANCED:r=value*(w<=c->transition_dp?wr:(c->transition_dp/c->design_width_dp)*(1+c->sensitivity*std::log(w/c->transition_dp)))*ar;break;
 case ADG_LOGARITHMIC:r=value*(1+c->sensitivity*std::log(w/c->design_width_dp))*ar;break;case ADG_POWER:r=value*std::pow(wr,c->exponent)*ar;break;
 case ADG_FLUID:case ADG_AUTOSIZE:{if(c->max_value<3.402823466e+38F){float t=clamp((w-c->min_viewport_dp)/(c->max_viewport_dp-c->min_viewport_dp),0,1);r=c->min_value+(c->max_value-c->min_value)*t;}break;}
 case ADG_INTERPOLATED:r=value*(1+(wr-1)*.5f)*ar;break;case ADG_DIAGONAL:r=value*std::hypot(s->width_dp,s->height_dp)/std::hypot(c->design_width_dp,c->design_height_dp);break;
 case ADG_PERIMETER:r=value*(s->width_dp+s->height_dp)/(c->design_width_dp+c->design_height_dp);break;case ADG_FIT:r=value*std::min(wr,hr);break;case ADG_FILL:r=value*std::max(wr,hr);break;}
 *out=clamp(std::max(0.f,r),c->min_value,c->max_value);return ADG_OK;
}
extern "C" adg_status adg_scale_batch(const float* in,size_t is,float* out,size_t os,size_t n,adg_strategy st,const adg_screen* s,const adg_config* c){
 if(!in||!out||!s||!c)return ADG_NULL_ARGUMENT;
 if((is&&is<sizeof(float))||(os&&os<sizeof(float)))return ADG_INVALID_ARGUMENT;
 is=is?is:sizeof(float);os=os?os:sizeof(float);
 if((uintptr_t)out>(uintptr_t)in&&(uintptr_t)out<(uintptr_t)in+n*is){for(size_t i=n;i-->0;){float v,r;std::memcpy(&v,(const char*)in+i*is,4);adg_status x=adg_scale(v,st,s,c,&r);if(x)return x;std::memcpy((char*)out+i*os,&r,4);}}
 else {for(size_t i=0;i<n;i++){float v,r;std::memcpy(&v,(const char*)in+i*is,4);adg_status x=adg_scale(v,st,s,c,&r);if(x)return x;std::memcpy((char*)out+i*os,&r,4);}}
 return ADG_OK;
}
extern "C" adg_status adg_calculate_viewport(float dw,float dh,const adg_screen* s,int mode,adg_viewport* o){if(!s||!o)return ADG_NULL_ARGUMENT;if(!finite_positive(dw)||!finite_positive(dh)||!valid(*s)||mode<0||mode>2)return ADG_INVALID_ARGUMENT;
 float uw=std::max(0.f,s->width_dp-s->safe_area.left-s->safe_area.right),uh=std::max(0.f,s->height_dp-s->safe_area.top-s->safe_area.bottom),sx=uw/dw,sy=uh/dh;if(mode<2)sx=sy=mode==0?std::min(sx,sy):std::max(sx,sy);o->width=dw*sx;o->height=dh*sy;o->scale_x=sx;o->scale_y=sy;o->offset_x=s->safe_area.left+(uw-o->width)*.5f;o->offset_y=s->safe_area.top+(uh-o->height)*.5f;return ADG_OK;}
