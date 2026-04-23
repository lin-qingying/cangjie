{
  "module": {
    "name": "${moduleName}",
    "type": "${moduleType}",
    "description": "$string:module_desc",
    "mainElement": "${abilityName}",
    "deviceTypes":  [${deviceTypes}],
    "deliveryWithInstall": true,
    "installationFree": ${isInstallationFree?c},
    "srcEntry": "ohos_app_cangjie_${moduleName}.MyAbilityStage",
    "abilities": [
    ]
  }
}