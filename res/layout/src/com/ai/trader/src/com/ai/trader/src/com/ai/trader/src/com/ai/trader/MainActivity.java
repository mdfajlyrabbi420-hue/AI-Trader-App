package com.ai.trader;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.widget.TextView;
import android.speech.tts.TextToSpeech;
import android.widget.Toast;
import java.util.Locale;
import java.io.File;
import org.json.JSONObject;

public class MainActivity extends Activity implements TextToSpeech.OnInitListener {

    private TextView txtSignal, txtDetails;
    private ScreenshotObserver observer;
    private TextToSpeech tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtSignal = findViewById(R.id.txtSignal);
        txtDetails = findViewById(R.id.txtDetails);
        
        tts = new TextToSpeech(this, this);

        String screenshotPath = Environment.getExternalStorageDirectory().toString() + "/DCIM/Screenshots";
        
        File folder = new File(screenshotPath);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        
        observer = new ScreenshotObserver(screenshotPath, new ScreenshotObserver.OnScreenshotDetectedListener() {
            @Override
            public void onScreenshotDetected(final String path) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        txtSignal.setText("ANALYZING CHART...");
                        txtSignal.setTextColor(0xFFFFB300); 
                        txtDetails.setText("AI is processing the screenshot. Checking market structure, liquidity zones, and price action...");
                    }
                });

                GeminiAnalyzer.analyzeChart(path, new GeminiAnalyzer.AnalysisCallback() {
                    @Override
                    public void onResult(final String jsonResponse) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    String cleanJson = jsonResponse.trim().replace("```json", "").replace("```", "");
                                    JSONObject obj = new JSONObject(cleanJson);
                                    
                                    String signal = obj.optString("signal", "No Trade");
                                    String confidence = obj.optString("confidence_score", "N/A");
                                    String entry = obj.optString("entry_point", "N/A");
                                    String sl = obj.optString("stop_loss", "N/A");
                                    String tp = obj.optString("take_profit", "N/A");
                                    String rr = obj.optString("risk_reward_ratio", "N/A");
                                    String duration = obj.optString("trade_duration", "N/A");
                                    String explanation = obj.optString("detailed_explanation", "");

                                    txtSignal.setText(signal + " (" + confidence + ")");
                                    
                                    if(signal.equalsIgnoreCase("BUY")) {
                                        txtSignal.setTextColor(0xFF00E676); 
                                    } else if(signal.equalsIgnoreCase("SELL")) {
                                        txtSignal.setTextColor(0xFFFF1744); 
                                    } else {
                                        txtSignal.setTextColor(0xFF9E9E9E); 
                                    }
                                    
                                    String report = "📊 MARKET ANALYSIS REPORT:\n\n" +
                                            "• Entry Point: " + entry + "\n" +
                                            "• Stop Loss (SL): " + sl + "\n" +
                                            "• Take Profit (TP): " + tp + "\n" +
                                            "• Risk/Reward Ratio: " + rr + "\n" +
                                            "• Duration: " + duration + "\n\n" +
                                            "🤖 Technical Reason:\n" + explanation;
                                            
                                    txtDetails.setText(report);
                                    speak(signal);

                                } catch (Exception e) {
                                    txtDetails.setText("JSON Parsing Error. Raw response:\n" + jsonResponse);
                                    speak("Error");
                                }
                            }
                        });
                    }

                    @Override
                    public void onError(final String error) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                txtSignal.setText("ERROR");
                                txtSignal.setTextColor(0xFFFF1744);
                                txtDetails.setText("Network/API Error: " + error);
                                speak("Error");
                            }
                        });
                    }
                });
            }
        });
        
        observer.startWatching();
        Toast.makeText(this, "AI Trading Radar Active!", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            tts.setLanguage(Locale.US);
        }
    }

    private void speak(String text) {
        if (tts != null) {
            String message = text;
            if(text.equalsIgnoreCase("BUY")) message = "Order Alert. Buy. Buy Now.";
            if(text.equalsIgnoreCase("SELL")) message = "Order Alert. Sell. Sell Now.";
            tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    @Override
    protected void onDestroy() {
        if (observer != null) observer.stopWatching();
        if (tts != null) {
            
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}
