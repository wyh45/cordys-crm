package cn.cordys.crm.system.utils;

import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.sms.v20210111.SmsClient;
import com.tencentcloudapi.sms.v20210111.models.SendSmsRequest;
import com.tencentcloudapi.sms.v20210111.models.SendSmsResponse;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.util.Map;

import org.springframework.util.Assert;

@Component
@Slf4j
public class SmsSender {

    @Value("${tencentcloud.sms.enabled:false}")
    private boolean enabled;

    @Value("${tencentcloud.sms.secret-id:}")
    private String secretId;

    @Value("${tencentcloud.sms.secret-key:}")
    private String secretKey;

    @Value("${tencentcloud.sms.sms-sdk-app-id:}")
    private String smsSdkAppId;

    @Value("${tencentcloud.sms.sign-name:}")
    private String signName;

    @Value("${tencentcloud.sms.template-id:}")
    private String templateId;

    @Value("${tencentcloud.sms.region:ap-guangzhou}")
    private String region;

    private SmsClient smsClient;

    @PostConstruct
    public void init() {
        if (!enabled) {
            log.info("腾讯云短信功能未启用（tencentcloud.sms.enabled=false）");
            return;
        }
        Assert.hasText(secretId, "tencentcloud.sms.secret-id 不能为空");
        Assert.hasText(secretKey, "tencentcloud.sms.secret-key 不能为空");
        Assert.hasText(smsSdkAppId, "tencentcloud.sms.sms-sdk-app-id 不能为空");
        Assert.hasText(signName, "tencentcloud.sms.sign-name 不能为空");
        if (StringUtils.isBlank(templateId)) log.warn("tencentcloud.sms.template-id 未配置，send/sendBatch 将不可用");

        Credential cred = new Credential(secretId, secretKey);
        HttpProfile httpProfile = new HttpProfile();
        httpProfile.setReqMethod("POST");
        httpProfile.setConnTimeout(10);
        httpProfile.setWriteTimeout(10);
        httpProfile.setReadTimeout(10);
        httpProfile.setEndpoint("sms.tencentcloudapi.com");

        ClientProfile clientProfile = new ClientProfile();
        clientProfile.setHttpProfile(httpProfile);

        smsClient = new SmsClient(cred, region, clientProfile);
        log.info("腾讯云短信客户端初始化成功，地域：{}，AppId：{}，签名：{}", region, smsSdkAppId, signName);
    }

    /**
     * 发送短信验证码
     *
     * @param phoneNumber 手机号（E.164 格式，如 +86138xxxxxxx）
     * @param code        验证码
     */
    public void sendVerificationCode(String phoneNumber, String code) {
        send(phoneNumber, new String[]{code});
    }

    /**
     * 发送短信
     *
     * @param phoneNumber 手机号（E.164 格式）
     * @param params      模板参数（与模板变量一一对应）
     */
    public void send(String phoneNumber, String[] params) {
        if (!enabled || smsClient == null) {
            log.warn("短信功能未启用或客户端未初始化，跳过发送");
            return;
        }
        if (StringUtils.isBlank(phoneNumber)) {
            log.warn("手机号为空，跳过发送");
            return;
        }

        try {
            SendSmsRequest req = new SendSmsRequest();
            req.setSmsSdkAppId(smsSdkAppId);
            req.setSignName(signName);
            req.setTemplateId(templateId);
            req.setPhoneNumberSet(new String[]{phoneNumber});
            if (params != null && params.length > 0) {
                req.setTemplateParamSet(params);
            }
            req.setSessionContext("");
            req.setExtendCode("");
            req.setSenderId("");

            SendSmsResponse res = smsClient.SendSms(req);
            String requestId = res.getRequestId();
            String code = res.getSendStatusSet()[0].getCode();
            String message = res.getSendStatusSet()[0].getMessage();

            log.info("短信发送结果 - 手机号：{}，请求ID：{}，状态码：{}，描述：{}", phoneNumber, requestId, code, message);

            if (!"Ok".equals(code)) {
                log.error("短信发送失败 - 手机号：{}，状态码：{}，描述：{}", phoneNumber, code, message);
            }
        } catch (TencentCloudSDKException e) {
            log.error("短信发送异常 - 手机号：{}，错误码：{}，错误信息：{}", phoneNumber, e.getErrorCode(), e.getMessage(), e);
        } catch (Exception e) {
            log.error("短信发送未知异常 - 手机号：{}", phoneNumber, e);
        }
    }

