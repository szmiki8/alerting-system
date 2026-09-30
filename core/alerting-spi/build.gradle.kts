// Extension interfaces (EventSource, SubscriberType, NotificationChannel) and their value types.
// Deliberately no Spring Boot dependency (architecture Section 6.2).
plugins {
    id("alerting.java-library-conventions")
}

dependencies {
    // Nullness annotations only (@NullMarked, @Nullable); exported so plugin authors see them.
    api(libs.jspecify)
}
