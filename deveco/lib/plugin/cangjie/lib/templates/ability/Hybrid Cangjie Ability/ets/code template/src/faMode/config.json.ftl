{
  "module": {
    "abilities": [
      {
        <#if hasSkill>
        "skills": [
          {
            "entities": [
              "entity.system.home"
            ],
            "actions": [
              "action.system.home"
            ]
          }
        ],
        </#if>
        "orientation": "unspecified",
        "formsEnabled": false,
        "name": ".${abilityName}",
        "srcLanguage": "${uiSyntax?lower_case}",
        "srcPath": "${abilityName}",
        "icon": "$media:icon",
        "description": "$string:${abilityName}_desc",
        "label": "$string:${abilityName}_label",
        "type": "page",
        <#if visible>
          "visible": true,
        </#if>
        "launchType": "standard"
      }
    ]
  }
}