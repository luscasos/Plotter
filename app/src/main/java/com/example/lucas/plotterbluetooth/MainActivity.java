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

    Button OKButton;
    Button graphButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        graphButton = findViewById(R.id.graphButton);

    }


    // Inicia outra Activity a partir de botão ou ActionBar
    public void manual(){
        Intent intent = new Intent(this, Manual.class);
        startActivity(intent);
    }
    public void GraphView(View view){
        Intent intent = new Intent(this, GraphActivity.class);
        startActivity(intent);
    }
    public void manual(View view){

        Intent intent = new Intent(this, Manual.class);
        startActivity(intent);
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
    protected void onDestroy() {
        super.onDestroy();
        Intent intent = new Intent(this,service.class);
        stopService(intent);
    }
}
