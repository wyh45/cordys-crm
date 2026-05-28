import { localDateToExcelSerial } from '../runtime/excel-date';
export default function NOW(ctx) {
    if (!ctx.evaluationNow)
        return;
    return localDateToExcelSerial(ctx.evaluationNow);
}
//# sourceMappingURL=now.js.map