// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


open class Base<T> {
    public static var i: Int32 = 2
    public static func foo(): Int32 {
        return 3
    }
}

class Test <: Base<Int32> {
    public var i1: Int32
    public var i2: Int32
    public init() {
        i1 = <!sema_object_cannot_access_static_member!>s<!>uper.i
        i2 = <!sema_object_cannot_access_static_member!>s<!>uper.foo()
    }
}

class Data<T> <: Base<T> {
    public var i1: Int32
    public var i2: Int32
    public init() {
        i1 = <!sema_object_cannot_access_static_member!>s<!>uper.i + 1
        i2 = <!sema_object_cannot_access_static_member!>s<!>uper.foo() + 1
    }
}

func run(): Int64 {
    var test: Test = Test()
    var data: Data<Int32> = Data<Int32>()
    return 0
}

main(): Int64 {
    return run()
}
