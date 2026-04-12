// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: a.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt a.cj -o a.%middle
// EXEC: %compiler %cmp_opt a.%middle %n.cj -o %output
// RUN-EXEC: %run %run_opt %output

package b

import a.*

class B <: Remote {
    public static func ctor() {
        Remote()
    }
}

main() {
    if (B.ctor().remoteCall) {
        0
    } else {
        1
    }
}
