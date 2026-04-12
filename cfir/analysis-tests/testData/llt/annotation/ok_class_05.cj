// Copyright (c) Huawei Technologies Co., Ltd. 2026. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: ok_class_05_dep.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt ok_class_05_dep.cj -o pkga.%middle
// EXEC: %compiler %cmp_opt pkga.%middle %f -o %output

import pkga.*

@B
class Test {
    @B
    public init() {
        println("testfunc")
    }
    @B
    private var a = 1
}

main() {}
