// /formula-runtime/functions/sum.ts
import { normalizeFormulaNumber } from '@/components/business/crm-formula/utils';
export default function SUM(ctx, ...args) {
    let total = 0;
    args.forEach((v) => {
        if (Array.isArray(v)) {
            total += SUM(ctx, ...v);
        }
        else if (typeof v === 'number') {
            total += v;
        }
    });
    return normalizeFormulaNumber(total);
}
//# sourceMappingURL=sum.js.map