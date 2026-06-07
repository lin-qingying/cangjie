// FILE: pkgs009002.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

internal import pkgs009001a.one
internal import pkgs009001a.two

// FILE: pkgs009003.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

internal import pkgs009001a.*

// FILE: pkgs009001.cj
// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


internal import pkgs009001a.NormalFunc
internal import pkgs009001a.Bar

main(): Int64 {
    var a: Int32 = 3
    var b = NormalFunc(a)
    var c: Int32 = 4
    var d = Foo<Int32, Bool>(c, true)
    var e: Int32 = 5
    var f = Bar<Int32>(e)

    var h = one
    var i = one
    var j = two
    var k = two
    var l = three
    var m = three

    if (b == 3 && f == 5) {
        return 0
    } else {
        return 1
    }
}
