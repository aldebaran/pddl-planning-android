# PDDL Planning for Android

![](https://img.shields.io/maven-central/v/com.softbankrobotics.pddl/pddl-planning "latest version on Maven Central")

This project provides a library to interface third-party PDDL planners on Android.

It includes utilities
[to parse and manipulate PDDL strings](pddl-planning/src/main/java/com/softbankrobotics/pddlplanning/PDDL.kt)
and
[represent PDDL entities in Kotlin](pddl-planning/src/main/java/com/softbankrobotics/pddlplanning/BaseOntology.kt).

It provides various helpers to perform planning around the
[`PlanSearchFunction` interface](pddl-planning/src/main/java/com/softbankrobotics/pddlplanning/Planning.kt)
that abstracts away the planner implementation.
It also provides a
[service interface](pddl-planning/src/main/java/com/softbankrobotics/pddlplanning/PDDLPlannerServiceClient.kt)
to allow third-party implementations to be deployed as stand-alone applications.

## Usage

This library is publicly available on Maven Central, but **not** in JCenter.
The `build.gradle` at the root of your project should mention:

```groovy
allprojects {
    repositories {
        mavenCentral()
    }
}
```

Then add the following dependency in your module's `build.gradle`:

```groovy
implementation 'com.softbankrobotics.pddl:pddl-planning:1.4.0'
```

## Testing

This project also provides
[helpers to check expected plans for given PDDL problems](pddl-planning-test/src/main/java/com/softbankrobotics/pddlplanning/test/PlanningTestUtil.kt).
It also includes some PDDL tests that you can run with a third-party planner.
Add the following test dependency in your module's `build.gradle`:

```groovy
androidTestImplementation 'com.softbankrobotics.pddl:pddl-planning-test:1.4.0'
```

Then, let a test extend the interface
[`PlanningInstrumentedTest`](pddl-planning-test/src/main/java/com/softbankrobotics/pddlplanning/test/PlanningInstrumentedTest.kt),
to provide the included test units.

You can also get only the non-instrumented tests by defining the following dependency:

```groovy
testImplementation 'com.softbankrobotics.pddl:pddl-planning-test:1.4.0'
```

Then, let a test extend the interface
[`PlanningTestUtil`](pddl-planning-test/src/main/java/com/softbankrobotics/pddlplanning/test/PlanningTestUtil.kt),
to provide the included test units.
However there are much fewer tests with this interface.

## License

This project is distributed under the [BSD-3 license](LICENSE).
