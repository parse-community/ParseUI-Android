/*
 *  Copyright (c) 2014, Parse, LLC. All rights reserved.
 *
 *  You are hereby granted a non-exclusive, worldwide, royalty-free license to use,
 *  copy, modify, and distribute this software in source code or binary form for use
 *  in connection with the web services and APIs provided by Parse.
 *
 *  As with any software that integrates with the Parse platform, your use of
 *  this software is subject to the Parse Terms of Service
 *  [https://www.parse.com/about/terms]. This copyright notice shall be
 *  included in all copies or substantial portions of the software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 *  FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 *  COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 *  IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 *  CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package com.parse;

import android.content.Context;
import android.database.DataSetObserver;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.parse.widget.util.ParseQueryPager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.WeakHashMap;

import bolts.CancellationTokenSource;

/**
 * A {@code ParseQueryAdapter} handles the fetching of objects by page, and displaying objects as
 * views in a {@link android.widget.ListView}.
 * <p>
 * This class is highly configurable, but also intended to be easy to get started with. See below
 * for an example of using a {@code ParseQueryAdapter} inside an {@link android.app.Activity}'s
 * {@code onCreate}:
 * <pre>
 * final ParseQueryAdapter adapter = new ParseQueryAdapter(this, &quot;TestObject&quot;);
 * adapter.setTextKey(&quot;name&quot;);
 *
 * ListView listView = (ListView) findViewById(R.id.listview);
 * listView.setAdapter(adapter);
 * </pre>
 * <p>
 * Below, an example showing off the level of configuration available with this class:
 * <pre>
 * // Instantiate a QueryFactory to define the ParseQuery to be used for fetching items in this
 * // Adapter.
 * ParseQueryAdapter.QueryFactory&lt;ParseObject&gt; factory =
 *     new ParseQueryAdapter.QueryFactory&lt;ParseObject&gt;() {
 *       public ParseQuery create() {
 *         ParseQuery query = new ParseQuery(&quot;Customer&quot;);
 *         query.whereEqualTo(&quot;activated&quot;, true);
 *         query.orderByDescending(&quot;moneySpent&quot;);
 *         return query;
 *       }
 *     };
 *
 * // Pass the factory into the ParseQueryAdapter's constructor.
 * ParseQueryAdapter&lt;ParseObject&gt; adapter = new ParseQueryAdapter&lt;ParseObject&gt;(this, factory);
 * adapter.setTextKey(&quot;name&quot;);
 *
 * // Perhaps set a callback to be fired upon successful loading of a new set of ParseObjects.
 * adapter.addOnQueryLoadListener(new OnQueryLoadListener&lt;ParseObject&gt;() {
 *   public void onLoading() {
 *     // Trigger any &quot;loading&quot; UI
 *   }
 *
 *   public void onLoaded(List&lt;ParseObject&gt; objects, ParseException e) {
 *     // Execute any post-loading logic, hide &quot;loading&quot; UI
 *   }
 * });
 *
 * // Attach it to your ListView, as in the example above
 * ListView listView = (ListView) findViewById(R.id.listview);
 * listView.setAdapter(adapter);
 * </pre>
 */
public class ParseQueryAdapter<T extends ParseObject> extends BaseAdapter {

  /**
   * Implement to construct your own custom {@link ParseQuery} for fetching objects.
   */
  public interface QueryFactory<T extends ParseObject> {
    ParseQuery<T> create();
  }

  /**
   * Implement with logic that is called before and after objects are fetched from Parse by the
   * adapter.
   */
  public interface OnQueryLoadListener<T extends ParseObject> {
    void onLoading();

    void onLoaded(List<T> objects, Exception e);
  }

  private final Object lock = new Object();
  private ParseQueryPager<T> pager;
  private CancellationTokenSource cts;

  //region Backwards compatibility
  private ParseQuery<T> query;
  private int objectsPerPage = 25;
  //endregion

  private Integer itemResourceId;

  // The key to use to display on the cell text label.
  private String textKey;

  // The key to use to fetch an image for display in the cell's image view.
  private String imageKey;

  // Whether the table should use the built-in pagination feature (default:
  // true)
  private boolean paginationEnabled = true;

  // A Drawable placeholder, to be set on ParseImageViews while images are loading. Can be null.
  private Drawable placeholder;

