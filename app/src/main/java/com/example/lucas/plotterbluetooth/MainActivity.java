package com.example.lucas.plotterbluetooth;

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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final int conectionRequest = 2;
    private static final int MESSAGE_READ = 3;

    private static String MAC = null;

    Handler mHandler;

    StringBuilder dadosRecebidos = new StringBuilder();

    BluetoothAdapter mBluetoothAdapter = null;
    BluetoothDevice meuDevice = null;
    BluetoothSocket meuSocket=null;
    UUID meuUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    ConnectedThread connectedThread;

    Button parearButton;
    boolean conectado=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicialização do bluetooth
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        parearButton = findViewById(R.id.parearButton);

        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            Toast.makeText(this, "Não há suporte a Bluetooth", Toast.LENGTH_LONG).show();
            Toast.makeText(this, "Finalizando", Toast.LENGTH_SHORT).show();
            finish();
        }

        parearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(conectado){
                    //desconectar
                    try{
                        meuSocket.close();
                        conectado = false;
                        parearButton.setText("Conectar");
                        Toast.makeText(getApplicationContext(), "Bluetooth desconectado", Toast.LENGTH_LONG).show();
                    }catch (IOException erro){
                        Toast.makeText(getApplicationContext(), "Ocorreu um erro", Toast.LENGTH_LONG).show();
                    }
                }else{
                    //conectar
                    Intent abrelista = new Intent(MainActivity.this, ListaDispositivos.class);
                    startActivityForResult(abrelista,conectionRequest);
                }
            }
        });

        mHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {

                if (msg.what == MESSAGE_READ){
                    String recebidos = (String) msg.obj;
                    dadosRecebidos.append(recebidos);
                    Log.d("Recebidos",recebidos);


                }

            }
        };

    }

    public void manual(){

        Intent intent = new Intent(this, Manual.class);
        startActivity(intent);
    }
    public void manual(View view){

        Intent intent = new Intent(this, Manual.class);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.primeiroBotao:
                manual();
                return true;
            case R.id.segundoBotao:
                Toast.makeText(this, "segundo botão", Toast.LENGTH_SHORT).show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode,int resultCode, Intent data){
        switch (requestCode){
            case conectionRequest:
                if (resultCode == Activity.RESULT_OK){
                    MAC = Objects.requireNonNull(data.getExtras()).getString(ListaDispositivos.ENDERECO_MAC);
                    //Toast.makeText(this, "MAC final" + MAC, Toast.LENGTH_LONG).show();
                    meuDevice = mBluetoothAdapter.getRemoteDevice(MAC);

                    try{
                        meuSocket = meuDevice.createInsecureRfcommSocketToServiceRecord(meuUUID);
                        meuSocket.connect();
                        parearButton.setText("Desconectar");
                        conectado = true;

                        connectedThread = new ConnectedThread(meuSocket);
                        connectedThread.start();

                        Toast.makeText(this, "Conectado", Toast.LENGTH_LONG).show();
                    }catch(IOException erro){
                        conectado = false;
                        Toast.makeText(this, "Erro na conexão"+erro, Toast.LENGTH_LONG).show();
                    }

                }else{
                    Toast.makeText(this, "Falha ao obter MAC", Toast.LENGTH_SHORT).show();
                }
        }
    }
    private class ConnectedThread extends Thread {
       // private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {

            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

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

        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {

            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { }
        }

    }
}
