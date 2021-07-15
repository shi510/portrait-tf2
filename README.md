## 0. Clone
```
git clone https://github.com/shi510/portrait-tf2
cd portrait-tf2
mkdir data
```

## 1. Prepare portrait dataset
[Download Portrait Dataset](https://drive.google.com/file/d/1UBLzvcqvt_fin9Y-48I_-lWQYfYpt_6J/view?usp=sharing)  
See https://github.com/anilsathyan7/Portrait-Segmentation for details.  

Extract the downloaded file to data folder.  
You have these files.  
```
data/img_uint8.npy
data/msk_uint8.npy
data/test_uint8.npy
data/test_xtrain.npy
data/test_ytrain.npy
```

## 2. Train
This step trains 2 times.  
First, It trains a portrait model from scratch.  
Second, It do quantization-aware-training (QTA).  
Then, It generates an 8-bit integer quantized model (tflite file).  

```
For Linux, export PYTHONPATH=$(pwd)
For Windows, $env:PYTHONPATH=$pwd
```
```
python train/main.py
```

## 3. Android example
Open the [android_example](android_example) with android studio.  
Build and Install to your phone.  
You can accelerate your model with Qualcommm Hexagon DSP.  
See https://www.tensorflow.org/lite/performance/hexagon_delegate.  
Download Hexagon libary from the above link.  
Then, Put that libary to the directory, app\src\main\jniLibs\arm64-v8a.  
```
app/src/main/jniLibs/arm64-v8a/libhexagon_nn_skel.so
app/src/main/jniLibs/arm64-v8a/libhexagon_nn_skel_v65.so
app/src/main/jniLibs/arm64-v8a/libhexagon_nn_skel_v66.so
```
Rebuild your android project and Install it.  
You can find the Pretrained file here [android_example/portrait/src/main/assets](android_example/portrait/src/main/assets).  
