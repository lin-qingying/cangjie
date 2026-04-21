{
  "app": {
    "bundleName": "${bundleName}",
    "vendor": "example",
    "version": {
      "code": 1000000,
      "name": "1.0.0"
    }
  },
  "deviceConfig": {},
  "module": {
<#if !deviceTypes?lower_case?contains("litewearable")>
  "package": "${packageName}",
</#if>
    "deviceType": [${deviceTypes}],
    "distro": {
      "deliveryWithInstall": true,
      "moduleName": "${moduleName}",
      "moduleType": "${moduleType}"
    }
<#if !deviceTypes?lower_case?contains("litewearable")>
,"uiSyntax": "${(uiSyntax?lower_case == 'ets')?then('ets', 'js')}"
</#if>
  }
}