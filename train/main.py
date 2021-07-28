from train.model_zoo import build_mobilenetV2
from train.input_pipeline import make_tfdataset
from train.utils import convert_tflite_int8
from train.utils import train_quant_aware
from train.utils import copy_weights_if_only_exits

import numpy as np
import tensorflow as tf


x_train=np.load('data/img_uint8.npy')
y_train=np.load('data/msk_uint8.npy')

x_test=np.load('data/test_xtrain.npy')
y_test=np.load('data/test_ytrain.npy')

shape = [128, 128, 3]

train_ds, test_ds = make_tfdataset((x_train, y_train), (x_test, y_test), 32, shape[:2])

model = build_mobilenetV2(shape)

model.compile(loss=tf.nn.sigmoid_cross_entropy_with_logits, optimizer=tf.keras.optimizers.Adam(lr=1e-4),metrics=['accuracy'])
model.summary()

early_stop = tf.keras.callbacks.EarlyStopping(
    monitor='val_accuracy',
    mode='max', patience=7,
    restore_best_weights=True)

model.fit(train_ds, validation_data=test_ds, epochs=6, verbose=1, callbacks=[early_stop])

quant_model = train_quant_aware(model, train_ds, test_ds, 6)

# TODO:
#       QAT works, but QAT model conversion is a bit odd.
#       Both quantization and dequantization operators are added to an output of annotated layer.
#       I think it is a bug on tensorflow lite converter for annotated QAT models.
#       To fix this problem, but not correct answer, we just copy QAT weights to the original model.
#       And then, apply post-quantization to find quantization ranges.
quant_model.evaluate(test_ds)
copy_weights_if_only_exits(model, quant_model)
model.evaluate(test_ds)
quant_model = model

train_ds, test_ds = make_tfdataset((x_train, y_train), (x_test, y_test), 1, shape[:2])
quant_model.inputs[0].set_shape([1] + shape)
convert_tflite_int8(quant_model, test_ds, '{}_quant_aware.tflite'.format(model.name), 1)
