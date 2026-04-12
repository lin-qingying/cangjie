// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: struct_data.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt struct_data.cj -o libdata.%lib_extension
// EXEC: %compiler %cmp_opt %n.cj libdata.%lib_extension -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

package example

import example.data.*

main() {
    let d = Data()
    d.foo()
    d.a_ + d.a
}
