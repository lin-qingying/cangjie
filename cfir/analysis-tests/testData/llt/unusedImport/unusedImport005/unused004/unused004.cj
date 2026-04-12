// DEPENDENCE: ./other.cj
// EXEC: %compiler %cmp_opt --compile-macro other.cj
// EXEC-PIPE: %compiler %cmp_opt %diag_json %f -o %output 2>&1 | compare %f

import a.Derive

@Derive[Equatable]
class A{
    var a = 1
    init(temp:Int64){
        a = temp
    }
}

main() {
}

// ASSERT: scan-not sema_unused_import

// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.
