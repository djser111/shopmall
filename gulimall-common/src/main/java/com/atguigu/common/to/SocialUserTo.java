package com.atguigu.common.to;

import lombok.Data;

@Data
public class SocialUserTo {
    private String access_token;
    private String token_type;
    private int expires_in;
    private String refresh_token;
    private String scope;
    private int created_at;
    private String uid;
    private String name;
    private String avatar_url;


}
