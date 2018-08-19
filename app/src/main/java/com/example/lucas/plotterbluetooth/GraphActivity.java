package com.example.lucas.plotterbluetooth;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.ArrayList;
import java.util.Objects;


public class GraphActivity extends AppCompatActivity implements ServiceConnection {

    private static final int conectionRequest = 2;
    boolean conectado=false;

    Button parearButton;
    Button OKButton;

    private ServiceConnection connection;

    private Templistiner temp;

    ArrayList<temperaturasBluetooth> listaTemperaturas=null;


    LineGraphSeries<DataPoint> series;

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph);

        connection = this;

        listaTemperaturas = new ArrayList<temperaturasBluetooth>();


        parearButton = findViewById(R.id.parearButton);
        OKButton = findViewById(R.id.OKButton);

        GraphView graph = findViewById(R.id.graph);
        series = new LineGraphSeries<DataPoint>();
        graph.addSeries(series);


        Viewport viewport = graph.getViewport();
        viewport.setScalable(true);
        viewport.setMinX(0);
        viewport.setMinY(0);
        viewport.setMaxX(20);
        viewport.setXAxisBoundsManual(true);

        parearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(conectado){
                    //desconectar
                    /*try{
                        meuSocket.close();
                        conectado = false;
                        parearButton.setText(R.string.Conectar);
                        Toast.makeText(getApplicationContext(), "Bluetooth desconectado", Toast.LENGTH_LONG).show();
                    }catch (IOException erro){
                        Toast.makeText(getApplicationContext(), "Ocorreu um erro", Toast.LENGTH_LONG).show();
                    }*/
                }else{
                    //conectar

                    Intent abrelista = new Intent(getApplicationContext(), ListaDispositivos.class);
                    startActivityForResult(abrelista,conectionRequest);
                }
            }
        });
    }

    public void printTemp(View view){
        super.onResume();
      //  bindService(new Intent(this,service.class),connection,0);
       // Toast.makeText(getApplicationContext(), "TEMP na graph activity"+temp.getTemplistiner(), Toast.LENGTH_SHORT).show();

    }

    protected void onActivityResult(int requestCode,int resultCode, Intent data){
        switch (requestCode){
            case conectionRequest:
                if (resultCode == Activity.RESULT_OK){
                    String MAC = Objects.requireNonNull(data.getExtras()).getString(ListaDispositivos.ENDERECO_MAC);
                    //Toast.makeText(this, "MAC final" + MAC, Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(this,service.class);
                    intent.putExtra("MAC",MAC);
                    startService(intent);

                }else{
                    Toast.makeText(this, "Falha ao obter MAC", Toast.LENGTH_SHORT).show();
                }
        }
    }

    public void addEntry(View view){
        int n = listaTemperaturas.size();
        int i;
        float x,y;
        for (i=0; i<n; i++) {
            temperaturasBluetooth dado = listaTemperaturas.get(i);
            x=dado.getX();
            y=dado.getY();
            Toast.makeText(this,"X = "+x +"Y = "+y, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
         com.example.lucas.plotterbluetooth.service.Controller controller = (com.example.lucas.plotterbluetooth.service.Controller)service;
         temp= controller.getTemplistiner();
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
       // unbindService(connection);
    }
}
