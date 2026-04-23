internal import std.collection.*
public type Tag = UInt32
extend Tag {
    public prop tagSize: Int64 {
        get() {
            if (this < (1 << 4)) { return 1 }
            if (this < (1 << 11)) { return 2 }
            if (this < (1 << 18)) { return 3 }
            if (this < (1 << 25)) { return 4 }
            5
        }
    }
}
 func getOneofIndex(): ?HashMap<Tag, Int64> { None }

main() {
    var tag : Tag= 1
    if(let Some(Some(index)) <- getOneofIndex()?.get(tag) ) {
        println("abcd")
    }
    0
}
