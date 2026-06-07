// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


open class A<T>{
    public func test(a:T){print("inside")}
    public var i : T
    public init(a: T) { this.i = a }
    public func b(){
    test(i)
}
<!parse_expected_decl!>return<!> 1;}

main():Int64{
    A<Int64>(0).b()
    return 0
}
