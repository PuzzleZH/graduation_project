# incident.py
# %%
import collections
from random import randint
# from patient import Patient
import random
import math
# %%
class Incident():

    def __init__(self, id, position, total_num=100, scale='red'):  # 确定初始化的信息,包括总病人数和严重程度
        self._dead_list = []
        self._current_patient_num = 0
        # _triage = []  # 已经分拣出来
        self._on_incident = []  # 在现场的患者
        self._on_the_way = []  # 正在运输的患者
        self._on_the_hospital = []  # 正在医院的
        self._discharge = []  # 已经出院的
        self._ambulance_arrive = []  # 到达现场的救护车
        self._total_patient = 0
        incident_info = collections.namedtuple(
            'incident_info', 'id position')  # 发生位置 和发生规模(潜在人数)
        self._incident_info = incident_info(
            id=id, position=position)
        self._scale = scale
        self._total_num=total_num

    def step(self):
        # 根据incident的规模确定产生患者的严重程度
        # 均为正太分布
        # red RPM 的分布中位数偏低 4
        # yellow RPM的分布的中位数中等 8
        # green RPM的分布的中位数偏高 10
        rand = randint(0, 100)  # 由二分之一的几率产生患者
        if(_total_patient ==self._total_num):
            return "患者被全部找到"
        if(self._scale == 'red'):
            if(rand < 50):
                self._on_incident.append(
                    patient(position=self, time=0, RPM=math.floor(random.normalvariate(mu=4, sigma=1)), id=self._total_patient))
                self._total_patient = self._total_patient+1
        if(self._scale == 'green'):
            if(rand < 50):
                self._on_incident.append(
                    patient(position=self, time=0, RPM=math.floor(random.normalvariate(mu=4, sigma=1)), id=self._total_patient))
                self._total_patient = self._total_patient+1
        if(self._scale == 'yellow'):
            if(rand < 50):
                self._on_incident.append(
                    patient(position=self, time=0, RPM=math.floor(random.normalvariate(mu=4, sigma=1)), id=self._total_patient))
                self._total_patient = self._total_patient+1

    def __str__(self):
        return "类型：事件  id： "+str(self._incident_info.id)+" 位置： "+str(self._incident_info.position)+" 事件等级： "+str(self._scale)
    def __getitem__(self,position):
        print("事件id:%s"%(self._incident_info.id))
        return self._on_incident[position]

