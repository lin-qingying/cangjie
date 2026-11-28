// MODULE: ancestor
// FILE: ancestor.cj
package g4ancestor

public class SharedReceiver {
    public init() {}
}

extend SharedReceiver {
    public func sharedValue(): Int64 {
        17
    }
}

// MODULE: left()()(ancestor)
// FILE: left.cj
package g4left

// MODULE: right()()(ancestor)
// FILE: right.cj
package g4right

// MODULE: consumer(ancestor)()(left right)
// MAIN_MODULE
// MAIN_FILE_NAME: caller.cj
// FILE: caller.cj
package g4diamond

// refinement 仅描述平台来源图；普通声明不引入多 common parent 的语言语义。
private import g4ancestor.SharedReceiver

public func useSharedExtension(): Int64 {
    SharedReceiver().sharedValue()
}

public func useSharedExtensionAgain(): Int64 {
    SharedReceiver().sharedValue()
}
