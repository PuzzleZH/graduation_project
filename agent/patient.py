# patient.py
#%% import something
import collections
from random import randint
#%% class
class patient():
    _position=[] # 位置记录 只能是事故发生地,救护车,医院类型
    _critical_time=[] # 改变地点阶段的关键时间记录
    _RPM=[]
    _stages=[4]# 4 表示无任何救治 3 表示在救护车上 2 表示在ED 1 表示在ICU 0 表示在GW
    def __init__(self,position,time,RPM,id):
        self.record(position,time,RPM) # 初始情况
        self._id=id  # 病人id
        
    def record(self,position,time,RPM):
        self._position.append(position)
        self._critical_time.append(time)
        self._RPM.append(RPM)

    def __str__(self):
        return '病人id:'+str(self._id)+'  位置:'+str(self._position[-1])+'  RPM:'+str(self._RPM[-1])

    def log(self):
        return str(self._position)+str(self._critical_time)+str(self._RPM)

    def step(self): # 根据当前病人状况改变RPM值
        # print(self._stages[-1])
        if(self._RPM[-1]>8):
            new_RPM=self._RPM[-1]-0.5*self._stages[-1]
        else :
            print("okk")
            if(self._RPM[-1]>4):
                new_RPM=self._RPM[-1]-randint(1,2)/2*self._stages[-1]
            else :
                new_RPM=self._stages[-1]-randint(1,2)*self._stages[-1]
        self._RPM.append(new_RPM)

#%% test part
patient_1=patient(id=3179801017,position=(1,2),time=19,RPM=12)
patient_1.record((2,3),1,10)
print(patient_1)
# %%
patient_1.log()
# %%
patient_1.step()
# %%
patient_1.log()

# %%
