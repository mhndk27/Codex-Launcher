package com.mhndk27.codex.data;

public class Account {
    
    private String uuid; 
    private String username; 
    private String accessToken; 
    private String accountType; // مثل: "Offline", "MSA", "Ely.by"

    // مُنشئ كامل - (لتجنب الخطأ في DataManager)
    public Account(String uuid, String username, String accessToken, String accountType) {
        this.uuid = uuid;
        this.username = username;
        this.accessToken = accessToken;
        this.accountType = accountType;
    }
    
    // مُنشئ فارغ - (ضروري لمكتبة Gson)
    public Account() {
    }

    // دوال Getter
    public String getUuid() { return uuid; }
    public String getUsername() { return username; }
    public String getAccessToken() { return accessToken; }
    public String getAccountType() { return accountType; }
}