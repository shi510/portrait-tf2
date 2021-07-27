import train.mobilenetv3

import tensorflow as tf


def EfficientNetB0(shape):
    model = tf.keras.applications.EfficientNetB0(
        input_shape=shape,
        classifier_activation=None,
        include_top=False,
        weights='imagenet',
        activation='relu')
    return model

def EfficientNetB3(shape):
    model = tf.keras.applications.EfficientNetB3(
        input_shape=shape,
        classifier_activation=None,
        include_top=False,
        weights='imagenet',
        activation='relu')
    return model

def MobileNetV2(shape):
    model = tf.keras.applications.MobileNetV2(
        input_shape=shape,
        classifier_activation=None,
        include_top=False,
        weights='imagenet',
        alpha=0.75)
    return model


def MobileNetV3Small(shape):
    model = train.mobilenetv3.MobileNetV3Small(
        input_shape=shape,
        classifier_activation=None,
        include_top=False,
        weights='imagenet',
        alpha=0.75)
    return model


model_list = {
    'MobileNetV2': MobileNetV2,
    'MobileNetV3Small': MobileNetV3Small,
    'EfficientNetB0': EfficientNetB0,
    'EfficientNetB3': EfficientNetB3
}


def get_model(name, shape):
    return model_list[name](shape)


