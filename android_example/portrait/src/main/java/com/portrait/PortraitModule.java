package com.portrait;

import android.app.Activity;
import android.graphics.Bitmap;
import android.util.Log;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.HexagonDelegate;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.GpuDelegate;
import org.tensorflow.lite.nnapi.NnApiDelegate;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.TensorOperator;
import org.tensorflow.lite.support.common.TensorProcessor;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.MappedByteBuffer;


public class PortraitModule {
    static {
        System.loadLibrary("mask_processing-lib");
    }
    public static final String TAG = "PortaitModule";

    /** The runtime device type used for executing classification. */
    public enum Device {
        CPU(0),
        NNAPI(1),
        GPU(2),
        HEXAGON(3);

        private final int value;

        Device(int value){
            this.value = value;
        }

        public int getValue(){
            return value;
        }
    }

    /** The model type used for classification. */
    public enum Model {
        FloatModel(0),
        QuantizedModel(1);

        private final int value;

        Model(int value){
            this.value = value;
        }

        public int getValue(){
            return value;
        }
    }

    /** Image size along the x axis. */
    private final int imageSizeX;

    /** Image size along the y axis. */
    private final int imageSizeY;

    /** Optional GPU delegate for accleration. */
    private GpuDelegate gpuDelegate = null;

    /** Optional NNAPI delegate for accleration. */
    private NnApiDelegate nnApiDelegate = null;

    /** Optional GPU delegate for accleration. */
    private HexagonDelegate hexagonDelegate = null;

    /** An instance of the driver class to irun model inference with Tensorflow Lite. */
    protected Interpreter tflite;

    /** Input image TensorBuffer. */
    private TensorImage inputImageBuffer;

    /** Output probability TensorBuffer. */
    private final TensorBuffer outputProbabilityBuffer;

    /** Processer to apply post processing of the output probability. */
    private final TensorProcessor probabilityProcessor;

    /** Initializes a {@code Classifier}. */
    public PortraitModule(Activity activity, Model model, PortraitModule.Device device, int numThreads) throws IOException {
        MappedByteBuffer tfliteModel = FileUtil.loadMappedFile(activity, getModelPath(model));
        Interpreter.Options tfliteOptions = new Interpreter.Options();
        switch (device) {
            case NNAPI:
                nnApiDelegate = new NnApiDelegate();
                tfliteOptions.addDelegate(nnApiDelegate);
                break;
            case GPU:
                gpuDelegate = new GpuDelegate();
                tfliteOptions.addDelegate(gpuDelegate);
                break;
            case HEXAGON:
                try {
                    hexagonDelegate = new HexagonDelegate(activity);
                    tfliteOptions.addDelegate(hexagonDelegate);
                } catch (UnsupportedOperationException e) {
                    Log.e(TAG, "Classifier: " + e);
                    // Hexagon delegate is not supported on this device.
                }
                break;
            case CPU:
                break;
        }
        tfliteOptions.setNumThreads(numThreads);
        tflite = new Interpreter(tfliteModel, tfliteOptions);

        // Reads type and shape of input and output tensors, respectively.
        int imageTensorIndex = 0;
        int[] imageShape = tflite.getInputTensor(imageTensorIndex).shape(); // {1, height, width, 3}
        imageSizeY = imageShape[1];
        imageSizeX = imageShape[2];
        DataType imageDataType = tflite.getInputTensor(imageTensorIndex).dataType();
        int probabilityTensorIndex = 0;
        int[] probabilityShape =
                tflite.getOutputTensor(probabilityTensorIndex).shape(); // {1, height, width, classes}
        DataType probabilityDataType = tflite.getOutputTensor(probabilityTensorIndex).dataType();

        // Creates the input tensor.
        inputImageBuffer = new TensorImage(imageDataType);

        // Creates the output tensor and its processor.
        outputProbabilityBuffer = TensorBuffer.createFixedSize(probabilityShape, probabilityDataType);

        // Creates the post processor for the output probability.
        probabilityProcessor = new TensorProcessor.Builder()
                .add(getPostprocessNormalizeOp()).build();
     }

     /** Runs inference and returns the classification results. */
    public Bitmap recognizeImage(final Bitmap bitmap, int sensorOrientation) {
        inputImageBuffer = loadImage(bitmap, sensorOrientation);
        tflite.run(inputImageBuffer.getBuffer(), outputProbabilityBuffer.getBuffer().rewind());
        float[] maskedOutput = probabilityProcessor.process(outputProbabilityBuffer).getFloatArray();
        String temp = "";
//        for(int i = 0; i < maskedOutput.length; ++i){
////            if( (int)(maskedOutput[i]*255) > 100){
////                temp += ", " + (int)(maskedOutput[i]*255);
////            }
//            temp += ", " + (int)(maskedOutput[i]);
//        }
//        Log.i("@@@@", temp);

        Bitmap postProcessed = Bitmap.createBitmap(imageSizeX, imageSizeY, Bitmap.Config.ARGB_8888);
        maskProcessing(postProcessed, maskedOutput);
        return postProcessed;
//        return outputProbabilityBuffer.getIntArray();
    }

    /** Closes the interpreter and model to release resources. */
    public void close() {
        if (tflite != null) {
            tflite.close();
            tflite = null;
        }
        if (gpuDelegate != null) {
            gpuDelegate.close();
            gpuDelegate = null;
        }
        if (nnApiDelegate != null) {
            nnApiDelegate.close();
            nnApiDelegate = null;
        }
        if (hexagonDelegate != null) {
            hexagonDelegate.close();
            hexagonDelegate = null;
        }
    }

    public int getImageSizeX() {
            return imageSizeX;
        }

    public int getImageSizeY() {
            return imageSizeY;
        }

    private TensorImage loadImage(final Bitmap bitmap, int sensorOrientation) {
        inputImageBuffer.load(bitmap);
        int numRotation = sensorOrientation / 90;
        ImageProcessor imageProcessor =
                new ImageProcessor.Builder()
                        .add(new ResizeOp(imageSizeX, imageSizeY, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
//                            .add(new Rot90Op(numRotation))
//                            .add(getPreprocessNormalizeOp())
                    .build();

        return imageProcessor.process(inputImageBuffer);
    }

    protected String getModelPath(Model model) {
        return "mobilenetv2_0.75_128_portrait_quant_aware.tflite";
    }

    protected TensorOperator getPreprocessNormalizeOp() {
        return new NormalizeOp( 0.f, 1.f);
    }

    protected TensorOperator getPostprocessNormalizeOp() {
            return new NormalizeOp(0.f, 255.f);
    }

    public native void maskProcessing(Bitmap bitmap_out, float[] maskProb);
}
