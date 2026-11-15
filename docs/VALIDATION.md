# Publishing validation — 1.2.0

Validated on 14 November 2026 with Eclipse Temurin Java 11, Gradle 8.10 and RuneLite 1.13.6 (the current Plugin Hub runelite.version).

- Normal project: clean build succeeded; 34 tests passed, no failures/errors.
- Official Hub standard-build.gradle and standard-settings.gradle templates: production compilation/JAR succeeded with the Hub's current client version injected.
- Both outputs contain the same 11 runtime class files; all target Java 11 (class-file version 55).
- Plugin class, PluginDescriptor name and root metadata agree. Version 1.2.0 and build=standard are set.
- No diagnostic option, logging class or diagnostic references remain in source or runtime bytecode.
- Production source scan found no filesystem, reflection, subprocess, Robot or alternate network-client APIs. JUnit/Mockito and the fixture's reflection are test-only.
- Original icon is 48×48 and below the Hub size limit; BSD 2-Clause licence is included.
- The intended marker name plugins/toa-applicant-gear was available when checked.
- The user confirmed that the refresh lifecycle fix works in-game before this publishing cleanup.

GitHub Actions is configured but cannot run in the user's repository until upload. This local preparation does not replace the Plugin Hub's own CI, API recording, review or approval. See SUBMISSION.md for that final workflow.
