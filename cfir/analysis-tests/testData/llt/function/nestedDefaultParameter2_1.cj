// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

public open class Class11 {
    public var var19: Bool = true;
}

public struct Record106 {
    public var var121: Int32 = Int32(36);
}

var var118: Record106 = Record106();

class Context {
    public var p: Int64 = 2
    public func test(a!: Int64 = p): Int64 {
        var x = 1
        func test2(a!: Int64 = x): Int64 {
            a
        }
        a
    }
}

class Class {
    private let var1: Int32 = 1;
    public func test(): Bool {
        true
    }
    public static func test2(a!: Bool = {
            =>
            func test3(b!: Bool = {
                    =>
                    Class().var1
                    true && test()
                }()): Bool {
                b;
            }
            true
        }()) {
        a
    }
}

func test(): Int64 {
    let var114: Int8 = Int8(36);

    func abc(): Bool {
        true
    }
    var xyz: Bool = true
    func function101(param102!: Bool = abc() && Int32(15) <= {
            =>
            var114;

            var118
        }().var121): Bool {
        param102 && xyz
    }
    if (function101() != true) {
        return 1
    }
    return 0;
}

main(): Int64 {
    return 0;
}

/* SCAN
error: invalid use of non-static member 'test' in static lambda expression
  ==> nestedDefaultParameter2_1.cj:40:29:
   | 
40 |                     true && test()
   |                             ^ 
   | 

1 error generated, 1 error printed.
*/
