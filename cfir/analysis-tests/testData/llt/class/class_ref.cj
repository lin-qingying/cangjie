// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


open class c {}
interface i {}
class C<T> <: c & i where T <: c {
    public var c : Int64 = 1
    public var i : Int64 = 1
}
interface I<T> <: i where T <: i {
    <!parse_unexpected_declaration_in_scope!>let<!> i : Int64 = 1
}

class E<T> <: T {
    public var T:Int32 = 1
}
class F<T> <: T {
    public var T: Int32 = 1
}

main() {
    var a = C<c>().c + I<i>.i
    return 0
}
