// FILE: level_syscap_check/canIUse.cj


package ohos.base

public func canIUse(syscap: String): Bool {
    return syscap == "camera"
}

// FILE: level_syscap_check/device_info.cj


package ohos.device_info

public class DeviceInfo {
    public static prop sdkApiVersion: Int64 {
        get() {19}
    }
}

// FILE: nested.cj



import ohos.base.canIUse
import ohos.device_info.DeviceInfo

func foo(a!: Unit = @IfAvailable(syscap:"camera", {=>println(0)}, {=>println(1)})) {
}

func bar() {
    foo(a: @IfAvailable(syscap:"camera", {=>println(0)}, {=>println(1)}))
}

main() {
    let v = @IfAvailable(syscap:"camera", {=>println(0)}, {=>println(1)})
    @IfAvailable(level:19, { => @IfAvailable(syscap:"camera", {=>println(0)}, {=>println(1)}); }, {=>println(3)})
    foo()
    bar()
    ()
}
