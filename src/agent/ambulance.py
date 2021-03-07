# %% port something
import collections
import random
import bisect
import sys

# %% class ambulance


class Ambulance():

    def __init__(self, id, position_x, position_y, hospital=None, patient=None, incident=None):
        self._patient = [] # 曾经搭载过的病人
        self._hospital = [] # 曾经的医院目标序列
        self._incident = [] # 曾经关注过的事故序列
        self._stage = 0  # 0 是空闲状态,1 是前往事故发生地状态,2 是搭载完成病人前往医院的状态
        self._position = [position_x, position_y]  # 地图上的坐标
        self._id = id    # 救护车id
        self._hospital.append(hospital)  # 初始化医院
        self._patient.append(patient)  # 初始化搭载病人
        self._incident.append(incident)  # 初始化前往事故发生地

    def __getitem__(self, position):
        return True

    def __str__(self):
        return '救护车ID:  '+str(self._id)+'  当前救护车位置:  '+str(self._position)+'  救护车目的地:  '+str(self._hospital.my_hospital_info.position)

    def pick_patient(self, patient):  # 搭载病人
        self._patient.append(patient)

    def set_hospital(self, hospital):  # 确定医院
        self._hospital.append(hospital)

    def set_incident(self, incident):  # 去哪个事故发生地
        self._incident.append(incident)

    def step(self):  # 用于地图上移动，加上搭载病人 和 卸载病人
        # todo: 和病人类和医院类还需要补充一定内容
        def case0():
            pass
        def case1():
            if(self._position[0] > self._incident[-1]._incident_info.position[0]):
                self._position[0] = self._position[0]-1
            if(self._position[0] < self._incident[-1]._incident_info.position[0]):
                self._position[0] = self._position[0]+1

            if(self._position[1] > self._incident[-1]._incident_info.position[1]):
                self._position[1] = self._position[1]-1
            if(self._position[1] < self._incident[-1]._incident_info.position[1]):
                self._position[1] = self._position[1]+1

            if(self._position[0] == self._incident[-1]._incident_info.position[0]):
                if(self._position[1] == self._incident[-1]._incident_info.position[1]):
                    print("到达事故地点")

        def case2():
            if(self._position[0] > self._hospital[-1].my_hospital_info.position[0]):
                self._position[0] = self._position[0]-1
            if(self._position[0] < self._hospital[-1].my_hospital_info.position[0]):
                self._position[0] = self._position[0]+1

            if(self._position[1] > self._hospital[-1].my_hospital_info.position[1]):
                self._position[1] = self._position[1]-1
            if(self._position[1] < self._hospital[-1].my_hospital_info.position[1]):
                self._position[1] = self._position[1]+1

            if(self._position[0] == self._hospital[-1].my_hospital_info.position[0]):
                if(self._position[1] == self._hospital[-1].my_hospital_info.position[1]):
                    print("到达医院")
        switch = {
            '0': case0,
            '1': case1,
            '2': case2,
        }
        switch.get(str(self._stage))()

# %%
