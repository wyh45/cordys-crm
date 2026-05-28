import { GetOperationLogDetailUrl, LoginLogListUrl, OperationLogListUrl } from '@lib/shared/api/requrls/system/log';
import CDR from '@/api/http/index';
// 登录日志
export function loginLogList(data) {
    return CDR.post({ url: LoginLogListUrl, data });
}
// 操作日志
export function operationLogList(data) {
    return CDR.post({ url: OperationLogListUrl, data });
}
// 操作日志-详情
export function operationLogDetail(id) {
    return CDR.get({ url: `${GetOperationLogDetailUrl}/${id}` });
}
//# sourceMappingURL=log.js.map