// FILE: a/a.cj


package a

import std.time.DateTime
import <!UNUSED_IMPORT!>std.collection.ArrayList<!>

public type List<T> = MetaList<T>

public interface I0{}

public class MetaList<T> where T <: I0 {
    public init(elements: Array<T>) {
    }
}

extend DateTime <: I0{
}

// FILE: main.cj



import std.time.DateTime
import a.*

public func maxDateTime(dateList:List<DateTime>):DateTime{
    return DateTime.now();
}

public func testFunc(tradeDate1: DateTime) {
    return List(maxDateTime(List(tradeDate1)))
}

main(): Int64 {
    return 0
}
