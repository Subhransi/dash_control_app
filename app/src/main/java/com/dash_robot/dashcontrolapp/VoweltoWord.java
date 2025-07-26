package com.dash_robot.dashcontrolapp;

import static android.content.ContentValues.TAG;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Looper;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Handler;

import java.util.ArrayList;
import java.util.Random;
import android.util.Log;


import com.dash_robot.dashcontrolapp.MainActivity;

public class VoweltoWord extends AppCompatActivity {

    private TextView tvWord;
    private ImageView imageWord;
    private Button btnNext, btnPrev, btnDisengaged;
    private ImageButton btnCorrect, btnWrong, btnNeutral;

    private ArrayList<Integer> questionOrder;
    private int currentIndex = 0;
    //MainActivity.writeCommand(byte[] command);
    private static final byte[] COMMAND_NECK_RED = new byte[]{(byte) 0x03, (byte) 0xFF, (byte) 0x00, (byte) 0x00};// Set head light to Red (hypothetical)
    private final byte[] LEFT_EAR_RED_COMMAND = new byte[]{
            (byte) 0x0b, (byte) 0xFF, (byte) 0x00, (byte) 0x00
    };

    // Confirmed from your logs (0x01 for right ear)
    private final byte[] RIGHT_EAR_RED_COMMAND = new byte[]{
            (byte) 0x0c, (byte) 0xFF, (byte) 0x00, (byte) 0x00
    };// Set both ears to Red (hypothetical)
    private static final byte[] COMMAND_MOVE_BACKWARD_SHORT = new byte[]{(byte) 0x0C, (byte) 0x0D}; // Move backward -10 units (hypothetical)
    private static final byte[] TURN_HEAD_RIGHT = new byte[]{(byte) 0x06, (byte) 0x20, (byte) 0x00, (byte) 0x00}; // Small positive yaw
    private static final byte[] TURN_HEAD_LEFT = new byte[]{(byte) 0x06, (byte) 0xE0, (byte) 0x00, (byte) 0x00}; // Small negative yaw

    private static final byte[] COMMAND_STOP_MOVING = new byte[]{(byte) 0x12, (byte) 0x13}; // Stop all movement (hypothetical)

