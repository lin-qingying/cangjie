// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


import define.*

main() {
    var a = <!lex_unterminated_single_line_string!>'@SAME{1}() <= @SAME{1}()<!>
    var b = @SAME[0]{1}() <= @SAME[0]{1}()
    if ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == b) && (a == true))
    {
        return 0
    }
    else
    {
        return 1
    }
}
