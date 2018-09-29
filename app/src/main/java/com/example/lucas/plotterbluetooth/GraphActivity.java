package com.example.lucas.plotterbluetooth;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.media.MediaScannerConnection;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GridLabelRenderer;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.DataPointInterface;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.OnDataPointTapListener;
import com.jjoe64.graphview.series.Series;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
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

    Viewport viewport;

    GraphView graph;
    boolean fInt = true;

    long longDate;
    float temp=0;

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
    SimpleDateFormat sdf2=new SimpleDateFormat("HH:mm:ss");
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

        graph = findViewById(R.id.graph);
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

        if(listaTemperaturas.size()>0){
            longDate=listaTemperaturas.get(0).getX().getTime();
        }else{
            longDate=date.getTime();
        }

        viewport = graph.getViewport();
        viewport.setXAxisBoundsManual(true);
        viewport.setYAxisBoundsManual(true);
        viewport.setMinX(longDate);
        viewport.setMaxX(longDate+60000);

        graph.getGridLabelRenderer().setHorizontalLabelsVisible(false);

        series2 = new LineGraphSeries<>();  // escala secundaria
        graph.getSecondScale().addSeries(series2);
        graph.getSecondScale().setMinY(0);
        graph.getSecondScale().setMaxY(2);
        series2.setColor(Color.RED);
        graph.getGridLabelRenderer().setVerticalLabelsSecondScaleColor(Color.RED);


        GridLabelRenderer glr = graph.getGridLabelRenderer();
        glr.setPadding(56); // should allow for 3 digits to fit on screen
        glr.setNumVerticalLabels(9);
        viewport.setYAxisBoundsManual(false);


        series.setOnDataPointTapListener(new OnDataPointTapListener() {
            @Override
            public void onTap(Series series, DataPointInterface dataPoint) {
                Date d = new Date((long)dataPoint.getX());
                Toast.makeText(getBaseContext(), "Hora:"+sdf2.format(d)+", Temperatura:"+dataPoint.getY(), Toast.LENGTH_SHORT).show();
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

                        listaTemperaturas.add(new temperaturasBluetooth(date, temp));
                        series.appendData(new DataPoint(date, temp), true, 60000);
                        series2.appendData(new DataPoint(date, Math.log10(temp)), true, 60000);
                        if (fInt) {
                            temperaturasBluetooth dado = listaTemperaturas.get(0);
                            viewport.setMinX(dado.getX().getTime());
                            longDate = dado.getX().getTime();
                            fInt = false;
                            int size = listaTemperaturas.size();
                            dado = listaTemperaturas.get(size - 1);
                            viewport.setMaxX(dado.getX().getTime() + 1);
                        }
                        viewport.setMinX(longDate);
                        viewport.setMinY(0);

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
                    series.appendData(new DataPoint(dado.getX(),dado.getY()),true,60000);
                    series2.appendData(new DataPoint(dado.getX(),Math.log10(dado.getY())),true,60000);
                    if (i==0){
                        longDate = dado.getX().getTime();
                        viewport.setMinX(dado.getX().getTime());
                        viewport.setMaxX(listaTemperaturas.get(n-1).getX().getTime());
                    }
                }
                temp=listaTemperaturas.get(n-1).getY();

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
                    Toast.makeText(this, "Desconectando...", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(this, "Você deve permitir a escrita para salvar arquivos", Toast.LENGTH_LONG).show();
                }

                return true;
            case R.id.terceiroBotao:

               // Toast.makeText(this, "Botão 3", Toast.LENGTH_SHORT).show();

                Intent it2 = new Intent(GraphActivity.this,service.class);
                it2.putExtra("operacao",3);
                startService(it2);

                listaTemperaturas= new ArrayList<>();

                GraphView graph = findViewById(R.id.graph);
                graph.removeAllSeries();
                graph.getSecondScale().removeAllSeries();

                Calendar calendar = Calendar.getInstance();
                Date date = calendar.getTime();
                longDate=date.getTime();

                series = new LineGraphSeries<>();
                graph.addSeries(series);
                series2 = new LineGraphSeries<>();  // escala secundaria
                graph.getSecondScale().addSeries(series2);
                series2.setColor(Color.RED);

                Viewport viewport = graph.getViewport();
                viewport.setMinX(longDate);
                viewport.setMaxX(longDate+1);
                series.setOnDataPointTapListener(new OnDataPointTapListener() {
                    @Override
                    public void onTap(Series series, DataPointInterface dataPoint) {
                        Date d = new Date((long)dataPoint.getX());
                        Toast.makeText(getBaseContext(), "Hora:"+sdf2.format(d)+", Temperatura:"+dataPoint.getY(), Toast.LENGTH_SHORT).show();
                    }
                });

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public Bitmap screenShot(View view) {
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap) {
            @Override
            public boolean isHardwareAccelerated() {
                return true;
            }
        };

// not it's work
        view.draw(canvas);
        return bitmap;
    }

    private void storeImage(Bitmap image) {
        File pictureFile = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        if (pictureFile == null) {
            Log.d("Error","creating media file, check storage permissions");// e.getMessage());
            return;
        }
        try {
            FileOutputStream fos = new FileOutputStream(pictureFile+"/print");
            image.compress(Bitmap.CompressFormat.PNG, 90, fos);
            fos.close();
        } catch (FileNotFoundException e) {
            Log.d("File not found: ", e.getMessage());
        } catch (IOException e) {
            Log.d("Error accessing file: ", e.getMessage());
        }
        MediaScannerConnection.scanFile(this, new String[] {pictureFile.toString()+"/print"}, null, null);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mService != null) {
            unbindService(connection);
        }
        conectado=false;
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        com.example.lucas.plotterbluetooth.service.LocalBinder binder = (com.example.lucas.plotterbluetooth.service.LocalBinder) service;
        mService = binder.getService();
        mBound = true;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    Calendar calendar = Calendar.getInstance();
                    Date date = calendar.getTime();
                    CSV csv= new CSV();
                    csv.FileWriter(this,sdf3.format(date)+".csv",listaTemperaturas);

                } else {
                    Toast.makeText(this, "Você deve permitir a escrita para salvar arquivos", Toast.LENGTH_SHORT).show();
                }
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
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
            float num;
            while (conectado) {
                Message message = new Message();

                message.what = 1;
                if (mBound) {
                    num = mService.getTemp();
                    message.obj =num;
                    //Toast.makeText(this, "number: " + num, Toast.LENGTH_SHORT).show();
                    if(num==-1000){
                        conectado=false;
                    }else if(num!=temp){
                    //Envio da mensagem.
                    handler.sendMessage(message);
                    //Log.d("Recebidos thread",temp+"");
                    temp=num;
                    }
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