  // A WeakHashMap, holding references to ParseImageViews that have been configured by this PQA.
  // Accessed and iterated over if setPlaceholder(Drawable) is called after some set of
  // ParseImageViews have already been instantiated and configured.
  private WeakHashMap<ParseImageView, Void> imageViewSet = new WeakHashMap<>();

  // A WeakHashMap, keeping track of the DataSetObservers on this class
  private WeakHashMap<DataSetObserver, Void> dataSetObservers = new WeakHashMap<>();

  // Whether the adapter should trigger loadObjects() on registerDataSetObserver(); Defaults to
  // true.
  private boolean autoload = true;

  private Context context;

  private List<OnQueryLoadListener<T>> onQueryLoadListeners =
      new ArrayList<>();

  private static final int VIEW_TYPE_ITEM = 0;
  private static final int VIEW_TYPE_NEXT_PAGE = 1;

  /**
   * Constructs a {@code ParseQueryAdapter}. Given a {@link ParseObject} subclass, this adapter will
   * fetch and display all {@link ParseObject}s of the specified class, ordered by creation time.
   *
   * @param context
   *          The activity utilizing this adapter.
   * @param clazz
   *          The {@link ParseObject} subclass type to fetch and display.
   */
  public ParseQueryAdapter(Context context, Class<? extends ParseObject> clazz) {
    this(context, ParseObject.getClassName(clazz));
  }

  /**
   * Constructs a {@code ParseQueryAdapter}. Given a {@link ParseObject} subclass, this adapter will
   * fetch and display all {@link ParseObject}s of the specified class, ordered by creation time.
   *
   * @param context
   *          The activity utilizing this adapter.
   * @param className
   *          The name of the Parse class of {@link ParseObject}s to display.
   */
  public ParseQueryAdapter(Context context, final String className) {
    this(context, new QueryFactory<T>() {
      @Override
      public ParseQuery<T> create() {
        ParseQuery<T> query = ParseQuery.getQuery(className);
        query.orderByDescending("createdAt");

        return query;
      }
    });

    if (className == null) {
      throw new RuntimeException("You need to specify a className for the ParseQueryAdapter");
    }
  }

  /**
   * Constructs a {@code ParseQueryAdapter}. Given a {@link ParseObject} subclass, this adapter will
   * fetch and display all {@link ParseObject}s of the specified class, ordered by creation time.
   *
   * @param context
   *          The activity utilizing this adapter.
   * @param clazz
   *          The {@link ParseObject} subclass type to fetch and display.
   * @param itemViewResource
   *        A resource id that represents the layout for an item in the AdapterView.
   */
  public ParseQueryAdapter(Context context, Class<? extends ParseObject> clazz,
      int itemViewResource) {
    this(context, ParseObject.getClassName(clazz), itemViewResource);
  }

  /**
   * Constructs a {@code ParseQueryAdapter}. Given a {@link ParseObject} subclass, this adapter will
   * fetch and display all {@link ParseObject}s of the specified class, ordered by creation time.
   *
   * @param context
   *          The activity utilizing this adapter.
   * @param className
   *          The name of the Parse class of {@link ParseObject}s to display.
   * @param itemViewResource
   *        A resource id that represents the layout for an item in the AdapterView.
   */
  public ParseQueryAdapter(Context context, final String className, int itemViewResource) {
    this(context, new QueryFactory<T>() {
      @Override
      public ParseQuery<T> create() {
        ParseQuery<T> query = ParseQuery.getQuery(className);
        query.orderByDescending("createdAt");

        return query;
      }
    }, itemViewResource);

    if (className == null) {
      throw new RuntimeException("You need to specify a className for the ParseQueryAdapter");
    }
  }
  /**
   * Constructs a {@code ParseQueryAdapter}. Allows the caller to define further constraints on the
   * {@link ParseQuery} to be used when fetching items from Parse.
   *
   * @param context
   *          The activity utilizing this adapter.
   * @param queryFactory
   *          A {@link QueryFactory} to build a {@link ParseQuery} for fetching objects.
   */
  public ParseQueryAdapter(Context context, QueryFactory<T> queryFactory) {
    this(context, queryFactory, null);
  }

