# Publish ToA Gear Inspector

This project is prepared against the [Plugin Hub guide](https://github.com/runelite/plugin-hub#creating-new-plugins). The source package is ready to upload; the repository URL and final GitHub commit hash can only be filled in after that upload.

## 1. Create the public repository

On GitHub, create a **public** repository named `toa-gear-inspector` with description:

> Inspect nearby players' equipment from the Tombs of Amascut party interface.

Use an empty repository, or the [RuneLite example-plugin template](https://github.com/runelite/example-plugin/generate). If using the template, replace its template files with this project and remove the old `src/main/java/com/example` and `src/test/java/com/example` folders. A fresh empty repository is simplest because the package, class names, metadata and licence have already been completed here.

## 2. Upload the extracted source

Extract the ZIP first. Upload its **contents** to the repository root, keeping directory structure. Do not upload the ZIP itself and do not place the whole project inside another folder in the repository.

The repository root must contain:

- README.md, SUBMISSION.md and LICENSE
- build.gradle, settings.gradle and runelite-plugin.properties
- icon.png
- gradlew, gradlew.bat and gradle/wrapper/
- src/main/java/ and src/test/java/
- .gitignore and .github/workflows/build.yml
- docs/REVIEW_NOTES.md

Use GitHub's Add file → Upload files page, or Git. Show hidden files in your file manager so .gitignore and .github are included (Ctrl+H on many Linux file managers). Do not add build/, dist/, .gradle/, compiled plugin JARs, logs or credentials. The wrapper JAR inside gradle/wrapper/ is required and is intentionally included.

If using Git locally, run these commands inside the extracted source folder, then add the remote URL GitHub gives you and push:

```bash
git init -b main
git add .
git commit -m "Initial release of ToA Gear Inspector"
```

GitHub's file-upload page may not retain the executable bit on gradlew. The included workflow uses `bash ./gradlew`, and Linux users can use the same command or run `chmod +x gradlew` locally.

## 3. Check the build

Open the repository's Actions tab and confirm **Build and test** succeeds. It runs a Java 11 build with the latest released RuneLite dependency.

The metadata is complete: display name ToA Gear Inspector; author William Chamberlain; plugin class com.toa.gearinspector.ToaGearInspectorPlugin; version 1.2.1; build=standard. The package and launcher already use these real class names, not template Example classes. The 48×48 icon and BSD 2-Clause licence are included.

## 4. Submit the marker to Plugin Hub

Fork [runelite/plugin-hub](https://github.com/runelite/plugin-hub), create a branch, and add **one extensionless file** named `plugins/toa-gear-inspector`.

The marker filename must match `^[a-z0-9-]+$`: lowercase letters, digits and dashes only, with no extension. The display name **ToA Gear Inspector** keeps its capitals and spaces. Your GitHub repository can also keep a name such as **ToA-Gear-Inspector**; use its actual URL in the marker. The lowercase rule applies to the marker filename in the plugin-hub repository.

If your current pull request contains `plugins/ToA-Gear-Inspector`, rename that file to `plugins/toa-gear-inspector` in the same pull request. Remove the old uppercase path so that only the lowercase file remains.

After replacing the source with this renamed version, commit it to your plugin repository and update the marker to that new commit. Do not keep the hash of the previous source revision.

Its complete contents must be these two fields, using your actual repository URL and the final full 40-character commit hash:

```properties
repository=https://github.com/YOUR_GITHUB_USERNAME/toa-gear-inspector.git
commit=YOUR_FULL_40_CHARACTER_COMMIT_HASH
```

These are the only placeholders in the submission instructions. Replace both; do not submit them literally. The main plugin project itself has no identity placeholders to fill in.

Find the hash by opening the final commit on GitHub and copying its full SHA, or run `git rev-parse HEAD` in your local repository. Commit all intended changes first; the marker must identify the exact source you want reviewed.

Commit the marker in your plugin-hub fork, then open a pull request against runelite/plugin-hub. Do not copy the whole Java project into the plugin-hub repository: it only needs the marker.

Suggested pull request title:

> Add ToA Gear Inspector

Suggested description:

```text
Adds hover equipment inspection to the native Tombs of Amascut party Members and Applicants tabs, including other parties a player is considering joining.

The plugin matches the displayed player names to nearby players' public appearances and shows nine visible worn-equipment slots. Ring, ammunition and inventory are not available and are not displayed. Empty party seats are excluded.

Uses standard RuneLite APIs, with no network requests, game actions or additional runtime dependencies. Native party controls are preserved, and the equipment card consumes clicks so it cannot activate obscured controls.

Validation: Java 11 build and automated regression tests, compilation with the Hub's current standard-build template, and user confirmation of the in-game refresh fix.
```

## 5. Complete the review

Watch the Plugin Hub build and review checks. Resolve requested changes in this same pull request, update the marker's commit hash to the new source commit, and wait for review and merging. Your own repository's green Actions result is useful validation; it is not Plugin Hub approval.

The Hub reviews code and gameplay behaviour. This package has been prepared and locally checked, but neither approval nor a review completion date can be guaranteed.

## Guide coverage

| Guide item | Prepared here |
| --- | --- |
| Appropriate public repository | Name/description above; you create it in your GitHub account |
| Java 11 development project | Gradle wrapper, Java 11 bytecode and developer run task |
| Rename example package/classes/launcher | com.toa.gearinspector and ToaGearInspectorLauncher already configured |
| PluginDescriptor and plugin metadata | Completed and consistent |
| Optional icon | Original 48×48 PNG at repository root |
| Useful README | Usage, limitations, configuration and build instructions |
| BSD 2-Clause licence | Included with William Chamberlain as copyright owner |
| Commit and push source | Upload/commit steps above; final SHA is created by that commit |
| Two-line Hub marker | Exact format and filename above |
| Build mode | standard; no extra runtime dependencies required |

## Interpreting the reported build error

`invalid plugin file name "ToA-Gear-Inspector"` is the fatal error. The marker's path must be `plugins/toa-gear-inspector`.

`Unable to download previous manifest, doing full build` reports the packager falling back to a full build. In the supplied log, processing continued until the invalid filename check failed; the filename error is the action required for that run. This does not claim that all later build/review steps have already passed.

When updating an existing GitHub repository, replace the previous `src` directory rather than merging the new package into it: the Java package and class names have changed. The repository must not retain the old plugin classes alongside the new ones.
