// FILE: unused013pkga.cj


package pkga

public import std.collection.HashMap as pkghash

public import std.collection.*
public type mymap = pkghash<String, Int64>

// FILE: unused013.cj



import <!UNUSED_IMPORT!>pkga.mymap as mymapanew<!>
import <!UNUSED_IMPORT!>pkga.pkghash as pkghashanew<!>
import pkga.*

main(): Int64 {
    var a : HashMap<String, Int64> = mymap(10)
    return a.size
}
