-injars build/classes
-injars app/libs/zxing-core-3.3.3.jar
-outjars build/proguard/classes.jar

-libraryjars /Users/henk/Library/Android/sdk/platforms/android-10/android.jar

-dontpreverify
-dontwarn
-overloadaggressively
-allowaccessmodification
-optimizationpasses 5

-keep public class * extends android.app.Activity
-keepclassmembers class * {
    public <init>(...);
}

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
