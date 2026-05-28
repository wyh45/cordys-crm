export class FunctionRegistry {
    functions = new Map();
    normalizeName(name) {
        return (name || '').trim().toUpperCase();
    }
    register(name, spec) {
        const normalizedName = this.normalizeName(name);
        if (!normalizedName) {
            throw new Error('[FunctionRegistry] function name is required');
        }
        this.functions.set(normalizedName, spec);
    }
    batchRegister(definitions) {
        Object.entries(definitions).forEach(([name, spec]) => {
            this.register(name, spec);
        });
    }
    get(name) {
        return this.functions.get(this.normalizeName(name));
    }
    has(name) {
        return this.functions.has(this.normalizeName(name));
    }
    unregister(name) {
        this.functions.delete(this.normalizeName(name));
    }
    clear() {
        this.functions.clear();
    }
    keys() {
        return Array.from(this.functions.keys());
    }
    entries() {
        return Array.from(this.functions.entries());
    }
}
export const functionRegistry = new FunctionRegistry();
//# sourceMappingURL=function-registry.js.map