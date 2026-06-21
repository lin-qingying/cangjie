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

// FILE: test02.cj



import ohos.device_info.*
import ohos.base.*

main() {
    @IfAvailable(level: 19, {=>}, {=>})
    @IfAvailable(syscap: "xxx", {=>}, {=>})
}
