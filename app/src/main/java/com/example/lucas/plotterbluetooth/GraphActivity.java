package com.example.lucas.plotterbluetooth;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
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

    service mService;
    boolean mBound = false;

    ThreadProcessamento threadProcessamento;

    float lastX=0;

    Handler handler;
    Intent intentService;

    Button parearButton;
    Button OKButton;
    TextView textView;

    private ServiceConnection connection;

    ArrayList<temperaturasBluetooth> listaTemperaturas=null;

    LineGraphSeries<DataPoint> series;

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph);

        listaTemperaturas = new ArrayList<temperaturasBluetooth>();

        connection=this;
        Boolean iniciado = false;

        GraphView graph = findViewById(R.id.graph);
        series = new LineGraphSeries<>();
        graph.addSeries(series);

        Viewport viewport = graph.getViewport();
        viewport.setScalable(true);
        viewport.setMinX(0);
        viewport.setMinY(0);
        viewport.setMaxX(10);
        viewport.setMaxY(20);
        viewport.setXAxisBoundsManual(true);

        intentService  = getIntent();
        if(intentService!=null){

            handler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    try{
                        float temp = (float) msg.obj;
                        Log.d("Recebidos",temp+"");
                        series.appendData(new DataPoint(lastX,temp),true,1000);
                        lastX= (float) (lastX+0.5);
                    }catch (Exception ignored){
                    }
                }
            };

            conectado = intentService.getBooleanExtra("conectado",false);
            Log.d("conectado ",""+conectado);

            listaTemperaturas= (ArrayList<temperaturasBluetooth>) intentService.getSerializableExtra("graphValues");
            iniciado = intentService.getBooleanExtra("iniciado",false);
            if(conectado){

                int n = listaTemperaturas.size();
                for (int i=0; i<n; i++) {
                    temperaturasBluetooth dado = listaTemperaturas.get(i);
                    series.appendData(new DataPoint(dado.getX(),dado.getY()),true,1000);
                    lastX=dado.getX();
                }

                threadProcessamento = new ThreadProcessamento(handler);
                threadProcessamento.start();
            }
        }
        Log.d("Iniciado ",""+iniciado);

        if(!iniciado){
            initGraph();
        }

        parearButton = findViewById(R.id.parearButton);
        OKButton = findViewById(R.id.OKButton);

        parearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(conectado){
                    //desconectar
                    Intent intent = new Intent(getApplicationContext(),service.class);
                    intent.putExtra("operacao",1);
                    conectado=false;
                    startService(intent);
                }else{
                    //conectar
                    Intent abrelista = new Intent(getApplicationContext(), ListaDispositivos.class);
                    startActivityForResult(abrelista,conectionRequest);
                }
            }
        });


    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("onResume ",""+true);
        Intent intent = getIntent();
        Log.d("Intent ",""+intent);

    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d("onRestart ",""+true);
        initGraph();
    }

    void initGraph(){
        Intent intent = new Intent(this,service.class);
        intent.putExtra("operacao",0);
        startService(intent);
        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Bind to LocalService
        Intent intent = new Intent(this, service.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unbind from the service
        if (mBound) {
            unbindService(connection);
            mBound = false;
        }
    }

    protected void onActivityResult(int requestCode,int resultCode, Intent data){
        switch (requestCode){
            case conectionRequest:
                if (resultCode == Activity.RESULT_OK){
                    String MAC = Objects.requireNonNull(data.getExtras()).getString(ListaDispositivos.ENDERECO_MAC);
                    //Toast.makeText(this, "MAC final" + MAC, Toast.LENGTH_LONG).show();
                    threadProcessamento = new ThreadProcessamento(handler);
                    threadProcessamento.start();
                    conectado=true;
                    Intent intent = new Intent(this,service.class);
                    intent.putExtra("MAC",MAC);
                    intent.putExtra("operacao",2);
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
    protected void onDestroy() {
        super.onDestroy();
        conectado=false;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        com.example.lucas.plotterbluetooth.service.LocalBinder binder = (com.example.lucas.plotterbluetooth.service.LocalBinder) service;
        mService = binder.getService();
        mBound = true;
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        mBound = false;
    }

    public class ThreadProcessamento extends Thread {

        private Handler handler;

        ThreadProcessamento(Handler handler) {
            this.handler = handler;
        }

        @Override
        public void run() {
            float temp=0;
            while (conectado) {
                Message message = new Message();

                float num=0;
                message.what = 1;
                if (mBound) {
                    num = mService.getTemp();
                    message.obj =num;
                    //Toast.makeText(this, "number: " + num, Toast.LENGTH_SHORT).show();
                }
                if(num!=temp){
                    //Envio da mensagem.
                    handler.sendMessage(message);
                    //Log.d("Recebidos thread",temp+"");
                    temp=num;
                }

                try {
                    //simula processamento de 1seg
                    Thread.sleep(100);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Log.d("Finalizando thread gph",""+true);

        }
    }
}
