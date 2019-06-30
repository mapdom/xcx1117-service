package shop.service;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;
import shop.dao.AuthMapper;
import shop.domain.*;
import shop.uitl.EncryptUtil;
import shop.val.VideoAuthMapper;

import javax.annotation.Resource;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by songningning1 on 2017/9/9.
 */
@Service
public class LoginService {

    private final static Logger log = LogManager.getLogger(LoginService.class);

    @Resource
    private AuthMapper authMapper;

    @Resource
    private VideoAuthMapper videoAuthMapper;

    public Map in(String code) throws Exception {

        String openId = null;
        try {
            String params = "appid=" + WXLoginFinal.getWxAppid().trim() + "&secret=" + WXLoginFinal.getwxSecret().trim() + "&js_code=" + code.trim() + "&grant_type=" + WXLoginFinal.getGrant_type().trim();
            //发送请求获取openId
            String data = HttpRequest.sendPost(WXLoginFinal.getUrl().trim(), params);
            if (StringUtils.isBlank(data)) {
                log.error("获取微信data失败");
                return ResMap.failedMap();
            }
            JSONObject jsonData = JSONObject.parseObject(data);
            if (jsonData == null) {
                log.error("获取微信jsonData失败");
                return ResMap.failedMap();
            }
            openId = jsonData.get("openid").toString().trim();
            String sessionKey = jsonData.get("session_key").toString().trim();
            if (StringUtils.isBlank(openId) || StringUtils.isBlank(sessionKey)) {
                return ResMap.failedMap();
            }
            if (StringUtils.isBlank(openId) || StringUtils.isBlank(sessionKey)) {
                log.error("openId or sessionKey is null,  code : " + code);
                return ResMap.failedMap("openId or sessionKey is null");
            }
        } catch (Exception e) {
            log.error("获取微信openId失败");
        }
        String key = "9ba45bfd500642328ec03ad8ef1b4321";// 自定义密钥
        EncryptUtil des = new EncryptUtil(key, "utf-8");
        String token = des.encode(openId);
        Map resMap = new HashMap();
        resMap.put("token", token);

        try {
            Auth rs = authMapper.selectToken(openId);
            if (rs != null && rs.getStatus() == 1) {
                resMap.put("auth", true);
                return resMap;
            }
        } catch (Exception e) {
            log.error("读取数据获取auth状态异常" + e);
        }

        try {
            Auth auth = new Auth();
            auth.setToken(openId);
            auth.setStatus(0);
            auth.setCreated(new Date());
            int rk = authMapper.insertSelective(auth);
            if (rk > 0) {
            }
            log.error("--------------------" + JSONObject.toJSONString(resMap));
            return resMap;
        } catch (Exception e) {
            log.error("插入登陆openId异常" + e);
        }
        log.error("login is failed,  code : " + code);
        return ResMap.failedMap("login is failed");
    }


    /**
     * 验证token
     *
     * @param token
     * @return
     */
    public Map auth(String token) throws Exception {

        String key = "9ba45bfd500642328ec03ad8ef1b4321";// 自定义密钥
        EncryptUtil des = new EncryptUtil(key, "utf-8");
        String r = des.decode(token);
        Map map = new HashMap(2);
        map.put("auth", false);
        try {
            Auth auths = authMapper.selectToken(r);
            if (auths != null) {
                map.put("auth", true);
                return map;
            }
        } catch (Exception e) {
            log.error("读取数据获取auth状态异常" + e);
            return map;
        }
        return map;
    }

    /**
     * 申请控制生产权限
     *
     * @param token
     */
    public Map authSC(String token, String code) {
        Map resMap = new HashMap();
        resMap.put("success", false);
        VideoAuth v = new VideoAuth();
        v.setStatus(0);
        v.setToken(token);
        v.setAuth(Integer.parseInt(code));
        v.setAccredit(0);
        try {
            int vi = videoAuthMapper.insert(v);
            resMap.put("success", true);
            resMap.put("code", v.getId());
            log.info("authSC apply, token:" + token);
            return resMap;
        } catch (Exception e) {
            log.error("authSC is exception, e:" + e);
            return resMap;
        }
    }

}


