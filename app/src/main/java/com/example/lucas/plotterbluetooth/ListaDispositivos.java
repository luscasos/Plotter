package com.example.lucas.plotterbluetooth;

import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Set;

public class ListaDispositivos extends ListActivity {
    private BluetoothAdapter meuBluetooth = null;
    private static final int bluetoothRequest = 1;

    static String ENDERECO_MAC = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        meuBluetooth = BluetoothAdapter.getDefaultAdapter();

        if (!meuBluetooth.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, bluetoothRequest);
        }else{
                ArrayAdapter<String> ArrayBluetooth = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1);
                Set<BluetoothDevice>dispositivosPareados = meuBluetooth.getBondedDevices();

                if (dispositivosPareados.size()>0){
                    for (BluetoothDevice dispositivo : dispositivosPareados){
                        String nomeBt = dispositivo.getName();
                        String macBt = dispositivo.getAddress();
                        ArrayBluetooth.add(nomeBt + "\n" + macBt);
                    }
                }
                setListAdapter(ArrayBluetooth);
        }

    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        String informacao = ((TextView) v).getText().toString();


        String enderecoMac = informacao.substring(informacao.length()-17);

        //Toast.makeText(getApplicationContext(),"Mac: "+ enderecoMac,Toast.LENGTH_SHORT).show();

        Intent retornaMac = new Intent();
        retornaMac.putExtra(ENDERECO_MAC,enderecoMac);
        setResult(RESULT_OK,retornaMac);
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode,int resultCode, Intent data){
        switch (requestCode){
            case bluetoothRequest:
                if (resultCode == Activity.RESULT_OK){
                    ArrayAdapter<String> ArrayBluetooth = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1);
                    Set<BluetoothDevice>dispositivosPareados = meuBluetooth.getBondedDevices();

                    if (dispositivosPareados.size()>0){
                        for (BluetoothDevice dispositivo : dispositivosPareados){
                            String nomeBt = dispositivo.getName();
                            String macBt = dispositivo.getAddress();
                            ArrayBluetooth.add(nomeBt + "\n" + macBt);
                        }
                    }
                    setListAdapter(ArrayBluetooth);
                }else{
                    Toast.makeText(this, "Não é possivel parear o dispositivo", Toast.LENGTH_LONG).show();
                    finish();
                }
        }
    }
}
