

unsafe func unsafeFunc() {
    println("call unsafeFunc")
}

func funcWithFuncParam(fn: () -> Unit) {
    fn()
}

class ClassWithUnsafeMember {
    unsafe func unsafeFunc() {
        println("call unsafeFunc")
    }
}

main() {
    unsafe { unsafeFunc() } // unsafe function can only be called

    // normal reference
    var fn = <!UNSAFE_FUNC_CAN_ONLY_BE_CALLED!>unsafeFunc<!>
    funcWithFuncParam(<!UNSAFE_FUNC_CAN_ONLY_BE_CALLED!>unsafeFunc<!>)
    // member access
    let obj = ClassWithUnsafeMember()
    fn = <!UNSAFE_FUNC_CAN_ONLY_BE_CALLED!>obj.unsafeFunc<!>
    funcWithFuncParam(<!UNSAFE_FUNC_CAN_ONLY_BE_CALLED!>obj.unsafeFunc<!>)
    return 0
}
