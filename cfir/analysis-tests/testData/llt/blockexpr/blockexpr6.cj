// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

class ClassB {
    public var var1: Unit = ();
}

var b = {
    =>
    func funca() {
        1
    };
    ClassB();
}()

main(): Int64 {
    return 0
}
