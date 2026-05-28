import useLicenseStore from '@/store/modules/setting/license';
/**
 * 权限指令,TODO:校验license
 * @param el dom 节点
 */
function checkHasLicenseExpiration(el) {
    const licenseStore = useLicenseStore();
    const isValid = licenseStore.expiredDuring;
    if (!isValid && el.parentNode) {
        el.parentNode.removeChild(el);
    }
}
export default {
    mounted(el) {
        checkHasLicenseExpiration(el);
    },
    updated(el) {
        checkHasLicenseExpiration(el);
    },
};
//# sourceMappingURL=index.js.map