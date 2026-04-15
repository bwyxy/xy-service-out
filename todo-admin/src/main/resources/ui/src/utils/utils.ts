export interface RouteItem {
    path: string;
    name?: string;
    component?: string;
    hidden?: boolean;
    meta?: {
        icon?: string;
        link?: string | null;
        noCache?: boolean;
        title?: string;
    };
    children?: RouteItem[];
}

export interface MatchResult {
    menu: RouteItem;
    fullPath: string;
    titlePath: string[];
}


/**
 * 模糊搜索菜单，并返回完整路径信息
 * @param routes 路由树
 * @param keyword 搜索关键词
 * @param parentPath 父级路径（用于递归）
 * @param parentTitles 父级标题数组（用于递归）
 * @returns 匹配结果数组（包含菜单项和路径信息）
 */
export function fuzzySearchMenuWithPath(
    routes: RouteItem[],
    keyword: string,
    parentPath: string = '',
    parentTitles: string[] = []
): MatchResult[] {
    if (!keyword || keyword.trim() === '') return [];

    const result: MatchResult[] = [];
    const lowerKeyword = keyword.toLowerCase();

    for (const route of routes) {
        // 构建当前菜单的完整路径
        const currentPath = route.path === '/'
            ? parentPath || '/'
            : `${parentPath}/${route.path}`.replace(/\/+/g, '/');

        const currentTitle = route.meta?.title || route.name || route.path || '';
        const currentTitles = [...parentTitles];
        if (currentTitle) currentTitles.push(currentTitle);

        // 模糊匹配
        if (currentTitle.toLowerCase().includes(lowerKeyword)) {
            result.push({
                menu: route,
                fullPath: currentPath,
                titlePath: [...currentTitles]
            });
        }

        // 递归搜索 children
        if (route.children?.length) {
            const childMatches = fuzzySearchMenuWithPath(
                route.children,
                keyword,
                currentPath,
                currentTitles
            );
            result.push(...childMatches);
        }
    }

    return result;
}

export interface PathMatchDetail {
    menu: RouteItem;
    fullPath: string;
    pathSegments: string[];
    breadcrumbTitles: string[];
    level: number;
}

/**
 * 查找路径并返回详细信息（支持多级菜单）
 */
export function findPathDetail(
    routes: RouteItem[],
    targetPath: string
): PathMatchDetail | null {
    // 标准化目标路径
    let normalizedTarget = targetPath;
    if (!normalizedTarget.startsWith('/')) {
        normalizedTarget = '/' + normalizedTarget;
    }
    normalizedTarget = normalizedTarget.replace(/\/$/, '');

    function search(
        routes: RouteItem[],
        parentPath: string = '',
        breadcrumbTitles: string[] = []
    ): PathMatchDetail | null {
        for (const route of routes) {
            const currentPath = buildFullPath(route, parentPath);
            const normalizedCurrent = currentPath.replace(/\/$/, '');

            const currentTitles = [...breadcrumbTitles];
            if (route.meta?.title) {
                currentTitles.push(route.meta.title);
            }

            if (normalizedCurrent === normalizedTarget) {
                return {
                    menu: route,
                    fullPath: normalizedCurrent,
                    pathSegments: normalizedCurrent.split('/').filter(Boolean),
                    breadcrumbTitles: currentTitles,
                    level: currentTitles.length
                };
            }

            if (route.children?.length) {
                const found = search(route.children, normalizedCurrent, currentTitles);
                if (found) return found;
            }
        }
        return null;
    }

    return search(routes);
}

function buildFullPath(route: RouteItem, parentPath: string = ''): string {
    // 处理根路径
    if (route.path === '/') {
        return parentPath || '/';
    }

    // 处理绝对路径（以 / 开头）
    if (route.path.startsWith('/')) {
        return route.path;
    }

    // 处理相对路径
    if (parentPath === '/' || parentPath === '') {
        return `/${route.path}`;
    }

    return `${parentPath}/${route.path}`;
}
