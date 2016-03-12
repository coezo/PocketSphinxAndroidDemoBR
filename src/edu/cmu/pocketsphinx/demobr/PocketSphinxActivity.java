package edu.cmu.pocketsphinx.demobr;

import static android.widget.Toast.makeText;
import static edu.cmu.pocketsphinx.Assets.syncAssets;
import static edu.cmu.pocketsphinx.SpeechRecognizerSetup.defaultSetup;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;


public class PocketSphinxActivity extends Activity implements
        RecognitionListener {

    private static final String KWS_SEARCH_NAME = "acorde";
    private static final String FREE_SEARCH = "livre";
    private static final String DIGITS_SEARCH = "números";
    private static final String MENU_SEARCH = "menu";
    private static final String KEYPHRASE = "oh poderoso computador";

    private SpeechRecognizer recognizer;
    private final Map<String, Integer> captions = new HashMap<String, Integer>();

    public PocketSphinxActivity() {
        captions.put(KWS_SEARCH_NAME, R.string.kws_caption);
        captions.put(MENU_SEARCH, R.string.menu_caption);
        captions.put(DIGITS_SEARCH, R.string.digits_caption);
        captions.put(FREE_SEARCH, R.string.forecast_caption);
    }

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        File appDir;

        try {
            appDir = syncAssets(getApplicationContext());
        } catch (IOException e) {
            throw new RuntimeException("failed to synchronize assets", e);
        }

        recognizer = defaultSetup()
                .setAcousticModel(new File(appDir, "models/hmm/pt-br-semi"))
                .setDictionary(new File(appDir, "models/lm/constituicao.dic"))
                .setRawLogDir(appDir)
                .setKeywordThreshold(1e-5f)
                .getRecognizer();

        recognizer.addListener(this);
        // Create keyword-activation search.
        recognizer.addKeywordSearch(KWS_SEARCH_NAME, KEYPHRASE);
        // Create grammar-based searches.
        File menuGrammar = new File(appDir, "models/grammar/menu.gram");
        recognizer.addGrammarSearch(MENU_SEARCH, menuGrammar);
        File digitsGrammar = new File(appDir, "models/grammar/digits.gram");
        recognizer.addGrammarSearch(DIGITS_SEARCH, digitsGrammar);
        // Create language model search.
        File languageModel = new File(appDir, "models/lm/ceten.lm.dmp");
        recognizer.addNgramSearch(FREE_SEARCH, languageModel);

        setContentView(R.layout.main);
        switchSearch(KWS_SEARCH_NAME);
    }

    @Override
    public void onPartialResult(Hypothesis hypothesis) {
        String text = hypothesis.getHypstr();
        Log.d(getClass().getSimpleName(), "on partial: " + text);

        if (text.equals(KEYPHRASE))
            switchSearch(MENU_SEARCH);
        else if (text.equals(DIGITS_SEARCH))
            switchSearch(DIGITS_SEARCH);
        else if (text.equals(FREE_SEARCH))
            switchSearch(FREE_SEARCH);
        else
            ((TextView) findViewById(R.id.result_text)).setText(text);
    }

    @Override
    public void onResult(Hypothesis hypothesis) {
        ((TextView) findViewById(R.id.result_text)).setText("");
        String text = hypothesis.getHypstr();
        makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
    }

    private void switchSearch(String searchName) {
        recognizer.stop();
        recognizer.startListening(searchName);

        String caption = getResources().getString(captions.get(searchName));
        ((TextView) findViewById(R.id.caption_text)).setText(caption);
    }

    @Override
    public void onBeginningOfSpeech() {
    }

    @Override
    public void onEndOfSpeech() {
        if (DIGITS_SEARCH.equals(recognizer.getSearchName())
                || FREE_SEARCH.equals(recognizer.getSearchName()))
            switchSearch(KWS_SEARCH_NAME);
    }
}