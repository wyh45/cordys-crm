import { toBoolean } from '../runtime/excel-runtime';
export default function IFS(ctx, ...args) {
    for (let i = 0; i < args.length; i += 2) {
        const cond = args[i];
        const value = args[i + 1];
        if (!cond || !value)
            break;
        if (toBoolean(cond())) {
            return value();
        }
    }
    return null;
}
//# sourceMappingURL=ifs.js.map