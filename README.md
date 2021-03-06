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

1. The view layer is a calendar view. 
2. The backend layer is a SQL database.
3. The main view consists of a calendar for a week, with "next" and "previous" buttons.
4. You can also use swipe to go to another week.
5. Any events that are shown are shown inside the appropriate day box.
6. One can click a "+" button inside a date box to enter an event.
7. Event consists of a title, location, description and time schedule.
8. Adding or editing an event happen on another page.
9. Keeping track of when an event was created and updated.
10. Adding, editing, or removing an event persists in a sqlite database in the app.
11. No Authentication is required.
12. I used the Room library to simply read and write to the Sqlite database using Entity and Dao
    objects.
13. Will trigger event reminder notification automatically

# Demo App

<table style="width:100%">
  <tr>
    <th>Screenshot 1: Initial home page</th>
    <th>Screenshot 2: Clicked a "+" button inside a date box to enter an event</th>
  </tr>
  <tr>
    <td><img src="github_assets/1.png"/></td>
    <td><img src="github_assets/2.png"/></td>
  </tr>
  <tr>
    <th>Screenshot 3: Add new event screen</th>
    <th>Screenshot 4: Popup dialog after add an Event</th>
  </tr>
  <tr>
    <td><img src="github_assets/3.png"/></td>
    <td><img src="github_assets/4.png"/></td>
  </tr>
  <tr>
    <th>Screenshot 5: Showing newly added event on the home page</th>
    <th>Screenshot 6: Edit an event</th>
  </tr>
  <tr>
    <td><img src="github_assets/5.png"/></td>
    <td><img src="github_assets/6.png"/></td>
  </tr>
  <tr>
    <th>Screenshot 7: New event by copy the existing event</th>
    <th>Screenshot 8: Warnings for unsaved changes</th>
  </tr>
  <tr>
    <td><img src="github_assets/7.png"/></td>
    <td><img src="github_assets/8.png"/></td>
  </tr>
  <tr>
    <th>Screenshot 9: Trigger an event reminder notification</th>
    <th>Screenshot 10: Delete an event</th>
  </tr>
  <tr>
    <td><img src="github_assets/9.png"/></td>
    <td><img src="github_assets/10.png"/></td>
  </tr>
  </table>