    // --- Original commands you were sending (renamed for clarity, but still likely incorrect) ---
    // Keeping these for reference if they were based on some partial info, but
    // they are not structured like typical per-action commands.


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vowel_to_word);

        tvWord = findViewById(R.id.tvWord);
        imageWord = findViewById(R.id.imageWord);
        btnNext = findViewById(R.id.btnNext);
        btnPrev = findViewById(R.id.btnPrev);
        btnNeutral = findViewById(R.id.btnNeutral);
        btnCorrect = findViewById(R.id.btnCorrect);
        btnWrong = findViewById(R.id.btnWrong);
        btnDisengaged = findViewById(R.id.btnDisengaged);

        questionOrder = getIntent().getIntegerArrayListExtra("question_order");
        if (questionOrder == null || questionOrder.isEmpty()) {
            Toast.makeText(this, "No questions found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        showQuestion(currentIndex);

        btnNext.setOnClickListener(v -> {
            if (currentIndex < questionOrder.size() - 1) {
                currentIndex++;
                showQuestion(currentIndex);
            }
        });

        btnPrev.setOnClickListener(v -> {
            if (currentIndex > 0) {
                currentIndex--;
                showQuestion(currentIndex);
            }
        });

        btnCorrect.setOnClickListener(v -> {
            playRandomCorrectAudio();
            triggerCorrectAnswerRobotSequence();
        });

        btnWrong.setOnClickListener(v -> {
            // Add robot reaction here
            playRandomIncorrectAudio();
            //1. Set ear and neck color to red
            triggerWrongAnswerRobotSequence();
        });

        btnDisengaged.setOnClickListener(v -> playRandomDisengagedAudio());

        btnNeutral.setOnClickListener(v -> {
            playNeutralAudioForQuestion(questionOrder.get(currentIndex));
            triggerNeutralRobotSequence();  // 👈 Add this
        });

    }

    private void showQuestion(int index) {
        int questionId = questionOrder.get(index);
        String word = getWordForQuestion(questionId);
        int imageResId = getImageForQuestion(questionId);

        tvWord.setText(word);
        imageWord.setImageResource(imageResId);
    }

    private String getWordForQuestion(int id) {
        switch (id) {
            case 1:
                return "Ha";
            case 2:
                return "He";
            case 3:
                return "Ho";
            case 4:
                return "Who";
            case 5:
                return "Hay";
            case 6:
                return "Ba";
            case 7:
                return "Bee";
            case 8:
                return "Boo";
            case 9:
                return "Bay";
            case 10:
                return "Bow";
            case 11:
                return "Ma";
            case 12:
                return "Me_girl";
            case 13:
                return "Me_boy";
            case 14:
                return "May";
            case 15:
                return "Mo";
            case 16:
                return "Moo";
            case 17:
                return "See";
            case 18:
                return "Saw";
            case 19:
                return "Say";
            case 20:
                return "Sew";
            case 21:
                return "Sow";
            case 22:
                return "Pa";
            case 23:
                return "Pea";
            case 24:
                return "Pay";
            case 25:
                return "Pooh";
            case 26:
                return "Car";
            case 27:
                return "Key";
            case 28:
                return "Ku";
            case 29:
                return "Cake";
            case 30:
                return "Kite";
            case 31:
                return "Two";
            case 32:
                return "Toe";
            case 33:
                return "Tea";
            case 34:
                return "Far";
            case 35:
                return "Fair";
            case 36:
                return "Fee";
            case 37:
                return "Food";
            default:
                return "";
        }

    }

    private int getImageForQuestion(int id) {
        switch (id) {
            case 1:
                return R.drawable.ha;
            case 2:
                return R.drawable.he;
            case 3:
                return R.drawable.ho;
            case 4:
                return R.drawable.who;
            case 5:
                return R.drawable.hay;
            case 6:
                return R.drawable.ba;
            case 7:
                return R.drawable.bee;
            case 8:
                return R.drawable.boo;
            case 9:
                return R.drawable.bay;
            case 10:
                return R.drawable.bow;
            case 11:
                return R.drawable.ma;
            case 12:
                return R.drawable.me_girl;
            case 13:
                return R.drawable.me_boy;
            case 14:
                return R.drawable.may;
            case 15:
                return R.drawable.mo;
            case 16:
                return R.drawable.moo;
            case 17:
                return R.drawable.see;
            case 18:
                return R.drawable.saw;
            case 19:
                return R.drawable.say;
            case 20:
                return R.drawable.sew;
            case 21:
                return R.drawable.sow;
            case 22:
                return R.drawable.pa;
            case 23:
                return R.drawable.pea;
            case 24:
                return R.drawable.pay;
            case 25:
                return R.drawable.pooh;
            case 26:
                return R.drawable.car;
            case 27:
                return R.drawable.key;
            case 28:
                return R.drawable.ku;
            case 29:
                return R.drawable.cake;
            case 30:
                return R.drawable.kite;
            case 31:
                return R.drawable.two;
            case 32:
                return R.drawable.toe;
            case 33:
                return R.drawable.tea;
            case 34:
                return R.drawable.far;
            case 35:
                return R.drawable.fair;
            case 36:
                return R.drawable.fee;
            case 37:
                return R.drawable.food;
            default:
                return 0;
        }

    }

    private void playRandomCorrectAudio() {
        int[] audios = {
                R.raw.correct_great_work,
                R.raw.good_job,
                R.raw.you_are_so_smart,
                R.raw.wow_you_did_it
        };
        playRandomFromArray(audios);
    }

    private void playRandomIncorrectAudio() {
        int[] audios = {
                R.raw.this_is_wrong_lets_try_again,
                R.raw.good_try_lets_do_this_once_more,
                R.raw.lets_try_again
        };
        playRandomFromArray(audios);
    }

    private void playRandomDisengagedAudio() {
        int[] audios = {
                R.raw.hey_look_here_lets_continue,
                R.raw.lets_look_at_the_card,
                R.raw.this_is_fun_lets_continue,
                R.raw.youre_doing_so_good_lets_continue
        };
        playRandomFromArray(audios);
    }

    private void playNeutralAudioForQuestion(int id) {
        int resId = getNeutralAudioForQuestion(id);
        if (resId != 0) playAudio(resId);
    }

    private int getNeutralAudioForQuestion(int id) {
        switch (id) {
            case 1:
                return R.raw.how_do_you_laugh;
            case 2:
                return R.raw.what_do_you_call_a_boy;
            case 3:
                return R.raw.what_does_santa_claus_say;
            case 4:
                return R.raw.how_do_you_ask_for_someone;
            case 5:
                return R.raw.what_is_this;
            case 6:
                return R.raw.what_does_the_sheep_say;
            case 7:
                return R.raw.what_is_this;
            case 8:
                return R.raw.what_does_the_ghost_say;
            case 9:
                return R.raw.what_is_the_other_name_for_beach;
            case 10:
                return R.raw.what_do_we_tie_on_the_neck;
            case 11:
                return R.raw.who_is_this;
            case 12:
                return R.raw.what_do_you_call_yourself;
            case 13:
                return R.raw.what_do_you_call_yourself;
            case 14:
                return R.raw.what_comes_after_april;
            case 15:
                return R.raw.what_is_the_boy_doing;
            case 16:
                return R.raw.what_does_the_cow_say;
            case 17:
                return R.raw.what_is_the_man_doing;
            case 18:
                return R.raw.what_is_this;
            case 19:
                return R.raw.what_is_the_lady_doing;
            case 20:
                return R.raw.what_is_the_girl_doing;
            case 21:
                return R.raw.what_is_the_man_doing;
            case 22:
                return R.raw.who_is_this;
            case 23:
                return R.raw.what_is_this;
            case 24:
                return R.raw.what_do_you_do_after_buying;
            case 25:
                return R.raw.who_is_this;
            case 26:
                return R.raw.what_is_this;
            case 27:
                return R.raw.what_is_this;
            case 28:
                return R.raw.what_does_the_bird_say;
            case 29:
                return R.raw.what_is_this;
            case 30:
                return R.raw.what_is_this;
            case 31:
                return R.raw.what_is_this;
            case 32:
                return R.raw.what_is_this;
            case 33:
                return R.raw.what_is_this;
            case 34:
                return R.raw.where_is_the_mountain;
            case 35:
                return R.raw.where_is_this;
            case 36:
                return R.raw.after_going_to_doctor_what_do;
            case 37:
                return R.raw.what_is_this;
            default:
                return 0;
        }
    }

    private void playRandomFromArray(int[] audios) {
        int index = new Random().nextInt(audios.length);
        playAudio(audios[index]);
    }

    private void playAudio(int resId) {
        MediaPlayer mp = MediaPlayer.create(this, resId);
        mp.setOnCompletionListener(MediaPlayer::release);
        mp.start();
    }

    private void triggerWrongAnswerRobotSequence() {
        if (MainActivity.bluetoothGatt == null || MainActivity.dashCharacteristic == null) {
            Log.e("BLE", "Bluetooth not connected");
            return;
        }

        Handler handler = new Handler(Looper.getMainLooper());
        int delay = 0;

        // 1. Set neck to red FIRST
        byte[] neckRed = new byte[]{0x03, (byte) 0xFF, 0x00, 0x00}; // Neck red
        handler.postDelayed(() -> MainActivity.writeCommand(neckRed), delay += 100);

        // 2. Then set both ears to red
        byte[] leftEarRed = new byte[]{0x0B, (byte) 0xFF, 0x00, 0x00};
        byte[] rightEarRed = new byte[]{0x0C, (byte) 0xFF, 0x00, 0x00};
        handler.postDelayed(() -> MainActivity.writeCommand(leftEarRed), delay += 200);
        handler.postDelayed(() -> MainActivity.writeCommand(rightEarRed), delay += 200);

        // 3. Set eye brightness to 50
        byte[] eyeBrightness = new byte[]{0x04, 0x32};
        handler.postDelayed(() -> MainActivity.writeCommand(eyeBrightness), delay += 300);

        // 4. Head yaw: Left → Right → Left → Right (2 times)
        byte[] headLeft = new byte[]{0x06, (byte) 0xE0, 0x00, 0x00};
        byte[] headRight = new byte[]{0x06, 0x20, 0x00, 0x00};
        for (int i = 0; i < 2; i++) {
            handler.postDelayed(() -> MainActivity.writeCommand(headLeft), delay += 500);
            handler.postDelayed(() -> MainActivity.writeCommand(headRight), delay += 500);
        }

        // 5. Move forward then stop
        // 🚀 Move forward


// 7. Reset head yaw and pitch
        byte[] headYawCenter = new byte[]{0x06, 0x00, 0x00, 0x00};       // Center yaw
        byte[] headPitchUp = new byte[]{0x07, 0x10, 0x00, 0x00};         // Slight up
        byte[] headPitchCenter = new byte[]{0x07, 0x00, 0x00, 0x00};     // Neutral pitch
        handler.postDelayed(() -> MainActivity.writeCommand(headYawCenter), delay += 300);
        handler.postDelayed(() -> MainActivity.writeCommand(headPitchUp), delay += 300);
        handler.postDelayed(() -> MainActivity.writeCommand(headPitchCenter), delay += 300);

    }
    public void triggerCorrectAnswerRobotSequence() {
        Handler handler = new Handler(Looper.getMainLooper());
        int delay = 0;

        // 1. Set ears to green
        byte[] leftearGreen = new byte[]{0x0B, (byte) 0x00, (byte) 0xFF, 0x00};  // Green
        byte[] rightearGreen = new byte[]{0x0C, (byte) 0x00, (byte) 0xFF, 0x00};  // Green

        handler.postDelayed(() -> MainActivity.writeCommand(leftearGreen), delay += 0);
        handler.postDelayed(() -> MainActivity.writeCommand(rightearGreen), delay += 0);


        // 2. Set neck to green
        byte[] neckGreen = new byte[]{0x03, 0x00, (byte) 0xFF, 0x00};  // Green
        handler.postDelayed(() -> MainActivity.writeCommand(neckGreen), delay += 0);

        // 3. Set eye brightness to 50%
        byte[] eyeBrightness50 = new byte[]{0x05, 0x32}; // 0x32 = 50 in decimal
        handler.postDelayed(() -> MainActivity.writeCommand(eyeBrightness50), delay += 200);

        // 4. Head pitch down (nod down)
        byte[] headPitchDown = new byte[]{0x07, (byte) 0xFB, 0x00, 0x00}; // -5 pitch
        handler.postDelayed(() -> MainActivity.writeCommand(headPitchDown), delay += 300);

        // 5. Head pitch up (nod up)
        byte[] headPitchUp = new byte[]{0x07, 0x0A, 0x00, 0x00}; // +10 pitch
        handler.postDelayed(() -> MainActivity.writeCommand(headPitchUp), delay += 400);

        // 6. Turn (rotate in place by ~180°)
        byte[] turnCommand = new byte[]{0x0A, (byte) 0xB4}; // ~180° turn, 0xB4 = 180 in decimal
        handler.postDelayed(() -> MainActivity.writeCommand(turnCommand), delay += 500);

        // 7. Center head yaw
        byte[] headYawCenter = new byte[]{0x06, 0x00, 0x00, 0x00};
        handler.postDelayed(() -> MainActivity.writeCommand(headYawCenter), delay += 500);

        // 8. Center head pitch
        byte[] headPitchCenter = new byte[]{0x07, 0x00, 0x00, 0x00};
        handler.postDelayed(() -> MainActivity.writeCommand(headPitchCenter), delay += 300);
    }

    private void triggerNeutralRobotSequence() {
        if (MainActivity.bluetoothGatt == null || MainActivity.dashCharacteristic == null) {
            Log.e("BLE", "Bluetooth not connected");
            return;
        }

        Handler handler = new Handler(Looper.getMainLooper());
        int delay = 0;

        // 1. Reset all
        byte[] resetCommand = new byte[]{0x00}; // You may need a real reset command here.
        handler.postDelayed(() -> MainActivity.writeCommand(resetCommand), delay += 100);

        // 2. Set ears and neck to yellow
        byte[] yellow = new byte[]{(byte) 0xFF, (byte) 0xFF, 0x00};  // RGB for yellow
        byte[] neckYellow = new byte[]{0x03, yellow[0], yellow[1], yellow[2]};
        byte[] leftEarYellow = new byte[]{0x0B, (byte)0xFF, (byte)0xFF, 0x00};
        byte[] rightEarYellow = new byte[]{0x0C, (byte)0xFF, (byte)0xFF, 0x00};
        byte[] topHeadYellow = new byte[]{0x04, yellow[0], yellow[1], yellow[2]};

        handler.postDelayed(() -> MainActivity.writeCommand(neckYellow), delay += 200);
        handler.postDelayed(() -> MainActivity.writeCommand(leftEarYellow), delay += 200);
        handler.postDelayed(() -> MainActivity.writeCommand(rightEarYellow), delay += 200);
        handler.postDelayed(() -> MainActivity.writeCommand(topHeadYellow), delay += 50);  // <-- This sends the top head color

        // 3. Eye brightness 50
        byte[] eyeBrightness = new byte[]{0x05, 0x32}; // 0x32 = 50
        handler.postDelayed(() -> MainActivity.writeCommand(eyeBrightness), delay += 200);

        // 5. Move backward for ~700ms at speed -50
        byte[] moveBackward = new byte[]{0x02, (byte) 0xCE, 0x00, 0x07}; // -50 in Dash protocol
        handler.postDelayed(() -> MainActivity.writeCommand(moveBackward), delay += 300);

// STOP after backward
        byte[] stopDrive = new byte[]{0x02, 0x00, 0x00, 0x00}; // STOP
        handler.postDelayed(() -> MainActivity.writeCommand(stopDrive), delay += 700);

// Move forward for 700ms at speed 50
        byte[] moveForward = new byte[]{0x02, 0x32, 0x00, 0x00}; // speed +50
        handler.postDelayed(() -> MainActivity.writeCommand(moveForward), delay += 1000);

// STOP forward
        handler.postDelayed(() -> MainActivity.writeCommand(stopDrive), delay += 700);


        // 7. Head pitch down -5
        byte[] headPitchDown5 = new byte[]{0x07, (byte) 0xFB, 0x00, 0x00};
        handler.postDelayed(() -> MainActivity.writeCommand(headPitchDown5), delay += 150);

        // 8. Head yaw center
        //byte[] headYawCenter = new byte[]{0x06, 0x00, 0x00, 0x00};
        //handler.postDelayed(() -> MainActivity.writeCommand(headYawCenter), delay += 300);

        // 9. Head pitch up +10
        byte[] headPitchUp10 = new byte[]{0x07, 0x0A, 0x00, 0x00};
        handler.postDelayed(() -> MainActivity.writeCommand(headPitchUp10), delay += 150);

        // 10. Reset again (optional)
        handler.postDelayed(() -> MainActivity.writeCommand(resetCommand), delay += 300);
    }


}
