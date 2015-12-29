package com.parse.ui.widget.sample;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ListView;
import android.widget.Toast;

import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseQueryAdapter;

import java.util.List;


public class ListActivity extends AppCompatActivity {

  private static final String TAG = "ListActivity";

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_list);

    ListView listView = (ListView) findViewById(R.id.list);

    ParseQueryAdapter<ParseObject> adapter = new ParseQueryAdapter<>(this,
        new ParseQueryAdapter.QueryFactory<ParseObject>() {
          @Override
          public ParseQuery<ParseObject> create() {
            return ParseQuery.getQuery("Contact")
                .orderByAscending("name")
                .setCachePolicy(ParseQuery.CachePolicy.CACHE_THEN_NETWORK);
          }
        }, android.R.layout.simple_list_item_1);
    adapter.setTextKey("name");
    adapter.addOnQueryLoadListener(new ParseQueryAdapter.OnQueryLoadListener<ParseObject>() {
      @Override
      public void onLoading() {
        Log.d(TAG, "loading");
      }

      @Override
      public void onLoaded(List<ParseObject> objects, Exception e) {
        Log.d(TAG, "loaded");
        if (e != null
            && e instanceof ParseException
            && ((ParseException) e).getCode() != ParseException.CACHE_MISS) {
          Toast.makeText(ListActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
      }
    });
    listView.setAdapter(adapter);
  }
}
