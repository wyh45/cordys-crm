// astToIr.ts Ast--->IR
import { IRNodeType } from '@lib/shared/enums/formula';
function wrapColumnIfNeeded(node) {
    // 已经是标量，直接返回
    if (node.type !== IRNodeType.Field)
        return node;
    // 不是子表字段
    if (!node.fieldId.includes('.'))
        return node;
    // SUM(column)
    return {
        type: IRNodeType.Function,
        name: 'SUM',
        args: [node],
    };
}
function resolveNode(node, ctx) {
    switch (node.type) {
        case IRNodeType.Literal:
            return {
                type: IRNodeType.Literal,
                value: node.value,
                valueType: node.valueType,
            };
        case IRNodeType.Field: {
            const ir = { ...node };
            return ctx.expectScalar ? wrapColumnIfNeeded(ir) : ir;
        }
        case IRNodeType.Binary:
            return {
                type: IRNodeType.Binary,
                operator: node.operator,
                left: resolveNode(node.left, { expectScalar: true }),
                right: resolveNode(node.right, { expectScalar: true }),
            };
        case IRNodeType.Compare:
            return {
                type: IRNodeType.Compare,
                operator: node.operator, // '=' | '!=' | '>' | '>=' | '<' | '<='
                left: resolveNode(node.left, { expectScalar: true }),
                right: resolveNode(node.right, { expectScalar: true }),
            };
        case IRNodeType.Function:
            // eslint-disable-next-line no-use-before-define
            return resolveFunction(node, ctx);
        default:
            return {
                type: IRNodeType.Invalid,
                reason: `unknown node type`,
            };
    }
}
function resolveFunction(node, _ctx) {
    const name = node.name?.toUpperCase();
    if (!name) {
        return {
            type: IRNodeType.Invalid,
            reason: 'function name missing',
        };
    }
    /**
     * 特殊函数规则
     */
    switch (name) {
        case 'SUM':
            return {
                type: IRNodeType.Function,
                name,
                args: node.args.map((arg) => resolveNode(arg, { expectScalar: false })),
            };
        case 'DAYS':
            if (node.args.length !== 2) {
                return {
                    type: IRNodeType.Invalid,
                    reason: 'DAYS requires 2 arguments',
                };
            }
            return {
                type: IRNodeType.Function,
                name,
                args: node.args.map((arg) => resolveNode(arg, { expectScalar: true })),
            };
        default:
            /**
             * 默认函数行为
             *
             * IF / IFS / AND / CONCATENATE / TODAY / NOW / TEXT 等
             */
            return {
                type: IRNodeType.Function,
                name,
                args: node.args.map((arg) => resolveNode(arg, { expectScalar: true })),
            };
    }
}
// 解析 AST -> IR,用于运行执行器计算
export default function resolveASTToIR(ast) {
    if (!ast)
        return null;
    return resolveNode(ast, { expectScalar: true });
}
//# sourceMappingURL=astToIr.js.map