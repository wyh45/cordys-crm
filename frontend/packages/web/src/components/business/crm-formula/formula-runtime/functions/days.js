export default function DAYS(ctx, end, start) {
    if (!Number.isFinite(end) || !Number.isFinite(start))
        return 0;
    return Math.floor(end) - Math.floor(start);
}
//# sourceMappingURL=days.js.map