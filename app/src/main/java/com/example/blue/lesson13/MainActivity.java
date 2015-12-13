package com.example.blue.lesson13;

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class MainActivity extends Activity {
    SQLiteDatabase db;
    TextView tv;
    EditText searchText;

    private static final String QUERY_URL = "http://openlibrary.org/search.json?q=";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        db = openOrCreateDatabase("book.db", Context.MODE_PRIVATE, null);
        db.execSQL("DROP TABLE IF EXISTS book");
        db.execSQL("CREATE TABLE book (_id INTEGER PRIMARY KEY AUTOINCREMENT, title VARCHAR, author VARCHAR, year VARCHAR)");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setFocusable(true);
        layout.setFocusableInTouchMode(true);

        searchText = new EditText(this);
        searchText.setWidth(200);

        Button searchButton = new Button(this);
        searchButton.setText("Search");
        searchButton.setOnClickListener(searchButtonListener);

        tv = new TextView(this);
        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(tv);

        layout.addView(searchText);
        layout.addView(searchButton);
        layout.addView(scrollView);

        setContentView(layout);


    }

    View.OnClickListener searchButtonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(v.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            db.execSQL("DELETE FROM book");
            db.execSQL("DELETE FROM sqlite_sequence WHERE name = 'book'");
            doQuery(searchText.getText().toString());
        }
    };


    private void doQuery(String searchString) {

        String urlString = "";

        try {
            urlString = URLEncoder.encode(searchString, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        AsyncHttpClient client = new AsyncHttpClient();

        client.get(QUERY_URL + urlString,
                new JsonHttpResponseHandler() {
                    @Override
                    public void onSuccess(JSONObject jsonObject) {
                        JSONArray storesArray = jsonObject.optJSONArray("docs");
                        for (int i = 0; i < storesArray.length(); i++) {

                            JSONObject store = null;

                            String title = null, author = null, year = null;
                            try {
                                store = storesArray.getJSONObject(i);
                                if (store.has("title")) {
                                    title = store.getString("title");
                                }

                                if (store.has("author_name")) {
                                    author = store.getString("author_name");
                                }
                                if (store.has("publish_year")) {
                                    year = store.getString("publish_year");
                                }
                                db.execSQL("INSERT INTO book VALUES ( NULL, ?, ?, ?)",
                                        new Object[]{
                                                title,
                                                author,
                                                year,
                                        }
                                );


                            } catch (JSONException e) {
                                e.printStackTrace();
                            }


                        }
                        Cursor resultSet = db.rawQuery("Select * from book", null);
                        int rowCount = resultSet.getCount();
                        resultSet.moveToFirst();

                        String displayString = "";
                        for (int i = 0; i < rowCount; i++) {
                            displayString = displayString + resultSet.getInt(0);
                            displayString = displayString + " ";
                            displayString = displayString + resultSet.getString(1);
                            displayString = displayString + " ";
                            displayString = displayString + resultSet.getString(2);
                            displayString = displayString + " ";
                            displayString = displayString + resultSet.getString(3);
                            displayString = displayString + "\n";
                            resultSet.moveToNext();
                        }
                        tv.setText(displayString);

                    }

                    @Override
                    public void onFailure(int statusCode, Throwable throwable, JSONObject error) {
                        Log.e("Query Failure", statusCode + " " + throwable.getMessage());
                    }
                });
    }
}