def _bottleneck_block(x, in_ch, out_ch):
    y = x
    y = shortcut = tf.keras.layers.Conv2D(in_ch, (3, 3), padding='same', use_bias=False)(y)
    y = tf.keras.layers.BatchNormalization()(y)
    y = tf.keras.layers.ReLU(6.)(y)

    y = tf.keras.layers.Conv2D(in_ch // 2, (1, 1), use_bias=False)(y)
    y = tf.keras.layers.BatchNormalization()(y)
    y = tf.keras.layers.ReLU(6.)(y)

    y = tf.keras.layers.Conv2D(in_ch, (3, 3), padding='same', use_bias=False)(y)
    y = tf.keras.layers.BatchNormalization()(y)
    y = tf.keras.layers.Add()([y, shortcut])
    y = tf.keras.layers.ReLU(6.)(y)

    y = tf.keras.layers.Conv2D(out_ch, (1, 1), use_bias=False)(y)
    y = tf.keras.layers.BatchNormalization()(y)
    y = tf.keras.layers.ReLU(6.)(y)
    return y


def build_efficientnetB0(shape):
    model = get_model('EfficientNetB0', shape)
    y = model.get_layer('block6a_expand_activation').output
    previous_16x16 = model.get_layer('block4a_expand_activation').output # 240
    previous_32x32 = model.get_layer('block3a_expand_activation').output # 144
    previous_64x64 = model.get_layer('block2a_expand_activation').output # 96

    y = tf.keras.layers.Conv2D(240, (1, 1))(y)
    y = tf.keras.layers.UpSampling2D(size=(2, 2), interpolation='bilinear')(y)
    y = tf.concat([y, previous_16x16])

    # add block
    y = _bottleneck_block(y, 240, 144)
    y = tf.keras.layers.UpSampling2D(size=(2, 2), interpolation='bilinear')(y)
    y = tf.concat([y, previous_32x32])

    # add block
    y = _bottleneck_block(y, 144, 96)
    y = tf.keras.layers.UpSampling2D(size=(2, 2), interpolation='bilinear')(y)
    y = tf.concat([y, previous_64x64])

    # add block
    y = _bottleneck_block(y, 96, 24)
    y = tf.keras.layers.UpSampling2D(size=(2, 2))(y)
    y = tf.keras.layers.Conv2D(24, (3, 3), padding='same')(y)
    y = tf.keras.layers.BatchNormalization()(y)
    y = tf.keras.layers.ReLU(6.)(y)
    y = tf.keras.layers.Conv2D(2, (3, 3), padding='same')(y)


    return tf.keras.Model(model.inputs, y, name='{}_portrait'.format(model.name))


def build_mobilenetV2(shape):
    model = get_model('MobileNetV2', shape)
    previous_16x16 = model.get_layer('block_6_expand_relu').output # 240
    previous_32x32 = model.get_layer('block_3_expand_relu').output # 144
    previous_64x64 = model.get_layer('block_1_expand_relu').output # 96
    y = model.get_layer('block_13_expand_relu').output

    y = tf.keras.layers.Conv2D(240, (1, 1))(y)
    y = tf.keras.layers.BatchNormalization()(y)
    y = tf.keras.layers.ReLU(6.)(y)
    y = tf.keras.layers.UpSampling2D(size=(2, 2), interpolation='bilinear')(y)
    y = tf.concat([y, previous_16x16], axis=-1)

    # add block
    y = _bottleneck_block(y, 240, 144)
    y = tf.keras.layers.UpSampling2D(size=(2, 2), interpolation='bilinear')(y)
    y = tf.concat([y, previous_32x32], axis=-1)

    # add block
    y = _bottleneck_block(y, 144, 96)
    y = tf.keras.layers.UpSampling2D(size=(2, 2), interpolation='bilinear')(y)
    y = tf.concat([y, previous_64x64], axis=-1)

    # add block
    y = _bottleneck_block(y, 96, 48)
    y = tf.keras.layers.UpSampling2D(size=(2, 2), interpolation='bilinear')(y)
    y = tf.keras.layers.Conv2D(16, (3, 3), padding='same', use_bias=False)(y)
    y = tf.keras.layers.BatchNormalization()(y)
    y = tf.keras.layers.ReLU(6.)(y)
    y = tf.keras.layers.Conv2D(2, (3, 3), padding='same')(y)

    return tf.keras.Model(model.inputs, y, name='{}_portrait'.format(model.name))

def build_mobilenetV3(shape):
    model = get_model('MobileNetV3Small', shape)
    model.summary()
    previous_14x14 = model.get_layer('multiply_11').output # 240
    previous_28x28 = model.get_layer('multiply_1').output # 96
    previous_54x54 = model.get_layer('re_lu_3').output # 72
    previous_112x112 = model.get_layer('multiply').output # 16
    y = model.get_layer('multiply_16').output

    y = tf.keras.layers.Conv2D(240, (1, 1))(y)
    y = tf.keras.layers.BatchNormalization()(y)
    y = tf.keras.layers.ReLU()(y)
    y = tf.keras.layers.UpSampling2D(size=(2, 2), interpolation='bilinear')(y)
    y = tf.concat([y, previous_14x14], axis=-1)

    # add block
    y = _bottleneck_block(y, 240, 96)
    y = tf.keras.layers.UpSampling2D(size=(2, 2), interpolation='bilinear')(y)
    y = tf.concat([y, previous_28x28], axis=-1)

    # add block
    y = _bottleneck_block(y, 96, 72)
    y = tf.keras.layers.UpSampling2D(size=(2, 2), interpolation='bilinear')(y)
    y = tf.concat([y, previous_54x54], axis=-1)

    # add block
    y = _bottleneck_block(y, 96, 16)
    y = tf.keras.layers.UpSampling2D(size=(2, 2), interpolation='bilinear')(y)
    y = tf.concat([y, previous_112x112], axis=-1)

    # add block
    y = _bottleneck_block(y, 16, 16)
    y = tf.keras.layers.UpSampling2D(size=(2, 2), interpolation='bilinear')(y)
    y = tf.keras.layers.Conv2D(16, (3, 3), padding='same', use_bias=False)(y)
    y = tf.keras.layers.BatchNormalization()(y)
    y = tf.keras.layers.ReLU()(y)
    y = tf.keras.layers.Conv2D(2, (3, 3), padding='same')(y)
    # y = tf.keras.activations.sigmoid(y)

    return tf.keras.Model(model.inputs, y, name='{}_portrait'.format(model.name))