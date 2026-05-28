import permission from './permission';
import validateExpiration from './validateExpiration';
export default {
    install(Vue) {
        Vue.directive('permission', permission);
        Vue.directive('expire', validateExpiration);
    },
};
//# sourceMappingURL=index.js.map