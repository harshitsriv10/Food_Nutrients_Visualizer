/*
    Group No. : 26
    Module Name : First_Activity.java
    Created by: Harshit Srivastava
    Last modified : 21/4/2018, Friday 7:00 pm
    Modified by: Akhil Chandra
    Recent Updates: Added feature for start camera view without login
    Bugs/fixes needed: None, Finalized

    classes : First_Activity

    functions :
              --- onCreate :
              --- onClick

    external Classes used:

    Overall module description:
        This module is used to define the first page of the application.
        It has two buttons : start camera(goes to the camera view) and login(goes to the login page)
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
import android.view.View;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.EditText;


/*
    This class controls the first page of the app. This has the login and open camera button.
 */

public class First_Activity extends AppCompatActivity implements ViewStub.OnClickListener {

    Button login_button , start_camera_button;  // This is used to declare the declare the buttons for login and camera.
    /*
        This function is used to set the layout for the page.
        Input : All the data that has been used previously in the app.
        Output : Layout is set for the first page.
     */

    @Override
    protected void onCreate(Bundle saved_instance_state) {
        super.onCreate(saved_instance_state);         //  Super is used to refer to the parent class of the current class.
        setContentView(R.layout.activity_first);    // This is used to set the layout.

        login_button = (Button) findViewById(R.id.buttonlogin);      // This is for the login button.
        start_camera_button = (Button) findViewById(R.id.buttonstartcamera);   // This is for camera button.

        start_camera_button.setOnClickListener(this);    // This is for the listening to the camera button.
        login_button.setOnClickListener(this);           // This is for the listening to the login button.

    }

    /*
        This function is used to handle the on click activities.

        Input : View
        Output : This main class is called according to the case.
     */
    @Override
    public void onClick(View button) {
        Intent redirect_new_view;   // Declaration of the new intent.

        switch (button.getId()){         // Switch case for buttons.
            case R.id.button_start_camera:        // if the button selected is for camera.
                redirect_new_view = new Intent(this , FoodUiActivity.class);    // go to the camera view
                startActivity(redirect_new_view);          // To start the activity for this.
                break;

            case R.id.button_login:              // if the button selected is for login.
                redirect_new_view = new Intent(this , MainActivity.class);      // go to the login page
                startActivity(redirect_new_view);          // To start the activity for this.
                break;

            default:            //if none, then break;
                break;
        }
    }
}