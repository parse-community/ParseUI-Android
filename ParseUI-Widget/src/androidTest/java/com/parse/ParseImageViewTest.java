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
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.test.InstrumentationTestCase;

import com.parse.widget.test.R;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

public class ParseImageViewTest extends InstrumentationTestCase {

  public void testParseImageViewWithNullParseFile() throws Exception {
    final Drawable drawable = new ColorDrawable();
    final ParseImageView imageView = new ParseImageView(getInstrumentation().getTargetContext());
    imageView.setPlaceholder(drawable);
    imageView.setParseFile(null);

    byte[] data = ParseTaskUtils.wait(imageView.loadInBackground());

    assertNull(data);
    assertEquals(drawable, imageView.getDrawable());
  }

  public void testParseImageViewWithNotImageParseFile() throws Exception {
    byte[] data = "hello".getBytes();
    ParseFile file = new ParseFile(data);

    final Drawable drawable = new ColorDrawable();
    final ParseImageView imageView = new ParseImageView(getInstrumentation().getTargetContext());
    imageView.setPlaceholder(drawable);
    imageView.setParseFile(file);

    byte[] dataAgain = ParseTaskUtils.wait(imageView.loadInBackground());

    assertTrue(Arrays.equals(data, dataAgain));
    // Since the parseFile can not be decode as an image, the getDrawable should not be changed
    assertEquals(drawable, imageView.getDrawable());
  }

  public void testParseImageViewWithImageParseFile() throws Exception {
    Context context = getInstrumentation().getTargetContext();
    final Drawable iconImage = context.getResources().getDrawable(R.drawable.icon);
    Bitmap iconBitmap = ((BitmapDrawable) iconImage).getBitmap();
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    iconBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
    final byte[] imageData = stream.toByteArray();
    ParseFile file = new ParseFile(imageData);

    final Drawable drawable = new ColorDrawable();
    final ParseImageView imageView = new ParseImageView(context);
    imageView.setPlaceholder(drawable);
    imageView.setParseFile(file);

    byte[] dataAgain = ParseTaskUtils.wait(imageView.loadInBackground());

    assertEquals(imageData, dataAgain);
    assertNotNull(imageView.getDrawable());
    // It is hard to assert whether the two images are equal or not, so we just verify the image has
    // been changed
    assertNotSame(drawable, imageView.getDrawable());
  }
}


