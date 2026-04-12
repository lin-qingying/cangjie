// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

main() {
    let future: Future<Unit> = spawn {
        var future: Future<Unit> = 0
        return future
        main() {}
    }
}

// ASSERT: regex unexpected main function in block
// ASSERT: regex 1 error generated, 1 error printed.
// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace
