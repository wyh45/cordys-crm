import { toBoolean } from '../runtime/excel-runtime';
export default function IF(ctx, cond, trueValue, falseValue) {
    const condition = toBoolean(cond());
    if (condition) {
        return trueValue();
    }
    if (falseValue) {
        return falseValue();
    }
    return null;
}
//# sourceMappingURL=if.js.map