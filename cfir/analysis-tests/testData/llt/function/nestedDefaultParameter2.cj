// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

public open class Class11 {
    public var var19: Bool = true;
}

public struct Record106 {
    public var var121: Int32 = Int32(36);
}

var var118: Record106 = Record106();

class Context {
    public let p: Bool = true
    public func test(): Int64 {
        let var114: Int8 = Int8(36);
        func abc(): Bool {
            true
        }
        let xyz: Bool = true
        func function101(param102!: Bool = abc() && Context().p && p && Int32(15) <= {
                =>
                var114;

                var118
            }().var121): Bool {
            param102 && xyz && p
        }
        if (function101() != true) {
            return 1
        }
        return 0;
    }
}

main(): Int64 {
    let var13: Class11 = Class11();
    func function8(param9!: Bool = var13.var19): Bool {
        param9
    }
    var b = Context()
    if (function8() != true || b.test() != 0) {
        return 1
    }
    return 0;
}
