# ambulance.py
import collections
import random
import bisect
import sys


class ambulance():
    ambulance_positions = collections.namedtuple(
        'ambulance_positions', ['x', 'y'])
    ambulance_info = collections.namedtuple(
        'ambulance_info', 'id manned destination')
    manned = 0

    def __init__(self, number):
        random_inter = []
        for i in range(2):
            random_inter.append(str(random.randint(1, 50)))
        (x, y) = random_inter[0:2]
        self._ambulance = [self.ambulance_positions(
            x, y), self.ambulance_info(id=number, manned=0, destination=0)]

    def __len__(self):
        return len(self._ambulance)

    def __getitem__(self, position):
        return self._ambulance[position]


ambulance_1 = ambulance(number=1)
ambulance_2 = ambulance(number=2)
for ambulance in ambulance_2:
    print(ambulance)
for ambulance in ambulance_1:
    print(ambulance)
