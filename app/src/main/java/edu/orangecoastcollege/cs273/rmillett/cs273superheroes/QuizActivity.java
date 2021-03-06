package edu.orangecoastcollege.cs273.rmillett.cs273superheroes;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <code>QuizActivity</code> is an interactive quiz that displays random images of CS273 students
 * drawn from a database and prompts the user to match the correct name, super power, or piece of
 * trivia depending on which mode of play has been selected
 */
public class QuizActivity extends AppCompatActivity {

    public static final String TAG = "Superhero Quiz";
    public static final int QUESTIONS_IN_QUIZ = 10;

    private Button[] mButtons = new Button[4];
    private LinearLayout[] mLinearLayouts = new LinearLayout[2];

    private List<Superhero> mAllSuperheroesList;
    private List<Superhero> mQuizSuperheroesList;

    private List<String> mQuizTypeList;

    private Superhero mCorrectSuperhero;
    private String mCorrectAnswer;

    private int mTotalGuesses;
    private int mCorrectGuesses;

    private SecureRandom rng;

    private Handler handler;

    private TextView mQuestionNumberTextView;
    private ImageView mSuperheroImageView;
    private TextView mQuestionTextView;
    private TextView mAnswerTextView;

    private String mQuizType;

    public static final String QUIZ_TYPE = "quizTypes";

    /**
     * Creates an instance of <code>QuizActivity</code> in the view
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        mQuizSuperheroesList = new ArrayList<>(QUESTIONS_IN_QUIZ);
        rng = new SecureRandom();
        handler = new Handler();

        // Connect GUI
        mQuestionNumberTextView = (TextView) findViewById(R.id.questionNumberTextView);
        mSuperheroImageView = (ImageView) findViewById(R.id.superHeroImageView);
        mQuestionTextView = (TextView) findViewById(R.id.questionTextView);
        mAnswerTextView = (TextView) findViewById(R.id.answerTextView);

        // Add buttons to mButtons array
        mButtons[0] = (Button) findViewById(R.id.button);
        mButtons[1] = (Button) findViewById(R.id.button2);
        mButtons[2] = (Button) findViewById(R.id.button3);
        mButtons[3] = (Button) findViewById(R.id.button4);

        // Initialize LinearLayouts
        mLinearLayouts[0] = (LinearLayout) findViewById(R.id.row1LinearLayout);
        mLinearLayouts[1] = (LinearLayout) findViewById(R.id.row2LinearLayout);

        // Set mQuestionNumberTextView's text
        mQuestionNumberTextView.setText(getString(R.string.question, 1, QUESTIONS_IN_QUIZ));

        // Load all superheroes from the JSON file using JSONLoader
        try {
            mAllSuperheroesList = JSONLoader.loadJSONFromAsset(this);
        }
        catch (IOException e) {
            Log.e(TAG, "Error loading JSON file", e);
        }

        // Register the OnSharedPreferencesChangeListener
        SharedPreferences mPreferences = android.preference.PreferenceManager.getDefaultSharedPreferences(this);
        mPreferences.registerOnSharedPreferenceChangeListener(mPreferenceChangeListener);

        // get preferences
        mQuizType = mPreferences.getString(getString(R.string.pref_key), getString(R.string.pref_default));
        //updateQuizType();

        // Reset quiz
        resetQuiz();
    }

    private void updateQuizType() {
        // set correct answer and populate lists based on preferences
        mQuizTypeList  = new ArrayList<>();

        if (mQuizType.equals(getString(R.string.guess_superhero))) {
            mCorrectAnswer = mCorrectSuperhero.getName();
            for (Superhero s : mAllSuperheroesList)
                mQuizTypeList.add(s.getName());
        }
        else if (mQuizType.equals(getString(R.string.guess_super_power))) {
            mCorrectAnswer = mCorrectSuperhero.getSuperpower();
            for (Superhero s : mAllSuperheroesList)
                mQuizTypeList.add(s.getSuperpower());
        }
        else if (mQuizType.equals(getString(R.string.guess_one_thing))) {
            mCorrectAnswer = mCorrectSuperhero.getOneThing();
            for (Superhero s : mAllSuperheroesList)
                mQuizTypeList.add(s.getOneThing());
        }

        Log.e(TAG, "updateQuizType() --> " + "mQuizTypeList.size() = " + mQuizTypeList.size());
        Log.e(TAG, "updateQuizType() --> " + "mQuizType = " + mQuizType);
    }

    /**
     * Initializes random questions and starts a new quiz
     */
    public void resetQuiz() {
        // reset guesses, clear list
        mCorrectGuesses = 0;
        mTotalGuesses = 0;
        mQuizSuperheroesList.clear();

        mQuestionTextView.setText(getString(R.string.question_type, mQuizType));

        // randomly add superheroes from all list into quiz list
        int size = mAllSuperheroesList.size();
        int randomPosition;
        Superhero randomSuperhero;
        while (mQuizSuperheroesList.size() < QUESTIONS_IN_QUIZ) {
            randomPosition = rng.nextInt(size);
            randomSuperhero = mAllSuperheroesList.get(randomPosition);

            if (!mQuizSuperheroesList.contains(randomSuperhero))
                mQuizSuperheroesList.add(randomSuperhero);
        }
        Log.e(TAG, "resetQuiz() --> " + "mQuizSuperheroesList.size() = " + mQuizSuperheroesList.size());
        loadNextImage();
    }

