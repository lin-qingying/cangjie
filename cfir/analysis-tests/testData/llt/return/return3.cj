// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace

main() {
    var a = () =>{
        if true{
            return 1
        }
        if false{
            return 2
        }
    }
    return 0
}