  /**
   * Constructs a {@code ParseQueryAdapter}. Allows the caller to define further constraints on the
   * {@link ParseQuery} to be used when fetching items from Parse.
   *
   * @param context
   *          The activity utilizing this adapter.
   * @param queryFactory
   *          A {@link QueryFactory} to build a {@link ParseQuery} for fetching objects.
   * @param itemViewResource
   *          A resource id that represents the layout for an item in the AdapterView.
   */
  public ParseQueryAdapter(Context context, QueryFactory<T> queryFactory, int itemViewResource) {
    this(context, queryFactory, Integer.valueOf(itemViewResource));
  }

  private ParseQueryAdapter(Context context, QueryFactory<T> queryFactory, Integer itemViewResource) {
    super();
    this.context = context;
    query = queryFactory.create();
    itemResourceId = itemViewResource;
  }

  /**
   * Return the context provided by the {@code Activity} utilizing this {@code ParseQueryAdapter}.
   *
   * @return The activity utilizing this adapter.
   */
  public Context getContext() {
    return context;
  }

  private ParseQueryPager<T> getPager() {
    synchronized (lock) {
      if (pager == null) {
        pager = new ParseQueryPager<T>(query, objectsPerPage) {
          @Override
          protected ParseQuery<T> createQuery(int page) {
            // Workaround for backwards compatibility
            ParseQuery<T> query = new ParseQuery<>(getQuery());
            if (paginationEnabled) {
              setPageOnQuery(page, query);
            }
            return query;
          }
        };
        cts = new CancellationTokenSource();
      }

      return pager;
    }
  }

  private List<T> getObjects() {
    return getPager().getObjects();
  }

  /** {@inheritDoc} **/
  @Override
  public T getItem(int index) {
    if (index == getPaginationCellRow()) {
      return null;
    }
    return getObjects().get(index);
  }

  /** {@inheritDoc} **/
  @Override
  public long getItemId(int position) {
    return position;
  }

  @Override
  public int getItemViewType(int position) {
    if (position == getPaginationCellRow()) {
      return VIEW_TYPE_NEXT_PAGE;
    }
    return VIEW_TYPE_ITEM;
  }

  @Override
  public int getViewTypeCount() {
    return 2;
  }

  @Override
  public void registerDataSetObserver(DataSetObserver observer) {
    super.registerDataSetObserver(observer);
    dataSetObservers.put(observer, null);
    if (autoload) {
      loadObjects();
    }
  }

  @Override
  public void unregisterDataSetObserver(DataSetObserver observer) {
    super.unregisterDataSetObserver(observer);
    dataSetObservers.remove(observer);
  }

  /**
   * Remove all elements from the list.
   */
  public void clear() {
    synchronized (lock) {
      if (cts != null) {
        cts.cancel();
      }
      pager = null;
      cts = null;
    }

    notifyDataSetChanged();
  }

  /**
   * Clears the table and loads the first page of objects asynchronously. This method is called
   * automatically when this {@code Adapter} is attached to an {@code AdapterView}.
   * <p>
   * {@code loadObjects()} should only need to be called if {@link #setAutoload(boolean)} is set to
   * {@code false}.
   */
  public void loadObjects() {
    loadNextPage(true);
  }

  private void loadNextPage(final boolean shouldClear) {
    synchronized (lock) {
      if (shouldClear && pager != null) {
        cts.cancel();
        pager = null;
      }
    }

    notifyOnLoadingListeners();

    getPager().loadNextPage(new FindCallback<T>() {
      @Override
      public void done(List<T> results, ParseException e) {
        if (results == null && e == null) { // cancelled
          return;
        }

        // Backwards compatibility
        if ((!Parse.isLocalDatastoreEnabled() &&
            query.getCachePolicy() == ParseQuery.CachePolicy.CACHE_ONLY) &&
            (e != null) && e.getCode() == ParseException.CACHE_MISS) {
          // no-op on cache miss
          return;
        }

        notifyDataSetChanged();

        notifyOnLoadedListeners(results, e);
      }
    }, cts.getToken());
  }

  /**
   * Loads the next page of objects, appends to table, and notifies the UI that the model has
   * changed.
   */
  public void loadNextPage() {
    loadNextPage(false);
  }

