// FILE: operatorOnType.cj
package sample.resolver.operators

class Meter {
}

extend Meter {
    public operator func <caret>-(other: Meter): Meter {
        return Meter()
    }
}
