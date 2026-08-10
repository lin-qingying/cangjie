type A = Int64

interface abc {
    func ab(): Unit

    func <!CONFLICTING_OVERLOADS!>ab<!>(): Unit
}

class <!CLASSIFIER_REDECLARATION!>abc<!> {
}

class <!CLASSIFIER_REDECLARATION!>abc<!> {
    prop a: Int {
        get() {
            1
        }
    }

    func <!REDECLARATION!>a<!>() {}

    let <!REDECLARATION!>a<!> = 1

    func <!CONFLICTING_OVERLOADS!>a<!>() {
    }
}

func <!REDECLARATION!>abc<!>() {}
func <!CONFLICTING_OVERLOADS!>abc<!>() {}

let <!REDECLARATION!>abc<!> = 1

let (<!REDECLARATION!>abc<!>, <!REDECLARATION!>abc<!>) = (1, 2)

func abc1(a: Int) {
    let _ = a
}

func abc1(a: Int, b: Int) {
    let _ = a
    let _ = b
}
