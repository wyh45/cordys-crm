import { toBoolean } from '../runtime/excel-runtime';
export default function AND(ctx, ...args) {
    return args.every((fn) => toBoolean(fn()));
}
//# sourceMappingURL=and.js.map