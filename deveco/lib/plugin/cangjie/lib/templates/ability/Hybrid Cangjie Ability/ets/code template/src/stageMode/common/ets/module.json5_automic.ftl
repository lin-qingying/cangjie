{
  "module": {
    "abilities": [
      {
        "name": "${abilityName}",
        "srcEntry": "./ets/${abilityName?lower_case}/${abilityName}.${abilityFileNameExt}",
        "description": "$string:${abilityName}_desc",
        "icon": "$media:icon",
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
