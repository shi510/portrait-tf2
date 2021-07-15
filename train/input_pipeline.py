import tensorflow as tf
import tensorflow_addons as tfa


TF_AUTOTUNE = tf.data.AUTOTUNE


def flip_left_right(x: tf.Tensor):
    return tf.image.flip_left_right(x)


def gray(x):
        return tf.image.grayscale_to_rgb(tf.image.rgb_to_grayscale(x))


def random_color(x: tf.Tensor):
    x = tf.image.random_hue(x, 0.2)
    x = tf.image.random_brightness(x, 0.2)
    x = tf.image.random_contrast(x, 0.8, 1.2)
    return x


def blur(x):
    choice = tf.random.uniform([], 0, 1, dtype=tf.float32)
    def gfilter(x):
        return tfa.image.gaussian_filter2d(x, [5, 5], 1.0, 'REFLECT', 0)


    def mfilter(x):
        return tfa.image.median_filter2d(x, [5, 5], 'REFLECT', 0)


    return tf.cond(choice > 0.5, lambda: gfilter(x), lambda: mfilter(x))


def cutout(x : tf.Tensor):

    def _cutout(x : tf.Tensor):
        const_rnd = tf.random.uniform([], 0., 1., dtype=tf.float32)
        size = tf.random.uniform([], 0, 20, dtype=tf.int32)
        size = size * 2
        return tfa.image.random_cutout(x, (size, size), const_rnd)


    choice = tf.random.uniform([], 0., 1., dtype=tf.float32)
    return tf.cond(choice > 0.5, lambda: _cutout(x), lambda: x)


def make_tfdataset(train_list, test_list, batch_size, img_shape):
    train_ds = tf.data.Dataset.from_tensor_slices(train_list)

    def _normalize(x: tf.Tensor):
        # Normalize images to the range [0, 1].
        x = tf.image.resize(x, img_shape)
        return x / 255.

    def split_label_channel(label : tf.Tensor):
        bg = 1. - label
        return tf.concat([bg, label], axis=-1)


    train_ds = train_ds.shuffle(10000)
    train_ds = train_ds.batch(batch_size)
    train_ds = train_ds.map(lambda img, label : (_normalize(img), _normalize(label)), num_parallel_calls=TF_AUTOTUNE)
    train_ds = train_ds.map(lambda img, label : (img, split_label_channel(label)), num_parallel_calls=TF_AUTOTUNE)
    augmentations = []#[random_color, blur, cutout, gray]
    for f in augmentations:
        choice = tf.random.uniform([], 0.0, 1.0)
        train_ds = train_ds.map(lambda x, label: (tf.cond(choice > 0.5, lambda: f(x), lambda: x), label),
            num_parallel_calls=TF_AUTOTUNE)
    # choice = tf.random.uniform([], 0.0, 1.0)
    # train_ds = train_ds.map(lambda x, label: (tf.cond(choice > 0.5, lambda: (flip_left_right(x), flip_left_right(label)), lambda: (x, label))),
        # num_parallel_calls=TF_AUTOTUNE)
    train_ds = train_ds.map(lambda x, label: (tf.clip_by_value(x, 0., 1.), label), num_parallel_calls=TF_AUTOTUNE)
    train_ds = train_ds.map(lambda x, label: (cutout(x), label), num_parallel_calls=TF_AUTOTUNE)
    train_ds = train_ds.prefetch(TF_AUTOTUNE)

    test_ds = tf.data.Dataset.from_tensor_slices(test_list)
    test_ds = test_ds.batch(batch_size)
    test_ds = test_ds.map(lambda img, label : (_normalize(img), _normalize(label)), num_parallel_calls=TF_AUTOTUNE)
    test_ds = test_ds.map(lambda img, label : (img, split_label_channel(label)), num_parallel_calls=TF_AUTOTUNE)

    return train_ds, test_ds
