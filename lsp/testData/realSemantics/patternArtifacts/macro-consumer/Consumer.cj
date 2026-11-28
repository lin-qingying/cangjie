package sample.macroconsumer

import sample.patternmacro.generatedCount as firstCount
import sample.patternmacro.generatedCount as secondCount
import sample.patternmacro.generatedLabel
import sample.patternmacro.generatedFlag

public func consume(): Int64 {
    println(generatedLabel)
    if (generatedFlag) {
        return firstCount + secondCount
    }
    return 0
}
