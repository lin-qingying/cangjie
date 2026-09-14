// FILE: ambiguityByConversion.cj
// TARGET_NAME: render
package sample.resolver.candidates

class Printer {
    public func render(): String {
        return "member"
    }
}

extend Printer {
    public func render(): String {
        return "extend"
    }
}

func usePrinter(): String {
    return Printer().render()
}
