package com.parse.ui.widget.sample;

import android.database.DataSetObserver;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.widget.util.ParseQueryPager;

import java.util.List;

import bolts.CancellationTokenSource;


public class ListActivity extends AppCompatActivity {

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_list);

    ListView listView = (ListView) findViewById(R.id.list);

    MyAdapter<ParseObject> adapter = new MyAdapter<>(createPager());
    listView.setAdapter(adapter);
  }

  private ParseQueryPager<ParseObject> createPager() {
    ParseQuery<ParseObject> query = ParseQuery.getQuery("TestObject");
    query.orderByAscending("name");
    query.setCachePolicy(ParseQuery.CachePolicy.CACHE_THEN_NETWORK);
    return new ParseQueryPager<>(query, 25);
  }

  public static class MyAdapter<T extends ParseObject> extends BaseAdapter {

    public static final int TYPE_ITEM = 0;
    public static final int TYPE_NEXT = 1;

    private static class ItemViewHolder extends ViewHolder {
      TextView textView;

      public ItemViewHolder(View itemView) {
        super(itemView);
        textView = (TextView) itemView;
      }
    }

    private static class NextViewHolder extends ViewHolder {
      TextView textView;
      ProgressBar progressBar;

      public NextViewHolder(View itemView) {
        super(itemView);
        textView = (TextView) itemView.findViewById(android.R.id.text1);
        progressBar = (ProgressBar) itemView.findViewById(android.R.id.progress);
      }

      public void setLoading(boolean loading) {
        if (loading) {
          textView.setVisibility(View.INVISIBLE);
          progressBar.setVisibility(View.VISIBLE);
          progressBar.setIndeterminate(true);
        } else {
          textView.setVisibility(View.VISIBLE);
          progressBar.setVisibility(View.INVISIBLE);
          progressBar.setIndeterminate(false);
        }
      }
    }

    private final Object lock = new Object();
    private ParseQueryPager<T> pager;
    private CancellationTokenSource cts;

    public MyAdapter(ParseQueryPager<T> pager) {
      swap(pager);
    }

    public ParseQueryPager<T> getPager() {
      synchronized (lock) {
        return pager;
      }
    }

    public void swap(ParseQueryPager<T> pager) {
      synchronized (lock) {
        if (cts != null) {
          cts.cancel();
        }
        this.pager = pager;
        this.cts = new CancellationTokenSource();
      }
    }

    private void loadNextPage() {
      final ParseQueryPager<T> pager;
      final CancellationTokenSource cts;

      synchronized (lock) {
        pager = this.pager;
        cts = this.cts;
      }

      // Utilizing callbacks to support CACHE_THEN_NETWORK
      pager.loadNextPage(new FindCallback<T>() {
        @Override
        public void done(List<T> results, ParseException e) {
          if (results == null && e == null) { // cancelled
            return;
          }

          if (e != null) {
            notifyDataSetChanged();
            return;
          }

          notifyDataSetChanged();
        }
      }, cts.getToken());
      notifyDataSetChanged();
    }

    @Override
    public int getCount() {
      ParseQueryPager<T> pager = getPager();
      return pager.getObjects().size() + (pager.hasNextPage() ? 1 : 0);
    }

    @Override
    public T getItem(int position) {
      List<T> objects = getPager().getObjects();
      return position < objects.size() ? objects.get(position) : null;
    }

    @Override
    public long getItemId(int position) {
      return position;
    }

    @Override
    public final View getView(int position, View convertView, ViewGroup parent) {
      ViewHolder holder;
      View view;
      if (convertView == null) {
        holder = onCreateViewHolder(parent, getItemViewType(position));
        view = holder.itemView;
        view.setTag(holder);
      } else {
        view = convertView;
        holder = (ViewHolder) view.getTag();
      }
      onBindViewHolder(holder, position);
      return view;
    }

    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
      LayoutInflater inflater = LayoutInflater.from(parent.getContext());
      switch (viewType) {
        case TYPE_ITEM: {
          View v = inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
          return new ItemViewHolder(v);
        }
        case TYPE_NEXT: {
          View v = inflater.inflate(R.layout.load_more_list_item, parent, false);
          v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
              if (!getPager().isLoadingNextPage()) {
                loadNextPage();
              }
            }
          });
          NextViewHolder vh = new NextViewHolder(v);
          vh.textView.setText(R.string.load_more);
          return vh;
        }
        default:
          throw new IllegalStateException("Invalid view type: " + viewType);
      }
    }

    public void onBindViewHolder(ViewHolder holder, int position) {
      switch (getItemViewType(position)) {
        case TYPE_ITEM: {
          ParseObject item = getItem(position);

          ItemViewHolder vh = (ItemViewHolder) holder;
          vh.textView.setText(item.getString("name"));
        }
        break;
        case TYPE_NEXT: {
          NextViewHolder vh = (NextViewHolder) holder;
          vh.setLoading(getPager().isLoadingNextPage());
        }
        break;
      }
    }

    @Override
    public int getViewTypeCount() {
      return 2;
    }

    @Override
    public int getItemViewType(int position) {
      return position < getPager().getObjects().size() ? TYPE_ITEM : TYPE_NEXT;
    }

    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
      super.registerDataSetObserver(observer);
      // We use this method as a notification that the ListView is bound to the adapter.
      loadNextPage();
    }

    public static class ViewHolder {
      private View itemView;

      public ViewHolder(View itemView) {
        this.itemView = itemView;
      }
    }
  }
}
