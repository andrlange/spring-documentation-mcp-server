@org.springframework.modulith.ApplicationModule(
    displayName = "Notifications Module",
    allowedDependencies = {"shared", "catalog::events", "members::events", "loans::events"}
)
package com.example.library.notifications;
