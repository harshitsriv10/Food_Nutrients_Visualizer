package vrlab.foodui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

public class SettingsFragment extends PreferenceFragment {

    public SettingsFragment() {
        // Required empty public constructor
    }


    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);

        getActivity().setTitle("Settings");
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
       // bindPreferenceSummaryToValue(findPreference("ringtone"))



        Preference button = findPreference("save_button");
        button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                SharedPreferences pref1 = PreferenceManager.getDefaultSharedPreferences(getActivity());
                Boolean Calorie = pref1.getBoolean("Calorie" , true);
                Boolean Protein = pref1.getBoolean("Protein" , true);
                Boolean Carbohydrate = pref1.getBoolean("Carbohydrate" , true);
                Boolean Fat = pref1.getBoolean("Fat" , true);
                Boolean Weight = pref1.getBoolean("Weight" , true);
                Boolean HealthHazard = pref1.getBoolean("HealthHazard" , true);

                if(Protein)Log.d("ddddddddddddddddd" , "yes");
                else Log.d("ddddddddddddddddd" , "no");

                Intent intent = new Intent(getActivity(), FoodUiActivity.class);

                intent.putExtra("Calorie" , Calorie);
                intent.putExtra("Protein",  Protein);
                intent.putExtra("Carbohydrate" , Carbohydrate);
                intent.putExtra("Fat" , Fat);
                intent.putExtra("Weight" , Weight);
                intent.putExtra("HealthHazard" , HealthHazard);

                startActivity(intent);
                return true;
            }
        });

    }
}

