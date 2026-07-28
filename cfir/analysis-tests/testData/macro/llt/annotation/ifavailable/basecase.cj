// API_LEVEL: 0

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

// FILE: basecase.cj


import ohos.device_info.DeviceInfo

main() {
    @IfAvailable(level:19, { => println(0) }, {=>println(3)})
}
