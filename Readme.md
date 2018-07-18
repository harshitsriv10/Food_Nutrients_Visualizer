## Info

Authors: Harshit Srivastava<br/>
         Aayush Sanjay Agarwal <br/>
         Akhil Chandra Panchumarthi <br/>

## Abstract

This tutorial describes the requirements and necessary steps to get the FoodUI
project working in Android Studio 2.1.2. FoodUI is a university project, which I realized within the master project VR-Lab from the master's degree program Human-Centered Computing at Reutlingen University in 2015/16.
It's an Android app for fruit detection and uses OpenCV for the image processing. The core task of the app is to detect fruits in the live camera frames. To detect fruits, you have to save them first. This learning process is realized with the hsv-mode within the app, which uses normalized hsv color histograms as a feature. There's a demo video available on Youtube: https://youtu.be/KSAPpJUC_74

## 1. Requirements

You need a few components in order to run this project:
* Android Studio 2.1.2:
https://developer.android.com/studio/index.html#downloads
* Android NDK R11c:
https://developer.android.com/ndk/downloads/index.html
* OpenCV 3.1 (for Android):
http://opencv.org/downloads.html
* FoodUI project:
https://github.com/DemkivV/FoodUI
* Disk space for the development tools and projects: 8GB

## 2. Installation

I recommend putting all the files for the android development into a mutual folder - in this tutorial I'll call the folder AndroidDevelopment. In the following explanations I will refer to this folder as a root folder.
Download the different components from the requirements, install Android Studio and extract the content from the Android NDK and OpenCV zip files to the folder AndroidDevelopment. Keep this folder in mind when defining the directory for Android Studio and the Android sdk in the Android Studio installation. Also, write down the path for the Android sdk and the extracted android ndk.
Now create a projects folder in your AndroidDevelopment and copy the FoodUI
project into it.


## 3. Testing the project

Now everything should be configured right and the project is ready to go. Eiter use your own smartphone - therefor you have to activate the developer tools and plug it in - or use the emulator within Android Studio. I tested it on my Sony Xperia Z3 with Android 6.0.1 and it works fine. Currently the settings in the gradles are as follows:

```
minSdkVersion 21
targetSdkVersion 23
```

If you want to run this app on a lower Android version, you have to change these
specifications accordingly to the desired sdk version in all gradle files. Though, keep in mind that the hardware requirements are tough with this implementation, so a modern smartphone is highly recommended.
