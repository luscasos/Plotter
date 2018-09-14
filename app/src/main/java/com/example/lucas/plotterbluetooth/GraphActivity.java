package com.example.lucas.plotterbluetooth;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.media.MediaScannerConnection;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.DataPointInterface;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.OnDataPointTapListener;
import com.jjoe64.graphview.series.Series;
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;


public class GraphActivity extends AppCompatActivity implements ServiceConnection {

    private static final int conectionRequest = 2;
    private static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 3;
    boolean conectado=false;

    service mService;
    boolean mBound = false;

    ThreadProcessamento threadProcessamento;

    Handler handler;
    Intent intentService;

    TextView textView;

    private ServiceConnection connection;

    ArrayList<temperaturasBluetooth> listaTemperaturas=null;

    LineGraphSeries<DataPoint> series;
    LineGraphSeries<DataPoint> series2;
    @SuppressLint("SimpleDateFormat")
    SimpleDateFormat sdf=new SimpleDateFormat("mm:ss");
    @SuppressLint("SimpleDateFormat")
    SimpleDateFormat sdf2=new SimpleDateFormat("hh:mm:ss");
    @SuppressLint("SimpleDateFormat")
    SimpleDateFormat sdf3=new SimpleDateFormat("HH:mm dd-MM-yyyy");

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph);

        listaTemperaturas = new ArrayList<>();

        connection=this;
        Boolean iniciado = false;

        GraphView graph = findViewById(R.id.graph);
        series = new LineGraphSeries<>();
        graph.addSeries(series);


        graph.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter(){
            @Override
            public String formatLabel(double value, boolean isValueX) {
                if(isValueX){
                    return sdf.format(new Date((long) value));
                }else{
                    return super.formatLabel(value, isValueX);
                }

            }
        });

        Calendar calendar = Calendar.getInstance();
        Date date = calendar.getTime();
        long longDate=date.getTime();
        graph.getGridLabelRenderer().setNumHorizontalLabels(5);
        graph.getGridLabelRenderer().setHumanRounding(false);

        Viewport viewport = graph.getViewport();
        viewport.setXAxisBoundsManual(true);
        viewport.setScalable(true);
        viewport.setMinX(longDate);
        viewport.setMaxX(longDate+60000);
        viewport.setMinY(0);
        viewport.setMaxY(20);

        series2 = new LineGraphSeries<>();  // escala secundaria
        graph.getSecondScale().addSeries(series2);
        graph.getSecondScale().setMinY(0);
        graph.getSecondScale().setMaxY(2);
        series2.setColor(Color.RED);
        graph.getGridLabelRenderer().setVerticalLabelsSecondScaleColor(Color.RED);


        series.setOnDataPointTapListener(new OnDataPointTapListener() {
            @Override
            public void onTap(Series series, DataPointInterface dataPoint) {
                Date d = new Date((long)dataPoint.getX());
                Toast.makeText(getBaseContext(), sdf2.format(d)+", Temperatura:"+dataPoint.getY(), Toast.LENGTH_SHORT).show();
            }
        });

        intentService  = getIntent();
        if(intentService!=null){

            handler = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    try{
                        float temp = (float) msg.obj;

                        Calendar calendar = Calendar.getInstance();
                        Date date = calendar.getTime();

                        listaTemperaturas.add(new temperaturasBluetooth(date,temp));
                        series.appendData(new DataPoint(date,temp),true,1000);
                        series2.appendData(new DataPoint(date,Math.log10(temp)),true,1000);

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
                    //Log.d("Reconstruindo",dado.getX()+""+dado.getY());
                    series.appendData(new DataPoint(dado.getX(),dado.getY()),true,1000);
                    series2.appendData(new DataPoint(dado.getX(),Math.log10(dado.getY())),true,1000);
                    if (i==0){
                        viewport.setMinX(dado.getX().getTime());
                        viewport.setMaxX(dado.getX().getTime()+60000);
                    }
                }

                threadProcessamento = new ThreadProcessamento(handler);
                threadProcessamento.start();
            }
        }
        Log.d("Iniciado ",""+iniciado);

        if(!iniciado){
            initGraph();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //Log.d("onResume ",""+true);
        Intent intent = getIntent();
        //Log.d("Intent ",""+intent);

    }

    @Override
    protected void onRestart() {
        super.onRestart();
        //Log.d("onRestart ",""+true);
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

    // iniciando ActionBar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    // Opções da ActionBar
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.primeiroBotao:
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
                return true;
            case R.id.segundoBotao:

                // Checa a permissão para escrita externa

                if (ContextCompat.checkSelfPermission(GraphActivity.this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {

                    // Should we show an explanation?
                    if (ActivityCompat.shouldShowRequestPermissionRationale(GraphActivity.this,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                        // Show an expanation to the user *asynchronously* -- don't block
                        // this thread waiting for the user's response! After the user
                        // sees the explanation, try again to request the permission.

                    } else {

                        // No explanation needed, we can request the permission.
                        ActivityCompat.requestPermissions(GraphActivity.this,
                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);

                        // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                        // app-defined int constant. The callback method gets the
                        // result of the request.
                    }
                }

                int permissionCheck = ContextCompat.checkSelfPermission(GraphActivity.this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE);

                if(permissionCheck == PackageManager.PERMISSION_GRANTED ){
                    Calendar calendar = Calendar.getInstance();
                    Date date = calendar.getTime();
                    CSV csv= new CSV();
                    csv.FileWriter(this,sdf3.format(date)+".csv",listaTemperaturas);

                }else{
                    Toast.makeText(this, "Você deve permitir a escrita para salvar arquivos", Toast.LENGTH_SHORT).show();
                }

                return true;
            default:
                return super.onOptionsItemSelected(item);
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
                    Thread.sleep(100);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            Log.d("Finalizando thread gph",""+true);

        }
    }
}
