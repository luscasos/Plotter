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

    float lastX=0;

    Handler handler;

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

        listaTemperaturas = new ArrayList<>();

        connection=this;

        parearButton = findViewById(R.id.parearButton);
        OKButton = findViewById(R.id.OKButton);

        GraphView graph = findViewById(R.id.graph);
        series = new LineGraphSeries<>();
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
                    Intent intent = new Intent(getApplicationContext(),service.class);
                    intent.putExtra("finalizar",true);
                    conectado=false;
                    startService(intent);
                }else{
                    //conectar
                    Intent abrelista = new Intent(getApplicationContext(), ListaDispositivos.class);
                    startActivityForResult(abrelista,conectionRequest);
                }
            }
        });

        //chamo um método para o tratamento da mensagem
        //e melhor organização do código.
        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                //chamo um método para melhor organização.

                //defino no meu TextView o texto.
                //textView.setText(texto);
                //int temp = Integer.parseInt(texto);
                try{
                    float temp = (float) msg.obj;
                    series.appendData(new DataPoint(lastX,temp),true,1000);
                    lastX= (float) (lastX+0.5);
                }catch (Exception ignored){

                }

            }
        };

        //Thread responsável pelo processamento de dados.
        //O handler é passado para que sejá possível atualizar a tela

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

    public void onButtonClick(View v) {
        if (mBound) {
            // Call a method from the LocalService.
            // However, if this call were something that might hang, then this request should
            // occur in a separate thread to avoid slowing down the activity performance.
            float num = mService.getTemp();
            Toast.makeText(this, "number: " + num, Toast.LENGTH_SHORT).show();
        }
    }

    protected void onActivityResult(int requestCode,int resultCode, Intent data){
        switch (requestCode){
            case conectionRequest:
                if (resultCode == Activity.RESULT_OK){
                    String MAC = Objects.requireNonNull(data.getExtras()).getString(ListaDispositivos.ENDERECO_MAC);
                    //Toast.makeText(this, "MAC final" + MAC, Toast.LENGTH_LONG).show();
                    ThreadProcessamento threadProcessamento = new ThreadProcessamento(handler);
                    threadProcessamento.start();
                    conectado=true;
                    Intent intent = new Intent(this,service.class);
                    intent.putExtra("MAC",MAC);
                    intent.putExtra("finalizar",false);
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
        //unbindService(connection);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        // We've bound to LocalService, cast the IBinder and get LocalService instance
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

        public ThreadProcessamento(Handler handler) {
            this.handler = handler;
        }

        @Override
        public void run() {
            float temp=0;
            while (conectado) {
                Message message = new Message();
                //defino um codigo para controle.

                float num=0;
                message.what = 1;
                if (mBound) {
                    // Call a method from the LocalService.
                    // However, if this call were something that might hang, then this request should
                    // occur in a separate thread to avoid slowing down the activity performance.
                    num = mService.getTemp();
                    message.obj =num;
                    //Toast.makeText(this, "number: " + num, Toast.LENGTH_SHORT).show();
                }
                if(num!=temp){
                    //Envio da mensagem.
                    handler.sendMessage(message);
                    temp=num;
                }

                try {
                    //simula processamento de 1seg
                    Thread.sleep(100);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }
    }
}
