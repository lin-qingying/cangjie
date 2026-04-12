// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %diag_json %f -o %output
// ASSERT: scan-2 sema_interface_member_must_be_implemented

interface I1 {
    func foo() {}
}

interface I2 {
    func foo() {}
}

class A {}

extend A <: I1 {}

extend A <: I2 {}

main(): Int64 {
    0
}