    /**
     * Initiates the process of loading the next image for the quiz, showing the
     * superhero's image along with 4 buttons, one of which contains the correct
     * answer.
     */
    public void loadNextImage() {
        mCorrectSuperhero = mQuizSuperheroesList.remove(0);
        mAnswerTextView.setText("");

        // update question number
        int questionNumber = QUESTIONS_IN_QUIZ - mQuizSuperheroesList.size();
        mQuestionNumberTextView.setText(getString(R.string.question, questionNumber, QUESTIONS_IN_QUIZ));

        // display correct image
        AssetManager am = getAssets();
        try {
            InputStream stream = am.open(mCorrectSuperhero.getFileName());
            Drawable image = Drawable.createFromStream(stream, mCorrectSuperhero.getName());
            mSuperheroImageView.setImageDrawable(image);
        }
        catch (IOException e) {
            Log.e(TAG, "Error loading image: " + mCorrectSuperhero.getFileName());
        }

        updateQuizType();

        // shuffle order of all the superheroes
        do {
            Collections.shuffle(mQuizTypeList);
            Log.e(TAG, "inside loadNextImage --> mQuizTypeList.size() = " + mQuizTypeList.size());
        }while (mQuizTypeList.subList(0, mButtons.length).contains(mCorrectAnswer));

        // enable all 4 buttons, set to superhero names
        for (int i = 0; i < mButtons.length; ++i) {
            mButtons[i].setEnabled(true);
            mButtons[i].setText(mQuizTypeList.get(i));
        }

        // randomly replace one of the buttons with correct answer
        mButtons[rng.nextInt(mButtons.length)].setText(mCorrectAnswer);
    }

    /**
     * Handles the click event of one of the 4 buttons indicating the guess of a superhero's name
     * to match the superhero image displayed.  If the guess is correct, the superhero's name
     * (in GREEN) will be shown, followed by a slight delay of 2 seconds, then the next image will
     * be loaded. Otherwise, the word "Incorrect Guess" will be shown in RED and the button will be
     * disabled.
     * @param view
     */
    public void makeGuess(View view) {
        Button clickedButton = (Button) view;
        String guess = clickedButton.getText().toString();

        mTotalGuesses++;

        if (guess.equals(mCorrectAnswer)) {
            mCorrectGuesses++;
            for (Button b : mButtons) b.setEnabled(false);
            mAnswerTextView.setText(mCorrectAnswer);
            mAnswerTextView.setTextColor(ContextCompat.getColor(this, R.color.correct_answer));

            if (mCorrectGuesses < QUESTIONS_IN_QUIZ) {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        loadNextImage();
                    }
                }, 2000);
            }
            else {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage(getString(R.string.results, mTotalGuesses, 100.0 * mCorrectGuesses / mTotalGuesses));
                builder.setPositiveButton(getString(R.string.restarting_quiz), new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i)
                    {
                        resetQuiz();
                    }
                });
                builder.setCancelable(false);
                builder.create();
                builder.show();
            }
        }
        else {
            clickedButton.setEnabled(false);
            mAnswerTextView.setText(getString(R.string.incorrect_answer));
            mAnswerTextView.setTextColor(ContextCompat.getColor(this, R.color.incorrect_answer));
        }

    }

    private SharedPreferences.OnSharedPreferenceChangeListener mPreferenceChangeListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            mQuizType = sharedPreferences.getString(getString(R.string.pref_key), getString(R.string.pref_default));
            //updateQuizType();
            resetQuiz();

            Toast.makeText(QuizActivity.this, R.string.restarting_quiz, Toast.LENGTH_SHORT).show();
        }
    };

    /**
     * Override onCreateOptionsMenu to inflate the settings menu within QuizActivity
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_settings, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Responds to user clicking the Settings menu icon
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // Make a new Intent going to SettingsActivity
        Intent settingsIntent = new Intent(this, SettingsActivity.class);
        startActivity(settingsIntent);

        return super.onOptionsItemSelected(item);
    }
}
