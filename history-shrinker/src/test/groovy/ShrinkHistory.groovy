import org.testng.annotations.Test

import java.nio.file.Files
import java.nio.file.Paths

class ShrinkHistory {
    Map<String, Long> latestLines = new HashMap<>()

    @Test
    void test() {
        def rawLines = new File("/home/aleksei/.zsh_history").readLines()


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
            lines.add(": $entry.value:0;$entry.key")
        }

        Files.write(Paths.get("/home/aleksei/.zsh_history"), lines)
    }

    void addToSet(String currentLine) {
        def timestamp = Long.parseLong(currentLine.substring(2, 12))
        def command = currentLine.substring(15)

        def latest = latestLines.get(command)
        if (latest == null || latest < timestamp) {
            latestLines.put(command, timestamp)
        }
    }
}
