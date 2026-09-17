import { defineStore } from 'pinia'
import type { RouteLocationNormalized } from 'vue-router'

export interface TagView {
  name: string
  title: string
  path: string
  query?: Record<string, any>
  fullPath: string
  meta?: Record<string, any>
}

const useTagsViewStore = defineStore('tagsView', {
  state: () => ({
    visitedViews: [] as TagView[],
    cachedViews: [] as string[]
  }),

  actions: {
    addView(view: RouteLocationNormalized) {
      this.addVisitedView(view)
      this.addCachedView(view)
    },

    addVisitedView(view: RouteLocationNormalized) {
      if (view.meta?.hidden || view.meta?.noTagsView) return
      if (this.visitedViews.some((v) => v.path === view.path)) return

      this.visitedViews.push({
        name: view.name as string,
        title: (view.meta?.title as string) || 'no-name',
        path: view.path,
        query: view.query,
        fullPath: view.fullPath,
        meta: view.meta
      })
    },

    addCachedView(view: RouteLocationNormalized) {
      const name = view.name as string
      if (!name) return
      if (this.cachedViews.includes(name)) return
      if (view.meta?.noCache) return
      this.cachedViews.push(name)
    },

    delView(view: TagView) {
      this.delVisitedView(view)
      this.delCachedView(view)
    },

    delVisitedView(view: TagView) {
      for (const [i, v] of this.visitedViews.entries()) {
        if (v.path === view.path) {
          this.visitedViews.splice(i, 1)
          break
        }
      }
    },

    delCachedView(view: TagView) {
      const name = view.name
      if (!name) return
      const index = this.cachedViews.indexOf(name)
      if (index > -1) {
        this.cachedViews.splice(index, 1)
      }
    },

    delOthersViews(view: TagView) {
      this.delOthersVisitedViews(view)
      this.delOthersCachedViews(view)
    },

    delOthersVisitedViews(view: TagView) {
      this.visitedViews = this.visitedViews.filter((v) => {
        return v.meta?.affix || v.path === view.path
      })
    },

    delOthersCachedViews(view: TagView) {
      const name = view.name
      if (!name) return
      this.cachedViews = this.cachedViews.filter((v) => {
        return v === name
      })
    },

    delAllViews() {
      this.delAllVisitedViews()
      this.delAllCachedViews()
    },

    delAllVisitedViews() {
      const affixTags = this.visitedViews.filter((v) => v.meta?.affix)
      this.visitedViews = affixTags
    },

    delAllCachedViews() {
      this.cachedViews = []
    },

    updateVisitedView(view: RouteLocationNormalized) {
      for (let v of this.visitedViews) {
        if (v.path === view.path) {
          v = Object.assign(v, view)
          break
        }
      }
    }
  }
})

export default useTagsViewStore
