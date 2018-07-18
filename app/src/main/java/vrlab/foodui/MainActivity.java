/*
    Group No. : 26
    Module Name : MainActivity.java
    Created by: Aayush Agarwal
    Last modified : 21/4/2018, Saturday 6:30 pm
    Modified by: Harshit Srivastava
    Recent Updates: Proper Commenting, added feature for starting camera without login
    Bugs/fixes needed: None, Finalized

    classes : MainActivity
    functions:
              --- onCreate
              --- onClick

    external Classes used: None

    Overall module description:
        This module is the main page of the application.
        It contains two buttons: Login feature and start camera feature.
 */
package vrlab.foodui;

/*
 Importing the required libraries.
*/

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewStub;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/*
    This class is used to manage the camera, login and fruit nutrition.
 */

public class MainActivity extends AppCompatActivity implements ViewStub.OnClickListener{

    private Button start_camera , search_fruit_nutrition ,signin_button;  // To define the buttons for start camera, fruit nutrition, and signin.
    TextView error_msg; // Textview refers to the text which is displayed.
    EditText username_input, password_input;   // This is also used to give the textbar for username and password.

    /*
        This function is run when the instance is created for the first time.

        Input : Data stored by previously used instances.
        Output : This sets the layout of the page.
     */


    @Override
    protected void onCreate(Bundle saved_instance_state) {
        super.onCreate(saved_instance_state);  // super is used to refer to the parent class of the current class.
        setContentView(R.layout.activity_foodui);    // To set the layout.

        // reference of start camera button and search button
        signin_button = (Button) findViewById(R.id.button);   // Button for sign in.
        error_msg = (TextView)findViewById(R.id.invalid);     // This is the text for showing the invalid login.
        username_input =(EditText)findViewById(R.id.username);   // This is to take input for the username.
        password_input =(EditText)findViewById(R.id.password);      // This is to take input for the password.

        signin_button.setOnClickListener(this);        // This is to check whether the button is clicked or not.
    }

    /*
        This function is used to manage the authentication on click of sign in button.

        Input : Information of the page.
        Output : Login authentication true. / Error message.
     */
    @Override
    public void onClick(View view) {

        Intent redirect_hazard_input;
        //intent is used to launch another Activity from current activity

        String username = username_input.getText().toString();   // This takes username string.
        String password = password_input.getText().toString();   // This takes password string.


        if(isValid(username, password)){
                        // If the username and password is authenticated, then login successful.
            redirect_hazard_input = new Intent(this , HazardInput.class);   // Define the instance of hazard input.
            startActivity(redirect_hazard_input);                  // This is used to start user hazard information activity.
        }
        else {                      //  If not authenticated, show error message.
            error_msg.setVisibility(View.VISIBLE);         // Make error message visible.
        }

    }
}