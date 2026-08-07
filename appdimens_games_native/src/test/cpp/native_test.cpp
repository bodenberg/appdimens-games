#include "appdimens_games.h"
#include <cassert>
#include <cmath>
#include <cstdio>
int main(){adg_screen s{360,800,3,{12,24,12,16}};adg_config c=adg_default_config();assert(adg_abi_version()==ADG_ABI_VERSION);
 for(int st=0;st<=12;st++){float out=-1;assert(adg_scale(48,(adg_strategy)st,&s,&c,&out)==ADG_OK);assert(std::isfinite(out)&&out>=0);std::printf("%d,%.6f\n",st,out);}
 float fluid=0;assert(adg_scale(48,ADG_FLUID,&s,&c,&fluid)==ADG_OK&&fluid==48);
 float data[5]{1,2,3,4,5};assert(adg_scale_batch(data,0,data+1,0,4,ADG_FIT,&s,&c)==ADG_OK);assert(data[2]>data[1]);
 adg_viewport v{};assert(adg_calculate_viewport(1920,1080,&s,0,&v)==ADG_OK);assert(v.width<=s.width_dp&&v.height<=s.height_dp);
 assert(adg_scale(-1,ADG_NONE,&s,&c,data)==ADG_INVALID_ARGUMENT);assert(adg_scale(1,ADG_NONE,nullptr,&c,data)==ADG_NULL_ARGUMENT);return 0;}
