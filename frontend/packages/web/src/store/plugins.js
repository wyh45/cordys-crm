import { debounce } from 'lodash-es';
// 基于lodash的防抖函数封装，读取store中cache配置的属性，针对已配置的属性更改操作进行防抖处理
export const debouncePlugin = ({ options, store }) => {
    if (options.debounce) {
        return Object.keys(options.debounce).reduce((debounceActions, action) => {
            debounceActions[action] = debounce(store[action], options.debounce[action]);
            return debounceActions;
        }, {});
    }
};
//# sourceMappingURL=plugins.js.map