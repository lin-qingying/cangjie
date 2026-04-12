// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f -Woff all

@Annotation
public class APILevel {
    let a: UInt8
    const init(v: UInt8) {
        a = v
    }
}

@APILevel[11]
var x = 10

@APILevel[11]
func f(@APILevel[11] v: Int64): Int64 {
    11
}

@APILevel[11]
struct S {
    @APILevel[11]
    let x: Int64 = 1

    @APILevel[11]
    static let y: Int64 = 1

    @APILevel[11]
    init(@APILevel[11] v: Int64) {
        c = 8
    }

    @APILevel[12]
    S(@APILevel[12] let c!: Int32 = 9) {}
}

@APILevel[11]
enum E {
    @APILevel[11]
    caseA
    | @APILevel[11]
    caseB(Int64)
}

@APILevel[11]
extend S {}

main() {}
