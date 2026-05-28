import pc from './json/pc.json';
import pcC from './json/pc-code.json';
import pca from './json/pca.json';
import pcaC from './json/pca-code.json';
const formatData = (data) => {
    return data.map((item) => {
        const children = item.children ? formatData(item.children) : undefined;
        return {
            value: item.code,
            label: item.name,
            children,
        };
    });
};
const provinceAndCityData = formatData(pcC);
const regionData = formatData(pcaC);
// code转汉字大对象
const codeToText = {};
pcaC.forEach((province) => {
    codeToText[province.code] = province.name;
    province.children.forEach((city) => {
        codeToText[city.code] = city.name;
        city.children?.forEach((area) => {
            codeToText[area.code] = area.name;
        });
    });
});
const pcTextArr = Object.entries(pc).map(([province, cities]) => ({
    label: province,
    value: province,
    children: cities.map((city) => ({
        label: city,
        value: city,
    })),
}));
const pcaTextArr = Object.entries(pca).map(([province, cities]) => ({
    label: province,
    value: province,
    children: Object.entries(cities).map(([city, areas]) => ({
        label: city,
        value: city,
        children: areas.map((area) => ({
            label: area,
            value: area,
        })),
    })),
}));
export { codeToText, pcaTextArr, pcTextArr, provinceAndCityData, regionData };
//# sourceMappingURL=utils.js.map