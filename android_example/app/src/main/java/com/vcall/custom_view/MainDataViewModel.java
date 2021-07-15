package com.vcall.custom_view;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.portrait.PortraitModule;

public class MainDataViewModel extends ViewModel {
    public MutableLiveData<String> modelName;

    public MainDataViewModel() {
        modelName = new MutableLiveData<>(PortraitModule.Model.FloatModel.toString());
    }
}
