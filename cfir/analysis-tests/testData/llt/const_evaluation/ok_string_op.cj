// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

const func test() {
    let s = "test"
    s == "test" && s != "tess"
    s + " it" == "test it" && s + "s" == "tests" && s * 3 == "testtesttest" && s < "tesz" && s <= "tesz" && s > "tess" &&
        s >= "tess"
}

main() {
    if (test()) {
        0
    } else {
        1
    }
}
