// EXEC: %compiler %cmp_opt %overflowOpt %enableO2 %n.cj -o %n.%suffix 
// EXEC: %run %run_opt %n.%suffix

@Deprecated
internal const func func_1747770243692_63<T0>() : Unit { // block id: 67

}

@Frozen
public func f1(): Unit {
    func f2<T0, T1>(p1!: Int8 = Int8(88), p2!: () -> Unit = func_1747770243692_63<UInt64>) : Unit {}

    func f3<T0, T1>(): Unit {
        let v1 : Int8 = Int8(88);
        let v2 = f2<Int16, String>(p2: { =>
                func f4<T0, T1>(): Unit {
                    f2<Int16, String>(p1: v1)
                }
                f4<Int16, Float64>()
            }
        );
    }
    f3<Range<Int8>, Range<Int8>>()
}

main() {}

// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.
