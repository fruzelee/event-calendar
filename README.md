# Welcome to Event Calendar

[![Platform](https://img.shields.io/badge/platform-Android-yellow.svg)](https://www.android.com)
[![Gradle Version](https://img.shields.io/badge/gradle-7.0.2-green.svg)](https://docs.gradle.org/current/release-notes)
[![Awesome Kotlin Badge](https://kotlin.link/awesome-kotlin.svg)](https://github.com/KotlinBy/awesome-kotlin)

Welcome to **Event Calendar.**
**Event Calendar** is a basic application in Android using Kotlin.

# Specifications
1. Used Android Studio to create the project.
2. Target Sdk is Android 11.
3. Kotlin is used as code in to the app.
4. Code maintains best practices & solid principle guideline.
5. Used standard Human Interface Guidelines to design the app.
6. App is work in Landscape mode too.
7. Added comments on Code where necessary.

# Necessary Technology
1. Android Jetpack
2. Kotlin
3. Sqlite Database
4. Room (AndroidX)
5. View Binding

# App Overview
This is a very basic Event Calendar application in Android using Kotlin along with some 
AndroidX libraries that are recommended. 

-The view layer is a calendar view. 
-The backend layer is a SQL database.
-The main view consists of a calendar for a week, with "next" and "previous" buttons. 
-You can also use swipe to go to another week.
-Any events that are shown are shown inside the appropriate day box. One can click a "+" button
inside a date box to enter an event, which consists of a title, location, description and time schedule.
-Adding or editing an event can happen on another page. 
-Keeping track of when an event was created and updated.
-Adding, editing, or removing an event persists in a sqlite database in the app. 
-"No Authentication is required." 
-I used the Room library to simply read and write to the Sqlite database using Entity and Dao objects.