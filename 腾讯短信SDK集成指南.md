# 腾讯云短信 SDK 集成指南

> 本文档用于指导在 CordysCRM 项目中集成腾讯云短信服务，供 Agent 开发二次开发参考。
> SDK 文档地址：https://cloud.tencent.com/document/product/382/43194
> API 文档地址：https://cloud.tencent.com/document/product/382/52077

---

## 一、前置条件

### 1.1 腾讯云账号准备

| 项目 | 说明 |
|------|------|
| 开通短信服务 | 腾讯云控制台 → 短信 → 国内短信 |
| 创建签名 | 短信控制台 → 签名管理（个人认证不支持，需企业认证） |
| 创建正文模板 | 短信控制台 → 正文模板管理 |
| 购买套餐包 | 国内短信需购买套餐包才能发送 |
| 获取密钥 | API 密钥管理：https://console.cloud.tencent.com/cam/capi |

### 1.2 凭证安全

**强烈建议通过环境变量管理密钥，不要硬编码：**

```bash
export TENCENTCLOUD_SECRET_ID="your-secret-id"
export TENCENTCLOUD_SECRET_KEY="your-secret-key"
```

### 1.3 必要参数清单

| 参数名 | 说明 | 示例 |
|--------|------|------|
| SecretId | 密钥 ID | 从环境变量获取 |
| SecretKey | 密钥 Key | 从环境变量获取 |
| SmsSdkAppId | 短信应用 ID | 1400006666（控制台查看） |
| SignName | 短信签名 | 腾讯云（需审核通过） |
| TemplateId | 模板 ID | 449739（需审核通过） |
| PhoneNumberSet | 手机号列表 | +8613711112222（E.164格式） |
| TemplateParamSet | 模板参数 | {"1234"}（与模板变量对应） |

---

## 二、Maven 依赖

```xml
<dependency>
    <groupId>com.tencentcloudapi</groupId>
    <artifactId>tencentcloud-sdk-java-tencentcloud</artifactId>
    <version>3.1.764</version>
</dependency>
```

> 地址：https://github.com/TencentCloud/tencentcloud-sdk-java

---

## 三、核心类说明

| 类名 | 包路径 | 说明 |
|------|--------|------|
| Credential | com.tencentcloudapi.common | 认证对象，封装 SecretId/SecretKey |
| HttpProfile | com.tencentcloudapi.common.profile | HTTP 配置（超时、代理、接入地域） |
| ClientProfile | com.tencentcloudapi.common.profile | 客户端配置（签名方式、HTTP 配置） |
| SmsClient | com.tencentcloudapi.sms.v20210111 | 短信客户端 |
| SendSmsRequest | com.tencentcloudapi.sms.v20210111.models | 发送短信请求 |
| SendSmsResponse | com.tencentcloudapi.sms.v20210111.models | 发送短信响应 |

---

## 四、发送短信

### 4.1 完整代码

