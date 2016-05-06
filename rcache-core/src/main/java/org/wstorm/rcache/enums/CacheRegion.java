package org.wstorm.rcache.enums;

/**
 * 缓存分类区域, 为了减少key长度节省空间,限制固定10个字符
 *
 * @author sunyp
 * @version 1.0
 * @created 2016年05月06日
 */
public enum CacheRegion {
    /**
     * 无区域的,未知的
     */
    unknown(null),
    /**
     * UDA 服务：短信
     */
    service_sms("udserv:s01"),
    /**
     * UDA 服务：文件管理
     */
    service_file("udserv:f01"),
    /**
     * UDA 服务：用户专用
     */
    service_app_user("udserv:usr"),
    /**
     * UDA 服务：ud APP
     */
    service_app("udserv:app"),
    /**
     * UDA 服务： 旅游开心答
     */
    service_answergame("udserv:g01"),
    /**
     * UDA 服务：消息
     */
    service_message("udserv:msg"),
    /**
     * UDA 服务：监控
     */
    service_metrix("udserv:h01"),
    /**
     * UDA 服务： 权限
     */
    service_permission("udserv:p01"),
    /**
     * UDA 服务： 计划任务
     */
    service_scheduler("udserv:j01"),
    /**
     * UDA web: 业务系统
     */
    web_admin("udwebs:adm"),
    /**
     * UDA web: 公众平台系统
     */
    web_mp("udwebs:mvp"),
    /**
     * UDA web: 权限系统
     */
    web_permission("udwebs:p01"),
    /**
     * UDA api接口层: message
     */
    api_message("udapis:msg"),
    /**
     * UDA api接口层: app
     */
    api_app("udapis:app"),
    /**
     * UDA api接口层: game
     */
    api_game("udapis:g01"),
    /**
     * UDA api接口层 卡牌游戏
     */
    api_cardgame("udapis:g02"),
    /**
     * 教育网站论坛
     */
    iplayar_bbs("active:bbs");

    public final String region;

    private CacheRegion(String region) {
        this.region = region;
    }
}
