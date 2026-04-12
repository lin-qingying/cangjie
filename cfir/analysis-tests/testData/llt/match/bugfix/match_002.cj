// EXEC: %compiler %cmp_opt %f -o %output 2>&1 | compare %f
// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace

enum TimeUnit2 {
| Year(Float32)
| Month(Float32, Float32)
| Day(Float32, Float32, Float32)
| Hour(Float32, Float32, Float32, Float32)
}

func test() {
    var T1 = TimeUnit2.Day(0.1, 0.2, 0.3)
    match (T1) {
    case Day(x) => match (x) {
    case
    }
    }
}


// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.
