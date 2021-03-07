# Model.py
#%%
import parl
from parl import layers
import paddle.fluid as fluid
#%%
class Model(parl.Model):
    def __init__(self,num_actions):
        # define the hyperparameters of the CNN
        # filter size
        self.filter_size = 5
        # number of filters
        self.num_filters = [32, 64, 128, 256, 256]
        # stride size
        self.stride = 2
        # pool size
        self.poolsize = 2
        self.rnn_hidden_size = 512
        # drop out probability
        self.dropout_probability = [0.3, 0.2]
        self.act_dim = num_actions

    def value(self, obs):
        # first convolutional layer
        self.conv1 = fluid.layers.conv2d(obs,
                                        num_filters = self.num_filters[0],
                                        filter_size = self.filter_size,
                                        stride = self.stride,
                                        act='relu')
        self.pool1 = fluid.layers.pool2d(self.conv1,
                                         pool_size = self.poolsize,
                                         pool_type = "max",
                                         pool_stride = self.stride)
        # second convolutional layer
        self.conv2 = fluid.layers.conv2d_transpose(self.pool1,
                                        num_filters = self.num_filters[1],
                                        filter_size = self.filter_size,
                                        stride = self.stride,
                                        act='relu')
        self.pool2 = fluid.layers.pool2d(self.conv2,
                                         pool_size = self.poolsize,
                                         pool_type = "max",
                                         pool_stride = self.stride)
        # third convolutional layer
        self.conv3 = fluid.layers.conv2d(self.pool2,
                                            num_filters = self.num_filters[2],
                                            filter_size = self.filter_size,
                                            stride = self.stride,
                                            act='relu')
        self.pool3 = fluid.layers.pool2d(self.conv3,
                                         pool_size = self.poolsize,
                                         pool_type = "max",
                                         pool_stride = self.stride)
        self.conv4 = fluid.layers.conv2d(self.pool3,
                                        num_filters = self.num_filters[3],
                                        filter_size = self.filter_size,
                                        stride = self.stride,
                                        act='relu')
        self.pool4 = fluid.layers.pool2d(self.conv4,
                                         pool_size = self.poolsize,
                                         pool_type = "max",
                                         pool_stride = self.stride)
        # second convolutional layer
        self.conv5 = fluid.layers.conv2d_transpose(self.pool4,
                                        num_filters = self.num_filters[4],
                                        filter_size = self.filter_size,
                                        stride = self.stride,
                                        act='relu')
        self.pool5 = fluid.layers.pool2d(self.conv5,
                                         pool_size = self.poolsize,
                                         pool_type = "max",
                                         pool_stride = self.stride)

        # add dropout and reshape the input
        self.fc1 = fluid.layers.fc(self.pool5, size = self.rnn_hidden_size*2, act="relu")
        self.drop1 = fluid.layers.dropout(self.fc1, dropout_prob=self.dropout_probability[0])
        self.fc2 = fluid.layers.fc(self.drop1, size = self.rnn_hidden_size*4, act="relu")
        self.drop2 = fluid.layers.dropout(self.fc2, dropout_prob=self.dropout_probability[1])
        self.fc3 = fluid.layers.fc(self.drop2, size = self.rnn_hidden_size*3, act="relu")
        self.prediction = fluid.layers.fc(self.fc3, size = self.act_dim)
        return self.prediction
# %%
