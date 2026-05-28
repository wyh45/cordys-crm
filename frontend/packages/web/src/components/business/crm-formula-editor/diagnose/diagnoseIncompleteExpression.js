import { useI18n } from '@lib/shared/hooks/useI18n';
import { FormulaErrorCode } from '../config';
const { t } = useI18n();
/**
 *
 * @param node ast节点
 * @returns 诊断错误信息列表
 */
export default function diagnoseIncompleteExpression(node) {
    const diagnostics = [];
    // 函数参数表达式不完整
    if (node.type === 'function') {
        const fnNode = node;
        fnNode.args.forEach((arg) => {
            if (arg.type === 'empty') {
                diagnostics.push({
                    type: 'error',
                    code: FormulaErrorCode.INCOMPLETE_EXPRESSION,
                    message: t('formulaEditor.diagnostics.expressionNotComplete'),
                    highlight: {
                        tokenRange: [fnNode.startTokenIndex, fnNode.endTokenIndex],
                    },
                });
            }
        });
        // function 已处理完，不再往下走
        return diagnostics;
    }
    //  二元表达式不完整
    if (node.type === 'binary') {
        const binNode = node;
        if (!binNode.left || binNode.left.type === 'empty') {
            diagnostics.push({
                type: 'error',
                code: FormulaErrorCode.INCOMPLETE_EXPRESSION,
                message: t('formulaEditor.diagnostics.operatorLeftMissingExpression'),
                highlight: {
                    tokenRange: [binNode.startTokenIndex, binNode.endTokenIndex],
                },
            });
        }
        if (!binNode.right || binNode.right.type === 'empty') {
            diagnostics.push({
                type: 'error',
                code: FormulaErrorCode.INCOMPLETE_EXPRESSION,
                message: t('formulaEditor.diagnostics.operatorRightMissingExpression'),
                highlight: {
                    tokenRange: [binNode.startTokenIndex, binNode.endTokenIndex],
                },
            });
        }
    }
    return diagnostics;
}
//# sourceMappingURL=diagnoseIncompleteExpression.js.map