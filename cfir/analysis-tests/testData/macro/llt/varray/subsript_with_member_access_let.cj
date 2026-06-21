

main() {
    var fdset = FdSetVal()
    fdOne(fdset)
}

@C
public struct FdSetVal {
    var fdBits = VArray<Int64, $6>(repeat: 0)
}

public func fdOne(fdset: FdSetVal): Unit {
    let size: Int64 = fdset.fdBits.size
    for (i in 0..size) {
        <!CANNOT_ASSIGN_TO_IMMUTABLE!>fdset.fdBits[i]<!> = 1
        println(fdset.fdBits[i])
    }
}
