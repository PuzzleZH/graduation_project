# p2.2.py
symbols = '$%$#@'
beyond_ascii=[ord(s) for s in symbols if ord(s)>20]
print(beyond_ascii)

colors =['black', 'white']
sizes = ['S','M','L']
tshifts =[(colors,size) for colors in colors 
                        for size in sizes]
print(tshifts)

for tshifts in ('%s %s'%(c,s) for c in colors for s in sizes):
    print(tshifts)

#%%
import collections
Cit1y=collections.namedtuple('City2','x y')
#%%
aa=Cit1y(10,11)
#%%
print(aa)
#%%
bb=(1,2)
bb.count(1)
#%% 
print(bb)
#%%
from collections import deque
collections