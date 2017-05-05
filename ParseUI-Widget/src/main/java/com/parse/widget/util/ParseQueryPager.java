package com.parse.widget.util;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import bolts.CancellationToken;
import bolts.Continuation;
import bolts.Task;
import bolts.TaskCompletionSource;

/**
 * A utility class to page through {@link ParseQuery} results.
 *
 * @param <T> A class that extends {@link ParseObject}
 */
public class ParseQueryPager<T extends ParseObject> {

  private static final int DEFAULT_PAGE_SIZE = 25;

  private static <T extends ParseObject> Task<List<T>> findAsync(
      ParseQuery<T> query, final CancellationToken ct) {
    return query.findInBackground().continueWithTask(new Continuation<List<T>, Task<List<T>>>() {
      @Override
      public Task<List<T>> then(Task<List<T>> task) throws Exception {
        if (ct != null && ct.isCancellationRequested()) {
          return Task.cancelled();
        }
        return task;
      }
    });
  }

  /**
   * The callback that is called by {@link ParseQueryPager} when the results have changed.
   *
   * @param <T> A class that extends {@link ParseQueryPager}
   */
  public interface OnObjectsChangedCallback<T extends ParseQueryPager> {
    /**
     * Called whenever a change of unknown type has occurred, such as the entire list being set to
     * new values.
     * @param sender The changing pager.
     */
    void onChanged(T sender);

    /**
     * Called whenever one or more items have changed.
     * @param sender The changing pager.
     * @param positionStart The starting index that has changed.
     * @param itemCount The number of items that have been changed.
     */
    void onItemRangeChanged(T sender, int positionStart, int itemCount);

    /**
     * Called whenever one or more items have been inserted into the result set.
     * @param sender The changing pager.
     * @param positionStart The starting index that has been inserted.
     * @param itemCount The number of items that have been inserted.
     */
    void onItemRangeInserted(T sender, int positionStart, int itemCount);

    /**
     * Called whenever one or more items have been moved from the result set.
     * @param sender The changing pager.
     * @param fromPosition The position from which the items were moved.
     * @param toPosition The destination position of the items.
     * @param itemCount The number of items that have been inserted.
     */
    void onItemRangeMoved(T sender, int fromPosition, int toPosition, int itemCount);

    /**
     * Called whenever one or more items have been removed from the result set.
     * @param sender The changing pager.
     * @param positionStart The starting index that has been inserted.
     * @param itemCount The number of items that have been inserted.
     */
    void onItemRangeRemoved(T sender, int positionStart, int itemCount);
  }

  private final ParseQuery<T> query;
  private final int pageSize;
  private final List<T> objects = new ArrayList<>();
  private final List<T> unmodifiableObjects = Collections.unmodifiableList(objects);
  private final List<OnObjectsChangedCallback> callbacks = new ArrayList<>();
  private final Object lock = new Object();

  private int currentPage = -1;
  private boolean hasNextPage = true;
  private Task<List<T>> loadNextPageTask;

  /**
   * Constructs a new instance of {@code ParseQueryPager} with the specified query.
   *
   * @param query The query for this {@code ParseQueryPager}.
   */
  public ParseQueryPager(ParseQuery<T> query) {
    this(query, DEFAULT_PAGE_SIZE);
  }

  /**
   * Constructs a new instance of {@code ParseQueryPager} with the specified query.
   *
   * @param query The query for this {@code ParseQueryPager}.
   * @param pageSize The size of each page.
   */
  public ParseQueryPager(ParseQuery<T> query, int pageSize) {
    this.query = new ParseQuery<>(query);
    this.pageSize = pageSize;
  }

  /**
   * @return the query for this {@code ParseQueryPager}.
   */
  public ParseQuery<T> getQuery() {
    return query;
  }

  /**
   * @return the size of each page for this {@code ParseQueryPager}.
   */
  public int getPageSize() {
    return pageSize;
  }

  /**
   * Returns the current page of the pager in the result set.
   *
   * The value is zero-based. When the row set is first returned the pager will be at position -1,
   * which is before the first page.
   *
   * @return the current page.
   */
  public int getCurrentPage() {
    synchronized (lock) {
      return currentPage;
    }
  }

  /**
   * @return whether the pager has more pages.
   */
  public boolean hasNextPage() {
    synchronized (lock) {
      return hasNextPage;
    }
  }

  /**
   * @return whether the pager is currently loading the next page.
   */
  public boolean isLoadingNextPage() {
    synchronized (lock) {
      return loadNextPageTask != null && !loadNextPageTask.isCompleted();
    }
  }

  /**
   * @return the loaded objects.
   */
  public List<T> getObjects() {
    return unmodifiableObjects;
  }

  public void addOnObjectsChangedCallback(OnObjectsChangedCallback callback) {
    synchronized (lock) {
      callbacks.add(callback);
    }
  }

  public void removeOnObjectsChangedCallback(OnObjectsChangedCallback callback) {
    synchronized (lock) {
      callbacks.remove(callback);
    }
  }

