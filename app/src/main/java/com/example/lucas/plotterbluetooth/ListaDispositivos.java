package com.example.lucas.plotterbluetooth;

import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.widget.ArrayAdapter;

import java.util.Set;

public class ListaDispositivos extends ListActivity {
    private BluetoothAdapter meuBluetooth = null;

    static String enderecoMac = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ArrayAdapter<String> ArrayBluetooth = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1);
        meuBluetooth = BluetoothAdapter.getDefaultAdapter();
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
