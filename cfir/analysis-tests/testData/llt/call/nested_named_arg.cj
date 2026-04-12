// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1:%frontendCompiler %cmp_opt %f 2>&1 | compare %f

class A {}

class B {
    public let success: Option<(A) -> Unit>

    public init(success!: Option<(A) -> Unit> = Option<(A) -> Unit>.None) {
        this.success = success
    }
}

class SwiperController {
    public func setSwipTo(index!: Int64) {}
}

func takeVideo(a: B) {}

class graphicVideo {
    public let swiperControllerl = SwiperController()

    public func takeVideo(a: Int64) {
        takeVideo(
            B(
                success: Some<(A) -> Unit>(
                    {
                        ret: A =>
                        swiperControllerl.setSwipTo(0);
                        return
                    }
                )
            )
        )
    }
}

main() {
}

/* SCAN
error: missing argument prefix 'index:' for named parameter
  ==> nested_named_arg.cj:34:53:
   | 
34 |                         swiperControllerl.setSwipTo(0);
   |                                                     ^ 
   | 

1 error generated, 1 error printed.
*/
