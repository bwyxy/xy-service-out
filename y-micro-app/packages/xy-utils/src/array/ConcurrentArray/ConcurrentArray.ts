/**
 * 处理数组操作
 * */
export class ConcurrentArray<T extends { id: string | number }> {
    private array: T[] = [];
    private operationQueue: Array<() => Promise<void>> = [];
    private isProcessing = false;

    // 添加元素（排队执行）
    async add(item: T): Promise<void> {
        return new Promise((resolve) => {
            const operation = async () => {
                // 检查是否已被删除（针对正在添加中的元素）
                const existingIndex = this.array.findIndex(i => i.id === item.id);
                if (existingIndex === -1) {
                    this.array.push(item);
                }
            };

            this.enqueueOperation(operation).then(resolve);
        });
    }

    // 批量添加
    async addMultiple(items: T[]): Promise<void> {
        return new Promise((resolve) => {
            const operation = async () => {
                for (const item of items) {
                    const existingIndex = this.array.findIndex(i => i.id === item.id);
                    if (existingIndex === -1) {
                        this.array.push(item);
                    }
                }
            };

            this.enqueueOperation(operation).then(resolve);
        });
    }

    // 删除元素
    async removeById(id: string | number): Promise<boolean> {
        return new Promise((resolve) => {
            const operation = async () => {
                const initialLength = this.array.length;
                this.array = this.array.filter(item => item.id !== id);
                const removed = initialLength !== this.array.length;
                resolve(removed);
            };

            this.enqueueOperation(operation);
        });
    }

    // 排队执行操作
    private async enqueueOperation(operation: () => Promise<void>): Promise<void> {
        return new Promise((resolve) => {
            this.operationQueue.push(async () => {
                await operation();
                resolve();
            });

            if (!this.isProcessing) {
                this.processQueue();
            }
        });
    }

    // 处理队列
    private async processQueue(): Promise<void> {
        if (this.isProcessing) return;

        this.isProcessing = true;

        while (this.operationQueue.length > 0) {
            const operation = this.operationQueue.shift();
            if (operation) {
                try {
                    await operation();
                } catch (error) {
                    console.error('Operation failed:', error);
                }
            }
        }

        this.isProcessing = false;
    }

    // 获取当前快照
    getSnapshot(): T[] {
        return [...this.array];
    }
}
