package com.example.lucas.plotterbluetooth;


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

    Button graphButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        graphButton = findViewById(R.id.graphButton);

    }

    public void GraphView(View view){
        Intent intent = new Intent(this, GraphActivity.class);
        startActivity(intent);
    }
    public void manual(View view){

        Intent intent = new Intent(this, Manual.class);
        startActivity(intent);
    }


    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Finaliza a Thread
        Intent intent = new Intent(this,service.class);
        intent.putExtra("operacao",1);
        startService(intent);
        //Finaliza o service
        stopService(new Intent(this,service.class));

    }
}
