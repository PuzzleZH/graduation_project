# hospital.py
#%% port something
import collections

#%%
class hospital():
    hospital_info=collections.namedtuple('hospital_info','id position type bed_total_number bed_remain_number')
    def __init__(self,id,x,y,bed_t,bed_re,type_):
        self._hospital=[self.hospital_info(id=id,position=(x,y),type=type_,bed_total_number=bed_t,bed_remain_number=bed_re)]