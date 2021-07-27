import tensorflow as tf
import tensorflow_model_optimization as tfmot

def convert_tflite_int8(model, calb_data, output_name, quant_level=0):
    """
    quant_level == 0:
        weights only quantzation, no requires calibration data.
    quant_level == 1:
        Full quantization for supported operators.
        It remains float for not supported operators.
    quant_level == 2:
        Full quantization for all operators.
        It can not be converted if the model contains not supported operators.
    """
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    def representative_dataset_gen():
        for n, (x, _ )in enumerate(calb_data.take(1000)):
            if n % 10 == 0:
                print(n)
            # Get sample input data as a numpy array in a method of your choosing.
            # The batch size should be 1.
            # So the shape of the x should be (1, height, width, channel)
            yield [x]
    if quant_level == 1:
        converter.representative_dataset = representative_dataset_gen
        converter.inference_input_type = tf.uint8  # or tf.int8
        converter.inference_output_type = tf.uint8  # or tf.int8
    elif quant_level == 2:
        converter.representative_dataset = representative_dataset_gen
        converter.target_spec.supported_ops = [tf.lite.OpsSet.TFLITE_BUILTINS_INT8]
        converter.inference_input_type = tf.uint8  # or tf.int8
        converter.inference_output_type = tf.uint8  # or tf.int8
    tflite_quant_model = converter.convert()
    with open(output_name, 'wb') as f:
        f.write(tflite_quant_model)


def train_quant_aware(base_model, train_ds, test_ds, num_epoch):
    early_stop = tf.keras.callbacks.EarlyStopping(
        monitor='val_accuracy',
        mode='max', patience=7,
        restore_best_weights=True)
    def apply_quantization_to_dense(layer):
        if isinstance(layer, tf.keras.layers.Conv2D) or \
            isinstance(layer, tf.keras.layers.DepthwiseConv2D):
            return tfmot.quantization.keras.quantize_annotate_layer(layer)
        return layer
    annotated_model = tf.keras.models.clone_model(base_model, clone_function=apply_quantization_to_dense)
    # TODO:
    #       QAT works, but QAT model conversion is a bit odd.
    #       Quantization and dequantization operators are added to an output of annotated layer.
    #       I think the quantization annotation should be stripped after QAT to convert tflite correctly.
    # quant_model = tfmot.quantization.keras.quantize_apply(annotated_model)
    quant_model = annotated_model
    quant_model.compile(loss=tf.nn.sigmoid_cross_entropy_with_logits, optimizer=tf.keras.optimizers.Adam(lr=1e-4),metrics=['accuracy'])
    quant_model.summary()
    quant_model.fit(train_ds, validation_data=test_ds, epochs=num_epoch, verbose=1, callbacks=[early_stop])
    return quant_model
