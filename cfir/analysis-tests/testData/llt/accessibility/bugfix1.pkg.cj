// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// LEVLE: 0
package a
interface I {
    func foo() {
        println(0)
    }
}

protected class C <: I {
}
public class D <: I {
}

public class E {}
extend E <: I {}
