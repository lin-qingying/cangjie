// FILE: operatorFunction.cj
package sample.resolver.operators

class Counter {
    public operator func <caret>+(other: Counter): Counter {
        return Counter()
    }
}
