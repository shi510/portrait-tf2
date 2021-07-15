
package com.vcall;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.navigation.NavigationView;

public class MenuListFragment extends Fragment {
    public interface menuClickCallback{
        void onConnectClicked();
        void onSettingsClicked();
    }
    private menuClickCallback clickCallback;

    MenuListFragment(){
        clickCallback = new menuClickCallback() {
            @Override
            public void onConnectClicked() { }

            @Override
            public void onSettingsClicked() { }
        };
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_menu, container,
                false);
        NavigationView vNavigation = (NavigationView) view.findViewById(R.id.vNavigation);

        vNavigation.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem menuItem) {
                String clickedItem = menuItem.getTitle().toString();
                String menu_connnect = getResources().getString(R.string.menu_connect);
                String menu_settings = getResources().getString(R.string.menu_settings_name);
                if(clickedItem.equals(menu_connnect)){
                    clickCallback.onConnectClicked();
                }
                else if(clickedItem.equals(menu_settings)){
                    clickCallback.onSettingsClicked();
                }
                Toast.makeText(getActivity(),menuItem.getTitle(),Toast.LENGTH_SHORT).show();
                return false;
            }
        });
        return view;
    }

    public void setCallbackOnMenuClicked(menuClickCallback callbackInterface){
        clickCallback = callbackInterface;
    }
}
