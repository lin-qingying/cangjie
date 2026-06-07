// FILE: p1.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package p1

public class Foo {}

// FILE: p2.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

package p2

internal import p1.*

extend Foo {
    public func f() {}
}

func g() {
    let a = Foo()
    a.f() // ok
}

// FILE: main.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


// 当直接扩展与被扩展类型的定义处在不同的 package 时，扩展永远不会被导出，只能在当前 package 使用。

internal import p1.*
internal import p2.*

main() {
    let a = Foo()
    a.<!NOT_MEMBER_OF!>f<!>()
    return 0
}
