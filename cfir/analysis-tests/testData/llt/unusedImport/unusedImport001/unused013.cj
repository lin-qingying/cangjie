// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: unused013pkga.cj

import <!UNUSED_IMPORT!>pkga.mymap as mymapanew<!>
import <!UNUSED_IMPORT!>pkga.pkghash as pkghashanew<!>
import pkga.*

main(): Int64 {
    var a : HashMap<String, Int64> = mymap(10)
    return a.size
}

