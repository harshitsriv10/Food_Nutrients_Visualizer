/*
    Group No. : 26
    Module Name : HazardInput.java
    Created by: Harshit Srivastava
    Last modified : 21/4/2018, Saturday 7:30 pm
    Modified by: Aayush Agarwal
    Bugs/fixes needed: None, Finalized

    classes : HazardInput

    functions :
              --- onCreate
              --- onClick

    external Classes used:

    Overall module description:
        This module allows the user to input their hazards so that they can
        get a warning if a foods hazard matches with the user
        It has one input text field for user hazards and one button to start the camera.
 */

package vrlab.foodui;
/*
 Importing the required libraries.
*/
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

/*
 This class is used to manage the input of health hazrd for a given user.
 */
public class HazardInput extends AppCompatActivity implements ViewStub.OnClickListener{

    EditText user_health_hazard;     // It keeps the user's health hazards.
    SharedPreferences shared_preferences;    // This is used the app data in temporary database.
    Button submit_button;   // This is used to declare button.
    String health_hazard_input;       // This is the input for the health hazards.

    /*
        This function is used to make the layout for the hazard input.

         Input : Bundle of data about the food items in the database.
         Output : Display layout is set.
     */

    @Override
    protected void onCreate(Bundle saved_instance_state) {
        super.onCreate(saved_instance_state);   // Super is used to refer to the parent class of current class.
        setContentView(R.layout.activity_hazard);   // This is used to set the view.

        user_health_hazard = (EditText)findViewById(R.id.editText);   // This is the input text box.
        submit_button = (Button) findViewById(R.id.buttoninput);    // The hazard submit button.

        submit_button.setOnClickListener(this);     // This is to listen to the click activity.

    }

    /*
        This function is used to handle the input hazard when the button is clicked.

        Input : Information of the current page.
        Output : Hazard is added to the list of hazards.
     */
    @Override
    public void onClick(View v) {
        health_hazard_input = user_health_hazard.getText().toString();  // This is the health hazard input string.

        shared_preferences = getSharedPreferences("MyPREFERENCES", Context.MODE_PRIVATE); // The list of hazards from the local database.

        if(health_hazard_input != null){          // If the input is null, nothing to do, else process.
            SharedPreferences.Editor editor = shared_preferences.edit();   // Editor declared to change the shared preferences.
            editor.putString("userHealthHazard" , health_hazard_input);    //Changes made to shared preference.
            editor.commit();                        // The changes are committed.
        }

        Intent redirect_camera_view = new Intent(this , FoodUiActivity.class);    // This is take us to the camera view.
        redirect_camera_view.putExtra("userHealthHazard" , health_hazard_input);
        startActivity(redirect_camera_view);   // This is to start the main activity.
    }

}