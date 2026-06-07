// FILE: unused013pkga.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package pkga

public import std.collection.HashMap as pkghash

public import std.collection.*
public type mymap = pkghash<String, Int64>

// FILE: unused013.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


import <!UNUSED_IMPORT!>pkga.mymap as mymapanew<!>
import <!UNUSED_IMPORT!>pkga.pkghash as pkghashanew<!>
import pkga.*

main(): Int64 {
    var a : HashMap<String, Int64> = mymap(10)
    return a.size
}