  /**
   * Overrides {@link Adapter#getCount()} method to return the number of cells to
   * display. If pagination is turned on, this count will include an extra +1 count for the
   * pagination cell row.
   *
   * @return The number of cells to be displayed by the {@link android.widget.ListView}.
   */
  @Override
  public int getCount() {
    int count = getObjects().size();

    if (shouldShowPaginationCell()) {
      count++;
    }

    return count;
  }

  /**
   * Override this method to customize each cell given a {@link ParseObject}.
   * <p>
   * If a view is not provided, a default view will be created based upon
   * {@code android.R.layout.activity_list_item}.
   * <p>
   * This method expects a {@code TextView} with id {@code android.R.id.text1} in your object views.
   * If {@link #setImageKey(String)} was used, this method also expects an {@code ImageView} with id
   * {@code android.R.id.icon}.
   * <p>
   * This method displays the text value specified by the text key (set via
   * {@link #setTextKey(String)}) and an image (described by a {@link ParseFile}, under the key set
   * via {@link #setImageKey(String)}) if applicable. If the text key is not set, the value for
   * {@link ParseObject#getObjectId()} will be displayed instead.
   *
   * @param object
   *          The {@link ParseObject} associated with this item.
   * @param v
   *          The {@code View} associated with this row. This view, if non-null, is being recycled
   *          and intended to be used for displaying this item.
   * @param parent
   *          The parent that this view will eventually be attached to
   * @return The customized view displaying the {@link ParseObject}'s information.
   */
  public View getItemView(T object, View v, ViewGroup parent) {
    if (v == null) {
      v = getDefaultView(parent.getContext());
    }

    TextView textView;
    try {
      textView = (TextView) v.findViewById(android.R.id.text1);
    } catch (ClassCastException ex) {
      throw new IllegalStateException(
          "Your object views must have a TextView whose id attribute is 'android.R.id.text1'", ex);
    }

    if (textView != null) {
      if (textKey == null) {
        textView.setText(object.getObjectId());
      } else if (object.get(textKey) != null) {
        textView.setText(object.get(textKey).toString());
      } else {
        textView.setText(null);
      }
    }

    if (imageKey != null) {
      ParseImageView imageView;
      try {
        imageView = (ParseImageView) v.findViewById(android.R.id.icon);
      } catch (ClassCastException ex) {
        throw new IllegalStateException(
            "Your object views must have a ParseImageView whose id attribute is 'android.R.id.icon'",
            ex);
      }
      if (imageView == null) {
        throw new IllegalStateException(
            "Your object views must have a ParseImageView whose id attribute is 'android.R.id.icon' if an imageKey is specified");
      }
      if (!imageViewSet.containsKey(imageView)) {
        imageViewSet.put(imageView, null);
      }
      imageView.setPlaceholder(placeholder);
      imageView.setParseFile((ParseFile) object.get(imageKey));
      imageView.loadInBackground();
    }

    return v;
  }

  /**
   * Override this method to customize the "Load Next Page" cell, visible when pagination is turned
   * on and there may be more results to display.
   * <p>
   * This method expects a {@code TextView} with id {@code android.R.id.text1}.
   *
   * @param v
   *          The view object associated with this row + type (a "Next Page" view, instead of an
   *          "Item" view).
   * @param parent
   *          The parent that this view will eventually be attached to
   * @return The view object that allows the user to paginate.
   */
  public View getNextPageView(View v, ViewGroup parent) {
    if (v == null) {
      v = getDefaultView(parent.getContext());
    }
    TextView textView = (TextView) v.findViewById(android.R.id.text1);
    textView.setText("Load more...");
    return v;
  }

  /**
   * The base class, {@code Adapter}, defines a {@code getView} method intended to display data at
   * the specified position in the data set. We override it here in order to toggle between
   * {@link #getNextPageView(View, ViewGroup)} and
   * {@link #getItemView(ParseObject, View, ViewGroup)} depending on the value of
   * {@link #getItemViewType(int)}.
   */
  @Override
  public final View getView(int position, View convertView, ViewGroup parent) {
    if (getItemViewType(position) == VIEW_TYPE_NEXT_PAGE) {
      View nextPageView = getNextPageView(convertView, parent);
      nextPageView.setOnClickListener(new OnClickListener() {
        @Override
        public void onClick(View view) {
          loadNextPage();
        }
      });
      return nextPageView;
    }
    return getItemView(getItem(position), convertView, parent);
  }

