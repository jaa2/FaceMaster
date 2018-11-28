package edu.illinois.cs.cs125.lab11;

import android.os.AsyncTask;
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
import java.util.List;
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
        responseTextView.setText("Fetching results");

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
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000);
            conn.setConnectTimeout(15000);
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);

            List<NameValuePair> paramss = new ArrayList<NameValuePair>();
            paramss.add(new BasicNameValuePair("api_key", ""));
            paramss.add(new BasicNameValuePair("api_secret", ""));
            paramss.add(new BasicNameValuePair("image_base64", base64));
            paramss.add(new BasicNameValuePair("return_attributes", attributes));

            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(os, "UTF-8"));
            writer.write(getQuery(paramss));
            writer.flush();
            writer.close();
            os.close();
            conn.connect();
            responseCode = conn.getResponseCode();
            System.out.println(responseCode);
            if (responseCode == HttpsURLConnection.HTTP_OK) {
                String line;
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                while ((line = br.readLine()) != null) {
                    response += line;
                }
            } else {
                responseTextView.setText("File too large");
                response = "nope";

            }
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
            String beautyString = String.format("%.2f", (maleScore + femaleScore) / 2);
            String ethnicityString = ethnicity.getString("value");
            String genderString = gender.getString("value");
            String ageString = age.getString("value");
            responseTextView.setText("gender = " + genderString + " age = "
                    + ageString + " ethnicity = " + ethnicityString + " beauty = " + beautyString);
        } catch (JSONException e) {
            e.printStackTrace();
            if (responseCode == 200) {
                responseTextView.setText("No face found");
            }
        }
    }

    /** Create a name value pair.
     * @param params params to include in the encoding
     * @return A url encoded name value pair
     * @throws UnsupportedEncodingException make sure encoding is legal
     */
    private String getQuery(final List<NameValuePair> params) throws UnsupportedEncodingException {
        StringBuilder result = new StringBuilder();
        boolean first = true;

        for (NameValuePair pair : params) {
            if (first) {
                first = false;
            } else {
                result.append("&");
            }
            result.append(URLEncoder.encode(pair.getName(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(pair.getValue(), "UTF-8"));
        }

        return result.toString();
    }

}