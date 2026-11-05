# Room, WorkManager and Compose ship their own consumer rules.
# Workers and receivers are created by name from the manifest / WorkManager; their own rules keep them.

# Release builds drop debug and verbose logs (B2). Log.i, Log.w and Log.e stay.
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
}
