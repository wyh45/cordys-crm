const modules = import.meta.glob('./modules/*.ts', { eager: true });
function formatModules(_modules, result) {
    Object.keys(_modules).forEach((key) => {
        const defaultModule = _modules[key].default;
        if (!defaultModule)
            return;
        const moduleList = Array.isArray(defaultModule) ? [...defaultModule] : [defaultModule];
        result.push(...moduleList);
    });
    return result;
}
const appRoutes = formatModules(modules, []);
export default appRoutes;
//# sourceMappingURL=index.js.map