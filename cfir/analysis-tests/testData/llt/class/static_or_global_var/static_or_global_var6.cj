// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %output 2>&1 |compare %f

class A {
    static var AFLAG: Bool
    static init() {
        AFLAG = B.BFLAG
    }
}

class B {
    public static let BFLAG: Bool = true
}

main(): Int64 {
    0
}

// ASSERT: scan global/static variable 'BFLAG' is used before initialization during initializing 'AFLAG'