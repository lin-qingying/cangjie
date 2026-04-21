{
  "module": {
     "extensionAbilities": [
      {
        "name": "${backupAbilityName}",
        "srcEntry": "./ets/${backupAbilityName?lower_case}/${backupAbilityName}.ets",
        "type": "backup",
        "exported": false,
        "metadata": [
          {
            "name": "ohos.extension.backup",
            "resource": "$profile:backup_config"
          }
        ],
      }
    ]
  }
}