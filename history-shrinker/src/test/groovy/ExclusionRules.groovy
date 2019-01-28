import java.util.regex.Pattern

class ExclusionRules {

    static def ipRegex='\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b'
    static def anything='(.*)'
    static def spaceOrQuote ='''([\\s]|["]|['])'''
    static def sudo ='(sudo )*'
    static Pattern ipPattern = Pattern.compile("$anything$spaceOrQuote$ipRegex$spaceOrQuote$anything", Pattern.DOTALL) // Any IP address
    static List<String> whitelistedIPs = ["8.8.8.8"]
    static int nowSeconds = System.currentTimeMillis() / 1000

    static List<Tuple2<Integer, Pattern>> exclusionPatterns = [
            // Incorrect lines
            exclude(0, '^.$'), // Single-char commands
            exclude(0, '".*', Pattern.DOTALL), // Starts with double quote
            exclude(0, '''([^a-z/$~'\\.#_]).*''', Pattern.DOTALL), // Starts with something weird. Possible problem: some commands actually start with caps: "LANG=ru playonlinux"
            exclude(0, '\\\\.*', Pattern.DOTALL), // If starts with backslash (\)
            exclude(0, '.*\\\\', Pattern.DOTALL), // If ends with backslash (\)
            exclude(0, '(.*)[^\\\\]\\R(.*)', Pattern.DOTALL), // If there is no backslash before newline
            exclude(0, '(.*)[^\\\\] \\R(.*)', Pattern.DOTALL), // If there is no backslash before newline

            // Individual exclusions
            exclude(0, '(.*)/usr/local/bin/csshX(.*)', Pattern.DOTALL),
            exclude(0, 'ssh-keygen -f ".*', Pattern.DOTALL),
            exclude(30, "$anything${spaceOrQuote}(asd)+$spaceOrQuote$anything", Pattern.DOTALL), // Remove commands with dummy 'asd' values

            // Command/retention based
            exclude(30, "$sudo(cd|ls|ll|la|mkdir|grep|cat|less|rm|touch|rmdir|cp|mv|wc|man|tail|youtube-dl) $anything", Pattern.DOTALL),
            exclude(90, "$sudo(telnet|dig|ping|gritt|ifconfig|id|pkill) $anything", Pattern.DOTALL),
            exclude(360, "$sudo(curl|find|dd|mvn|meld|docker|gr|./gradlew|watch|cut|sort|zip|unzip|set|xrandr) $anything", Pattern.DOTALL),
            exclude(720, "$sudo(vi|nano|geany|gedit|subl|ssh|scp|git|svn|apt-get|aptitude|java|go|echo|for|chmod|chown|while|aws|vault|ansible|python) $anything", Pattern.DOTALL),

            exclude(30, "$anything(\\|)(\\s)*(grep )$anything", Pattern.DOTALL), // Remove greps like "ps -ef | grep java"
    ]

    static Tuple2<Integer, Pattern> exclude(int ageDays, String regex) {
        return new Tuple2<Integer, Pattern>(ageDays, Pattern.compile(regex))
    }

    static Tuple2<Integer, Pattern> exclude(int ageDays, String regex, int regexFlag) {
        return new Tuple2<Integer, Pattern>(ageDays, Pattern.compile(regex,regexFlag))
    }

    static boolean matchesExclusions(String command, int timestamp) {
        if (excludeBasedOnRegex(command, timestamp)) {
            return true
        }

        if (command.matches(ipPattern) && tooOld(30, timestamp)) {
            for (String whitelistedIp : whitelistedIPs) {
                if (command.contains(whitelistedIp)) {
                    return false
                }
            }
            return true
        }

        if (command.count('\n') > 3 && tooOld(30, timestamp)) {
            return true
        }

        return false
    }

    static boolean excludeBasedOnRegex(String command, int timestamp) {
        for (Tuple2<Integer, Pattern> exclusionPattern : exclusionPatterns) {
            if (tooOld(exclusionPattern.first, timestamp) && command.matches(exclusionPattern.second)) {
                return true
            }
        }
        return false
    }

    static boolean tooOld(int oldDays, int commandTimestampSeconds) {
        (nowSeconds - oldDays * 60 * 60 * 24) > commandTimestampSeconds
    }
}
