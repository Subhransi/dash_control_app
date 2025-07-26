package com.dash_robot.dashcontrolapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Date;

public class LearningActivity extends AppCompatActivity {

    private Spinner learningSpinner;
    private Button btnStartLesson;
    private TextView tvTitle1, tvTitle2;

    private String selectedLesson = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_learning);

        tvTitle1 = findViewById(R.id.tvLearningTitleLine1);
        tvTitle2 = findViewById(R.id.tvLearningTitleLine2);
        learningSpinner = findViewById(R.id.learningSpinner);
        btnStartLesson = findViewById(R.id.btnStartLearning);

        // Spinner options
        String[] lessons = {"Vowel to Word", "Word to Phrase", "Conversation"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, lessons);
        learningSpinner.setAdapter(adapter);

        // Capture selection
        learningSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedLesson = lessons[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedLesson = "";
            }
        });

        // Start button action
        btnStartLesson.setOnClickListener(v -> {
            if (!selectedLesson.isEmpty()) {
                Toast.makeText(this, "Starting: " + selectedLesson, Toast.LENGTH_SHORT).show();

                Calendar calendar = Calendar.getInstance();
                int hrOfDay = calendar.get(Calendar.HOUR_OF_DAY);

                // Build audio queue
                List<Integer> audioQueue = new ArrayList<>();

                // Greeting based on time
                if (hrOfDay < 12) {
                    audioQueue.add(R.raw.good_morning);
                } else if (hrOfDay < 15) {
                    audioQueue.add(R.raw.good_afternoon);
                } else {
                    audioQueue.add(R.raw.good_evening);
                }
                if (selectedLesson.equals("Vowel to Word")) {
                    audioQueue.add(R.raw.my_name_is_dash);
                    audioQueue.add(R.raw.shall_we_learn_words_together);

                    playAudioQueue(audioQueue, VoweltoWord.class );

                }
                else if (selectedLesson.equals("Conversation")) {
                    audioQueue.add(R.raw.my_name_is_dash);
                    audioQueue.add(R.raw.okay_lets_start);

                    // Directly start ConversationActivity after audio
                    playAudioQueue(audioQueue, conversation.class);

                } else {
                    audioQueue.add(R.raw.my_name_is_dash);
                    audioQueue.add(R.raw.okay_lets_start);
                    // Add handling for "Word to Phrase" here if needed
                    Toast.makeText(this, "Word to Phrase coming soon!", Toast.LENGTH_SHORT).show();
                }

            } else {
                Toast.makeText(this, "Please select a lesson.", Toast.LENGTH_SHORT).show();
            }
        });
    }
        private void playAudioQueue (List < Integer > audioQueue,Class<?> activityToStart) {
            if (audioQueue.isEmpty()) {
                // After all audios → start VoweltoWord with today's question order
                Intent intent = new Intent(this, activityToStart );
                intent.putIntegerArrayListExtra("question_order", new ArrayList<>(getOrderVtow()));
                startActivity(intent);
                finish();
                return;
            }

            int audioResId = audioQueue.remove(0);

            MediaPlayer mediaPlayer = MediaPlayer.create(this, audioResId);
            mediaPlayer.setOnCompletionListener(mp -> {
                mp.release();
                playAudioQueue(audioQueue, activityToStart); // play next audio
            });

            mediaPlayer.start();

        }
    private List<Integer> getOrderVtow() {
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        SharedPreferences prefs = getSharedPreferences("q_order_vtow", MODE_PRIVATE);

        String savedOrder = prefs.getString(today, null);
        if (savedOrder != null) {
            String[] parts = savedOrder.split(",");
            List<Integer> cachedOrder = new ArrayList<>();
            for (String s : parts) cachedOrder.add(Integer.parseInt(s.trim()));
            return cachedOrder;
        }

        List<List<Integer>> blocks = Arrays.asList(
                Arrays.asList(1, 2, 3, 4, 5),      // h
                Arrays.asList(6, 7, 8, 9, 10),     // b
                Arrays.asList(11, 12, 13, 14, 15, 16), // m
                Arrays.asList(17, 18, 19, 20, 21),     // s
                Arrays.asList(22, 23, 24, 25),        // p
                Arrays.asList(26, 27, 28, 29, 30),    // k
                Arrays.asList(31, 32, 33),            // t
                Arrays.asList(34, 35, 36, 37)         // f
        );

        Collections.shuffle(blocks);

        List<Integer> orderChoices = new ArrayList<>();
        for (List<Integer> block : blocks) orderChoices.addAll(block);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < orderChoices.size(); i++) {
            sb.append(orderChoices.get(i));
            if (i < orderChoices.size() - 1) sb.append(",");
        }
        prefs.edit().putString(today, sb.toString()).apply();

        return orderChoices;
    }

}



