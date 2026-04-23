{
  "module": {
    "abilities": [
      {
        "name": "${abilityName}",
        "srcEntry": "ohos_app_cangjie_${moduleName}.MainAbility",
        "description": "$string:${abilityName}_desc",
        "icon": "$media:layered_image",
        "label": "$string:${abilityName}_label",
        "startWindowIcon" : "$media:startIcon",
        "startWindowBackground" : "$color:start_window_background"
        <#if visible>
          ,
        "exported": true
        </#if>
        <#if hasSkill>
          ,
        "skills": [
          {
            "entities": [
              "entity.system.home"
            ],
            "actions": [
              "action.system.home"
            ]
          }
        ]
        </#if>
      }
    ]
  }
}
