import org.testng.annotations.Test

import java.nio.file.Files
import java.nio.file.Paths
import java.util.regex.Pattern

class ShrinkHistory {
    Map<String, Long> latestLines = new HashMap<>()

    @Test
    void test() {
        def rawLines = new File(System.getProperty("user.home") + "/.zsh_history").readLines()


        String currentLine = null
        for (String rawLine : rawLines) {
            if (!rawLine.isEmpty()) {
                if (rawLine.startsWith(": 1")) {
                    if (currentLine != null) {
                        addToSet(currentLine)
                    }
                    currentLine = rawLine
                } else {
                    currentLine += "\n" + rawLine
                }
            }
        }
        if (currentLine != null) {
            addToSet(currentLine)
        }

        List<Map.Entry<String, Long>> entries = []
        for (Map.Entry<String, Long> entry : latestLines.entrySet()) {
            entries.add(entry)
        }

        Collections.sort(entries, new Comparator<Map.Entry<String, Long>>() {
            @Override
            int compare(Map.Entry<String, Long> o1, Map.Entry<String, Long> o2) {
                return Long.compare(o1.value, o2.value)
            }
        })

        def lines = []

        for (Map.Entry<String, Long> entry : entries) {
            if (matchesExclusions(entry.key)) continue;

            lines.add(": $entry.value:0;$entry.key")
        }

        Files.write(Paths.get(System.getProperty("user.home") + "/.zsh_history"), lines)
    }

    void addToSet(String currentLine) {
        def timestamp = Long.parseLong(currentLine.substring(2, 12))
        def command = currentLine.substring(15).trim()

        def latest = latestLines.get(command)
        if (latest == null || latest < timestamp) {
            latestLines.put(command, timestamp)
        }
    }

    static def ipRegex='\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b'
    static def anything='(.*)'
    static def spaceOrQuote ='''([\\s]|["]|['])'''
    static Pattern ipPattern = Pattern.compile("$anything$spaceOrQuote$ipRegex$spaceOrQuote$anything", Pattern.DOTALL) // Any IP address
    static List<String> whitelistedIPs = ["8.8.8.8"]

    List<Pattern> exclusionPatterns = [
            Pattern.compile('^.$'), // Single-char commands
            Pattern.compile('\\\\.*', Pattern.DOTALL), // If starts with backslash (\)
            Pattern.compile('.*\\\\', Pattern.DOTALL), // If ends with backslash (\)
            Pattern.compile('(.*)[^\\\\]\\R(.*)', Pattern.DOTALL), // If there is no backslash before newline
            Pattern.compile('(.*)[^\\\\] \\R(.*)', Pattern.DOTALL), // If there is no backslash before newline
            // Individual exclusions
            Pattern.compile('(.*)/usr/local/bin/csshX(.*)', Pattern.DOTALL),
            Pattern.compile('grep(.*)', Pattern.DOTALL),
            Pattern.compile("$anything${spaceOrQuote}(asd)+$spaceOrQuote$anything", Pattern.DOTALL),
    ]

    boolean matchesExclusions(String command) {
        if (excludeBasedOnRegex(command)) {
            return true
        }

        if (command.matches(ipPattern)) {
            for (String whitelistedIp : whitelistedIPs) {
                if (command.contains(whitelistedIp)) {
                    return false
                }
            }
            return true
        }

        if (command.count('\n') > 3) {
            return true
        }

        return false
    }

    def excludeBasedOnRegex(String command) {
        for (Pattern exclusionPattern : exclusionPatterns) {
            if (command.matches(exclusionPattern)) {
                return true
            }
        }
        return false
    }
}
