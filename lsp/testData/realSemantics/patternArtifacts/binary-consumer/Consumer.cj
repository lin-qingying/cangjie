package sample.binaryconsumer

import sample.binarypatterns.libraryCount as firstCount
import sample.binarypatterns.libraryCount as secondCount
import sample.binarypatterns.libraryLabel
import sample.binarypatterns.libraryFlag

public func consume(): Int64 {
    println(libraryLabel)
    if (libraryFlag) {
        return firstCount + secondCount
    }
    return 0
}
