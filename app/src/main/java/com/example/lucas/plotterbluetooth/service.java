package com.example.lucas.plotterbluetooth;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class service extends Service {

    private static final int MESSAGE_READ = 3;

    Handler mHandler;

    StringBuilder dadosRecebidos = new StringBuilder();
    BluetoothAdapter mBluetoothAdapter = null;
    BluetoothDevice meuDevice = null;
    BluetoothSocket meuSocket=null;
    UUID meuUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    ConnectedThread connectedThread;
    boolean conectado=false;
    float temp;

    int notifyID = 1;
    Notification notification;

    NotificationManager mNotificationManager;

    private final IBinder mBinder = new LocalBinder();

    public class LocalBinder extends Binder {
        service getService() {
            // Return this instance of LocalService so clients can call public methods
            return service.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /** method for clients */
    public float getTemp() {
        return temp;
    }

    @SuppressLint("HandlerLeak")
    @Override
    public void onCreate() {
        super.onCreate();

        notification=new Notification();
        startForeground(notifyID,notification);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            Toast.makeText(this, "Não há suporte a Bluetooth", Toast.LENGTH_LONG).show();
        }

        mHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {

                if (msg.what == MESSAGE_READ){
                    String recebidos = (String) msg.obj;
                    dadosRecebidos.append(recebidos);           // acumula dados recebidos
                    Log.d("Recebidos parcial",recebidos);
                    int fimInformacao = dadosRecebidos.lastIndexOf("\n");

                    if(fimInformacao > 0){
                        String dadosCompletos = dadosRecebidos.substring(0,fimInformacao);
                        Log.d("Recebidos",dadosCompletos);
                        try {
                            float num = Float.parseFloat(dadosCompletos);
                            //listaTemperaturas.add(new temperaturasBluetooth(lastX,num));
                            showNotification(num);
                            temp=num;

                        }
                        catch(NumberFormatException e){
                            //Log.i();
                        }
                        dadosRecebidos = new StringBuilder();       // reinicia o acumulador de dados
                    }
                }    }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Boolean finalizar = intent.getBooleanExtra("finalizar",false);

        if(conectado && finalizar){
            //desconectar
            try{
                meuSocket.close();
                conectado = false;
                stopForeground(true);
                Toast.makeText(getApplicationContext(), "Bluetooth desconectado", Toast.LENGTH_SHORT).show();
            }catch (IOException erro){
                Toast.makeText(getApplicationContext(), "Ocorreu um erro", Toast.LENGTH_LONG).show();
            }

        }

        if(!conectado && !finalizar) {
            String MAC = intent.getStringExtra("MAC");
            Toast.makeText(this, "MAC no service" + MAC, Toast.LENGTH_SHORT).show();

            meuDevice = mBluetoothAdapter.getRemoteDevice(MAC);

            try {
                meuSocket = meuDevice.createInsecureRfcommSocketToServiceRecord(meuUUID);
                meuSocket.connect();
                conectado = true;

                connectedThread = new service.ConnectedThread(meuSocket);
                connectedThread.start();

                Toast.makeText(this, "Conectado", Toast.LENGTH_SHORT).show();
            } catch (IOException erro) {
                conectado = false;
                Toast.makeText(this, "Erro na conexão" + erro, Toast.LENGTH_SHORT).show();
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    public void showNotification(float temp){
        mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
// Sets an ID for the notification, so it can be updated
        NotificationCompat.Builder mNotifyBuilder = new NotificationCompat.Builder(this,"id")
                //.setContentTitle("Temperatura")
                //.setContentText("You've received new messages.")
                .setSmallIcon(R.drawable.temperature)
                .setColor(getResources().getColor(R.color.colorBackgroundTemp))
                .setStyle(new NotificationCompat.DecoratedCustomViewStyle());

        mNotifyBuilder.setContentTitle("Temperatura "+temp+"ºC");
        // Because the ID remains unchanged, the existing notification is
        // updated.
        if (mNotificationManager != null) {
            mNotificationManager.notify(
                    notifyID,
                    mNotifyBuilder.build());
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
            byte[] buffer;  // buffer store for the stream
            buffer = new byte[1024];
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (conectado) {
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
