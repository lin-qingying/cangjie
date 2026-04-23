// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DIAGNOSTICS: -warnings

// DEPENDENCE: pkgs

import pkgs.*

main(): Int64 {
    return run()
}

func run(): Int64 {
    var res: Int32
    var a: Int32 = 3
    var b: Int32 = 4
    let data: <!UNRESOLVED_REFERENCE!>Data<!><Int32> = <!UNRESOLVED_REFERENCE!>Data<!><Int32>(3, 4)
    let data2: <!UNRESOLVED_REFERENCE!>Data<!><Int32> = <!UNRESOLVED_REFERENCE!>Data<!><Int32>(3, 4)
    res = data.area()
    if (res == 3) {
        return 0
    }
    return 1
}
