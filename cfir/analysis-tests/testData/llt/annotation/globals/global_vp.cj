// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f
@Annotation[target: [GlobalVariable]]
public class APMLevel {
    let a: UInt8
    const init(v: UInt8) {
        a = v
    }
}

@APMLevel[11]
const (c, d) = (3, 4)

@APMLevel[11]
let (a, b) = (1, 2)

@APMLevel[11]
let _ = 1

@APMLevel[11]
var (_, _, _) = (1, 2, 3)

main() {}
