# hospital.py
# %% port something
import collections
# from ambulance import Ambulance

# %%


class Hospital():
    def __init__(self, id, position, bed_t, type_): # bed_t(x,y,z) x ED总床数,y ICU总床数 ,z GW总床数
        self._bed_remain = 0
        self._patient_list = []
        self._ED_available_count = bed_t[0]  # ED 总床位
        self._wait_ED_patient_list = []
        self._in_ED_patient_list = []
        self._ICU_available_count = bed_t[1]  # ICU 总床位
        self._in_ICU_patient_list = []
        self._wait_ICU_patient_list = []
        self._GW_available_count = bed_t[2]  # GW 总床位
        self._wait_GW_patient_list = []
        self._in_GW_patient_list = []

        hospital_info = collections.namedtuple(
            'hospital_info', 'id position type bed_total_number')  # 确定医院的唯一标识,位置,类型,总床数
        self.my_hospital_info = hospital_info(
            id=id, position=position, type=type_, bed_total_number=bed_t[0]+bed_t[1]+bed_t[2])
        self.bed_remain = self.my_hospital_info.bed_total_number
        self._hospital = [self.my_hospital_info]

    def __str__(self):
        return str(self.my_hospital_info)+'  医院剩余床数:  '+str(self.bed_remain)

    def step(self):
        # ED ICU GW 排队
        # 患者从救护车上下来等等
        if(len(self._in_ED_patient_list) < self._ED_available_count):
            if(len(self._wait_ED_patient_list) > 0):
                self._in_ED_patient_list.append(self._wait_ED_patient_list[0])
                self._in_ED_patient_list[-1]._stages.append(2) # 修改患者的状态
                self._wait_ED_patient_list.pop() # 等待队列弹出
        if(len(self._in_ICU_patient_list) < self._ICU_available_count):
            if(len(self._wait_ICU_patient_list) > 0):
                self._in_ICU_patient_list.append(self._wait_ICU_patient_list[0])
                self._in_ICU_patient_list[-1]._stages.append(1)
                self._wait_ICU_patient_list.pop()
        if(len(self._in_GW_patient_list) < self._GW_available_count):
            if(len(self._wait_GW_patient_list) > 0):
                self._in_GW_patient_list.append(self._wait_GW_patient_list[0])
                self._in_ICU_patient_list[-1]._stages.append(0)
                self._wait_GW_patient_list.pop()
        for i in range(len(self._in_ED_patient_list)):
            if(self._in_ED_patient_list[i].step()==True): # 每位患者都进行PRM更新,这里面会检查死亡
                self._in_ED_patient_list.remove(i+1)
        for i in range(len(self._wait_ED_patient_list)):
            if(self._wait_ED_patient_list[i].step()==True): # 每位患者都进行PRM更新,这里面会检查死亡
                self._wait_ED_patient_list.remove(i+1)
        for i in range(len(self._in_ICU_patient_list)):
            if(self._in_ICU_patient_list[i].step()==True): # 每位患者都进行PRM更新,这里面会检查死亡
                self._in_ICU_patient_list.remove(i+1)
        for i in range(len(self._wait_ICU_patient_list)):
            if(self._wait_ICU_patient_list[i].step()==True): # 每位患者都进行PRM更新,这里面会检查死亡
                self._wait_ICU_patient_list.remove(i+1)
        for i in range(len(self._wait_GW_patient_list)):
            if(self._wait_GW_patient_list[i].step()==True): # 每位患者都进行PRM更新,这里面会检查死亡
                self._wait_GW_patient_list.remove(i+1)
        for i in range(len(self._in_GW_patient_list)):
            if(self._in_GW_patient_list[i].step()==True): # 每位患者都进行PRM更新,这里面会检查死亡，和治愈
                self._in_GW_patient_list.remove(i+1)
# %%
