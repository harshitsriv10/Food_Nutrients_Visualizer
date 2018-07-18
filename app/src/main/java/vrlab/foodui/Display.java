/*
    Group No. : 26
    Module Name : CameraActivity.java
    Created by: Aayush Agarwal
    Last modified : 21/4/2018, Saturday 7:30 pm
    Modified by: Harshit Srivastava
    Recent Updates: The HSV values were modified, exclamation marks were added for hazards,
    augmented tag was modified.
    Bugs/fixes needed: None, Finalized

    functions:
              --- checkCameraPermission
              --- onCameraFrame
              --- initializeFromDatabase
              --- detectFruit
              --- compareHsHistograms
              --- findContoursAboveThreshold
              --- handleDetectedFruit


    external Classes used:
                --- Fruit
                --- Nutritional Value

    Overall module description:
        This module is the is used to take the images from the camera and then extract
        the name of the fruit is extracted from the image.
        It has camera view and an augmented button that leads to full information of the food item.
 */

package vrlab.foodui;
/*
    Required imports were made.
*/
import ...

// HSV refers to Hue Saturation Value

public class FoodUiActivity
        extends Activity
        implements CvCameraViewListener2,
        View.OnTouchListener {

    // load opencv library
    static {
        System.loadLibrary("opencv_java3");
        System.loadLibrary("foodui");
    }

    // constants for permissions
    private static final int PERMISSIONS_REQUEST_CAMERA = 0;
    // constants for development purposes only

    // histogram related settings
    private static final int HISTOGRAM_SIZE = 256;  // size of histogram (value pairs) [max=256]
    // used method for histogram comparison
    // histogram drawing related settings
    private static final int MIN_CONTOUR_SIZE = 10000;   // threshold for found contours
    private static final int MAX_CONTOUR_SIZE = 300000;   // threshold for found contours

    // constants related to blurring the image
    private static final Size ERODE_SIZE = new Size(9, 9);
    private static final Size DILATE_SIZE = new Size(9 , 9);

    private CameraBridgeViewBase camera_view;

    private boolean freezeCamera;    // freeze camera, e.g. during input operations

    private List<Fruit> detectedFruits = new ArrayList<>();
    private Fruit detectedFruit;
    private DbAccessFruitTypes food_database;
    //empty standard constructor
    public FoodUiActivity() {}
    /*
     * executes various initialization operations
     * - initializes camera view from opencv
     * - initializes fruit type list with data from database
     * - initializes user interface
     * Input : savedInstanceState
     */

    @Override
    public void onCreate(Bundle savedInstanceState) {
        // check for permissions if API is lvl 23 or higher
        //getting permissions to open the camera
        if (android.os.Build.VERSION.SDK_INT >= 23){
            checkCameraPermission();
        }
        // open camera view using open cv
        camera_view = (CameraBridgeViewBase) findViewById(R.id.activity_main_surface_view);
        camera_view.setCvCameraViewListener(this);
        // initialize existing fruitTypes from database
        initializeFromDatabase();

    }

    /*
        Takes the permission from the user to  the camera
    */
    @Override
    public void checkCameraPermission() {
        // If request is cancelled, the result arrays are empty.
        if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // permission was granted

        } else {
            // feedback to user for not allowing camera
            String text = "This app needs a camera for it to work";
            Toast toast = Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG);
            toast.show();           // show information to the user
        }

    }



    /**
     * starting point for every frame, arranges steps depending on the active mode
     *
     * Input : input_frame        contains data from the live frame of the camera
     * Output :                  returns rgba frame as a result of the frame handling processes
     *                          to display it in the next step
     */
    public Mat onCameraFrame(CvCameraViewFrame input_frame) {
        if (!freezeCamera) {
            // input frame has RBGA format
            mRgba = input_frame.rgba();
            // convert the image frame to hsv mode
            Imgproc.cvtColor(mRgba, mHsvOriginal, Imgproc.COLOR_RGB2HSV_FULL);

            // execute fruit detection
            for (FruitType fruit : fruitTypes)
                detectFruit(fruit);

            // handle detected fruits and clear the list afterwards
            if (detectedFruits.size() > 0)
                for (Fruit fruits : detectedFruits)
                    handleDetectedFruit(fruits);
            detectedFruits.clear();
        }
        return mRgba;
    }


    /**
     * initialize food information with saved values from database
     *
     */
    public void initializeFromDatabase() {
        // get database
        food_database = new DbAccessFruitTypes(this);

        // get data if table already exists
        if (food_database.isTableExisting(food_database.TABLE_NAME_FRUIT_TYPES)) {
            // process data
            fruitTypes = food_database.getAllData();
        }
    }

    /**
     * executes the whole fruit detection process, shows histograms and mask in hsv mode
     *
     * Input : fruit         defines fruit type, for which the check occures
     */
    public void detectFruit(FruitType fruit) {
        // variable declaration for temporary saving
        Fruit detectedFruit = null;
        List<MatOfPoint> contours;
        MatOfPoint biggestContour;
        List<Mat> image_histogram;
        LinkedList<Fruit> FruitList;

        // find biggest contour from hsv filtered frame
        contours = findContoursAboveThreshold();
        biggestContour = contours.get(0);

        //calcuate histogram for the image
        image_histogram = calculateHistogram();

        // if a match was found then set the detected fruit
        if (compareHsHistograms(fruit, image_histogram)) {
            detectedFruit = fruit;
        }
    }


    /**
     * find closest match of food to picture using hs color histogram comparison
     * Input : FruitType, histograms
     * Output :  returns the detected FruitType, which is most likely the captured fruit
     */
    public boolean compareHsHistograms(FruitType fruit, List<Mat> image_histogram) {
        double highestComparisonValueH = 0;
        double highestComparisonValueS = 0;
        boolean is_fruit_present = false;

        // find closest match to picture --> hs color histogram comparison with chi square method
        // histogram comparison for the currently checked fruit type
        double comparisonValueH = 0;
        double comparisonValueS = 0;

        // compare histograms with intersect method
        comparisonValueH = Imgproc.compareHist(fruit.getHsvHistogramH(),
                image_histogram.get(0), Imgproc.CV_COMP_CORREL);
        comparisonValueS = Imgproc.compareHist(fruit.getHsvHistogramS(),
                image_histogram.get(1), Imgproc.CV_COMP_CORREL);

        if (comparisonValueH > highestComparisonValueH) {
            highestComparisonValueH = comparisonValueH;
        }
        if (comparisonValueS > highestComparisonValueS) {
            highestComparisonValueS = comparisonValueS;
        }

        // if the comparision factor of either hue or saturation lies in the given range
        // then fruit is detected
        if ( (highestComparisonValueH >= 0.2 && highestComparisonValueH <= 1 )
                || (highestComparisonValueS >= 0.2 && highestComparisonValueS <= 1) ){
            is_fruit_present = true;
        }

        return is_fruit_present;
    }


    /*
     * finds biggest contour in mHsvThreshed (binary mask from hsv threshold filtering) above
     * the defined threshold, or even all contours above this threshold,
     * depending on the parameter isOnlyBiggestDesired
     *
     * Input  :							contours in the image
     * Output :                         biggest contour found in the image(only if it is of required size)
     */
    public List<MatOfPoint> findContoursAboveThreshold() {
        List<MatOfPoint> results = new ArrayList<>();
        // contour specific variables
        List<MatOfPoint> contours = new ArrayList<>();
        MatOfPoint biggestContour = new MatOfPoint();

        // find all contours in clone of binary mask
        Imgproc.findContours(temp, contours, new Mat(), Imgproc.RETR_EXTERNAL,
                Imgproc.CHAIN_APPROX_SIMPLE);

        // for the case of no found contours
        if (contours.size() > 0) {
            // walk through all contours
            for (int i = 0; i < contours.size(); i++) {
                // finding the biggest contour from a set of contours
                if (i == 0)
                    biggestContour = contours.get(i);
                else if (Imgproc.contourArea(contours.get(i))
                        > Imgproc.contourArea(biggestContour)
                        && Imgproc.contourArea(contours.get(i)) > MIN_CONTOUR_SIZE
                        && Imgproc.contourArea(contours.get(i)) < MAX_CONTOUR_SIZE)
                    biggestContour = contours.get(i);
            }

            //// handle found contours
            // if there was found one above the threshold size: smooth and save the found contour
            if (Imgproc.contourArea(biggestContour) > MIN_CONTOUR_SIZE
                    && Imgproc.contourArea(biggestContour) < MAX_CONTOUR_SIZE) {

                // convert mats of contour and smooth contour
                biggestContour.convertTo(mMOP2f1, CvType.CV_32FC2);
                Imgproc.approxPolyDP(mMOP2f1, mMOP2f2, 5, true);
                mMOP2f2.convertTo(biggestContour, CvType.CV_32S);
                results.add(biggestContour);

            }
        }
        return results;
    }


    /**
     * handle the detected fruit: draw contour, line and label
     *
     * Input : Fruit                   fruit, which should be handled
     * Output :
     */
    public void handleDetectedFruit(final Fruit fruit) {

        List<String> display_information = new ArrayList<>();       //list of items to be displayed on the augumented tag
        NutritionalValue nutrition_values                           // nutitional information of the food identified

        String user_hazard_input , health_hazard_info;
        Boolean hazard_match;

        // if a food was detected
        if (fruit != null) {
            // redirect to the new page with more information about the fruit
            if(fruit.getIsInformationDisplayed()){
                Intent redirect_more_info = new Intent(this, Display.class);
                //send the food name to the more information page
                redirect_more_info.putExtra("name" , fruit.getType().getName());
                // start the activity of more information page
                startActivity(redirect_more_info);
                // end the camera view activity
                finish();
            }

            // get the nutrition information of the food from the database
            nutrition_values = fruit.getType().getNutritionalValue();
            // getting the user hazards that were input by the user
            user_hazard_input = getString("userHealthHazard" , "");
            //health hazard of the food item
            health_hazard_info = nutrition_values.getHealthHazardInformation();
            // name of the food to be shown in the augumented tag
            String name_shown = fruit.getType().getName();
            // if the hazards of the food contains hazard entered by the user
            hazard_match = health_hazard_info.contains(user_hazard_input);
            //if hazard matches with user hazards show an exclamation mark along with the fruit name
            if(hazard_match && user_hazard_input!=""){
                name_shown = name_shown + " !!";
            }
            // adding the information to be displayed in the augumented tag
            display_information.add("" + nutrition_values.getCaloricValuePer100g() + " kcal");
            display_information.add("Protein: " + nutrition_values.getProteinContentPer100g() + "g";
            display_information.add("Fat: " + nutrition_values.getFatContentPer100g() + " g");

        }
    }
}
