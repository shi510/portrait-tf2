/*
 * Copyright 2019 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.vcall;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Matrix;
import android.graphics.Typeface;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.portrait.BlurModule;
import com.vcall.custom_view.MainDataViewModel;
import com.mxn.soul.flowingdrawer_core.ElasticDrawer;

import java.io.IOException;

import com.vcall.env.BorderedText;
import com.vcall.env.Logger;

import com.portrait.PortraitModule;
import com.portrait.PortraitModule.Device;
import com.portrait.PortraitModule.Model;

import static java.lang.Math.abs;


public class ClassifierActivity extends CameraActivity implements OnImageAvailableListener {
  private static final Logger LOGGER = new Logger();
  private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 480);
  private static final float TEXT_SIZE_DIP = 10;
  private Bitmap rgbFrameBitmap = null;
  private Integer sensorOrientation;
  private PortraitModule portraitNet;
  private BorderedText borderedText;
  /** Input image size of the model along x axis. */
  private int imageSizeX;
  /** Input image size of the model along y axis. */
  private int imageSizeY;
  int cur_cam_num = 1;
  MainDataViewModel mainDataVM;
  BlurModule blurModule = new BlurModule();

  @Override
  protected void onCreate(final Bundle savedInstanceState) {
    super.onCreate(null);
    mDrawer.setOnDrawerStateChangeListener(new ElasticDrawer.OnDrawerStateChangeListener() {
      @Override
      public void onDrawerStateChange(int oldState, int newState) {
        if (newState == ElasticDrawer.STATE_CLOSED) {
        }
        else if (newState == ElasticDrawer.STATE_OPEN) {
        }
      }

      @Override
      public void onDrawerSlide(float openRatio, int offsetPixels) {
      }
    });
    ImageButton switch_cam_btn = (ImageButton)findViewById(R.id.switch_camera);
    switch_cam_btn.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if(cur_cam_num == 0){
          setCamera_fragment("1");
          cur_cam_num = 1;
        }
        else{
          setCamera_fragment("0");
          cur_cam_num = 0;
        }
      }
    });
    switch_cam_btn.setOnLongClickListener(new View.OnLongClickListener() {
      @Override
      public boolean onLongClick(View v) {
        setCamera_fragment("2");
        cur_cam_num = 2;
        return false;
      }
    });

    mainDataVM = new ViewModelProvider(this).get(MainDataViewModel.class);
    mainDataVM.modelName.observe(this, new Observer<String>() {
      @Override
      public void onChanged(String model) {
        if(Model.QuantizedModel.toString().equals(model)){
          recreateClassifier(Model.valueOf(model), Device.HEXAGON, 1);
        }
        else if(Model.FloatModel.toString().equals(model)){
          recreateClassifier(Model.valueOf(model), Device.NNAPI, 1);
        }
      }
    });
  }

  @Override
  protected int getLayoutId() {
    return R.layout.texture_view_camera;
  }

  @Override
  protected Size getDesiredPreviewFrameSize() {
    return DESIRED_PREVIEW_SIZE;
  }

  @Override
  public void onPreviewSizeChosen(final Size size, final int rotation) {
    final float textSizePx =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
    borderedText = new BorderedText(textSizePx);
    borderedText.setTypeface(Typeface.MONOSPACE);

    previewWidth = size.getWidth();
    previewHeight = size.getHeight();

    sensorOrientation = rotation - getScreenOrientation();
    LOGGER.i("Camera orientation relative to screen canvas: %d", sensorOrientation);
    LOGGER.i("Camera orientation relative to screen canvas 1: %d", rotation);
    LOGGER.i("Camera orientation relative to screen canvas 2: %d", getScreenOrientation());

    LOGGER.i("Initializing at size %dx%d", previewWidth, previewHeight);
    rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Config.ARGB_8888);
  }

  @Override
  protected void processImage() {
    rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);
    runInBackground(
            new Runnable() {
              @RequiresApi(api = Build.VERSION_CODES.N)
              @Override
              public void run() {
                int sx = 1;
                int sy = 1;

                Log.d("@@@@", ""+sensorOrientation);
                if(sensorOrientation == 270){
                  sx = -1;
                }
                else if(sensorOrientation == 90){
                  sx=1;
                }
                Matrix matrix = new Matrix();
                matrix.postRotate(-90);
                matrix.postScale(sx, sy);
                Bitmap origin = Bitmap.createBitmap(rgbFrameBitmap, 0, 0, rgbFrameBitmap.getWidth(), rgbFrameBitmap.getHeight(), matrix, false);
                Bitmap masked = portraitNet.recognizeImage(origin, 0);
                Bitmap output = blurModule.maskedBlur(origin, masked);

                runOnUiThread(new Runnable() {
                  @Override
                  public void run() {
                    blurryView.set(output);
                    blurryView.invalidate();
                  }
                });

                readyForNextImage();
              }
            });
  }

  private void recreateClassifier(Model model, Device device, int numThreads) {
    if (portraitNet != null) {
      LOGGER.d("Closing classifier.");
      portraitNet.close();
      portraitNet = null;
    }
//    if (device == Device.GPU
//        && model == Model.QuantizedModel) {
//      LOGGER.d("Not creating classifier: GPU doesn't support quantized models.");
//      runOnUiThread(
//          () -> {
//            Toast.makeText(this, R.string.tfe_ic_gpu_quant_error, Toast.LENGTH_LONG).show();
//          });
//      return;
//    }
    try {
      LOGGER.d(
          "Creating classifier (model=%s, device=%s, numThreads=%d)", model, device, numThreads);
      portraitNet = new PortraitModule(this, model, Device.HEXAGON, numThreads);
    } catch (IOException | IllegalArgumentException e) {
      LOGGER.e(e, "Failed to create classifier.");
      runOnUiThread(
          () -> {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
          });
      return;
    }
    // Updates the input image size.
    imageSizeX = portraitNet.getImageSizeX();
    imageSizeY = portraitNet.getImageSizeY();
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data){

    Log.d("onActivityResult ", ""+resultCode);
    if(requestCode == 0) {
      String model_name = data.getStringExtra("model_name");
      if(!mainDataVM.modelName.getValue().equals(model_name)){
        mainDataVM.modelName.setValue(model_name);
      }
    }
    super.onActivityResult(requestCode, resultCode, data);
  }

}
