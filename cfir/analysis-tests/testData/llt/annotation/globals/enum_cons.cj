// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f

@Annotation[target: [EnumConstructor]]
public class APILevel {
    let a: UInt8
    const init(v: UInt8) {
        a = v
    }
}

enum E {
    @APILevel[11]
    caseA
    | @APILevel[11]
    caseB(Int64)
}

main() {}
