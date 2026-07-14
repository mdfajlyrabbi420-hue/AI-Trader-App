package com.ai.trader;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import org.json.JSONObject;
import org.json.JSONArray;

public class GeminiAnalyzer {

    // আপনার দেওয়া সঠিক API Key এখানে সেট করা হয়েছে
    private static final String API_KEY = "AQ.Ab8RN6KcmOXI2iXdKDBtjRBXXB3wbl4MGi1_eW-3jWHEemRsXQ"; 
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + API_KEY;

    public interface AnalysisCallback {
        void onResult(String jsonResponse);
        void onError(String error);
    }

    public static void analyzeChart(final String imagePath, final AnalysisCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Bitmap bm = BitmapFactory.decodeFile(imagePath);
                    if (bm == null) {
                        callback.onError("Failed to decode image file.");
                        return;
                    }
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    bm.compress(Bitmap.CompressFormat.JPEG, 70, baos); 
                    byte[] b = baos.toByteArray();
                    String encodedImage = Base64.encodeToString(b, Base64.DEFAULT).trim().replace("\n", "");

                    String promptText = "You are an expert financial market analyst. Analyze this chart (TradingView/Quotex/Binomo/Pocket Option). " +
                            "Examine: Candlestick Pattern, Market Structure, Trend, Support/Resistance, Breakout/Fakeout, Liquidity Zone, EMA, SMA, RSI, MACD, Bollinger Bands, Volume, ATR, Price Action. " +
                            "Predict the next candle direction. If uncertain, return 'No Trade'. " +
                            "You MUST respond ONLY with a raw JSON object matching this structure exactly, with no markdown formatting: " +
                            "{\"signal\":\"BUY/SELL/No Trade\",\"entry_point\":\"...\",\"stop_loss\":\"...\",\"take_profit\":\"...\",\"risk_reward_ratio\":\"...\",\"confidence_score\":\"...%\",\"green_candle_probability\":\"...%\",\"red_candle_probability\":\"...%\",\"suitable_time_to_trade\":\"...\",\"trade_duration\":\"...\",\"detailed_explanation\":\"...\"}";

                    JSONObject jsonBody = new JSONObject();
                    JSONArray contents = new JSONArray();
                    JSONObject contentObj = new JSONObject();
                    JSONArray parts = new JSONArray();

                    JSONObject textPart = new JSONObject();
                    textPart.put("text", promptText);
                    parts.put(textPart);

                    JSONObject imagePart = new JSONObject();
                    JSONObject inlineData = new JSONObject();
                    inlineData.put("mimeType", "image/jpeg");
                    inlineData.put("data", encodedImage);
                    imagePart.put("inlineData", inlineData);
                    parts.put(imagePart);

                    contentObj.put("parts", parts);
                    contents.put(contentObj);
                    jsonBody.put("contents", contents);

                    URL url = new URL(API_URL);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);
                    conn.setConnectTimeout(30000); 
                    conn.setReadTimeout(30000);

                    OutputStream os = conn.getOutputStream();
                    os.write(jsonBody.toString().getBytes("UTF-8"));
                    os.close();

                    int responseCode = conn.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = br.readLine()) != null) {
                            sb.append(line);
                        }
                        br.close();

                        JSONObject responseJson = new JSONObject(sb.toString());
                        String aiText = responseJson.getJSONArray("candidates")
                                .getJSONObject(0)
                                .getJSONObject("content")
                                .getJSONArray("parts")
                                .getJSONObject(0)
                                .getString("text");

                        callback.onResult(aiText);
                    } else {
                        callback.onError("Server returned error code: " + responseCode);
                    }

                } catch (Exception e) {
                    callback.onError("Analysis failed: " + e.getMessage());
                }
            }
        }).start();
    }
}
