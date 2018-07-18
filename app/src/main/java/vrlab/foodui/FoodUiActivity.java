/*
    Group No. : 26
    Module Name : HazardInput.java
    Created by: Aayush Agarwal
    Last modified : 21/4/2018, Saturday 7:30 pm
    Modified by: Harshit Srivastava
    Recent Updates: Proper Commenting, the HSV values were modified, exclamation marks were added for hazards,
    augmented tag was modified.
    Bugs/fixes needed: None, Finalized

    classes :
    1. FoodUIActivity

        functions (name and description) :
                  --- onCreate
                  --- onPause
                  --- onResume
                  --- onDestroy
                  --- onRequestPermissionsResult
                  --- onCameraViewStarted
                  --- onCameraViewStopped
                  --- onCameraFrame
                  --- onTouch
                  --- initializeFromDatabase
                  --- detectFruitType
                  --- getHsvFilterMask
                  --- calcHsvHistograms
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
    // defines, which histograms should be compared - choose ONE
    // threshold for comparison results
    private static final double HISTOGRAM_COMPARISON_THRESHOLD_HUE_MIN = 0.25;
    private static final double HISTOGRAM_COMPARISON_THRESHOLD_HUE_MAX = 1;
    private static final double HISTOGRAM_COMPARISON_THRESHOLD_SATURATION_MIN = 0.225;
    private static final double HISTOGRAM_COMPARISON_THRESHOLD_SATURATION_MAX = 1;
    // constants for contour relating settings
    private static final int CONTOUR_THRESHOLD_BOTTOM = 10000;   // threshold for found contours
    private static final int CONTOUR_THRESHOLD_TOP = 300000;     // threshold for found contours
    private static final double APPROX_DISTANCE = 5;     // distance between points of contour line
    // constants relating tracking operations
    private static final int FRUIT_TRACKER_FRAME_RANGE = 10;
    // amount of frames to consider in tracking
    // constants related to morphing operations
    private static final Size ERODE_SIZE = new Size(9, 9);
    private static final Size DILATE_SIZE = new Size(9 , 9);
    // fruit label and information display related settings
    private static final int SPACE_BUFFER_TEXT_FRONT = 5;
    private static final int SPACE_BUFFER_TEXT_END = SPACE_BUFFER_TEXT_FRONT;
    private static final int LABEL_LINE_1_HEIGHT = 30;
    private static final int LABEL_LINE_1_WIDTH = 30;
    private static final int LABEL_LINE_1_THICKNESS = 5;
    private static final int LABEL_LINE_2_THICKNESS = 3;
    private static final int TEXT_FONT_FACE = 3;
    private static final int TEXT_FONT_SCALE = 1;
    private static final int TEXT_THICKNESS = 2;
    private static final int TEXT_LINE_TYPE = 0;
    private static final int TEXT_MARGIN = 3 + LABEL_LINE_2_THICKNESS;

    // opencv specific variables
    private CameraBridgeViewBase mOpenCvCameraView;

    // various general mats(datatype for matrix)
    private Mat mRgba;
    private Mat mHsvOriginal;
    private Mat mGray;
    private Mat mIntermediateMat;
    private Mat mRgbaMask;

    // variables for hsv mode (including histogram operations)
    private Mat mHsv;
    private Mat mHsvThreshed;
    private List<Mat> hsvHistograms;          // saves histograms for hue and saturation
    private Mat mMat0;
    private MatOfInt mChannels[];
    private MatOfInt mHistSize;
    private MatOfFloat mRanges;
    private Scalar mWhite;

    // control variables
    private boolean freezeCamera;    // freeze camera, e.g. during input operations

    //// Tracking Targets
    // vector for user defined filter values: hMin, sMin, vMin, hMax, sMax, vMax
    // initialized with values for a good detection of a banana

    // objects for fruit and fruit type handling
    private List<FruitType> fruitTypes = new ArrayList<>();
    private List<Fruit> detectedFruits = new ArrayList<>();
    private Fruit detectedFruit;

    // objects for tracking of detected fruits --> using for remembering past states
    private List<LinkedList<Fruit>> fruitTracker = new ArrayList<LinkedList<Fruit>>();
    private DbAccessFruitTypes dbFruitTypes;
    SharedPreferences sharedPreferences;

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

        super.onCreate(savedInstanceState);
        sharedPreferences = getSharedPreferences("MyPREFERENCES", Context.MODE_PRIVATE);
        Bundle user_hazards = getIntent().getExtras();
        if(user_hazards != null){
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("userHealthHazard" , user_hazards.getString("userHealthHazard"));
            editor.apply();
        }

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);


        // check for permissions if API is lvl 23 or higher
        //getting permissions to open the camera
        if (android.os.Build.VERSION.SDK_INT >= 23){
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        Manifest.permission.CAMERA)) {
                    // permission available, no further steps needed
                } else {
                    // ask for permission of camera
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.CAMERA},
                            PERMISSIONS_REQUEST_CAMERA);
                }
            }
        }

        //// initialize variables
        // initialize opencv variables
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.activity_main_surface_view);
        mOpenCvCameraView.setCvCameraViewListener(this);
        // initialize existing fruitTypes from database
        initializeFromDatabase();

    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        // If request is cancelled, the result arrays are empty.
        if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // permission was granted

        } else {
            // feedback to user for not allowing camera
            String text = "This app needs a camera for it to work";
            Toast toast = Toast.makeText(getApplicationContext(), text, Toast.LENGTH_LONG);
            toast.show();
        }

    }

    /**
     * initializes all mats, scalars and other variables, which are needed for the frame processing
     * Input :
     *  width     the width of the frames that will be delivered
     *  height    the height of the frames that will be delivered
     */
    public void onCameraViewStarted(int width, int height) {
        // initialize main mats
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mGray = new Mat(height, width, CvType.CV_8UC1);
        mIntermediateMat = new Mat(height, width, CvType.CV_8UC4);
        mHsv = new Mat(height, width, CvType.CV_8UC4);
        mHsvOriginal = new Mat(height, width, CvType.CV_8UC4);
        mHsvThreshed = new Mat(height, width, CvType.CV_8UC1);
        mRgbaMask = new Mat(height, width, CvType.CV_8UC3);

        // initialize variables for hsv histograms
        mIntermediateMat = new Mat();
        hsvHistograms = new ArrayList<>(Arrays.asList(
                new Mat(HISTOGRAM_SIZE, 1, CvType.CV_32F),
                new Mat(HISTOGRAM_SIZE, 1, CvType.CV_32F)));
        mChannels = new MatOfInt[]{new MatOfInt(0), new MatOfInt(1), new MatOfInt(2)};
        mHistSize = new MatOfInt(HISTOGRAM_SIZE);
        mRanges = new MatOfFloat(0f, 256f);
        mMat0 = new Mat();
        mWhite = Scalar.all(255);

        // initialize other variables
        freezeCamera = false;
    }


    /**
     * cleanup operations, when camera view stops
     *
     */
    public void onCameraViewStopped() {
        mRgba.release();
        mGray.release();
        mIntermediateMat.release();

        mHsv.release();
        mHsvOriginal.release();
        mHsvThreshed.release();
        mRgbaMask.release();
    }


    /**
     * starting point for every frame, arranges steps depending on the active mode
     *
     * Input : inputFrame        contains data from the live frame of the camera
     * Output :                  returns rgba frame as a result of the frame handling processes
     *                          to display it in the next step
     */
    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
        if (!freezeCamera) {
            // input frame has RBGA format
            mRgba = inputFrame.rgba();
            Imgproc.cvtColor(mRgba, mHsvOriginal, Imgproc.COLOR_RGB2HSV_FULL);

            // execute fruit detection, if at least one fruit type is saved
            for (FruitType ft : fruitTypes)
                detectFruitType(ft);

            // handle detected fruits and clear the list afterwards
            if (detectedFruits.size() > 0)
                for (Fruit fruits : detectedFruits)
                    handleDetectedFruit(fruits);
            detectedFruits.clear();
        }
        return mRgba;
    }

    /**
     * handle onTouch events (such as displaying nutritional values to a touched fruit)
     *
     * Input :
     * arg0      standard transfer variable
     * event     standard transfer variable, which contains information about the touch event
     *
     */
    @Override
    public boolean onTouch(View arg0, MotionEvent event) {
        // variable declaration
        double[] coordinates = new double[2];
        Point touchedPoint;

        // define size of mRgba
        double cols = mRgba.cols();// mRgba is your image frame
        double rows = mRgba.rows();

        // determine size of used screen and define offset of mRgba frame
        int width = mOpenCvCameraView.getWidth();
        int height = mOpenCvCameraView.getHeight();
        double scaleFactor = cols / width;
        double xOffset = (width * scaleFactor - cols) / 2;
        double yOffset = (height * scaleFactor - rows) / 2;

        // temporary variable for conversion tasks
        MatOfPoint2f mMOP2f = new MatOfPoint2f();

        // determine coordinates of touched point in mRgba frame and create a Point for the result
        coordinates[0] = (event).getX() * scaleFactor - xOffset;
        coordinates[1] = (event).getY() * scaleFactor - yOffset;
        touchedPoint = new Point(coordinates[0], coordinates[1]);

        //// check depending on detection scale if a touch occured within the contour of a fruit
        // convert contour to needed format
        for(LinkedList<Fruit> currentFruitList : fruitTracker)
            if (currentFruitList.size() > 0) {
                if (currentFruitList.getLast() != null) {
                    currentFruitList.getLast().getContour().convertTo(mMOP2f, CvType.CV_32FC2);

                    // toggle, that information should be displayed, since a touch
                    // occured within the contour
                    if (Imgproc.pointPolygonTest(mMOP2f, touchedPoint, false) > 0) {
                        // set, that information is desired, if it's not set - reset, if it's set
                        if (!currentFruitList.getLast().getIsInformationDisplayed())
                            currentFruitList.getLast().setIsInformationDisplayed(true);
                        else
                            currentFruitList.clear();
                    }
                }
            }

        return false;       //false: no subsequent events ; true: subsequent events
    }




    /**
     * initialize fruitTypes with saved values from database
     *
     */
    public void initializeFromDatabase() {
        // get database
        dbFruitTypes = new DbAccessFruitTypes(this);

        // get data if table already exists
        if (dbFruitTypes.isTableExisting(dbFruitTypes.TABLE_NAME_FRUIT_TYPES)) {
            // process data
            fruitTypes = dbFruitTypes.getAllData();
        }

        // update fruit tracker
        for(FruitType ft : fruitTypes)
            fruitTracker.add(new LinkedList<Fruit>());
    }

    /**
     * executes the whole fruit detection process, shows histograms and mask in hsv mode
     *
     * Input : fruit         defines fruit type, for which the check occures
     */
    public void detectFruitType(FruitType ft) {
        // variable declaration for temporary saving
        Fruit detectedFruit = null;
        List<MatOfPoint> contours;
        MatOfPoint biggestContour;
        List<Mat> candidateHistograms;
        LinkedList<Fruit> currentFruitList = fruitTracker.get(fruitTypes.indexOf(ft));


        //// handle single detection --> use largest contour of fruit candidates
        // apply hsv filter from currently checked fruit type to screen
        getHsvFilterMask(ft.getHsvFilterValues());

        // find biggest contour in binary mask from hsv filtered frame
        // (uses: mHsvThreshed for contour searching, without modifying it (uses clone))
        contours = findContoursAboveThreshold();

        // reject further actions if there's no contour within the bottom and top threshold
        if (contours.size() > 0) {
            // get the biggest contour
            biggestContour = contours.get(0);

            //// apply the biggest contour as a new mask to reseted mask
            // reset needed mats
            Core.setIdentity(mHsvThreshed, new Scalar(0, 0, 0));
            mRgba.copyTo(mIntermediateMat);

            // draw contour
            Imgproc.drawContours(mHsvThreshed,
                    new ArrayList<>(Arrays.asList(biggestContour)), 0, mWhite, -1);

            // convert to rgba mask and apply mask to rgb frame
            Imgproc.cvtColor(mHsvThreshed, mRgbaMask, Imgproc.COLOR_GRAY2RGBA, 4);
            Core.bitwise_and(mIntermediateMat, mRgbaMask, mIntermediateMat);

            // calculate the histograms from mIntermediateMat
            candidateHistograms = calcHsvHistograms();

            // create new fruit with detected type and accompanying contour and handle it
            if (compareHsHistograms(ft, candidateHistograms)) {
                detectedFruit = new Fruit(ft, biggestContour);

                // information should be displayed, when the detected type appeared in the last
                // few frames (according to FRUIT_TRACKER_FRAME_RANGE)
                if (currentFruitList.size() > 0)
                    for (Fruit f : currentFruitList)
                        if (f != null)
                            if (f.getIsInformationDisplayed() && (f.getType().getName()
                                    .compareToIgnoreCase(ft.getName()) == 0))
                                detectedFruit.setIsInformationDisplayed(true);

                // save found object in tracker to remember the state (fifo list)
                if (currentFruitList.size() < FRUIT_TRACKER_FRAME_RANGE)
                    currentFruitList.add(detectedFruit);
                else {
                    currentFruitList.removeFirst();
                    currentFruitList.add(detectedFruit);
                }

                //save detected fruit
                detectedFruits.add(detectedFruit);

                // clear object of detected fruit, so it's clean for the next frame
                detectedFruit = null;
            } else {
                // put null as an object into the list, so it can act as a counter for frames
                // with abandoned candidates
                if (currentFruitList.size() < FRUIT_TRACKER_FRAME_RANGE)
                    currentFruitList.add(null);
                else {
                    currentFruitList.removeFirst();
                    currentFruitList.add(null);
                }
            }
        } else {
            // put null as an object into the list, so it can act as a counter for frames
            // without detected objects of this fruit type
            if (currentFruitList.size() < FRUIT_TRACKER_FRAME_RANGE)
                currentFruitList.add(null);
            else {
                currentFruitList.removeFirst();
                currentFruitList.add(null);
            }
        }
    }


    /**
     * applies defined filter values to hsv image, copies result to intermediate mat
     * also be done in the background - another result is a binary mask
     * (mask is saved in mHsvThreshed)
     * (modified global variable: mIntermediateMat gets original rgba values)
     * (modified global variable: mHsv gets hsv values from original rgba values)
     * (modified global variable: mHsvThreshed gets mask from hsv image with
     *      applied hsv filter values)
     *
     * Input : hsvFilterValues       defines the min/max values for h, s and v filtering
     */
    public void getHsvFilterMask(int[] hsvFilterValues) {
        // create elements for morphing operations
        Mat mErodeElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, ERODE_SIZE);
        Mat mDilateElement = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, DILATE_SIZE);

        // save mRgba value (live screen) temporary in mIntermediateMat, to use the latter here
        mHsvOriginal.copyTo(mHsv);

        // apply hsv filter values to hsv frame
        Core.inRange(mHsv,
                new Scalar(hsvFilterValues[0], hsvFilterValues[1], hsvFilterValues[2]),
                new Scalar(hsvFilterValues[3], hsvFilterValues[4], hsvFilterValues[5]),
                mHsvThreshed);

        //// erode, dilate and blur frame to get rid of interference
        // erode and dilate in each two iterations
        Imgproc.erode(mHsvThreshed, mHsvThreshed, mErodeElement);
        Imgproc.erode(mHsvThreshed, mHsvThreshed, mErodeElement);
        Imgproc.dilate(mHsvThreshed, mHsvThreshed, mDilateElement);
        Imgproc.dilate(mHsvThreshed, mHsvThreshed, mDilateElement);
    }


    /**
     * calculates hsv histograms and feedback graph and puts the feedback graph of the color
     * histograms (hue, saturation) into a copy of mRgba (mIntermediateMat), so it can also
     * be done in the background without affecting the live frame
     * (needs rgba copy in mIntermediateMat, which is converted to mHsv)
     *
     */
    public List<Mat> calcHsvHistograms() {
        // declaration and initialization of variables
        float highestValue;
        float mBuff[] = new float[HISTOGRAM_SIZE];
        Mat hist = new Mat(HISTOGRAM_SIZE, 1, CvType.CV_32F);
        List<Mat> resultHistograms = new ArrayList<>();

        // get hsv mat from mat buffer (intermediate mat --> frame with applied mask)
        Imgproc.cvtColor(mIntermediateMat, mHsv, Imgproc.COLOR_RGB2HSV_FULL);

        //// calculate and display histogram for hue
        Imgproc.calcHist(Arrays.asList(mHsv), mChannels[0], mMat0, hist, mHistSize, mRanges);

        //// normalize, so the values are [0..1] and reset the first value, when it's at maximum
        //// (this happens, when the mask is applied, since a lot of area is black)
        Core.normalize(hist, hsvHistograms.get(0), 1, 0, Core.NORM_INF);
        hsvHistograms.get(0).get(0, 0, mBuff);

        // find out highest value
        highestValue = 0;
        for (float d : mBuff)
            if (d > highestValue) highestValue = d;

        // reset first value to zero, if it's the highest value, or trim it to zero
        if (mBuff[0] == highestValue) {
            // reset the freak value (comes from the mask probably)
            mBuff[0] = 0;

            // update histogram and normalize it again
            hsvHistograms.get(0).put(0, 0, mBuff);
            Core.normalize(hsvHistograms.get(0), hsvHistograms.get(0), 1, 0, Core.NORM_INF);
        }

        //// calculate and display histogram for saturation
        Imgproc.calcHist(Arrays.asList(mHsv), mChannels[1], mMat0, hist, mHistSize, mRanges);

        // normalize, so the values are [0..1] and reset the first value, when it's at maximum
        // (this happens, when the mask is applied, since a lot of area is black)
        Core.normalize(hist, hsvHistograms.get(1), 1, 0, Core.NORM_INF);
        hsvHistograms.get(1).get(0, 0, mBuff);

        // find out highest value
        highestValue = 0;
        for (float d : mBuff)
            if (d > highestValue) highestValue = d;

        // reset first value to zero, if it's the highest value, or trim it to zero
        if (mBuff[0] == highestValue) {
            mBuff[0] = 0;       //reset

            // update histogram and normalize it again
            hsvHistograms.get(1).put(0, 0, mBuff);
            Core.normalize(hsvHistograms.get(1), hsvHistograms.get(1), 1, 0, Core.NORM_INF);
        }

        // clone calculated histograms and return them as a result
        for (Mat m : hsvHistograms)
            resultHistograms.add(m.clone());

        return resultHistograms;
    }

    /**
     * find closest match to picture --> hs color histogram comparison with specified method
     * Input : FruitType, histograms
     * Output :  returns the detected FruitType, which is most likely the captured fruit
     */
    public boolean compareHsHistograms(FruitType ft, List<Mat> candidateHistograms) {
        double highestComparisonValueH = 0;
        double highestComparisonValueS = 0;
        boolean isFruitType = false;

        // find closest match to picture --> hs color histogram comparison with chi square method
        // histogram comparison for the currently checked fruit type
        double comparisonValueH = 0;
        double comparisonValueS = 0;

        // check if all histograms are available
        if (ft.getHsvHistogramH() != null && ft.getHsvHistogramS() != null
                && candidateHistograms.get(0) != null && candidateHistograms.get(1) != null) {

            // compare histograms with intersect method
            comparisonValueH = Imgproc.compareHist(ft.getHsvHistogramH(),
                    candidateHistograms.get(0), Imgproc.CV_COMP_CORREL);
            comparisonValueS = Imgproc.compareHist(ft.getHsvHistogramS(),
                    candidateHistograms.get(1), Imgproc.CV_COMP_CORREL);

            if (comparisonValueH > highestComparisonValueH) {
                highestComparisonValueH = comparisonValueH;
            }
            if (comparisonValueS > highestComparisonValueS) {
                highestComparisonValueS = comparisonValueS;
            }
        }

        if ( (highestComparisonValueH >= HISTOGRAM_COMPARISON_THRESHOLD_HUE_MIN
                && highestComparisonValueH <= HISTOGRAM_COMPARISON_THRESHOLD_HUE_MAX )
                || (highestComparisonValueS >= HISTOGRAM_COMPARISON_THRESHOLD_SATURATION_MIN
                && highestComparisonValueS <= HISTOGRAM_COMPARISON_THRESHOLD_SATURATION_MAX) ){
            isFruitType = true;
        }

        return isFruitType;
    }


    /*
     * finds biggest contour in mHsvThreshed (binary mask from hsv threshold filtering) above
     * the defined threshold, or even all contours above this threshold,
     * depending on the parameter isOnlyBiggestDesired
     *
     * Input : isOnlyBiggestDesired      defines, if the task is to find the biggest contour or all
     *                                  above the threshold
     * Output :                          list of contours with biggest contour only are all above
     *                                  the threshold
     */
    public List<MatOfPoint> findContoursAboveThreshold() {
        //// prepare variables for getting the contours
        //// out of the binary image (filter threshold result)
        // general variables
        List<MatOfPoint> results = new ArrayList<>();
        Mat temp = new Mat();       // needed, since original mat is modified

        // contour specific variables
        List<MatOfPoint> contours = new ArrayList<>();
        MatOfPoint biggestContour = new MatOfPoint();

        // temporary variable for conversion tasks
        MatOfPoint2f mMOP2f1 = new MatOfPoint2f();
        MatOfPoint2f mMOP2f2 = new MatOfPoint2f();

        // apply hsv filter values from each registered fruit type and clone the resulting
        // binary mask into temporary Mat for further processing
        mHsvThreshed.copyTo(temp);

        // find all contours in clone of binary mask
        Imgproc.findContours(temp, contours, new Mat(), Imgproc.RETR_EXTERNAL,
                Imgproc.CHAIN_APPROX_SIMPLE);

        // security measures for the case of no found contours
        if (contours.size() > 0) {
            // walk through all contours
            for (int i = 0; i < contours.size(); i++) {
                // operations, if only the biggest contour is desired
                // save index, if the contour is the biggest yet
                if (i == 0)
                    biggestContour = contours.get(i);
                else if (Imgproc.contourArea(contours.get(i))
                        > Imgproc.contourArea(biggestContour)
                        && Imgproc.contourArea(contours.get(i)) > CONTOUR_THRESHOLD_BOTTOM
                        && Imgproc.contourArea(contours.get(i)) < CONTOUR_THRESHOLD_TOP)
                    biggestContour = contours.get(i);
            }

            //// handle found contours
            // if there was found one above the threshold size: smooth and save the found contour
            if (Imgproc.contourArea(biggestContour) > CONTOUR_THRESHOLD_BOTTOM
                    && Imgproc.contourArea(biggestContour) < CONTOUR_THRESHOLD_TOP) {

                // convert mats of contour and smooth contour
                biggestContour.convertTo(mMOP2f1, CvType.CV_32FC2);
                Imgproc.approxPolyDP(mMOP2f1, mMOP2f2, APPROX_DISTANCE, true);
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
        // variable declaration
        Size textSize;
        int labelLine2Width;

        Point start;
        Point p1;
        Point p2;
        final Point locationName;
        Point locationFurtherInformation;

        List<String> furtherInformation = new ArrayList<>();
        String format = "%3.1f";  // width = 3 and 2 digits after the dot
        NutritionalValue nutrition_values;

        String user_hazard_input , health_hazard_info;
        Boolean found;

        // safety measurement
        if (fruit != null) {
            // variable initialization
            textSize = Imgproc.getTextSize(fruit.getType().getName(), TEXT_FONT_FACE, TEXT_FONT_SCALE,
                    TEXT_THICKNESS, null);
            labelLine2Width = (int) (textSize.width
                    + SPACE_BUFFER_TEXT_FRONT + SPACE_BUFFER_TEXT_END);
            start =  fruit.getLocationLabel().clone();

            // dig line of label into object (otherwise it could float)
            start.x -= 8;
            start.y -= 0;

            // calculate points for the label lines and the text
            p1 = new Point(start.x + LABEL_LINE_1_WIDTH, start.y - LABEL_LINE_1_HEIGHT);
            p2 = new Point(p1.x + labelLine2Width, p1.y);
            locationName = new Point(p1.x + SPACE_BUFFER_TEXT_FRONT, p1.y - TEXT_MARGIN);

            // draw lines of label
            Imgproc.line(mRgba, start, p1, fruit.getType().getMarkerColor(),
                    LABEL_LINE_1_THICKNESS);
            Imgproc.line(mRgba, p1, p2, fruit.getType().getMarkerColor(), LABEL_LINE_2_THICKNESS);

            // draw text of label with name of detected fruit type

            // redirect to the new page with further information
            if(fruit.getIsInformationDisplayed()){
                Intent intent = new Intent(this, Display.class)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                intent.putExtra("name" , fruit.getType().getName());
                startActivity(intent);
                finish();
            }
            // nutrition information
            nutrition_values = fruit.getType().getNutritionalValue();

            sharedPreferences = getSharedPreferences("MyPREFERENCES", Context.MODE_PRIVATE);

            user_hazard_input = sharedPreferences.getString("userHealthHazard" , "");
            health_hazard_info = nutrition_values.getHealthHazardInformation();


            String name_shown = fruit.getType().getName();
            found = health_hazard_info.contains(user_hazard_input);

            //if hazard matches with user hazards show an exclamation mark along with the fruit name
            if(found&&user_hazard_input!=""){
                name_shown = name_shown + " !!";
            }
            Imgproc.putText(mRgba, name_shown, locationName,
                    TEXT_FONT_FACE, TEXT_FONT_SCALE, fruit.getType().getMarkerColor(),
                    TEXT_THICKNESS, TEXT_LINE_TYPE, false);

            furtherInformation.add("" + (int) (nutrition_values.getCaloricValuePer100g() *
                    nutrition_values.getAverageWeightServing()/100) + " kcal");

            furtherInformation.add("Protein: " + String.format(Locale.ENGLISH, format,
                    (nutrition_values.getProteinContentPer100g() *
                            nutrition_values.getAverageWeightServing()/100)) + "g");

            furtherInformation.add("Fat: " + String.format(Locale.ENGLISH, format,
                    (nutrition_values.getFatContentPer100g() *
                            nutrition_values.getAverageWeightServing()/100)) + "g");

            // prepare location for showing nutrition info of food detected
            locationFurtherInformation = new Point(p1.x, p1.y + TEXT_MARGIN + textSize.height);

            // check if any text was prepared yet before drawing it
            if (!furtherInformation.isEmpty()) {
                for(int i=0; i < furtherInformation.size(); i++) {
                    Imgproc.putText(mRgba, furtherInformation.get(i),
                            new Point(locationFurtherInformation.x,
                                    locationFurtherInformation.y
                                            + i*(textSize.height + TEXT_MARGIN)),
                            TEXT_FONT_FACE, TEXT_FONT_SCALE, fruit.getType().getMarkerColor(),
                            TEXT_THICKNESS, TEXT_LINE_TYPE, false);
                }
            }
        }
    }
}