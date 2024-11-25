// Verify the patched files exist
File stringUtilFile = new File(basedir, "target/source-project/src/main/java/org/jsoup/internal/StringUtil.java")
assert stringUtilFile.exists()

// Verify the patch content was applied by checking for specific changes
String stringUtilContent = stringUtilFile.text

// Check for new method addition
assert stringUtilContent.contains("private static final Pattern controlChars = Pattern.compile(\"[\\\\x00-\\\\x1f]*\");")
assert stringUtilContent.contains("private static String stripControlChars(final String input)")

// Verify test files were patched
File stringUtilTestFile = new File(basedir, "target/source-project/src/test/java/org/jsoup/internal/StringUtilTest.java")
assert stringUtilTestFile.exists()
String testContent = stringUtilTestFile.text
assert testContent.contains("void stripsControlCharsFromUrls()")
assert testContent.contains("void allowsSpaceInUrl()")

// Verify CleanerTest was patched
File cleanerTestFile = new File(basedir, "target/source-project/src/test/java/org/jsoup/safety/CleanerTest.java")
assert cleanerTestFile.exists()
String cleanerTestContent = cleanerTestFile.text
assert cleanerTestContent.contains("void dropsConcealedJavascriptProtocolWhenRelativesLinksEnabled()")
assert cleanerTestContent.contains("void dropsConcealedJavascriptProtocolWhenRelativesLinksDisabled()")

println "Patch verification completed successfully"