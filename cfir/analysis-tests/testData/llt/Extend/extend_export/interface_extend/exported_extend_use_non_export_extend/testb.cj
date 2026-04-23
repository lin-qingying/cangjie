// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: ./testa.cj
package a.c

import a.b.*

interface I {
    func foo(): Unit
}

<!EXPORT_EXTEND_DEPEND_NON_EXPORT_EXTEND!>extend A <: I {}<!>

extend A {
    public func foo(): Unit {}
}

main() {}
