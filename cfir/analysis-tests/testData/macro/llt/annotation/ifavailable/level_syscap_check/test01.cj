// FILE: canIUse.cj


package ohos.base

public func canIUse(syscap: String): Bool {
    return syscap == "camera"
}

// FILE: device_info.cj


package ohos.device_info

public class DeviceInfo {
    public static prop sdkApiVersion: Int64 {
        get() {19}
    }
}

// FILE: test01.cj



main() {
    <!USE_EXPR_WITHOUT_IMPORT!>@IfAvailable(level: 19, {=>}, {=>})<!>
    <!USE_EXPR_WITHOUT_IMPORT!>@IfAvailable(syscap: "xxx", {=>}, {=>})<!>
}