```java
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import com.tencentcloudapi.sms.v20210111.SmsClient;
import com.tencentcloudapi.sms.v20210111.models.SendSmsRequest;
import com.tencentcloudapi.sms.v20210111.models.SendSmsResponse;

public class SendSms {

    public static void main(String[] args) {
        try {
            // 1. 实例化认证对象（从环境变量获取）
            Credential cred = new Credential(
                System.getenv("TENCENTCLOUD_SECRET_ID"),
                System.getenv("TENCENTCLOUD_SECRET_KEY")
            );

            // 2. 配置 HTTP 选项
            HttpProfile httpProfile = new HttpProfile();
            httpProfile.setReqMethod("GET");           // GET 请求（默认 POST）
            httpProfile.setConnTimeout(10);            // 连接超时 10 秒（默认 60）
            httpProfile.setWriteTimeout(10);           // 写入超时 10 秒（默认 0）
            httpProfile.setReadTimeout(10);             // 读取超时 10 秒（默认 0）
            httpProfile.setEndpoint("sms.tencentcloudapi.com");  // 接入地域

            // 3. 配置客户端选项
            ClientProfile clientProfile = new ClientProfile();
            clientProfile.setSignMethod("HmacSHA256");  // SDK 默认签名方式
            clientProfile.setHttpProfile(httpProfile);

            // 4. 实例化 SMS 客户端（地域：ap-guangzhou）
            SmsClient client = new SmsClient(cred, "ap-guangzhou", clientProfile);

            // 5. 构建请求
            SendSmsRequest req = new SendSmsRequest();

            // 短信应用 ID（控制台查看）
            req.setSmsSdkAppId("1400009099");

            // 短信签名（需审核通过）
            req.setSignName("腾讯云");

            // 模板 ID（需审核通过）
            req.setTemplateId("449739");

            // 模板参数（与模板变量个数对应，无参数则不设置）
            String[] templateParamSet = {"1234"};
            req.setTemplateParamSet(templateParamSet);

            // 手机号列表（E.164 格式，最多 200 个）
            String[] phoneNumberSet = {
                "+8621212313123",
                "+8612345678902"
            };
            req.setPhoneNumberSet(phoneNumberSet);

            // 用户 session（可选，原样返回）
            req.setSessionContext("");

            // 短信码号扩展号（国内短信无需填写）
            req.setExtendCode("");

            // SenderId（国内短信无需填写）
            req.setSenderId("");

            // 6. 发送请求
            SendSmsResponse res = client.SendSms(req);

            // 7. 输出结果
            System.out.println(SendSmsResponse.toJsonString(res));

        } catch (TencentCloudSDKException e) {
            e.printStackTrace();
        }
    }
}
```

### 4.2 请求参数速查

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| SmsSdkAppId | String | 是 | 短信应用 ID |
| SignName | String | 是 | 短信签名（已审核） |
| TemplateId | String | 是 | 模板 ID（已审核） |
| TemplateParamSet | String[] | 否 | 模板参数 |
| PhoneNumberSet | String[] | 是 | 手机号（E.164格式） |
| SessionContext | String | 否 | 用户 session，原样返回 |
| ExtendCode | String | 否 | 码号扩展号 |
| SenderId | String | 否 | SenderId（国际/港澳台） |

### 4.3 响应示例

```json
{
  "RequestId": "a3a7d34e-5s3e-4f1e-9e1f-5s3e4f1e9e1f",
  "SendStatusSet": [
    {
      "Code": "Ok",
      "Message": "send success",
      "Country": "86",
      "PhoneNumber": "+8613711112222",
      "SerialNo": "1234:56789012345678901234567890123456"
    }
  ]
}
```

### 4.4 常见错误码

| 错误码 | 说明 | 解决方案 |
|--------|------|----------|
| FailedOperation.SignatureIncorrectOrUnapproved | 签名未审核或被驳回 | 在控制台检查签名状态 |
| FailedOperation.TemplateIncorrectOrUnapproved | 模板未审核或被驳回 | 在控制台检查模板状态 |
| UnauthorizedOperation.SmsSdkAppIdVerifyFail | SmsSdkAppId 验证失败 | 检查 SmsSdkAppId 是否正确 |
| UnsupportedOperation.ContainDomesticAndInternationalPhoneNumber | 混合了国内和国际手机号 | 国内短信和国际短信分开发送 |

---

## 五、拉取回执状态

> 需要联系腾讯云短信小助手开通此功能

```java
import com.tencentcloudapi.sms.v20210111.SmsClient;
import com.tencentcloudapi.sms.v20210111.models.PullSmsSendStatusRequest;
import com.tencentcloudapi.sms.v20210111.models.PullSmsSendStatusResponse;

// 实例化请求
PullSmsSendStatusRequest req = new PullSmsSendStatusRequest();
req.setSmsSdkAppId("1400009099");
req.setLimit(5L);  // 最多 100 条

// 发送请求
PullSmsSendStatusResponse res = client.PullSmsSendStatus(req);
System.out.println(PullSmsSendStatusResponse.toJsonString(res));
```

---

## 六、统计短信发送数据

