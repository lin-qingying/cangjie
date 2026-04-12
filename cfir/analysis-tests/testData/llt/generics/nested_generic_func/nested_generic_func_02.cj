// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

func test() {
    let var1: Option<Int16> = Option<Int16>.None
    let var2: Option<Option<Int16>> = Option<Option<Int16>>.Some(None)
    (var2 ?? var1) ?? match (var1) {
        case _ =>
            func func1<T0, T1>(param1: Rune, param2: Unit): Int64 {
                Int64(1)
            };
            func1<Bool, UInt16>(match ("B") {
                case _ =>
                    if (true) {
                        r'A'
                    } else {
                        r'B'
                    }
            }, if (true) {
            });
            Int16(1)
    };
}

main() {
    let var1: Bool = ("A" is Option<Bool>)
    var var2: Bool = (UInt64(1) is UInt8);
    if (var2) {
    } else if ((if (var2) {
        Option<Bool>.Some(var1)
    } else {
        Option<Bool>.Some(var1)
    } ?? try {
        false
    } catch (e: Exception) {
        func func1<T0>(): Int64 {
            Int64(1)
        };
        func1<Int8>();
        false
    } finally {
    })) {
    }
}
