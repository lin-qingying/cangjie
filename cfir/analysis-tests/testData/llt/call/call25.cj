

class A {
    public func g(): Int64 { // g1
        func g(): Bool {
            return true
        } // g2

        // g2 shadows g1, only 1 candidate here
        let x: Int64 = <!TYPE_MISMATCH!>g()<!>
        let y: () -> Int64 = <!TYPE_MISMATCH!>g<!>
        return 1
    }
    public func f(): Int32 {
        return 0
    }
    public func aaa(): Int64 {
        let x: () -> Int64 = <!TYPE_MISMATCH!>f<!>
        return <!TYPE_MISMATCH!>f()<!>
    }
}

func f(): Int64 { // f1
    func f(): Bool {
        return true
    } // f2

    // f2 shadows f1, only 1 candidate here
    let x: Int64 = <!TYPE_MISMATCH!>f()<!>
    let y: () -> Int64 = <!TYPE_MISMATCH!>f<!>
    return 1
}

main(): Int64 {
    return 0
}
