// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: unused014pkga.cj

import pkga.mymap as mymapanew
import pkga.pkghash as pkghashanew
import <!UNUSED_IMPORT!>pkga.mymap<!>
import <!UNUSED_IMPORT!>pkga.pkghash<!>

main(): Int64 {
    var a : mymapanew = pkghashanew(10)
    return a.size
}

