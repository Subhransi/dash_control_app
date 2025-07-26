package com.dash_robot.dashcontrolapp;

import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class conversation extends AppCompatActivity {

    private ImageView conversationImage;
    LinearLayout feedbackButtons;
    private PlayerTurn lastDicePlayer = PlayerTurn.ROBOT;

    PlayerTurn currentTurn = PlayerTurn.ROBOT;  // Example starting point
    private RequestQueue requestQueue;
    private HashMap<Integer, String> convImgMap = new HashMap<>();
    private TextView conversationTitle;
    private TextView nextTurnTextView;
    private TextView turnTextView;

    private Button btnDisengaged, btnBack, btnRobotAnswer;
    private ImageButton dice1, dice2, dice3, dice4, dice5, dice6;

    private enum PlayerTurn { ROBOT, CHILD, TEACHER }

    private int turnCount = 0;
    private int lastRobotQuestionBlock = 0;

    private int previousChildCell = 0;

    private int robotCell = 0;
    private int childCell = 0;
    private int teacherCell = 0;

    private boolean isFirstDiceClicked = false;

    private MediaPlayer mediaPlayer;

    // Array of audio file names (without .mp3 extension)
    private final String[] disengagedAudios = {
            "hey_look_here_lets_continue",
            "lets_look_at_the_card",
            "this_is_fun_lets_continue",
            "youre_doing_so_good_lets_continue"
    };


    private final HashMap<Integer, Integer> snakesAndLadders = new HashMap<Integer, Integer>() {{
        put(5, 7);  // Ladder example
        put(13, 2);
        put(15, 22);
        put(20, 29);
    }};

    private Map<Integer, int[]> robotAudioMap = new HashMap<>();
    private Map<Integer, int[]> childAudioMap = new HashMap<>();


    private void playRobotBlockAudio(int blockNumber) {
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }

        // audio files must be in res/raw named like: audio_1, audio_2, ..., audio_36
        int audioResId = getResources().getIdentifier("audio_" + blockNumber, "raw", getPackageName());

        if (audioResId != 0) {
            mediaPlayer = MediaPlayer.create(this, audioResId);
            mediaPlayer.start();
        } else {
            // Optional: log or show error if audio not found
            // Log.d("AUDIO", "No audio for block: " + blockNumber);
        }
    }

    private void playTurnTransitionAudio(String audioName) {
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }

        int resId = getResources().getIdentifier(audioName, "raw", getPackageName());
        if (resId != 0) {
            mediaPlayer = MediaPlayer.create(this, resId);
            mediaPlayer.start();
        } else {
            Toast.makeText(this, "Audio not found: " + audioName, Toast.LENGTH_SHORT).show();
        }
    }

    private void playBlockAudioForRobotAnswer(int blockNumber) {
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }

        String audioFileName = "robot_block_" + blockNumber;
        int audioResId = getResources().getIdentifier(audioFileName, "raw", getPackageName());

        if (audioResId != 0) {
            mediaPlayer = MediaPlayer.create(this, audioResId);
            if (mediaPlayer != null) {
                mediaPlayer.setOnCompletionListener(mp -> {
                    mp.release();
                    mediaPlayer = null;
                });
                mediaPlayer.start();
            } else {
                Toast.makeText(this, "MediaPlayer returned null for robot answer block " + blockNumber, Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Robot answer audio not found for block " + blockNumber, Toast.LENGTH_SHORT).show();
        }
    }

    private void playAudioAtIndex(int[] audioResIds, int index) {
        if (index >= audioResIds.length) return;

        releaseMediaPlayer(); // Clean up any previous instance

        mediaPlayer = MediaPlayer.create(this, audioResIds[index]);
        if (mediaPlayer != null) {
            mediaPlayer.setOnCompletionListener(mp -> {
                mp.release();
                mediaPlayer = null;
                playAudioAtIndex(audioResIds, index + 1);
            });
            mediaPlayer.start();
        }
    }

    private void releaseMediaPlayer() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
    private void playAudio(int resId) {
        MediaPlayer mediaPlayer = MediaPlayer.create(this, resId);
        mediaPlayer.setOnCompletionListener(mp -> {
            mp.stop();
            mp.release();
        });
        mediaPlayer.start();
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);

        conversationTitle = findViewById(R.id.conversationTitle);
        conversationImage = findViewById(R.id.conversationImage);
        nextTurnTextView = findViewById(R.id.nextTurnTextView);
        turnTextView = findViewById(R.id.turnTextView);
        feedbackButtons = findViewById(R.id.feedbackButtons);
        btnRobotAnswer = findViewById(R.id.btnRobotAnswer);

        nextTurnTextView.setVisibility(View.GONE);
        feedbackButtons.setVisibility(View.GONE);
        btnRobotAnswer.setVisibility(View.GONE);

        btnDisengaged = findViewById(R.id.btnDisengaged);
        btnBack = findViewById(R.id.btnBack);
        ImageButton btnCorrect = findViewById(R.id.btnCorrect);// make sure this ID matches your XML
        ImageButton btnNeutral = findViewById(R.id.btnNeutral);

        // Block 1 audios for each player
        //int[] robotBlock1Audios = {R.raw.try_to_answer , R.raw.robot_block_1 ,R.raw.this_is};
        //int[] childBlock1Audios = {R.raw.who , R.raw.who_is_this ,R.raw.ask_the_question };

        robotAudioMap.put(1, new int[]{R.raw.try_to_answer , R.raw.robot_block_1 ,R.raw.this_is, R.raw.who_is_this });
        robotAudioMap.put(2, new int[]{R.raw.try_to_answer , R.raw.robot_block_2 ,R.raw.this_is, R.raw.who_is_this});
        robotAudioMap.put(3, new int[]{R.raw.try_to_answer , R.raw.robot_block_3 ,R.raw.this_is, R.raw.who_is_this});
        robotAudioMap.put(4, new int[]{R.raw.try_to_answer , R.raw.robot_block_4 ,R.raw.this_is, R.raw.who_is_this});
        robotAudioMap.put(5, new int[]{R.raw.try_to_answer , R.raw.robot_block_5 ,R.raw.this_is, R.raw.who_is_this});
        robotAudioMap.put(6, new int[]{R.raw.try_to_answer , R.raw.robot_block_6 ,R.raw.this_is, R.raw.who_is_this});
        robotAudioMap.put(24, new int[]{R.raw.try_to_answer , R.raw.robot_block_1 ,R.raw.this_is, R.raw.who_is_this});
        robotAudioMap.put(23, new int[]{R.raw.try_to_answer , R.raw.robot_block_2 ,R.raw.this_is, R.raw.who_is_this});
        robotAudioMap.put(22, new int[]{R.raw.try_to_answer , R.raw.robot_block_3 ,R.raw.this_is, R.raw.who_is_this});
        robotAudioMap.put(21, new int[]{R.raw.try_to_answer , R.raw.robot_block_4 ,R.raw.this_is, R.raw.who_is_this});
        robotAudioMap.put(20, new int[]{R.raw.try_to_answer , R.raw.robot_block_5 ,R.raw.this_is, R.raw.who_is_this});
        robotAudioMap.put(19, new int[]{R.raw.try_to_answer , R.raw.robot_block_6 ,R.raw.this_is, R.raw.who_is_this});

        robotAudioMap.put(7, new int[]{R.raw.try_to_answer , R.raw.robot_block_7 ,R.raw.audio_7, R.raw.the_teacher });
        robotAudioMap.put(8, new int[]{R.raw.try_to_answer , R.raw.robot_block_8 ,R.raw.audio_8, R.raw.the_policeman  });
        robotAudioMap.put(9, new int[]{R.raw.try_to_answer , R.raw.robot_block_9 ,R.raw.audio_9, R.raw.the_doctor  });
        robotAudioMap.put(10, new int[]{R.raw.try_to_answer , R.raw.robot_block_10 ,R.raw.audio_10, R.raw.the_chef });
        robotAudioMap.put(11, new int[]{R.raw.try_to_answer , R.raw.robot_block_11 ,R.raw.audio_11, R.raw.the_postman  });
        robotAudioMap.put(12, new int[]{R.raw.try_to_answer , R.raw.robot_block_12 ,R.raw.audio_12, R.raw.the_farmer });
        robotAudioMap.put(25, new int[]{R.raw.try_to_answer , R.raw.robot_block_25 ,R.raw.audio_25, R.raw.the_farmer });
        robotAudioMap.put(26, new int[]{R.raw.try_to_answer , R.raw.robot_block_26 ,R.raw.audio_26, R.raw.the_postman });
        robotAudioMap.put(27, new int[]{R.raw.try_to_answer , R.raw.robot_block_27 ,R.raw.audio_27, R.raw.the_chef });
        robotAudioMap.put(28, new int[]{R.raw.try_to_answer , R.raw.robot_block_28 ,R.raw.audio_28, R.raw.the_doctor });
        robotAudioMap.put(29, new int[]{R.raw.try_to_answer , R.raw.robot_block_29 ,R.raw.audio_29, R.raw.the_policeman  });
        robotAudioMap.put(30, new int[]{R.raw.try_to_answer , R.raw.robot_block_30 ,R.raw.audio_30, R.raw.the_teacher });

        robotAudioMap.put(13, new int[]{R.raw.try_to_answer , R.raw.robot_block_13 ,R.raw.audio_13, R.raw.the_teacher });
        robotAudioMap.put(14, new int[]{R.raw.try_to_answer , R.raw.robot_block_14 ,R.raw.audio_14, R.raw.the_policeman  });
        robotAudioMap.put(15, new int[]{R.raw.try_to_answer , R.raw.robot_block_15 ,R.raw.audio_15, R.raw.the_doctor  });
        robotAudioMap.put(16, new int[]{R.raw.try_to_answer , R.raw.robot_block_16 ,R.raw.audio_16, R.raw.the_chef });
        robotAudioMap.put(17, new int[]{R.raw.try_to_answer , R.raw.robot_block_17 ,R.raw.audio_17, R.raw.the_postman  });
        robotAudioMap.put(18, new int[]{R.raw.try_to_answer , R.raw.robot_block_18 ,R.raw.audio_18, R.raw.the_farmer });
        robotAudioMap.put(31, new int[]{R.raw.try_to_answer , R.raw.robot_block_31 ,R.raw.audio_31, R.raw.the_farmer });
        robotAudioMap.put(32, new int[]{R.raw.try_to_answer , R.raw.robot_block_32 ,R.raw.audio_32, R.raw.the_postman });
        robotAudioMap.put(33, new int[]{R.raw.try_to_answer , R.raw.robot_block_33 ,R.raw.audio_33, R.raw.the_chef });
        robotAudioMap.put(34, new int[]{R.raw.try_to_answer , R.raw.robot_block_34 ,R.raw.audio_34, R.raw.the_doctor });
        robotAudioMap.put(35, new int[]{R.raw.try_to_answer , R.raw.robot_block_35 ,R.raw.audio_35, R.raw.the_policeman  });
        robotAudioMap.put(36, new int[]{R.raw.try_to_answer , R.raw.robot_block_36 ,R.raw.audio_36, R.raw.the_teacher });

        childAudioMap.put(1, new int[]{R.raw.who , R.raw.who_is_this ,R.raw.ask_the_question });
        childAudioMap.put(2, new int[]{R.raw.who , R.raw.who_is_this ,R.raw.ask_the_question });
        childAudioMap.put(3, new int[]{R.raw.who , R.raw.who_is_this ,R.raw.ask_the_question });
        childAudioMap.put(4, new int[]{R.raw.who , R.raw.who_is_this ,R.raw.ask_the_question });
        childAudioMap.put(5, new int[]{R.raw.who , R.raw.who_is_this ,R.raw.ask_the_question });
        childAudioMap.put(6, new int[]{R.raw.who , R.raw.who_is_this ,R.raw.ask_the_question });
        childAudioMap.put(19, new int[]{R.raw.who , R.raw.who_is_this ,R.raw.ask_the_question });
        childAudioMap.put(20, new int[]{R.raw.who , R.raw.who_is_this ,R.raw.ask_the_question });
        childAudioMap.put(21, new int[]{R.raw.who , R.raw.who_is_this ,R.raw.ask_the_question });
        childAudioMap.put(22, new int[]{R.raw.who , R.raw.who_is_this ,R.raw.ask_the_question });
        childAudioMap.put(23, new int[]{R.raw.who , R.raw.who_is_this ,R.raw.ask_the_question });
        childAudioMap.put(24, new int[]{R.raw.who , R.raw.who_is_this ,R.raw.ask_the_question });

        childAudioMap.put(13, new int[]{R.raw.what , R.raw.audio_13 ,R.raw.ask_the_question });
        childAudioMap.put(14, new int[]{R.raw.what , R.raw.audio_14 ,R.raw.ask_the_question });
        childAudioMap.put(15, new int[]{R.raw.what , R.raw.audio_15 ,R.raw.ask_the_question });
        childAudioMap.put(16, new int[]{R.raw.what , R.raw.audio_16 ,R.raw.ask_the_question });
        childAudioMap.put(17, new int[]{R.raw.what , R.raw.audio_17 ,R.raw.ask_the_question });
        childAudioMap.put(18, new int[]{R.raw.what , R.raw.audio_18 ,R.raw.ask_the_question });
        childAudioMap.put(31, new int[]{R.raw.what , R.raw.audio_31 ,R.raw.ask_the_question });
        childAudioMap.put(32, new int[]{R.raw.what , R.raw.audio_32 ,R.raw.ask_the_question });
        childAudioMap.put(33, new int[]{R.raw.what , R.raw.audio_33 ,R.raw.ask_the_question });
        childAudioMap.put(34, new int[]{R.raw.what , R.raw.audio_34 ,R.raw.ask_the_question });
        childAudioMap.put(35, new int[]{R.raw.what , R.raw.audio_35 ,R.raw.ask_the_question });
        childAudioMap.put(36, new int[]{R.raw.what , R.raw.audio_36 ,R.raw.ask_the_question });

        childAudioMap.put(7, new int[]{R.raw.where , R.raw.audio_7 ,R.raw.ask_the_question });
        childAudioMap.put(8, new int[]{R.raw.where , R.raw.audio_8 ,R.raw.ask_the_question });
        childAudioMap.put(9, new int[]{R.raw.where , R.raw.audio_9 ,R.raw.ask_the_question });
        childAudioMap.put(10, new int[]{R.raw.where , R.raw.audio_10 ,R.raw.ask_the_question });
        childAudioMap.put(11, new int[]{R.raw.where , R.raw.audio_11 ,R.raw.ask_the_question });
        childAudioMap.put(12, new int[]{R.raw.where , R.raw.audio_12 ,R.raw.ask_the_question });
        childAudioMap.put(25, new int[]{R.raw.where , R.raw.audio_25 ,R.raw.ask_the_question });
        childAudioMap.put(26, new int[]{R.raw.where , R.raw.audio_26 ,R.raw.ask_the_question });
        childAudioMap.put(27, new int[]{R.raw.where , R.raw.audio_27 ,R.raw.ask_the_question });
        childAudioMap.put(28, new int[]{R.raw.where , R.raw.audio_28 ,R.raw.ask_the_question });
        childAudioMap.put(29, new int[]{R.raw.where , R.raw.audio_29 ,R.raw.ask_the_question });
        childAudioMap.put(30, new int[]{R.raw.where , R.raw.audio_30 ,R.raw.ask_the_question });





        btnNeutral.setOnClickListener(v -> {
            Log.d("BTN_NEUTRAL", "Clicked! Turn: " + currentTurn);

            switch (lastDicePlayer ) {
                case ROBOT:
                    int[] robotAudios = robotAudioMap.get(robotCell);
                    if (robotAudios != null && robotAudios.length > 0) {
                        int i = new Random().nextInt(robotAudios.length);
                        playAudio(robotAudios[i]);
                        Log.d("BTN_NEUTRAL", "Played robot audio for block " + robotCell);
                    } else {
                        Log.d("BTN_NEUTRAL", "No robot audio for block " + robotCell);
                    }
                    break;

                case CHILD:
                    int[] childAudios = childAudioMap.get(childCell);
                    if (childAudios != null && childAudios.length > 0) {
                        int i = new Random().nextInt(childAudios.length);
                        playAudio(childAudios[i]);
                        Log.d("BTN_NEUTRAL", "Played child audio for block " + childCell);
                    } else {
                        Log.d("BTN_NEUTRAL", "No child audio for block " + childCell);
                    }
                    break;

                default:
                    Log.d("BTN_NEUTRAL", "No audio for this player/turn");
            }
        });



        btnCorrect.setOnClickListener(v -> {
            //PlayerTurn next = getNextPlayer();

            if (currentTurn == PlayerTurn.CHILD) {
                playTurnTransitionAudio("good_answering"); // from robot -> child
            } else if (currentTurn == PlayerTurn.TEACHER) {
                playTurnTransitionAudio("good_asking"); // from child -> teacher
            }


            // Add your existing logic for what happens after correct button is clicked
        });

        btnRobotAnswer.setOnClickListener(v -> {
            int currentBlock = teacherCell;  // ✅ Always use teacherCell
            // Robot gives the answer for the block the TEACHER or CHILD landed on
            playBlockAudioForRobotAnswer(currentBlock);
        });

        btnDisengaged.setOnClickListener(v -> {
            // Stop any currently playing audio
            if (mediaPlayer != null) {
                mediaPlayer.release();
                mediaPlayer = null;
            }

            // Pick a random audio from the array
            int randomIndex = new Random().nextInt(disengagedAudios.length);
            String audioFileName = disengagedAudios[randomIndex];

            // Get the resource ID
            int audioResId = getResources().getIdentifier(audioFileName, "raw", getPackageName());

            // Play the selected audio
            if (audioResId != 0) {
                mediaPlayer = MediaPlayer.create(getApplicationContext(), audioResId);
                mediaPlayer.start();
            } else {
                Toast.makeText(this, "Audio file not found!", Toast.LENGTH_SHORT).show();
            }
        });

        //ImageButton btnWrong = findViewById(R.id.btnWrong);
        Log.d("SETUP", "btnWrong found and initialized");

        ImageButton btnWrong = findViewById(R.id.btnWrong);

        btnWrong.setOnClickListener(v -> {
            Log.d("AUDIO", "btnWrong clicked: currentTurn = " + currentTurn);

            // Only allow for CHILD or ROBOT turns
            if (lastDicePlayer != PlayerTurn.CHILD && lastDicePlayer != PlayerTurn.ROBOT) {
                Log.d("AUDIO", "Ignored: Not CHILD or ROBOT turn");
                return;
            }


            int currentBlock = (lastDicePlayer == PlayerTurn.CHILD) ? childCell : robotCell;

            Log.d("AUDIO", "Current block = " + currentBlock);

            // Only play who_is_this if block is between 1 and 6
            boolean shouldPlayAudio8 = (currentBlock == 8 || currentBlock == 29);
            boolean shouldPlayAudio9 = (currentBlock == 9 || currentBlock == 28);
            boolean shouldPlayAudio10 = (currentBlock == 10 || currentBlock == 27);
            boolean shouldPlayAudio11 = (currentBlock == 11 || currentBlock == 26);
            boolean shouldPlayAudio12 = (currentBlock == 12 || currentBlock == 25);
            boolean shouldPlayAudio13 = (currentBlock == 13 || currentBlock == 36);
            boolean shouldPlayAudio14 = (currentBlock == 14 || currentBlock == 35);
            boolean shouldPlayAudio15 = (currentBlock == 15 || currentBlock == 34);
            boolean shouldPlayAudio16 = (currentBlock == 16 || currentBlock == 33);
            boolean shouldPlayAudio17 = (currentBlock == 17 || currentBlock == 32);
            boolean shouldPlayAudio18 = (currentBlock == 18 || currentBlock == 31);
            boolean shouldPlayWhereDoesTeacherWork = (currentBlock == 7 || currentBlock == 30);
            boolean shouldPlayWhoIsThis = !shouldPlayWhereDoesTeacherWork && !shouldPlayAudio8 && !shouldPlayAudio9 && !shouldPlayAudio10 &&!shouldPlayAudio11 &&!shouldPlayAudio12 &&!shouldPlayAudio13 &&!shouldPlayAudio14 &&!shouldPlayAudio15 &&!shouldPlayAudio16 &&!shouldPlayAudio17 &&!shouldPlayAudio18 &&
                    ((currentBlock >= 1 && currentBlock <= 6) || (currentBlock >= 19 && currentBlock <= 24));


            // First: release any existing media player
            if (mediaPlayer != null) {
                try {
                    mediaPlayer.stop();
                } catch (IllegalStateException e) {
                    Log.e("AUDIO", "Error stopping player: " + e.getMessage());
                }
                mediaPlayer.release();
                mediaPlayer = null;
            }

            // Pick a random base "wrong" sound
            int[] wrongSounds = {
                    R.raw.lets_try_again,
                    R.raw.good_try_lets_do_this_once_more,
                    R.raw.this_is_wrong_lets_try_again
            };
            int baseSound = wrongSounds[new Random().nextInt(wrongSounds.length)];

            // Create and play base sound
            mediaPlayer = MediaPlayer.create(this, baseSound);
            if (mediaPlayer == null) {
                Log.e("AUDIO", "Failed to create mediaPlayer for baseSound");
                return;
            }

            mediaPlayer.setOnCompletionListener(mp -> {
                mp.release();
                mediaPlayer = null;

                if (shouldPlayWhereDoesTeacherWork) {
                    Log.d("AUDIO", "Playing where_does_the_teacher_work...");
                    mediaPlayer = MediaPlayer.create(this, R.raw.audio_7);
                    if (mediaPlayer != null) {
                        mediaPlayer.setOnCompletionListener(mp2 -> {
                            mp2.release();
                            mediaPlayer = null;
                            Log.d("AUDIO", "where_does_the_teacher_work completed.");
                        });
                        mediaPlayer.start();
                    }
                } else if (shouldPlayWhoIsThis) {
                    Log.d("AUDIO", "Playing who_is_this...");
                    mediaPlayer = MediaPlayer.create(this, R.raw.who_is_this);
                    if (mediaPlayer != null) {
                        mediaPlayer.setOnCompletionListener(mp2 -> {
                            mp2.release();
                            mediaPlayer = null;
                            Log.d("AUDIO", "who_is_this completed.");
                        });
                        mediaPlayer.start();
                    } else {
                        Log.e("AUDIO", "Could not create mediaPlayer for who_is_this");
                    }
                }else if (shouldPlayAudio8) {
                        Log.d("AUDIO", "Playing audio_8...");
                        mediaPlayer = MediaPlayer.create(this, R.raw.audio_8);
                        if (mediaPlayer != null) {
                            mediaPlayer.setOnCompletionListener(mp2 -> {
                                mp2.release();
                                mediaPlayer = null;
                                Log.d("AUDIO", "audio_8 completed.");
                            });
                            mediaPlayer.start();
                        }
                }else if (shouldPlayAudio9) {
                    Log.d("AUDIO", "Playing audio_9...");
                    mediaPlayer = MediaPlayer.create(this, R.raw.audio_9);
                    if (mediaPlayer != null) {
                        mediaPlayer.setOnCompletionListener(mp2 -> {
                            mp2.release();
                            mediaPlayer = null;
                            Log.d("AUDIO", "audio_8 completed.");
                        });
                        mediaPlayer.start();
                    }
                }else if (shouldPlayAudio10) {
                    Log.d("AUDIO", "Playing audio_10...");
                    mediaPlayer = MediaPlayer.create(this, R.raw.audio_10);
                    if (mediaPlayer != null) {
                        mediaPlayer.setOnCompletionListener(mp2 -> {
                            mp2.release();
                            mediaPlayer = null;
                            Log.d("AUDIO", "audio_8 completed.");
                        });
                        mediaPlayer.start();
                    }
                }else if (shouldPlayAudio11) {
                    Log.d("AUDIO", "Playing audio_11...");
                    mediaPlayer = MediaPlayer.create(this, R.raw.audio_11);
                    if (mediaPlayer != null) {
                        mediaPlayer.setOnCompletionListener(mp2 -> {
                            mp2.release();
                            mediaPlayer = null;
                            Log.d("AUDIO", "audio_8 completed.");
                        });
                        mediaPlayer.start();
                    }
                }else if (shouldPlayAudio12) {
                    Log.d("AUDIO", "Playing audio_12...");
                    mediaPlayer = MediaPlayer.create(this, R.raw.audio_12);
                    if (mediaPlayer != null) {
                        mediaPlayer.setOnCompletionListener(mp2 -> {
                            mp2.release();
                            mediaPlayer = null;
                            Log.d("AUDIO", "audio_8 completed.");
                        });
                        mediaPlayer.start();
                    }
                }else if (shouldPlayAudio13) {
                    Log.d("AUDIO", "Playing audio_13...");
                    mediaPlayer = MediaPlayer.create(this, R.raw.audio_13);
                    if (mediaPlayer != null) {
                        mediaPlayer.setOnCompletionListener(mp2 -> {
                            mp2.release();
                            mediaPlayer = null;
                            Log.d("AUDIO", "audio_8 completed.");
                        });
                        mediaPlayer.start();
                    }
                }
                else if (shouldPlayAudio14) {
                    Log.d("AUDIO", "Playing audio_14...");
                    mediaPlayer = MediaPlayer.create(this, R.raw.audio_14);
                    if (mediaPlayer != null) {
                        mediaPlayer.setOnCompletionListener(mp2 -> {
                            mp2.release();
                            mediaPlayer = null;
                            Log.d("AUDIO", "audio_8 completed.");
                        });
                        mediaPlayer.start();
                    }
                }else if (shouldPlayAudio15) {
                    Log.d("AUDIO", "Playing audio_15...");
                    mediaPlayer = MediaPlayer.create(this, R.raw.audio_15);
                    if (mediaPlayer != null) {
                        mediaPlayer.setOnCompletionListener(mp2 -> {
                            mp2.release();
                            mediaPlayer = null;
                            Log.d("AUDIO", "audio_8 completed.");
                        });
                        mediaPlayer.start();
                    }
                }else if (shouldPlayAudio16) {
                    Log.d("AUDIO", "Playing audio_16...");
                    mediaPlayer = MediaPlayer.create(this, R.raw.audio_16);
                    if (mediaPlayer != null) {
                        mediaPlayer.setOnCompletionListener(mp2 -> {
                            mp2.release();
                            mediaPlayer = null;
                            Log.d("AUDIO", "audio_8 completed.");
                        });
                        mediaPlayer.start();
                    }
                }else if (shouldPlayAudio17) {
                    Log.d("AUDIO", "Playing audio_17...");
                    mediaPlayer = MediaPlayer.create(this, R.raw.audio_17);
                    if (mediaPlayer != null) {
                        mediaPlayer.setOnCompletionListener(mp2 -> {
                            mp2.release();
                            mediaPlayer = null;
                            Log.d("AUDIO", "audio_8 completed.");
                        });
                        mediaPlayer.start();
                    }
                }else if (shouldPlayAudio18) {
                    Log.d("AUDIO", "Playing audio_18...");
                    mediaPlayer = MediaPlayer.create(this, R.raw.audio_18);
                    if (mediaPlayer != null) {
                        mediaPlayer.setOnCompletionListener(mp2 -> {
                            mp2.release();
                            mediaPlayer = null;
                            Log.d("AUDIO", "audio_8 completed.");
                        });
                        mediaPlayer.start();
                    }
                }
                else {
                    Log.d("AUDIO", "Skipping who_is_this: block not in 1-6");
                }
            });

            mediaPlayer.start();
        });


        dice1 = findViewById(R.id.dice1);
        dice2 = findViewById(R.id.dice2);
        dice3 = findViewById(R.id.dice3);
        dice4 = findViewById(R.id.dice4);
        dice5 = findViewById(R.id.dice5);
        dice6 = findViewById(R.id.dice6);

        setDiceTouchAnimation(dice1);
        setDiceTouchAnimation(dice2);
        setDiceTouchAnimation(dice3);
        setDiceTouchAnimation(dice4);
        setDiceTouchAnimation(dice5);
        setDiceTouchAnimation(dice6);

        dice1.setOnClickListener(v -> handleDiceClick(1));
        dice2.setOnClickListener(v -> handleDiceClick(2));
        dice3.setOnClickListener(v -> handleDiceClick(3));
        dice4.setOnClickListener(v -> handleDiceClick(4));
        dice5.setOnClickListener(v -> handleDiceClick(5));
        dice6.setOnClickListener(v -> handleDiceClick(6));

        btnBack.setOnClickListener(v -> finish());

        requestQueue = Volley.newRequestQueue(this);
        initializeImageMap();
    }

    private void initializeImageMap() {
            convImgMap.put(1, "who_teacher");
            convImgMap.put(2, "who_police");
            convImgMap.put(3, "who_doctor");
            convImgMap.put(4, "who_chef");
            convImgMap.put(5, "who_postman");
            convImgMap.put(6, "who_farmer");

            convImgMap.put(7, "where_teacher");
            convImgMap.put(8, "where_police");
            convImgMap.put(9, "where_doctor");
            convImgMap.put(10, "where_chef");
            convImgMap.put(11, "where_postman");
            convImgMap.put(12, "where_farmer");

            convImgMap.put(13, "what_teacher");
            convImgMap.put(14, "what_police");
            convImgMap.put(15, "what_doctor");
            convImgMap.put(16, "what_chef");
            convImgMap.put(17, "what_postman");
            convImgMap.put(18, "what_farmer");

            convImgMap.put(19, "who_farmer");
            convImgMap.put(20, "who_postman");
            convImgMap.put(21, "who_chef");
            convImgMap.put(22, "who_doctor");
            convImgMap.put(23, "who_police");
            convImgMap.put(24, "who_teacher");

            convImgMap.put(25, "where_farmer");
            convImgMap.put(26, "where_postman");
            convImgMap.put(27, "where_chef");
            convImgMap.put(28, "where_doctor");
            convImgMap.put(29, "where_police");
            convImgMap.put(30, "where_teacher");

            convImgMap.put(31, "what_farmer");
            convImgMap.put(32, "what_postman");
            convImgMap.put(33, "what_chef");
            convImgMap.put(34, "what_doctor");
            convImgMap.put(35, "what_police");
            convImgMap.put(36, "what_teacher_finish");

            // Continue as before...



    }

    private void setDiceTouchAnimation(ImageButton dice) {
        dice.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.shrink));
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.startAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.grow));
                    break;
            }
            return false;
        });
    }

    private void handleDiceClick(int number) {
        isFirstDiceClicked = true;

        int startCell = 0;

        // Step 1: Update cell for current player
        switch (currentTurn) {
            case ROBOT:
                startCell = robotCell + number;
                break;
            case CHILD:
                startCell = childCell + number;
                Log.d("DICE_CLICK", "CHILD moved to block: " + childCell);

                break;
            case TEACHER:
                teacherCell += number;
                startCell = teacherCell;
                //playBlockAudioForRobotAnswer(teacherCell);
                break;
        }

        // Clamp to max board size
        if (startCell > 36) startCell = 36;

        // Step 2: Apply snakes/ladders
        int finalCell = applySnakesAndLadders(startCell);

        if (currentTurn == PlayerTurn.ROBOT) {
            robotCell = finalCell;  // update position
            lastRobotQuestionBlock = robotCell;// Update robot position
            playRobotBlockAudio(robotCell);  // Now use the correct cell
        }



        // Step 3: Update player's position to final cell
        switch (currentTurn) {
            case ROBOT: robotCell = finalCell; break;
            case CHILD: previousChildCell = childCell; // Save current position BEFORE moving
                childCell = finalCell;
                Log.d("DICE_CLICK", "CHILD moved to block (final): " + childCell);
                break;
            case TEACHER: teacherCell = finalCell; break;
        }

        // Step 4: Show image for FINAL position (after snake/ladder applied)
        String imageKey = convImgMap.get(finalCell);
        if (imageKey != null) {
            int imageResId = getResources().getIdentifier(imageKey, "drawable", getPackageName());
            if (imageResId != 0) {
                conversationImage.setImageResource(imageResId);
                conversationImage.setVisibility(View.VISIBLE);
            } else {
                conversationTitle.setText("Image not found for key: " + imageKey);
                conversationImage.setVisibility(View.GONE);
            }
        } else {
            conversationTitle.setText("No image mapped for block " + finalCell);
            conversationImage.setVisibility(View.GONE);
        }

        // Step 5: Show next turn + UI logic
        if (turnCount == 0) {
            nextTurnTextView.setVisibility(View.VISIBLE);
        }

        turnCount++;
        updateTurnUI();
        lastDicePlayer = currentTurn;  // ✅ Store who rolled the dice

        nextTurn();
        //updateTurnUI();
    }



    private int applySnakesAndLadders(int position) {
        if (position > 36) return 36;
        if (snakesAndLadders.containsKey(position)) {
            return snakesAndLadders.get(position);
        }
        return position;
    }

    private void nextTurn() {
        switch (currentTurn) {
            case ROBOT: currentTurn = PlayerTurn.CHILD; break;
            case CHILD: currentTurn = PlayerTurn.TEACHER; break;
            case TEACHER: currentTurn = PlayerTurn.ROBOT; break;
        }
    }

    private void updateTurnUI() {
        turnTextView.setText("Turn to ask: " + currentTurn.name());

        PlayerTurn nextPlayer;
        switch (currentTurn) {
            case ROBOT: nextPlayer = PlayerTurn.CHILD; break;
            case CHILD: nextPlayer = PlayerTurn.TEACHER; break;
            case TEACHER: nextPlayer = PlayerTurn.ROBOT; break;
            default: nextPlayer = PlayerTurn.ROBOT;
        }

        nextTurnTextView.setText("Next to roll dice: " + nextPlayer.name());

        if (!isFirstDiceClicked) {
            feedbackButtons.setVisibility(View.GONE);
            btnRobotAnswer.setVisibility(View.GONE);
            return;
        }


        turnCount++;
        if ((currentTurn == PlayerTurn.ROBOT && nextPlayer == PlayerTurn.CHILD) ||
                (currentTurn == PlayerTurn.CHILD && nextPlayer == PlayerTurn.TEACHER)) {
            feedbackButtons.setVisibility(View.VISIBLE);
            btnRobotAnswer.setVisibility(View.GONE);
        } else if (currentTurn == PlayerTurn.TEACHER && nextPlayer == PlayerTurn.ROBOT) {
            feedbackButtons.setVisibility(View.GONE);
            btnRobotAnswer.setVisibility(View.VISIBLE);
        } else {
            feedbackButtons.setVisibility(View.GONE);
            btnRobotAnswer.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onDestroy() {
        if (mediaPlayer != null) mediaPlayer.release();
        super.onDestroy();
    }
}
