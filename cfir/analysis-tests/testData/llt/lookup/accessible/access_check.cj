// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: pkga.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt pkga.cj -o pkga.%middle
// EXEC-PIPE-0: %compiler %cmp_opt %f pkga.%middle -o %output

import pkga.A

public func map(): Int64 {
    return 20
}

extend A {
    public func test(): Unit {
        var a: Int64 = map()
    }
}

main() {}
