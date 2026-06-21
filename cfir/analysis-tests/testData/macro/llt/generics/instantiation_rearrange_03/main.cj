// FILE: test.cj


package test.a

class MyListIterator<T> <: Iterator<T> {
    @Frozen
    @OverflowWrapping
    public func next(): Option<T> {
        return None
    }
}
public class MyList<T> <: Iterable<T> {
    public func fold(): Unit {}
    public func reduce(): Unit {}
    @Frozen
    public func iterator(): Iterator<T> {
        println("call iterator")
        return MyListIterator<T>()
    }
    @Frozen
    public func isEmpty(): Bool {
        return false
    }
}

extend<T> MyList<T> <: ToString where T <: ToString {
    @Frozen
    public func toString(): String {
        return collectionToString<MyList<T>, T>(this)
    }
}

@Frozen
func collectionToString<C, T>(col: C): String where C <: Iterable<T>, T <: ToString {
    col.iterator()
    return ""
}

// FILE: main.cj



package test

import test.a.MyList

main() {
    var a = MyList<String>()
    println(a)
}
