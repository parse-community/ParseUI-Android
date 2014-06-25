# ParseUI
## Overview
This project contains the `ParseLoginUI` library for building login and signup flows with the Parse Android SDK.
You can easily configure the look and feel of the login screens by either specifying XML configurations or constructing an Intent in code.
To use this project with your app, you should import it as a library project in Android Studio or Eclipse.

![sample screens](http://parseui-android.parseapp.com/images/parse_login_sample_screens.png)

We also provide several sample projects demonstrating how to use the `ParseLoginUI` library.  To run these
sample projects, you need to do the following:

1. Clone this repository onto your machine.
2. Fetch the Facebook SDK Git submodule by running the following command in your local ParseUI-Android repository directory: `git submodule update --init --recursive`
3. Import this repository's projects with Android Studio or Eclipse.
    1. For Android Studio, just import your local ParseUI-Android repository directory.
    2. For Eclipse, import the following projects.  In the Eclipse import pop-up, be sure to edit the "New Project Name" column to match each sample project's folder name as shown below (or else Eclipse will automatically use the name of the first activity in `AndroidManifest.xml` and cause name collision error "SampleProfileActivity already exists").
        * `ParseLoginUI`
        * `ParseLoginSampleBasic`
        * `ParseLoginSampleCodeCustomization`
        * `ParseLoginSampleLayoutOverride`
        * `ParseLoginSampleWithDispatchActivity`
        * `facebook-sdk/facebook`
        ![eclipse import](http://parseui-android.parseapp.com/images/eclipse_import_after_edit.png)
4. Make sure you have Android Build Tools 19.1 installed through Android SDK Manager.  If you are using Android Studio, please also change <code>facebook-sdk/gradle.properties</code> to say <code>ANDROID_BUILD_TOOLS_VERSION=19.1.0</code>
5. Place the following in `res/values/strings.xml` of each sample project:
  * Parse application id and client key
  * Facebook application id
  * Twitter consumer key and consumer secret
6. Build and run the sample project using Android Studio or Eclipse.

## Documentation
For complete details about this library project, please see our [documentation](http://www.parse.com/docs/android_guide#ui-login) on the Parse website.
We'll discuss some highlights here.

To start the login flow from your own activity, you launch the `ParseLoginActivity` with two lines of code:

```java
ParseLoginBuilder builder = new ParseLoginBuilder(MyActivity.this);
startActivityForResult(builder.build(), 0);
```

`ParseLoginActivity` will guide the user through the login experience, where the user can also sign up or reset a forgotten password.
Each screen in the login experience is implemented by a fragment hosted within this activity.
When `ParseLoginActivity` finishes, you can check `ParseUser.getCurrentUser()` in your own activity to see whether the user actually logged in.

This library is ultra-customizable, allowing you to configure the login experience through either XML or code.
For example, you can directly configure the login experience through the activity meta-data in `AndroidManifest.xml`.

```xml
<activity
    android:name="com.parse.ui.ParseLoginActivity"
    android:label="@string/my_app_name"
    android:launchMode="singleTop">
    <meta-data
        android:name="com.parse.ui.ParseLoginActivity.PARSE_LOGIN_ENABLED"
        android:value="true"/>
    <meta-data
        android:name="com.parse.ui.ParseLoginActivity.PARSE_LOGIN_EMAIL_AS_USERNAME"
        android:value="true"/>
    <meta-data
        android:name="com.parse.ui.ParseLoginActivity.FACEBOOK_LOGIN_ENABLED"
        android:value="true"/>
</activity>
```

Please see the [Parse website](http://www.parse.com/docs/android_guide#ui-login) for additional documentation.

## Contributing
See the CONTRIBUTING file for how to help out.

## License
Copyright (c) 2014, Facebook, Inc. All rights reserved.

You are hereby granted a non-exclusive, worldwide, royalty-free license to use,
copy, modify, and distribute this software in source code or binary form for use
in connection with the web services and APIs provided by Facebook.

As with any software that integrates with the Facebook platform, your use of
this software is subject to the Facebook Developer Principles and Policies
[http://developers.facebook.com/policy/]. This copyright notice shall be
included in all copies or substantial portions of the software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.