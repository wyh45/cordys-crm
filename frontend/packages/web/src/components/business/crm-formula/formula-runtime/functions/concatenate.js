export default function CONCATENATE(ctx, ...args) {
    if (!args || args.length === 0)
        return '';
    const strVal = args.reduce((result, arg) => {
        // null / undefined → 跳过处理
        if (arg == null) {
            return result;
        }
        //  子表数组支持
        if (Array.isArray(arg)) {
            const arrayResult = arg.reduce((subResult, item) => {
                if (item != null) {
                    return subResult + String(item);
                }
                return subResult;
            }, '');
            return result + arrayResult;
        }
        //  普通值
        return result + String(arg);
    }, '');
    return strVal;
}
//# sourceMappingURL=concatenate.js.map