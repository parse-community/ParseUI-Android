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

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.test.ActivityInstrumentationTestCase2;

public abstract class BaseActivityInstrumentationTestCase2<T extends Activity>
    extends ActivityInstrumentationTestCase2<T> {

  protected Activity activity = null;

  public BaseActivityInstrumentationTestCase2(Class<T> activityClass) {
    super(activityClass);
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();

    activity = getActivity();

    Instrumentation instrumentation = this.getInstrumentation();
    Context context = instrumentation.getTargetContext();
    // Wait for the application context to exist (to avoid an Android bug)
    // http://stackoverflow.com/questions/6516441/why-does-androidtestcase-getcontext-getapplicationcontext-return-null
    while (context == null || context.getApplicationContext() == null) {
      Thread.sleep(100);
      context = instrumentation.getTargetContext();
    }

    // Work around a bug with Mockito and dexmaker on 4.3: https://code.google.com/p/dexmaker/issues/detail?id=2
    System.setProperty("dexmaker.dexcache", context.getCacheDir().toString());
  }

  @Override
  protected void tearDown() throws Exception {
    activity = null;
    super.tearDown();
  }
}