    public Map<String, Object> sendWithTemplate(String phoneNumber, String[] params, String overrideTemplateId) {
        if (!enabled || smsClient == null) {
            log.warn("短信功能未启用或客户端未初始化，跳过发送");
            return Map.of("success", false, "message", "短信功能未启用");
        }
        if (StringUtils.isBlank(phoneNumber)) {
            log.warn("手机号为空，跳过发送");
            return Map.of("success", false, "message", "手机号为空");
        }
        try {
            SendSmsRequest req = new SendSmsRequest();
            req.setSmsSdkAppId(smsSdkAppId);
            req.setSignName(signName);
            req.setTemplateId(overrideTemplateId);
            req.setPhoneNumberSet(new String[]{phoneNumber});
            if (params != null && params.length > 0) req.setTemplateParamSet(params);
            req.setSessionContext("");
            req.setExtendCode("");
            req.setSenderId("");
            SendSmsResponse res = smsClient.SendSms(req);
            String code = res.getSendStatusSet()[0].getCode();
            String message = res.getSendStatusSet()[0].getMessage();
            log.info("短信发送 - 手机：{}，模板：{}，状态：{}，描述：{}", phoneNumber, overrideTemplateId, code, message);
            if ("Ok".equals(code)) {
                return Map.of("success", true, "message", "发送成功");
            } else {
                log.error("短信发送失败 - 手机：{}，状态码：{}，描述：{}", phoneNumber, code, message);
                return Map.of("success", false, "message", code + ": " + message);
            }
        } catch (TencentCloudSDKException e) {
            log.error("短信发送异常 - 手机：{}，错误码：{}", phoneNumber, e.getErrorCode(), e);
            return Map.of("success", false, "message", e.getErrorCode() + ": " + e.getMessage());
        } catch (Exception e) {
            log.error("短信发送未知异常 - 手机：{}", phoneNumber, e);
            return Map.of("success", false, "message", "未知错误: " + e.getMessage());
        }
    }

    /**
     * 批量发送短信
     *
     * @param phoneNumbers 手机号列表（E.164 格式，最多 200 个）
     * @param params       模板参数
     */
    public void sendBatch(String[] phoneNumbers, String[] params) {
        if (!enabled || smsClient == null) {
            log.warn("短信功能未启用或客户端未初始化，跳过发送");
            return;
        }
        if (phoneNumbers == null || phoneNumbers.length == 0) {
            log.warn("手机号列表为空，跳过发送");
            return;
        }

        try {
            SendSmsRequest req = new SendSmsRequest();
            req.setSmsSdkAppId(smsSdkAppId);
            req.setSignName(signName);
            req.setTemplateId(templateId);
            req.setPhoneNumberSet(phoneNumbers);
            if (params != null && params.length > 0) {
                req.setTemplateParamSet(params);
            }
            req.setSessionContext("");
            req.setExtendCode("");
            req.setSenderId("");

            SendSmsResponse res = smsClient.SendSms(req);
            String requestId = res.getRequestId();
            log.info("批量短信发送完成 - 请求ID：{}，发送数量：{}", requestId, phoneNumbers.length);

            for (var status : res.getSendStatusSet()) {
                if (!"Ok".equals(status.getCode())) {
                    log.warn("部分短信发送失败 - 手机号：{}，状态码：{}，描述：{}", status.getPhoneNumber(), status.getCode(), status.getMessage());
                }
            }
        } catch (TencentCloudSDKException e) {
            log.error("批量短信发送异常 - 错误码：{}，错误信息：{}", e.getErrorCode(), e.getMessage(), e);
        } catch (Exception e) {
            log.error("批量短信发送未知异常", e);
        }
    }
}
