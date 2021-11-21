package lau.assignment.technewsapp;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    ArrayList<String> articleT = new ArrayList<>();
    ArrayList<String> articleC = new ArrayList<>();
    ArrayAdapter arrayAdapter;

    SQLiteDatabase myDB;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        myDB = this.openOrCreateDatabase("News", MODE_PRIVATE, null);
        myDB.execSQL("CREATE TABLE IF NOT EXISTS articles (id INTEGER PRIMARY KEY, aID INTEGER, title VARCHAR, content VARCHAR)");

        DownloadTask dl = new DownloadTask();
        try {
            dl.execute("https://hacker-news.firebaseio.com/v0/topstories.json");
        } catch (Exception e) {
            e.printStackTrace();
        }

        ListView articleList = findViewById(R.id.articleList);
        changeArticles();
        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, articleT);
        articleList.setAdapter(arrayAdapter);

        articleList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                Intent intent = new Intent(getApplicationContext(), NewsActivity.class);
                intent.putExtra("content", articleC.get(i));

                startActivity(intent);
            }
        });
    }

    public void changeArticles() {
        Cursor cursor = myDB.rawQuery("SELECT * FROM articles", null);
        int ti = cursor.getColumnIndex("title");
        int ci = cursor.getColumnIndex("content");

        if (cursor.moveToFirst()) {

            articleT.add(cursor.getString(ti));
            articleC.add(cursor.getString(ci));
            while (cursor.moveToNext()){
                articleT.add(cursor.getString(ti));
                articleC.add(cursor.getString(ci));
            }
        }
    }

    public class DownloadTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... urls) {

            String str = "";
            URL url;
            HttpURLConnection urlConnection = null;

            try {
                url = new URL(urls[0]);
                urlConnection = (HttpURLConnection) url.openConnection();

                InputStream inputStream = urlConnection.getInputStream();
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);

                int input = inputStreamReader.read();

                while (input != -1) {
                    char cur = (char) input;
                    str += cur;
                    input = inputStreamReader.read();
                }

                JSONArray jsonArray = new JSONArray(str);
                int num = 20;

                if (jsonArray.length() < 20) {
                    num = jsonArray.length();
                }

                myDB.execSQL("DELETE FROM articles");

                for (int i = 0; i < num; i++) {
                    String aID = jsonArray.getString(i);
                    url = new URL("https://hacker-news.firebaseio.com/v0/item/" + aID + ".json?print=pretty");
                    urlConnection = (HttpURLConnection) url.openConnection();

                    inputStream = urlConnection.getInputStream();
                    inputStreamReader = new InputStreamReader(inputStream);

                    input = inputStreamReader.read();

                    String str2 = "";

                    while (input != -1) {
                        char cur = (char) input;
                        str2 += cur;
                        input = inputStreamReader.read();
                    }

                    JSONObject jsonObject = new JSONObject(str2);

                    if (!jsonObject.isNull("title") && !jsonObject.isNull("url")) {
                        String artTitle = jsonObject.getString("title");
                        String artURL = jsonObject.getString("url");

                        url = new URL(artURL);
                        urlConnection = (HttpURLConnection) url.openConnection();
                        inputStream = urlConnection.getInputStream();
                        inputStreamReader = new InputStreamReader(inputStream);
                        input = inputStreamReader.read();
                        String artCont = "";
                        while (input != -1) {
                            char cur = (char) input;
                            artCont += cur;
                            input = inputStreamReader.read();
                        }

                        SQLiteStatement entryQuery = myDB.compileStatement("INSERT INTO articles (aID, title, content) VALUES (?, ?, ?)");

                        entryQuery.bindString(1,aID);
                        entryQuery.bindString(2,artTitle);
                        entryQuery.bindString(3,artCont);

                        entryQuery.execute();
                    }
                }
                return str;
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            changeArticles();
        }
    }
}