#%% import something
import sys
sys.path.append('..')
from common.common import common
from agent.ambulance import Ambulance
from agent.patient import Patient
from agent.hospital import Hospital
from agent.incident import Incident
import parl
from model.Model import Model
# %% 事件开始
time=common(0)
incident_one=Incident(id=1,position=(30,21),total_num=100,scale='yellow')
hospital_one=Hospital(id=1,position=(30,40),bed_t=(20,30,40),type_='骨科')
patient_one=Patient(id=1,position=hospital_one,time=time,RPM=12)
# %%
print(patient_one)
# %%
patient_one.log()
model_=Model(10)
# %%
parl.algorithms.DQN(model=model_,act_dim=10,gamma=0.1,lr=0.1)
# %%

# %%
