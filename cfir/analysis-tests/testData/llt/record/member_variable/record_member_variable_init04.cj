<!MISSING_ENTRY!>/<!>/ Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// CHECK_PROGRAM_ENTRY
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


struct A {
    public let b: Int64 = 1
    public static var a: () -> Int64 = {=> <!STATIC_LAMBDA_CANNOT_ACCESS_NON_STATIC!>b<!>}
}
