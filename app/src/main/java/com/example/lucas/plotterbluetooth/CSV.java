package com.example.lucas.plotterbluetooth;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.MediaScannerConnection;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.jjoe64.graphview.series.DataPoint;
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class CSV {

    @SuppressLint("SimpleDateFormat")
    SimpleDateFormat sdf=new SimpleDateFormat("hh:mm:ss");


    public void FileWriter(Context context,String fileName,ArrayList<temperaturasBluetooth> listaTemperaturas) {

        try{
            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            CSVWriter writer = new CSVWriter(new FileWriter(path.toString()+"/"+fileName));
            // feed in your array (or convert your data to an array)
            Date date = null;

            int n = listaTemperaturas.size();
            for (int i=0; i<n; i++) {
                temperaturasBluetooth dado = listaTemperaturas.get(i);
                date = dado.getX();
                String[] entries = (sdf.format(date)+"#"+dado.getY()).split("#");
                writer.writeNext(entries);
            }
            Log.d("caminho: ",path.toString());
            MediaScannerConnection.scanFile(context, new String[] {path.toString()+"/"+fileName}, null, null);
            Toast.makeText(context, "Arquivo salvo na pasta Downloads", Toast.LENGTH_SHORT).show();
            writer.close();
        }catch (IOException e){
            e.printStackTrace();
        }

    }
}