// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// LEVLE: 0
// EXEC-PIPE-0: %compiler %cmp_opt %f --no-sub-pkg -o %output 

package test

public interface A {
    func a(): Int
}

public interface MarkerA <: A {
}

class Foo {}

extend Foo <: A {
    public func a() { 9 }
}

extend Foo <: MarkerA {
}

main() {
    println(Foo().a())
    0
}

