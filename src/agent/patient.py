# patient.py
# %% import something
import collections
from random import randint
# %% class
class Patient():
    def __init__(self, position, time, RPM, id):
        self._position = []  # 位置记录 只能是事故发生地,救护车,医院类型
        self._critical_time = []  # 改变地点阶段的关键时间记录
        self._RPM = [] # 生命曲线的变化趋势
        dead_info = collections.namedtuple('dead_info', 'time position')
        self._stages = [4]  # 4 表示无任何救治 3 表示在救护车上 2 表示在ED 1 表示在ICU 0 表示在GW
        self.record(position, time, RPM)  # 初始情况
        self._id = id  # 病人id

    def record(self, position, time, RPM):
        self._position.append(position)
        self._critical_time.append(time)
        self._RPM.append(RPM)

    def __str__(self):
        return '病人id:'+str(self._id)+'\n'+'位置:'+'\n     '+str(self._position[-1])+'\n'+'RPM:'+str(self._RPM[-1])

    def log(self):
        return str(self._position)+str(self._critical_time)+str(self._RPM)


## todo: 依据概率改变患者的生命值，是step组成函数的重要部分
    def change(self):
        pass

##
    def step(self):  # 根据当前病人状况改变RPM值
        if(self._RPM[-1] > 8):
            new_RPM = self._RPM[-1]-0.5*self._stages[-1] #根据当前状态减少或者不变
        else:
            if(self._RPM[-1] > 4):
                new_RPM = self._RPM[-1]-randint(1, 2)/2*self._stages[-1]
            else:
                new_RPM = self._stages[-1]-randint(1, 2)*self._stages[-1]
        self._RPM.append(new_RPM)
        self.isDead()

    def isDead(self): # 判断是否死亡 RPM小于等于0 即死亡
        if(self._RPM[-1] <= 0):
            self._dead_info = dead_info(
                time=self._critical_time[-1], position=self._position[-1])
            return True
        else:
            return False


# %%

