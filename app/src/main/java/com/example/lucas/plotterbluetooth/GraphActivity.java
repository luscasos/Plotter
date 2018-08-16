package com.example.lucas.plotterbluetooth;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;
import java.util.UUID;

import static java.lang.Math.sin;


public class GraphActivity extends AppCompatActivity {

    private static final int conectionRequest = 2;
    private static final int MESSAGE_READ = 3;

    Handler mHandler;

    StringBuilder dadosRecebidos = new StringBuilder();

    BluetoothAdapter mBluetoothAdapter = null;
    BluetoothDevice meuDevice = null;
    BluetoothSocket meuSocket=null;
    UUID meuUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    ConnectedThread connectedThread;
    boolean conectado=false;
    Button parearButton;
    Button OKButton;

    double lastX=0;
    LineGraphSeries<DataPoint> series;

    @SuppressLint("HandlerLeak")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        parearButton = findViewById(R.id.parearButton);
        OKButton = findViewById(R.id.OKButton);

        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            Toast.makeText(this, "Não há suporte a Bluetooth", Toast.LENGTH_LONG).show();
            Toast.makeText(this, "Finalizando", Toast.LENGTH_SHORT).show();
            finish();
        }
        // Inicia conexão bluetooth
        parearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(conectado){
                    //desconectar
                    try{
                        meuSocket.close();
                        conectado = false;
                        parearButton.setText(R.string.Conectar);
                        Toast.makeText(getApplicationContext(), "Bluetooth desconectado", Toast.LENGTH_LONG).show();
                    }catch (IOException erro){
                        Toast.makeText(getApplicationContext(), "Ocorreu um erro", Toast.LENGTH_LONG).show();
                    }
                }else{
                    //conectar
                    Intent abrelista = new Intent(getApplicationContext(), ListaDispositivos.class);
                    startActivityForResult(abrelista,conectionRequest);
                }
            }
        });

        // Gerencia o recebimento de dados do bluetooth
        mHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {

                if (msg.what == MESSAGE_READ){
                    String recebidos = (String) msg.obj;
                    dadosRecebidos.append(recebidos);           // acumula dados recebidos
                    int fimInformacao = dadosRecebidos.indexOf("}");

                    //Log.d("Recebidos parcial",recebidos);

                    if(fimInformacao > 0){
                        String dadosCompletos = dadosRecebidos.substring(0,fimInformacao);
                        Log.d("Recebidos",dadosCompletos);
                            try {
                                float num = Float.parseFloat(dadosCompletos);

                                series.appendData(new DataPoint(lastX,num),true,1000);
                            }
                            catch(NumberFormatException e){
                                //Log.i();
                            }
                        lastX=lastX+0.5;
                        dadosRecebidos = new StringBuilder();       // reinicia o acumulador de dados
                    }
                }    }
        };


        GraphView graph = findViewById(R.id.graph);
        series = new LineGraphSeries<DataPoint>();
        graph.addSeries(series);


        Viewport viewport = graph.getViewport();
        viewport.setScalable(true);
        viewport.setMinX(0);
        viewport.setMinY(0);
        viewport.setMaxX(20);
        viewport.setXAxisBoundsManual(true);


        // Envia uma string via bluetooth por meio de click em botão
        OKButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(conectado){
                    connectedThread.write("O");
                }else{
                    Toast.makeText(getApplicationContext(), "Não conectado a nenhum dispositivo", Toast.LENGTH_SHORT).show();
                }
            }
        });


    }

    public void addEntry(View view){

        series.appendData(new DataPoint(lastX,sin(lastX)+1),true,1000);
        lastX=lastX+0.5;
    }


    // Checa o resultado da janela de conexão
    @Override
    protected void onActivityResult(int requestCode,int resultCode, Intent data){
        switch (requestCode){
            case conectionRequest:
                if (resultCode == Activity.RESULT_OK){
                    String MAC = Objects.requireNonNull(data.getExtras()).getString(ListaDispositivos.ENDERECO_MAC);
                    //Toast.makeText(this, "MAC final" + MAC, Toast.LENGTH_LONG).show();
                    meuDevice = mBluetoothAdapter.getRemoteDevice(MAC);

                    try{
                        meuSocket = meuDevice.createInsecureRfcommSocketToServiceRecord(meuUUID);
                        meuSocket.connect();
                        parearButton.setText(R.string.Desconectar);
                        conectado = true;

                        connectedThread = new ConnectedThread(meuSocket);
                        connectedThread.start();

                        Toast.makeText(this, "Conectado", Toast.LENGTH_SHORT).show();
                    }catch(IOException erro){
                        conectado = false;
                        Toast.makeText(this, "Erro na conexão"+erro, Toast.LENGTH_SHORT).show();
                    }

                }else{
                    Toast.makeText(this, "Falha ao obter MAC", Toast.LENGTH_SHORT).show();
                }
        }
    }


    // Gerenciamento da conexão bluetooth
    private class ConnectedThread extends Thread {
        // private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        ConnectedThread(BluetoothSocket socket) {

            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException ignored) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);

                    String dadosBt = new String(buffer,0,bytes);
                    // Send the obtained bytes to the UI activity
                    mHandler.obtainMessage(MESSAGE_READ, bytes, -1, dadosBt).sendToTarget();

                } catch (IOException e) {
                    break;
                }
            }
        }

        // connectedThread.write(string);   Para enviar dados
        /* Call this from the main activity to send data to the remote device */
        public void write(String dadosEnviar) {

            byte[] msgBuffer = dadosEnviar.getBytes();
            try {
                mmOutStream.write(msgBuffer);
            } catch (IOException ignored) { }
        }

    }


}
