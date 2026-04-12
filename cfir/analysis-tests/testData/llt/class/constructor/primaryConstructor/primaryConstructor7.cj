// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f
// EXEC: %run %run_opt %n.%suffix
// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace

class Data {
    var b: Float32
    public Data(let a: Int32) {
        b = 2.0
    }
}

func run(): Int64 {
    var value: Data = Data(1);
    if ((value.a == 1) && (value.b == 2.0)) {
        return 0
    } else {
        return 1
    }
}

main(): Int64 {
    return run()
}
