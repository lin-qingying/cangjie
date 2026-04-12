// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


interface A {
    func add(): Unit
}

interface B {
    func add(): Unit
}

class S {} // EXPECTED: regex class 'S' missing abstract modifier or abstract function 'add' should be implemented

extend S <: A {
    public func add(): Unit {}
}

extend S <: B {}
