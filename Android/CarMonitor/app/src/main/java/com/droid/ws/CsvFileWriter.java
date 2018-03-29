package com.droid.ws;

import android.app.Application;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.media.MediaScannerConnection;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import static com.droid.ws.MainActivity.MAX_KNOWN_SENSORS;

public class CsvFileWriter {

    private static File root;
    private static File finalFolder;
    private static File file;
    private static FileOutputStream fStream;
    private static PrintWriter printWriter;


    private static String fileName = "rawdata.csv";
    private static String folderName = "CarMonitor";
    private static FileWriter fileWriter = null;

    // function opens file and printwriter and leaves them open
    public static void CreateCsvFile(String [] knownSensors, int MAX_KNOWN_SENSORS, Context appContext) {
        root = android.os.Environment.getExternalStorageDirectory();
        //tv.append("\nExternal file system root: "+root);
        // See http://stackoverflow.com/questions/3551821/android-write-to-sd-card-folder

        finalFolder = new File (root.getAbsolutePath() + "/" + folderName);
        finalFolder.mkdirs();
        file = new File(finalFolder, fileName);
        // NOTE: the file ends up here: Computer\Xperia Z5 Compact\Intern delt lagerplads\CarMonitor (i.e. on internal storage, not the SD card)

        try {
            fStream = new FileOutputStream(file);  //TODO: If file already exists, just open it. Dont overwrite
            printWriter = new PrintWriter(fStream);
        } catch (FileNotFoundException e) {
            e.printStackTrace(); // Did you add a WRITE_EXTERNAL_STORAGE permission to the   manifest?
        }

        // define and write header line
        String line = "UTC";
        for (int i=0;i<MAX_KNOWN_SENSORS;i++) {
            line = line + ", " + knownSensors[i] + "-A, " + knownSensors[i] + "-B";
        }
        printWriter.println(line);
        printWriter.flush();

        // make the file readable from a PC via USB
        MediaScannerConnection.scanFile(appContext,
                new String[] { file.toString() }, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
                        System.out.println("ExternalStorage Scanned " + path + ":");
                    }
                });

    }

    public static void AppendValueToFile(MainActivity.Sensor s) {

        String line;

        line = s.lastSampleTime.toString();

        // we only write the values of known, predefined sensors
        if (s.sensorID < MAX_KNOWN_SENSORS) {
            for (int i = 0; i < s.sensorID; i++) {
                line = line + ",,";
            }
            line = line + ", " + s.value1 + ", " + s.value2;
            try {
                printWriter.println(line);
                printWriter.flush();
                //fileWriter.append(line + "\n");
            } catch (Exception e) {
                System.out.println("Error in CsvFileWriter !!!");
                e.printStackTrace();
            }
        }
    }

    public static void CloseCsvFile(Context appContext) {
        try {
            printWriter.close();
            fStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            //Log.i(TAG, "******* File not found. Did you add a WRITE_EXTERNAL_STORAGE permission to the   manifest?");
        } catch (IOException e) {
            e.printStackTrace();
        }

        // make the file readable from a PC via USB
        MediaScannerConnection.scanFile(appContext,
                new String[] { file.toString() }, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
                        System.out.println("ExternalStorage Scanned " + path + ":");
                    }
                });

    }

    /** Method to check whether external media available and writable. This is adapted from
     http://developer.android.com/guide/topics/data/data-storage.html#filesExternal */

    public static boolean checkSDCard(){
        boolean mExternalStorageAvailable = false;
        boolean mExternalStorageWriteable = false;
        String state = Environment.getExternalStorageState();

        if (Environment.MEDIA_MOUNTED.equals(state)) {
            // Can read and write the media
            mExternalStorageAvailable = mExternalStorageWriteable = true;
            return true;
        } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            // Can only read the media
            mExternalStorageAvailable = true;
            mExternalStorageWriteable = false;
            return false;
        } else {
            // Can't read or write
            mExternalStorageAvailable = mExternalStorageWriteable = false;
            return false;
        }
        //tv.append("\n\nExternal Media: readable="
        //        +mExternalStorageAvailable+" writable="+mExternalStorageWriteable);
    }

    /** Method to write ascii text characters to file on SD card. Note that you must add a
     WRITE_EXTERNAL_STORAGE permission to the manifest file or this method will throw
     a FileNotFound Exception because you won't have write permission. */
/*    public static void writeToSDFile(){

        // Find the root of the external storage.
        // See http://developer.android.com/guide/topics/data/data-  storage.html#filesExternal

        File root = android.os.Environment.getExternalStorageDirectory();
        //tv.append("\nExternal file system root: "+root);

        // See http://stackoverflow.com/questions/3551821/android-write-to-sd-card-folder

        File dir = new File (root.getAbsolutePath() + "/CarMonitor");
        dir.mkdirs();
        File file = new File(dir, "myData.txt");
        // NOTE: the file actually ends up here: Computer\Xperia Z5 Compact\Intern delt lagerplads\CarMonitor (i.e. on internal storage, not the SD card)

        try {
            FileOutputStream f = new FileOutputStream(file);
            PrintWriter pw = new PrintWriter(f);
            pw.println("Hi , How are you");
            pw.println("Hello");
            pw.flush();
            pw.close();
            f.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //tv.append("\n\nFile written to "+file);
    }
*/

/*
    private void readRaw(){

        tv.append("\nData read from res/raw/textfile.txt:");
        InputStream is = this.getResources().openRawResource(R.raw.textfile);
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr, 8192);    // 2nd arg is buffer size

        // More efficient (less readable) implementation of above is the composite expression
    //BufferedReader br = new BufferedReader(new InputStreamReader(
    //        this.getResources().openRawResource(R.raw.textfile)), 8192);

        try {
            String test;
            while (true){
                test = br.readLine();
                // readLine() returns null if no more lines in the file
                if(test == null) break;
                tv.append("\n"+"    "+test);
            }
            isr.close();
            is.close();
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        tv.append("\n\nThat is all");
    }
*/


}

