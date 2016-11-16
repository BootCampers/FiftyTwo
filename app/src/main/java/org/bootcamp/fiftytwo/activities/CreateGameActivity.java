package org.bootcamp.fiftytwo.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.bootcamp.fiftytwo.R;

import java.util.Random;

public class CreateGameActivity extends AppCompatActivity {

    TextView gameIdNumber;
    Button startGameButton;
    Button chooseCardsButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_game);

        gameIdNumber = (TextView) findViewById(R.id.gameIDNumber);
        startGameButton = (Button) findViewById(R.id.startGameButton);
        chooseCardsButton = (Button) findViewById(R.id.chooseCardsButton);

        int gameId = new Random().nextInt(99999);
        String gameIDString = String.format("%05d", gameId);
        gameIdNumber.setText(gameIDString);


        startGameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(CreateGameActivity.this, GameViewManagerActivity.class));
            }
        });

        chooseCardsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getApplicationContext(), "Functionality coming soon...", Toast.LENGTH_SHORT).show();
            }
        });

    }
}