  /**
   * Override this method to manually paginate the provided {@code ParseQuery}. By default, this
   * method will set the {@code limit} value to {@link #getObjectsPerPage()} and the {@code skip}
   * value to {@link #getObjectsPerPage()} * {@code page}.
   * <p>
   * Overriding this method will not be necessary, in most cases.
   *
   * @param page
   *          the page number of results to fetch from Parse.
   * @param query
   *          the {@link ParseQuery} used to fetch items from Parse. This query will be mutated and
   *          used in its mutated form.
   */
  protected void setPageOnQuery(int page, ParseQuery<T> query) {
    query.setLimit(objectsPerPage + 1);
    query.setSkip(page * objectsPerPage);
  }

  public void setTextKey(String textKey) {
    this.textKey = textKey;
  }

  public void setImageKey(String imageKey) {
    this.imageKey = imageKey;
  }

  public void setObjectsPerPage(int objectsPerPage) {
    this.objectsPerPage = objectsPerPage;
  }

  public int getObjectsPerPage() {
    return objectsPerPage;
  }

  /**
   * Enable or disable pagination of results. Defaults to true.
   *
   * @param paginationEnabled
   *          Defaults to true.
   */
  public void setPaginationEnabled(boolean paginationEnabled) {
    this.paginationEnabled = paginationEnabled;
  }

  /**
   * Sets a placeholder image to be used when fetching data for each item in the {@code AdapterView}
   * . Will not be used if {@link #setImageKey(String)} was not used to define which images to
   * display.
   *
   * @param placeholder
   *          A {@code Drawable} to be displayed while the remote image data is being fetched. This
   *          value can be null, and {@code ImageView}s in this AdapterView will simply be blank
   *          while data is being fetched.
   */
  public void setPlaceholder(Drawable placeholder) {
    if (this.placeholder == placeholder) {
      return;
    }
    this.placeholder = placeholder;
    Iterator<ParseImageView> iter = imageViewSet.keySet().iterator();
    ParseImageView imageView;
    while (iter.hasNext()) {
      imageView = iter.next();
      if (imageView != null) {
        imageView.setPlaceholder(this.placeholder);
      }
    }
  }

  /**
   * Enable or disable the automatic loading of results upon attachment to an {@code AdapterView}.
   * Defaults to true.
   *
   * @param autoload
   *          Defaults to true.
   */
  public void setAutoload(boolean autoload) {
    if (this.autoload == autoload) {
      // An extra precaution to prevent an overzealous setAutoload(true) after assignment to an
      // AdapterView from triggering an unnecessary additional loadObjects().
      return;
    }
    this.autoload = autoload;
    if (this.autoload && !dataSetObservers.isEmpty() && getObjects().isEmpty()) {
      loadObjects();
    }
  }

  public void addOnQueryLoadListener(OnQueryLoadListener<T> listener) {
    onQueryLoadListeners.add(listener);
  }

  public void removeOnQueryLoadListener(OnQueryLoadListener<T> listener) {
    onQueryLoadListeners.remove(listener);
  }

  private View getDefaultView(Context context) {
    if (itemResourceId != null) {
      return View.inflate(context, itemResourceId, null);
    }
    LinearLayout view = new LinearLayout(context);
    view.setPadding(8, 4, 8, 4);

    ParseImageView imageView = new ParseImageView(context);
    imageView.setId(android.R.id.icon);
    imageView.setLayoutParams(new LinearLayout.LayoutParams(50, 50));
    view.addView(imageView);

    TextView textView = new TextView(context);
    textView.setId(android.R.id.text1);
    textView.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT));
    textView.setPadding(8, 0, 0, 0);
    view.addView(textView);

    return view;
  }

  private int getPaginationCellRow() {
    return getObjects().size();
  }

  private boolean shouldShowPaginationCell() {
    ParseQueryPager<T> pager = getPager();
    return paginationEnabled && pager.getObjects().size() > 0 && pager.hasNextPage();
  }

  private void notifyOnLoadingListeners() {
    for (OnQueryLoadListener<T> listener : onQueryLoadListeners) {
      listener.onLoading();
    }
  }

  private void notifyOnLoadedListeners(List<T> objects, Exception e) {
    for (OnQueryLoadListener<T> listener : onQueryLoadListeners) {
      listener.onLoaded(objects, e);
    }
  }
}