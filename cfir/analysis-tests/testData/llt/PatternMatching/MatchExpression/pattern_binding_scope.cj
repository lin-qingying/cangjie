func ifCollision(value: ?Int64): Unit {
    if (let Some(b) <- value) {
        let <!REDECLARATION!>b<!> = <!UNRESOLVED_REFERENCE!>b<!>
        print(b)
    }
}

func ifCollisionOuter(value: ?Int64): Unit {
    let b = 20
    if (let Some(b) <- value) {
        let <!REDECLARATION!>b<!> = b
        print(b)
    }
}

func ifLegalShadow(value: ?Int64): Int64 {
    let b = 20
    if (let Some(b) <- value) {
        let value = b
        value
    } else { b }
}

func ifNestedBody(value: ?Int64): Unit {
    if (let Some(b) <- value) {
        if (true) {
            let b = b
            print(b)
        }
    }
}

func whileCollision(value: ?Int64): Unit {
    while (let Some(b) <- value) {
        let <!REDECLARATION!>b<!> = <!UNRESOLVED_REFERENCE!>b<!>
        print(b)
        break
    }
}

func matchCollision(value: ?Int64): Unit {
    match (value) {
        case Some(b) =>
            let <!REDECLARATION!>b<!> = <!UNRESOLVED_REFERENCE!>b<!>
            print(b)
        case None => ()
    }
}

func parameterCollision(b: Int64): Unit {
    let <!REDECLARATION!>b<!> = b
    print(b)
}

func localCollision(): Unit {
    let b = 1
    let <!REDECLARATION!>b<!> = b
    print(b)
}

func forCollision(values: Array<Int64>): Unit {
    for (b in values) {
        let <!REDECLARATION!>b<!> = b
        print(b)
    }
}

func ifPartialCollision(value: ?(Int64, Int64)): Unit {
    if (let Some((a, b)) <- value) {
        let <!REDECLARATION!>a<!> = <!UNRESOLVED_REFERENCE!>a<!>
        print(<!UNRESOLVED_REFERENCE!>b<!>)
    }
}

func collisionSecond(value: ?(Int64, Int64)): Unit {
    if (let Some((a, b)) <- value) {
        let <!REDECLARATION!>b<!> = <!UNRESOLVED_REFERENCE!>b<!>
        print(a)
    }
}

func laterCondition(value: ?Int64): Unit {
    if (let Some(a) <- value && let Some(b) <- value) {
        let <!REDECLARATION!>a<!> = <!UNRESOLVED_REFERENCE!>a<!>
        print(b)
    }
}

func collisionInGuard(value: ?Int64): Unit {
    if (let Some(a) <- value && <!UNRESOLVED_REFERENCE!>a<!> > 0) {
        let <!REDECLARATION!>a<!> = <!UNRESOLVED_REFERENCE!>a<!>
    }
}

func duplicatePattern(value: (Int64, Int64)): Unit {
    match (value) {
        case (a, <!REDECLARATION!>a<!>) => print(a)
    }
}

func typedCollision(value: Any): Unit {
    if (let a: Int64 <- value) {
        let <!REDECLARATION!>a<!> = a
        print(a)
    }
}

func typedWildcards(value: (Any, Any)): Unit {
    match (value) {
        case (_: Int64, _: Int64) => ()
        case _ => ()
    }
}
