# Parse UI for Android
[![](https://travis-ci.org/parse-community/ParseUI-Android.svg?branch=master)](https://travis-ci.org/parse-community/ParseUI-Android)
[![](https://img.shields.io/badge/license-BSD-lightgrey.svg)](https://github.com/parse-community/ParseUI-Android/blob/master/LICENSE)
[![](https://jitpack.io/v/parse-community/ParseUI-Android.svg)](https://jitpack.io/#parse-community/ParseUI-Android)
[![Join The Conversation](https://img.shields.io/discourse/https/community.parseplatform.org/topics.svg)](https://community.parseplatform.org/c/parse-server)
[![Backers on Open Collective](https://opencollective.com/parse-server/backers/badge.svg)](#backers)
[![Sponsors on Open Collective](https://opencollective.com/parse-server/sponsors/badge.svg)](#sponsors)
![Twitter Follow](https://img.shields.io/twitter/follow/ParsePlatform.svg?label=Follow%20us%20on%20Twitter&style=social)

This project contains two libraries, `login` and `widget`.

The `login` library is used for building login and signup flows with the Parse Android SDK. You can easily configure the look and feel of the login screens by either specifying XML configurations or constructing an Intent in code.

The `widget` library provides several useful UI widgets which integrate with [Parse SDK](https://github.com/ParsePlatform/Parse-SDK-Android) seamlessly.


## Dependency

Add this in your root `build.gradle` file (**not** your module `build.gradle` file):

```gradle
allprojects {
	repositories {
		...
		maven { url "https://jitpack.io" }
	}
}
```

Then, add the library to your project `build.gradle`
```gradle
dependencies {
    // for the widget dependency
    implementation "com.github.parse-community.ParseUI-Android:widget:latest.version.here"
    // for the login dependency
    implementation "com.github.parse-community.ParseUI-Android:login:latest.version.here"
}
```

## Usage
Please check the tutorial in our [wiki](https://github.com/parse-community/ParseUI-Android/wiki)

## Documentation
For complete details about this library project, please see our [documentation](https://docs.parseplatform.org/android/guide/#user-interface) on the Parse website.

## How Do I Contribute?
We want to make contributing to this project as easy and transparent as possible. Please refer to the [Contribution Guidelines](https://github.com/parse-community/Parse-SDK-Android/blob/master/CONTRIBUTING.md).

## License
    Copyright (c) 2015-present, Parse, LLC.
    All rights reserved.

    This source code is licensed under the BSD-style license found in the
    LICENSE file in the root directory of this source tree. An additional grant
    of patent rights can be found in the PATENTS file in the same directory.

-----
As of April 5, 2017, Parse, LLC has transferred this code to the parse-community organization, and will no longer be contributing to or distributing this code.
