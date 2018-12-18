package edu.illinois.cs.cs125.lab11;

import android.os.AsyncTask;
import android.transition.ChangeBounds;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.view.animation.BounceInterpolator;
import android.widget.TextView;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import javax.net.ssl.HttpsURLConnection;

/** AsyncTask to call the face++ api.
 */
public class CallAPI extends AsyncTask<String, String, String> {
    /**Store API response. */
    private String response = "";
    /**Response code to request. */
    private int responseCode;
    /**Reference to Main Activity. */
    private WeakReference<MainActivity> activityReference;

    /** Send request face++ API.
     * @param context reference to MainActivity
     */
    public CallAPI(final MainActivity context) {
        activityReference = new WeakReference<>(context);
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        MainActivity activity = activityReference.get();
        TextView responseTextView = activity.findViewById(R.id.textView_response);
        activity.changeResultText("Fetching results...");
    }

    @Override
    protected String doInBackground(final String... params) {
        MainActivity activity = activityReference.get();
        TextView responseTextView = activity.findViewById(R.id.textView_response);
        String urlString = params[0];
        String attributes = params[1];
        String base64 = params[2];
        try {

            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setReadTimeout(10000);
            connection.setConnectTimeout(15000);
            connection.setRequestMethod("POST");
            connection.setDoInput(true);
            connection.setDoOutput(true);

            List<NameValuePair> paramss = new ArrayList<NameValuePair>();
            paramss.add(new BasicNameValuePair("api_key", "0HFtUVIIWdcDNHFmAXYiCEetnogHt_xe"));
            paramss.add(new BasicNameValuePair("api_secret", "iZ5zKHrfpXq9SdNvQ3rfFqUfE7gSRDlF"));
            paramss.add(new BasicNameValuePair("image_base64", base64));
            paramss.add(new BasicNameValuePair("return_attributes", attributes));

            OutputStream outputStream = connection.getOutputStream();
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(outputStream, "UTF-8"));
            writer.write(getAppend(paramss));
            writer.flush();
            writer.close();
            outputStream.close();
            connection.connect();
            responseCode = connection.getResponseCode();
            System.out.println(responseCode);
            if (responseCode == HttpsURLConnection.HTTP_OK) {
                String line;
                BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                while ((line = br.readLine()) != null) {
                    response += line;
                }
            } else if (responseCode == 401 || responseCode == 403) {
                activity.changeResultText("API Authentication error.");
                response = "nope";
            } else {
                activity.changeResultText("File too large.");
                response = "nope";

            }
            connection.disconnect();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        return response;
    }
    @Override
    protected void onPostExecute(final String result) {
        MainActivity activity = activityReference.get();
        TextView responseTextView = activity.findViewById(R.id.textView_response);
        try {
            JSONObject obj = new JSONObject(result);
            JSONArray faces = obj.getJSONArray("faces");

            if (faces.length() == 0) {
                activity.changeResultText("No face found!");
            }

            Object facesString = faces.get(0);
            JSONObject facesJSON = (JSONObject) facesString;
            JSONObject attributes = facesJSON.getJSONObject("attributes");
            System.out.println(attributes);
            JSONObject gender = attributes.getJSONObject("gender");
            JSONObject age = attributes.getJSONObject("age");
            JSONObject ethnicity = attributes.getJSONObject("ethnicity");
            JSONObject beauty = attributes.getJSONObject("beauty");
            Double maleScore = beauty.getDouble("male_score");
            Double femaleScore = beauty.getDouble("female_score");
            String beautyString = String.format(Locale.US, "%.2f", (maleScore + femaleScore) / 2);
            String ethnicityString = ethnicity.getString("value");
            String genderString = gender.getString("value");
            String ageString = age.getString("value");

            JSONObject emotion = attributes.getJSONObject("emotion");
            String emotionString = "neutral";
            double maxCertainty = -1.0;
            Iterator<String> emotionIterator = emotion.keys();
            while (emotionIterator.hasNext()) {
                String thisEmotionString = emotionIterator.next();

                try {
                    double thisCertainty = emotion.getDouble(thisEmotionString);
                    if (thisCertainty > maxCertainty) {
                        emotionString = thisEmotionString;
                        maxCertainty = thisCertainty;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            // Smooth transition
            Transition changeTransition = new ChangeBounds();
            changeTransition.setDuration(1000L);
            changeTransition.setInterpolator(new BounceInterpolator());
            TransitionManager.beginDelayedTransition(activity.findViewById(R.id.tableLayout));

            activity.changeResultText("Gender: " + genderString + "\nAge: " + ageString
                + "\nEthnicity: " + ethnicityString + "\nBeauty: " + beautyString
                + "\nEmotion: " + emotionString + " (confidence: " + maxCertainty + "%)");
        } catch (JSONException e) {
            e.printStackTrace();
            if (responseCode == 200) {
                activity.changeResultText("No face found");
                System.out.println(result);
            }
        }
    }

    /** Create a name value pair.
     * @param parameters params to include in the encoding
     * @return A url encoded name value pair
     * @throws UnsupportedEncodingException make sure encoding is legal
     */
    private String getAppend(final List<NameValuePair> pairs) throws UnsupportedEncodingException {
        boolean onFirst = true;
        String finalStr = "";

        for (int i = 0; i < pairs.size(); i++) {
            if (!onFirst) {
                finalStr += "&";
            } else {
                onFirst = false;   
            }
            NameValuePair thisPair = pairs.get(i);
            finalStr += URLEncoder.encode(thisPair.getName(), "UTF-8") + "=" + URLEncoder.encode(thisPair.getValue(), "UTF-8");
        }

        return finalStr;
    }

}