  @SuppressWarnings("unchecked")
  private void notifyRangeChanged(int positionStart, int positionEnd) {
    synchronized (lock) {
      for (OnObjectsChangedCallback callback : callbacks) {
        callback.onItemRangeChanged(this, positionStart, positionEnd);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private void notifyRangeInserted(int positionStart, int positionEnd) {
    synchronized (lock) {
      for (OnObjectsChangedCallback callback : callbacks) {
        callback.onItemRangeInserted(this, positionStart, positionEnd);
      }
    }
  }

  private void setLoadNextPageTask(Task<List<T>> task) {
    synchronized (lock) {
      loadNextPageTask = task.continueWithTask(new Continuation<List<T>, Task<List<T>>>() {
        @Override
        public Task<List<T>> then(Task<List<T>> task) throws Exception {
          synchronized (lock) {
            loadNextPageTask = null;
          }
          return task;
        }
      });
    }
  }

  /**
   * Returns a new instance of {@link ParseQuery} to be used to load the next page of results.
   *
   * Its limit should be one more than {@code pageSize} so that {@code hasNextPage} can be
   * determined.
   *
   * @param page The page the query should load.
   * @return a new instance of {@link ParseQuery}.
   */
  protected ParseQuery<T> createQuery(int page) {
    ParseQuery<T> query = new ParseQuery<>(getQuery());
    query.setSkip(getPageSize() * page);
    // Limit is pageSize + 1 so we can detect if there are more pages
    query.setLimit(getPageSize() + 1);
    return query;
  }

  // Note: This should not be called multiple times.
  public Task<List<T>> loadNextPage() {
    return loadNextPage((CancellationToken) null);
  }

  /**
   * Loads the next page.
   *
   * The next page is defined by {@code currentPage + 1}.
   *
   * @param ct Token used to cancel the task.
   * @return A {@link Task} that resolves to the result of the next page.
   */
  public Task<List<T>> loadNextPage(CancellationToken ct) {
    if (!hasNextPage()) {
      throw new IllegalStateException("Unable to load next page when there are no more pages available");
    }

    final int page = getCurrentPage() + 1;

    // TODO(grantland): Utilize query.findInBackground(CancellationToken)
    final ParseQuery<T> query = createQuery(page);
    Task<List<T>> task = findAsync(query, ct).continueWithTask(new Continuation<List<T>, Task<List<T>>>() {
      @Override
      public Task<List<T>> then(Task<List<T>> task) throws Exception {
        if (task.isCancelled() || task.isFaulted()) {
          return task;
        }

        List<T> results = task.getResult();
        onPage(query, page, results);

        return task;
      }
    }, Task.UI_THREAD_EXECUTOR);

    setLoadNextPageTask(task);

    return task;
  }

  /**
   * Loads the next page.
   *
   * The next page is defined by {@code currentPage + 1}.
   *
   * @param callback A {@code callback} that will be called with the result of the next page.
   */
  public void loadNextPage(final FindCallback<T> callback) {
    loadNextPage(callback, null);
  }

  /**
   * Loads the next page.
   *
   * The next page is defined by {@code currentPage + 1}.
   *
   * @param callback A {@code callback} that will be called with the result of the next page.
   * @param ct Token used to cancel the task.
   */
  public void loadNextPage(final FindCallback<T> callback, final CancellationToken ct) {
    if (!hasNextPage()) {
      throw new IllegalStateException("Unable to load next page when there are no more pages available");
    }

    final int page = getCurrentPage() + 1;

    final TaskCompletionSource<List<T>> tcs = new TaskCompletionSource<>();
    final ParseQuery<T> query = createQuery(page);
    query.findInBackground(new FindCallback<T>() {

      AtomicInteger callbacks = new AtomicInteger();

      @Override
      public void done(List<T> results, ParseException e) {
        boolean isCancelled = ct != null && ct.isCancellationRequested();
        if (!isCancelled && e == null) {
          onPage(query, page, results);
        }

        boolean isCacheThenNetwork = false;
        try {
          ParseQuery.CachePolicy policy = getQuery().getCachePolicy();
          isCacheThenNetwork = policy == ParseQuery.CachePolicy.CACHE_THEN_NETWORK;
        } catch (IllegalStateException ex) {
          // do nothing, LDS is enabled and we can't use CACHE_THEN_NETWORK
        }
        if (!isCacheThenNetwork || callbacks.incrementAndGet() >= 2) {
          if (isCancelled) {
            tcs.trySetCancelled();
          } else {
            tcs.trySetResult(results);
          }
        }

        callback.done(results, e);
      }
    });

    setLoadNextPageTask(tcs.getTask());
  }

  private void onPage(ParseQuery<T> query, int page, List<T> results) {
    synchronized (lock) {
      int itemCount = results.size();

      currentPage = page;
      int limit = query.getLimit();
      if (limit == -1 || limit == pageSize) {
        // Backwards compatibility hack to support ParseQueryAdapter#setPaginationEnabled(false)
        hasNextPage = false;
      } else {
        // We detect if there are more pages by setting the limit pageSize + 1 and we remove the extra
        // if there are more pages.
        hasNextPage = itemCount >= pageSize + 1;
        if (itemCount > pageSize) {
          results.remove(pageSize);
        }
      }
      int objectsSize = objects.size();
      boolean inserted = true;
      if (objectsSize > pageSize * page) {
        inserted = false;
        objects.subList(pageSize * page, Math.min(objectsSize, pageSize * (page + 1))).clear();
      }
      objects.addAll(pageSize * page, results);

      int positionStart = pageSize * page;
      if (inserted) {
        notifyRangeInserted(positionStart, itemCount);
      } else {
        notifyRangeChanged(positionStart, itemCount);
      }
    }
  }
}
