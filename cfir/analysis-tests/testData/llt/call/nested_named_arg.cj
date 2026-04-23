// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

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
                        swiperControllerl.setSwipTo(<!NEED_NAMED_ARGUMENT!>0<!>);
                        return
                    }
                )
            )
        )
    }
}

main() {
}
