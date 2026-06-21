// FILE: unused014pkga.cj


package pkga

public import std.collection.HashMap as pkghash

public import std.collection.*
public type mymap = pkghash<String, Int64>

// FILE: unused014.cj



import pkga.mymap as mymapanew
import pkga.pkghash as pkghashanew
import <!UNUSED_IMPORT!>pkga.mymap<!>
import <!UNUSED_IMPORT!>pkga.pkghash<!>

main(): Int64 {
    var a : mymapanew = pkghashanew(10)
    return a.size
}
