// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: file2.cj

class A {
    public func foo() {
        <!CANNOT_ASSIGN_TO_IMMUTABLE!>res1<!> += 1
        <!CANNOT_ASSIGN_TO_IMMUTABLE!>res2<!> += 1
        return
    }
}
