@org.springframework.modulith.ApplicationModule(
    displayName = "Loans Module",
    type = org.springframework.modulith.ApplicationModule.Type.OPEN,
    allowedDependencies = {"shared", "catalog::api", "members::api"}
)
package com.example.library.loans;
