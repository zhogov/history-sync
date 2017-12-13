import org.testng.annotations.Test

import java.nio.file.Files
import java.nio.file.Paths

class ShrinkHistory {
    Map<String, Integer> latestLines = new HashMap<>()

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

        List<Map.Entry<String, Integer>> entries = []
        for (Map.Entry<String, Integer> entry : latestLines.entrySet()) {
            entries.add(entry)
        }

        Collections.sort(entries, new Comparator<Map.Entry<String, Integer>>() {
            @Override
            int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
                return Integer.compare(o1.value, o2.value)
            }
        })

        def lines = []

        for (Map.Entry<String, Integer> entry : entries) {
            if (ExclusionRules.matchesExclusions(entry.key, entry.value)) continue

            lines.add(": $entry.value:0;$entry.key")
        }

        Files.write(Paths.get(System.getProperty("user.home") + "/.zsh_history"), lines)
    }

    void addToSet(String currentLine) {
        def timestamp = Integer.parseInt(currentLine.substring(2, 12))
        def command = currentLine.substring(15).trim()

        def latest = latestLines.get(command)
        if (latest == null || latest < timestamp) {
            latestLines.put(command, timestamp)
        }
    }

}
