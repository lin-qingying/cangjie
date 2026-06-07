// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


main():Int64{
    var score: Int64 = 90
    var result: String = match <!parse_selector_or_match_expression_body!>score<!> {
        case 0 => "fail"
        case 60 => "pass"
        case 80 => "good"
        case 90 => "excellent"
        case _ => ""
    }

    if (result == "excellent") {
        return 0
    }
    return 1

}
