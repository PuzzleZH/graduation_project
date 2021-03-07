# common.py
class common():
    _current_time=0
    def __init__(self,current_time=0):
        _current_time=current_time
    def step(self):
        _current_time+=1
    def __str__(self):
        return str(_current_time)