```java
import com.tencentcloudapi.sms.v20210111.SmsClient;
import com.tencentcloudapi.sms.v20210111.models.SendStatusStatisticsRequest;
import com.tencentcloudapi.sms.v20210111.models.SendStatusStatisticsResponse;

// 实例化请求
SendStatusStatisticsRequest req = new SendStatusStatisticsRequest();
req.setSmsSdkAppId("1400009099");
req.setLimit(5L);
req.setOffset(0L);
req.setBeginTime("2019071100");  // 格式：yyyymmddhh
req.setEndTime("2019071123");    // EndTime > BeginTime

// 发送请求
SendStatusStatisticsResponse res = client.SendStatusStatistics(req);
System.out.println(SendStatusStatisticsResponse.toJsonString(res));
```

---

## 七、接入地域

| 地域 | 域名 |
|------|------|
| 就近接入（默认） | sms.tencentcloudapi.com |
| 广州 | sms.ap-guangzhou.tencentcloudapi.com |
| 上海 | sms.ap-shanghai.tencentcloudapi.com |
| 北京 | sms.ap-beijing.tencentcloudapi.com |

---

## 八、在 CordysCRM 中的集成建议

### 8.1 建议的配置文件

在 `cordys-crm.properties` 中添加：

```properties
# 腾讯云短信配置
tencentcloud.sms.enabled=true
tencentcloud.sms.secret-id=${TENCENTCLOUD_SECRET_ID:}
tencentcloud.sms.secret-key=${TENCENTCLOUD_SECRET_KEY:}
tencentcloud.sms.sms-sdk-app-id=1400009099
tencentcloud.sms.sign-name=CordysCRM
tencentcloud.sms.template-id=449739
tencentcloud.sms.region=ap-guangzhou
```

### 8.2 建议的 Service 层

```java
@Service
public class SmsService {

    @Value("${tencentcloud.sms.secret-id}")
    private String secretId;

    @Value("${tencentcloud.sms.secret-key}")
    private String secretKey;

    @Value("${tencentcloud.sms.sms-sdk-app-id}")
    private String smsSdkAppId;

    @Value("${tencentcloud.sms.sign-name}")
    private String signName;

    @Value("${tencentcloud.sms.template-id}")
    private String templateId;

    public void sendVerificationCode(String phoneNumber, String code) {
        // 构建 SmsClient
        Credential cred = new Credential(secretId, secretKey);
        HttpProfile httpProfile = new HttpProfile();
        httpProfile.setEndpoint("sms.tencentcloudapi.com");
        ClientProfile clientProfile = new ClientProfile();
        clientProfile.setHttpProfile(httpProfile);
        SmsClient client = new SmsClient(cred, "ap-guangzhou", clientProfile);

        // 构建请求
        SendSmsRequest req = new SendSmsRequest();
        req.setSmsSdkAppId(smsSdkAppId);
        req.setSignName(signName);
        req.setTemplateId(templateId);
        req.setTemplateParamSet(new String[]{code});
        req.setPhoneNumberSet(new String[]{phoneNumber});

        // 发送
        client.SendSms(req);
    }
}
```

### 8.3 短信使用场景

| 场景 | 模板变量示例 |
|------|-------------|
| 验证码 | {1} 为验证码 |
| 登录通知 | {1} 为验证码，{2} 为有效期 |
| 订单通知 | {1} 为订单号，{2} 为状态 |
| 营销短信 | {1} 为用户姓名，{2} 为优惠内容 |

---

## 九、相关链接

| 资源 | 地址 |
|------|------|
| SDK 下载 | https://cloud.tencent.com/document/product/382/43193 |
| Java SDK 源码 | https://github.com/TencentCloud/tencentcloud-sdk-java |
| API 文档 | https://cloud.tencent.com/document/product/382/52077 |
| 短信控制台 | https://console.cloud.tencent.com/smsv2 |
| 国内短信快速入门 | https://cloud.tencent.com/document/product/382/37745 |
| 签名管理 | https://cloud.tencent.com/document/product/382/37794 |
| 模板管理 | https://cloud.tencent.com/document/product/382/37795 |
