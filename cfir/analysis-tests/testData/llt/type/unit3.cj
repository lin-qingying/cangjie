// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %n.cj

main(): Int64 {
    var <!chir_dce_unused_variable!>a<!>: (Unit) -> UInt64
    func test(<!chir_dce_unused_variable!>input<!>: Unit): UInt64 {
        return 0
    }
    a = test
    return 0
}
