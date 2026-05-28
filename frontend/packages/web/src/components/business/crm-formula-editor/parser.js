export default function parseTokensToAST(tokens) {
    const meaningfulTokens = tokens;
    let index = 0;
    const peek = () => meaningfulTokens[index];
    const consume = () => meaningfulTokens[index++];
    function parsePrimary() {
        const token = peek();
        if (!token) {
            return {
                type: 'empty',
                startTokenIndex: index,
                endTokenIndex: index,
            };
        }
        if (token.type === 'string') {
            const t = consume();
            return {
                type: 'literal',
                value: t.value,
                valueType: 'string',
                startTokenIndex: index - 1,
                endTokenIndex: index - 1,
            };
        }
        if (token.type === 'number') {
            const t = consume();
            return {
                type: 'literal',
                value: t.value,
                valueType: 'number',
                startTokenIndex: index - 1,
                endTokenIndex: index - 1,
            };
        }
        if (token.type === 'boolean') {
            const t = consume();
            return {
                type: 'literal',
                value: t.value,
                valueType: 'boolean',
                startTokenIndex: index - 1,
                endTokenIndex: index - 1,
            };
        }
        if (token.type === 'field') {
            const t = consume();
            return {
                type: 'field',
                fieldId: t.fieldId,
                name: t.name,
                fieldType: t.fieldType,
                startTokenIndex: index - 1,
                endTokenIndex: index - 1,
            };
        }
        if (token.type === 'function') {
            // eslint-disable-next-line no-use-before-define
            return parseFunctionCall();
        }
        if (token.type === 'paren' && token.value === '(') {
            const startIndex = index;
            consume(); // '('
            // eslint-disable-next-line no-use-before-define
            const expr = parseComparison();
            const close = peek();
            if (close?.type === 'paren' && close.value === ')') {
                consume();
            }
            expr.startTokenIndex = startIndex;
            expr.endTokenIndex = index - 1;
            expr.parenthesized = true;
            return expr;
        }
        consume();
        return {
            type: 'empty',
            startTokenIndex: index - 1,
            endTokenIndex: index - 1,
        };
    }
    function parseMultiplicative() {
        let node = parsePrimary();
        let next = peek();
        while (next?.type === 'operator' && (next.value === '*' || next.value === '/')) {
            const op = consume();
            const right = parsePrimary();
            node = {
                type: 'binary',
                operator: op.value,
                left: node,
                right,
                startTokenIndex: node.startTokenIndex,
                endTokenIndex: right.endTokenIndex,
            };
            next = peek();
        }
        return node;
    }
    function parseAdditive() {
        let node = parseMultiplicative();
        let next = peek();
        while (next?.type === 'operator' && (next.value === '+' || next.value === '-')) {
            const op = consume();
            const right = parseMultiplicative();
            node = {
                type: 'binary',
                operator: op.value,
                left: node,
                right,
                startTokenIndex: node.startTokenIndex,
                endTokenIndex: right.endTokenIndex,
            };
            next = peek();
        }
        return node;
    }
    function parseFunctionCall() {
        const fnToken = consume();
        const startIndex = index - 1;
        const args = [];
        const next = peek();
        if (next?.type === 'paren' && next.value === '(') {
            consume(); // '('
            let current = peek();
            while (current && !(current.type === 'paren' && current.value === ')')) {
                if (current.type === 'comma') {
                    args.push({
                        type: 'empty',
                        startTokenIndex: index,
                        endTokenIndex: index,
                    });
                    consume();
                }
                else {
                    // eslint-disable-next-line no-use-before-define
                    const expr = parseComparison();
                    args.push(expr);
                    const after = peek();
                    if (after?.type === 'comma') {
                        consume();
                    }
                }
                current = peek();
            }
            if (peek()?.type === 'paren') {
                consume();
            }
        }
        return {
            type: 'function',
            name: fnToken.name,
            args,
            startTokenIndex: startIndex,
            endTokenIndex: index - 1,
        };
    }
    // 2) 新增 compare 判定
    function isCompareOperator(token) {
        return (token?.type === 'operator' &&
            (token.value === '=' ||
                token.value === '<>' ||
                token.value === '>' ||
                token.value === '>=' ||
                token.value === '<' ||
                token.value === '<='));
    }
    // 3) 新增 parseComparison（放在 parseAdditive 后面）
    function parseComparison() {
        let node = parseAdditive();
        let next = peek();
        while (isCompareOperator(next)) {
            const op = consume();
            const right = parseAdditive();
            node = {
                type: 'compare',
                operator: op.value,
                left: node,
                right,
                startTokenIndex: node.startTokenIndex,
                endTokenIndex: right.endTokenIndex,
            };
            next = peek();
        }
        return node;
    }
    const astList = [];
    while (index < meaningfulTokens.length) {
        astList.push(parseComparison());
    }
    // todo xinxinwu debugger
    // console.log(astList, 'astList');
    return astList;
}
//# sourceMappingURL=parser.js.map