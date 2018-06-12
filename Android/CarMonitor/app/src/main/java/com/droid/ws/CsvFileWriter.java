package com.droid.ws;

import android.app.Application;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.media.MediaScannerConnection;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;

import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import static com.droid.ws.Sensor.MAX_KNOWN_SENSORS;

public class CsvFileWriter {

    private static File root;
    private static File finalFolder;
    private static File csvFile;
    private static FileOutputStream fStream;
    private static PrintWriter printWriter;

    private static String baseFileName = "rawdata";
    private static String fileNameExtension = ".csv";
    private static String folderName = "CarMonitor";
    private static FileWriter fileWriter = null;
    private static String fileName;

    // function opens file and printwriter and leaves them open
    public static void CreateCsvFile(Context appContext) {

        Calendar calender;
        SimpleDateFormat simpledateformat;
        String Date;
        String path;

        root = android.os.Environment.getExternalStorageDirectory();
        //tv.append("\nExternal file system root: "+root);
        // See http://stackoverflow.com/questions/3551821/android-write-to-sd-card-folder

        path = root.getAbsolutePath() + "/" + folderName;
        finalFolder = new File (path);
        finalFolder.mkdirs();

        calender = Calendar.getInstance();
        simpledateformat = new SimpleDateFormat("yyyy-MM-dd_HHmmss");
        Date = simpledateformat.format(calender.getTime());
        fileName = baseFileName + "_" + Date + fileNameExtension;

        csvFile = new File(finalFolder, fileName);
        // NOTE: the file ends up here: Computer\Xperia Z5 Compact\Intern delt lagerplads\CarMonitor (i.e. on internal storage, not the SD card)

        // make the file readable from a PC via USB
        MediaScannerConnection.scanFile(
                appContext,
                new String[] { csvFile.toString() }, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) {
                        System.out.println("ExternalStorage Scanned " + path + ":");
                    }
            });

        try {
            fStream = new FileOutputStream(csvFile);  //TODO: If file already exists, just open it. Dont overwrite
            printWriter = new PrintWriter(fStream);
        } catch (FileNotFoundException e) {
            e.printStackTrace(); // Did you add a WRITE_EXTERNAL_STORAGE permission to the   manifest?
        }

        // define and write header line
        String line = "UTC";
        for (int i=0;i<MAX_KNOWN_SENSORS;i++) {
            line = line + ", " + Sensor.knownSensors[i] + "-A, " + Sensor.knownSensors[i] + "-B";
        }
        printWriter.println(line);
        printWriter.flush();
    }

    public static void AppendValueToFile(Sensor s) {

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

        printWriter.close();
        try {
            fStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            //Log.i(TAG, "******* File not found. Did you add a WRITE_EXTERNAL_STORAGE permission to the   manifest?");
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    public static void copyFileEasy(File sourceFile, File destFile) throws IOException {
        if(!destFile.exists()) {
            destFile.createNewFile();
        }

        FileChannel source = null;
        FileChannel destination = null;

        try {
            source = new FileInputStream(sourceFile).getChannel();
            destination = new FileOutputStream(destFile).getChannel();
            destination.transferFrom(source, 0, source.size());
        }
        finally {
            if(source != null) {
                source.close();
            }
            if(destination != null) {
                destination.close();
            }
        }
    }

    public static void copyFileAllBytesCopy(File src, File dst, Context appContext) throws IOException {
        InputStream in = new FileInputStream(src);
        try {
            OutputStream out = new FileOutputStream(dst);
            MediaScannerConnection.scanFile(appContext,
                    new String[] { dst.toString() }, null,
                    new MediaScannerConnection.OnScanCompletedListener() {
                        public void onScanCompleted(String path, Uri uri) {
                            System.out.println("ExternalStorage Scanned " + path + ":");
                        }
                    });

            try {
                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
    }


}

