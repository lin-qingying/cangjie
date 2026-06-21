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

// FILE: level_limit.cj



import ohos.base.canIUse
import ohos.device_info.DeviceInfo

main() {
    <!IFAVAILABLE_LEVEL_LIMIT!>@IfAvailable(level: 18, { => }, { => })<!>
    @IfAvailable(level: 19, { => }, { => })
    @IfAvailable(syscap: "xxx", 
        { => <!IFAVAILABLE_LEVEL_LIMIT!>@IfAvailable(level: 18, { => }, { => })<!> },
        { => <!IFAVAILABLE_LEVEL_LIMIT!>@IfAvailable(level: 18, { => }, { => })<!> }
    )
    0
}
