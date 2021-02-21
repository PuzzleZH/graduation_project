#%% port something
import collections
import random
import bisect
import sys

#%% class ambulance
class ambulance():
    ambulance_positions = collections.namedtuple(
        'ambulance_positions', ['x', 'y'])
    ambulance_info = collections.namedtuple(
        'ambulance_info', 'id manned destination')
    manned = 0

    def __init__(self, number,hospital):
        random_inter = []
        for i in range(2):
            random_inter.append(str(random.randint(1, 50)))
        (x, y) = random_inter[0:2]
        self._ambulance = [self.ambulance_positions(
            x, y), self.ambulance_info(id=number, manned=0, destination=hospital)]

    def __len__(self):
        return len(self._ambulance)

    def __getitem__(self, position):
        return self._ambulance[0]+self._ambulance[1]


#%% test part
import hospital
hospital_1=hospital.hospital(10,25,26,100,20,'烧伤')
ambulance_1 = ambulance(10,hospital_1)

#%%
print(ambulance_1[1])
print(hospital_1)