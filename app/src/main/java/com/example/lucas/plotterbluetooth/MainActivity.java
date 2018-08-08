package com.example.lucas.plotterbluetooth;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private static final int bluetoothRequest = 1;
    private static final int conectionRequest = 2;

    BluetoothAdapter mBluetoothAdapter;
    Button parearButton;
    boolean conectado;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicialização do bluetooth
        conectado = false;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        parearButton = (Button)findViewById(R.id.parearButton);

        if (mBluetoothAdapter == null) {
            // Device does not support Bluetooth
            Toast.makeText(this, "Não há suporte a Bluetooth", Toast.LENGTH_LONG).show();
            Toast.makeText(this, "Finalizando", Toast.LENGTH_SHORT).show();
            finish();
        }   else if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, bluetoothRequest);
        }

        parearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(conectado){
                    // desconecta
                }else{
                    Intent abrelista = new Intent(MainActivity.this, ListaDispositivos.class);
                    startActivityForResult(abrelista,conectionRequest);
                }
            }
        });

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
}
