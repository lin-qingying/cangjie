// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


var f: () -> Int64 = {=> 1}

open class A {
    var x: Int64 = 123
    init() {
        f = {=> <!sema_illegal_capture_this!>t<!>his.x}
    }
}

abstract class B {
    var x: Int64 = 123
    init() {
        f = {=> <!sema_illegal_capture_this!>t<!>his.x}
    }
}

class C {
    var x: Int64 = 123
    init() {
        f = {=> this.x}
    }
}

main() {}
